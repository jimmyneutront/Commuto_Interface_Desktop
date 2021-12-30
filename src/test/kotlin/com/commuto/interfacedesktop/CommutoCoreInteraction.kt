package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.contractwrapper.CommutoERC20
import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import com.commuto.interfacedesktop.contractwrapper.CommutoTransactionManager
import com.commuto.interfacedesktop.contractwrapper.WorkingCommutoSwap
import org.web3j.codegen.SolidityFunctionWrapperGenerator
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.ChainIdLong
import org.web3j.tx.Transfer
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CommutoCoreInteraction {

    @Test
    fun testTransferEth() {
        //Restore Hardhat account #1
        val key_one = Credentials.create("59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d")

        //Restore Hardhat account #2
        val key_two = Credentials.create("5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a")

        //Establish connection to Hardhat node
        val endpoint = "http://192.168.1.12:8545"
        val web3 = Web3j.build(HttpService(endpoint))

        //Get initial balance of account #2
        val initialBalance = web3.ethGetBalance(key_two.address, DefaultBlockParameterName.LATEST).send().balance

        //Transfer 1 ETH from account #1 to account #2
        val transactionReceipt = Transfer.sendFunds(web3, key_one, key_two.address, BigDecimal.valueOf(1.0),
            Convert.Unit.ETHER).send()

        //Get final balance of account #2 and check for proper amount
        val finalBalance = web3.ethGetBalance(key_two.address, DefaultBlockParameterName.LATEST).send().balance
        //Apparently BigDecimal(0) != BigDecimal(0.0), so add 0.0 to final balance so tests pass
        assert((BigDecimal(initialBalance) + Convert.toWei(BigDecimal.valueOf(1.0), Convert.Unit.ETHER))
            .equals(BigDecimal(finalBalance) + Convert.toWei(BigDecimal.valueOf(0.0), Convert.Unit.ETHER)))
    }

    @Test
    fun testGenerateEthAccount() {
        val initialEntropy = Random.Default.nextBytes(32)
        val password = "web3j"
        val mnemonics = MnemonicUtils.generateMnemonic(initialEntropy)
        val credentials = WalletUtils.loadBip39Credentials(password, mnemonics)
        println("Public Address: " + credentials.address)
        println("Password: " + password)
        println("Mnemonics: " + mnemonics)
    }

    //TODO: DON'T COMMIT MY FILE HIERARCHY
    @Test
    fun testGenerateContractWrapper() {
        val binFile: String? = null
        val abiFile = ""
        val destinationDir = ""
        val contractName = "CommutoSwap"
        val basePackageName = "com.commuto.interfacedesktop.contractwrapper"
        val addressLength = 20
        val wrapperGenerator = SolidityFunctionWrapperGenerator(
            null,
            File(abiFile),
            File(destinationDir),
            contractName,
            basePackageName,
            true,
            false,
            addressLength
        )
        wrapperGenerator.generate()
    }

    //Setup swap direction and participant role enums for testSwapProcess()
    enum class SwapDirection {
        BUY, SELL
    }

    enum class ParticipantRole {
        MAKER, TAKER
    }

    @Test
    fun testSwapProcess() {
        //Specify swap direction and participant roles
        val direction = SwapDirection.SELL
        val role = ParticipantRole.MAKER

        //Restore Hardhat account #2
        val key_two = Credentials.create("5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a")

        //Establish connection to Hardhat node
        val endpoint = "http://192.168.0.195:8545"
        val web3 = Web3j.build(HttpService(endpoint))

        //Setup CommutoSwap contract interface
        val commutoSwapContractAddress = "0x5FC8d32690cc91D4c39d9d3abcBD16989F875707"
        class DumbGasProvider : ContractGasProvider {
            override fun getGasPrice(contractFunc: String?): BigInteger {
                return BigInteger.valueOf(875000000)
            }
            override fun getGasPrice(): BigInteger {
                return BigInteger.valueOf(875000000)
            }
            override fun getGasLimit(contractFunc: String?): BigInteger {
                return BigInteger.valueOf(30000000)
            }
            override fun getGasLimit(): BigInteger {
                return BigInteger.valueOf(30000000)
            }
        }
        val gasProvider = DumbGasProvider()

        val txManager = CommutoTransactionManager(web3, key_two, ChainIdLong.NONE)
        val commutoSwap = WorkingCommutoSwap.load(commutoSwapContractAddress, web3, txManager, gasProvider)

        //Don't parse any blocks earlier than that with this block number looking for events emitted by Commuto, because there won't be any
        var lastParsedBlockNumber: BigInteger = BigInteger.valueOf(0)//web3.ethBlockNumber().send().blockNumber

        //Start listening for OfferOpened event emission

        //Setup dummy Dai contract interface
        //TODO: Custom ERC20 wrapper with 'approval' funciton that immediately returns txId
        val dummyDaiContractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
        val dai = CommutoERC20.load(dummyDaiContractAddress, web3, txManager, gasProvider)

        //Get initial Dai balance
        val initialDaiBalance = dai.balanceOf(key_two.address).sendAsync().get()

        var functionCallResult: Pair<String, CompletableFuture<TransactionReceipt>>? = null
        var offer: CommutoSwap.Offer? = null
        var offerId: ByteArray? = null
        var swap: CommutoSwap.Swap? = null

        if (role == ParticipantRole.MAKER) {
            //Approve transfer to open offer
            functionCallResult = dai.approveAndGetTXID(commutoSwapContractAddress, BigInteger.valueOf(11))
            var isDaiApprovalTxConfirmed = false
            while (!isDaiApprovalTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isDaiApprovalTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }

            //Open swap offer
            val offerIdUUID = UUID.randomUUID()
            val offerIdByteBuffer = ByteBuffer.wrap(ByteArray(16))
            offerIdByteBuffer.putLong(offerIdUUID.mostSignificantBits)
            offerIdByteBuffer.putLong(offerIdUUID.leastSignificantBits)
            offerId = offerIdByteBuffer.array()
            var directionInt: BigInteger? = null
            if (direction == SwapDirection.BUY) {
                directionInt = BigInteger.valueOf(0)
            } else if (direction == SwapDirection.SELL) {
                directionInt = BigInteger.valueOf(1)
            }
            val settlementMethods = ArrayList<ByteArray>()
            settlementMethods.add("USD-SWIFT".toByteArray(Charsets.UTF_8))
            offer = CommutoSwap.Offer(
                true,
                false,
                key_two.address,
                "maker's interface Id here".toByteArray(Charsets.UTF_8),
                dummyDaiContractAddress,
                BigInteger.valueOf(100),
                BigInteger.valueOf(100),
                BigInteger.valueOf(10),
                directionInt!!,
                "a price here".toByteArray(Charsets.UTF_8),
                settlementMethods,
                BigInteger.valueOf(1),
                MessageDigest.getInstance("SHA-256").digest("A bunch of extra data in here".toByteArray())
            )
            functionCallResult = commutoSwap.openOfferAndGetTXID(offerId, offer)
            var isOpenOfferTxConfirmed = false
            while (!isOpenOfferTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isOpenOfferTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }

            //Wait for offer to be taken
            var isOfferTaken = false
            while(!isOfferTaken) {
                if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                    val blockToParseNumber = DefaultBlockParameter
                        .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                    val lastBlockTxns = web3.ethGetBlockByNumber(blockToParseNumber, false).send()
                        .block.transactions
                    val lastBlockTxnHashes = lastBlockTxns.map { it.get() }
                    var lastBlockTxnReceipts = ArrayList<TransactionReceipt>()
                    for (txnHash in lastBlockTxnHashes) {
                        val txReceiptOptional = web3.ethGetTransactionReceipt(txnHash as String).send()
                            .transactionReceipt
                        if (txReceiptOptional.isPresent) {
                            lastBlockTxnReceipts.add(txReceiptOptional.get())
                        }
                    }
                    for (txnReceipt in lastBlockTxnReceipts) {
                        val events = commutoSwap.getOfferTakenEvents(txnReceipt)
                        if (events.count() > 0) {
                            isOfferTaken = true
                        }
                    }
                    lastParsedBlockNumber += BigInteger.valueOf(1)
                }
                if (!isOfferTaken) {
                    Thread.sleep(1000)
                }
            }

            //Get the newly taken swap
            swap = commutoSwap.getSwap(offerId).sendAsync().get()
        } else if (role == ParticipantRole.TAKER) {
            //Listen for new offers
            /*
            Note: As of now, this will try to take the first OfferOpened event that it finds, even if the offer is closed
            or there exists more than one open offer
             */
            var foundOpenOfferEvent = false
            var interfaceId: ByteArray = ByteArray(32)
            while (!foundOpenOfferEvent) {
                if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                    val blockToParseNumber = DefaultBlockParameter
                        .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                    val lastBlockTxns = web3.ethGetBlockByNumber(blockToParseNumber, false).send()
                        .block.transactions
                    val lastBlockTxnHashes = lastBlockTxns.map { it.get() }
                    var lastBlockTxnReceipts = ArrayList<TransactionReceipt>()
                    for (txnHash in lastBlockTxnHashes) {
                        val txReceiptOptional = web3.ethGetTransactionReceipt(txnHash as String).send()
                            .transactionReceipt
                        if (txReceiptOptional.isPresent) {
                            lastBlockTxnReceipts.add(txReceiptOptional.get())
                        }
                    }
                    for (txnReceipt in lastBlockTxnReceipts) {
                        val events = commutoSwap.getOfferOpenedEvents(txnReceipt)
                        if (events.count() > 0) {
                            foundOpenOfferEvent = true
                            offerId = events.elementAt(0).offerID
                            interfaceId = events.elementAt(0).offerID
                        }
                    }
                    lastParsedBlockNumber += BigInteger.valueOf(1)
                }
                if (!foundOpenOfferEvent) {
                    Thread.sleep(1000)
                }
            }

            //Get new offer
            offer = commutoSwap.getOffer(offerId).send()

            //Create allowance to take offer
            var allowanceValue: BigInteger? = null
            if (direction == SwapDirection.BUY) {
                allowanceValue = BigInteger.valueOf(111)
            } else if (direction == SwapDirection.SELL) {
                allowanceValue = BigInteger.valueOf(11)
            }
            functionCallResult = dai.approveAndGetTXID(commutoSwapContractAddress, allowanceValue)
            var isDaiApprovalTxConfirmed = false
            while (!isDaiApprovalTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isDaiApprovalTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }

            //Create swap object and take offer
            swap = CommutoSwap.Swap(
                true,
                true,
                offer.maker, //maker
                offer.interfaceId, //makerInterfaceId
                key_two.address, //taker
                "maker's interface id here".toByteArray(), //takerInterfaceId
                offer.stablecoin,
                offer.amountLowerBound,
                offer.amountUpperBound,
                offer.securityDepositAmount,
                BigInteger.valueOf(100), //takenSwapAmount
                BigInteger.valueOf(1), //securityDepositAmount
                offer.direction,
                offer.price,
                offer.settlementMethods[0], //settlementMethod
                BigInteger.valueOf(1), //protocolVersion
                offer.extraData, //makerExtraData
                MessageDigest.getInstance("SHA-256").digest("taker's extra data in here".toByteArray()), //takerExtraData
                false,
                false,
                false,
                false
            )
            functionCallResult = commutoSwap.takeOfferAndGetTXID(offerId, swap)
            var isTakeOfferTxConfirmed = false
            while (!isTakeOfferTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isTakeOfferTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }
        }

        if (direction == SwapDirection.SELL) {
            if (role == ParticipantRole.MAKER) {
                //Create allowance to fill swap
                functionCallResult = dai.approveAndGetTXID(commutoSwapContractAddress, BigInteger.valueOf(100))
                var isDaiApprovalTxConfirmed = false
                while (!isDaiApprovalTxConfirmed) {
                    try {
                        if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                            isDaiApprovalTxConfirmed = true
                        }
                    } catch (exception: NullPointerException) {}
                }

                //Fill swap
                functionCallResult = commutoSwap.fillSwapAndGetTXID(offerId)
                var isFillSwapTxConfirmed = false
                while (!isFillSwapTxConfirmed) {
                    try {
                        if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                            isFillSwapTxConfirmed = true
                        }
                    } catch (exception: NullPointerException) {}
                }
            } else if (role == ParticipantRole.TAKER) {
                //Start listening for SwapFilled event
                /*
                Note: As of now, this will try to take the first SwapFilled event that it finds
                 */
                var foundSwapFilledEvent = false
                while (!foundSwapFilledEvent) {
                    if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                        val blockToParseNumber = DefaultBlockParameter
                            .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                        val lastBlockTxns = web3.ethGetBlockByNumber(blockToParseNumber, false).send()
                            .block.transactions
                        val lastBlockTxnHashes = lastBlockTxns.map { it.get() }
                        var lastBlockTxnReceipts = ArrayList<TransactionReceipt>()
                        for (txnHash in lastBlockTxnHashes) {
                            val txReceiptOptional = web3.ethGetTransactionReceipt(txnHash as String).send().transactionReceipt
                            if (txReceiptOptional.isPresent) {
                                lastBlockTxnReceipts.add(txReceiptOptional.get())
                            }
                        }
                        for (txnReceipt in lastBlockTxnReceipts) {
                            val events = commutoSwap.getSwapFilledEvents(txnReceipt)
                            if (events.count() > 0) {
                                foundSwapFilledEvent = true
                            }
                        }
                        lastParsedBlockNumber += BigInteger.valueOf(1)
                    }
                    if (!foundSwapFilledEvent) {
                        Thread.sleep(1000)
                    }
                }
            }
        }

        if ((direction == SwapDirection.BUY && role == ParticipantRole.MAKER) ||
            (direction == SwapDirection.SELL && role == ParticipantRole.TAKER)) {
            //Report payment sent
            functionCallResult = commutoSwap.reportPaymentSentAndGetTXID(offerId)
            var isReportPaymentSentTxConfirmed = false
            while (!isReportPaymentSentTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isReportPaymentSentTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }

            //Start listening for PaymentReceived event
            var foundPaymentReceivedEvent = false
            while (!foundPaymentReceivedEvent) {
                if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                    val blockToParseNumber = DefaultBlockParameter
                        .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                    val lastBlockTxns = web3.ethGetBlockByNumber(blockToParseNumber, false).send()
                        .block.transactions
                    val lastBlockTxnHashes = lastBlockTxns.map { it.get() }
                    var lastBlockTxnReceipts = ArrayList<TransactionReceipt>()
                    for (txnHash in lastBlockTxnHashes) {
                        val txReceiptOptional = web3.ethGetTransactionReceipt(txnHash as String).send()
                            .transactionReceipt
                        if (txReceiptOptional.isPresent) {
                            lastBlockTxnReceipts.add(txReceiptOptional.get())
                        }
                    }
                    for (txnReceipt in lastBlockTxnReceipts) {
                        val events = commutoSwap.getPaymentReceivedEvents(txnReceipt)
                        if (events.count() > 0) {
                            foundPaymentReceivedEvent = true
                        }
                    }
                    lastParsedBlockNumber += BigInteger.valueOf(1)
                }
                if (!foundPaymentReceivedEvent) {
                    Thread.sleep(1000)
                }
            }
        } else if ((direction == SwapDirection.SELL && role == ParticipantRole.MAKER) ||
            (direction == SwapDirection.BUY && role == ParticipantRole.TAKER)) {
            //Start listening for PaymentSent event
            var foundPaymentSentEvent = false
            while (!foundPaymentSentEvent) {
                if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                    val blockToParseNumber = DefaultBlockParameter
                        .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                    val lastBlockTxns = web3.ethGetBlockByNumber(blockToParseNumber, false).send()
                        .block.transactions
                    val lastBlockTxnHashes = lastBlockTxns.map { it.get() }
                    var lastBlockTxnReceipts = ArrayList<TransactionReceipt>()
                    for (txnHash in lastBlockTxnHashes) {
                        val txReceiptOptional = web3.ethGetTransactionReceipt(txnHash as String).send()
                            .transactionReceipt
                        if (txReceiptOptional.isPresent) {
                            lastBlockTxnReceipts.add(txReceiptOptional.get())
                        }
                    }
                    for (txnReceipt in lastBlockTxnReceipts) {
                        val events = commutoSwap.getPaymentSentEvents(txnReceipt)
                        if (events.count() > 0) {
                            foundPaymentSentEvent = true
                        }
                    }
                    lastParsedBlockNumber += BigInteger.valueOf(1)
                }
                if (!foundPaymentSentEvent) {
                    Thread.sleep(1000)
                }
            }

            //Report payment received
            functionCallResult = commutoSwap.reportPaymentReceivedAndGetTXID(offerId)
            var isReportReceivedTxConfirmed = false
            while (!isReportReceivedTxConfirmed) {
                try {
                    if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                        isReportReceivedTxConfirmed = true
                    }
                } catch (exception: NullPointerException) {}
            }
        }

        //call closeSwap
        functionCallResult = commutoSwap.closeSwapAndGetTXID(offerId)
        var isSwapClosedTxConfirmed = false
        while (!isSwapClosedTxConfirmed) {
            try {
                if (web3.ethGetTransactionByHash(functionCallResult.first).send().result.blockNumber != null) {
                    isSwapClosedTxConfirmed = true
                }
            } catch (exception: NullPointerException) {}
        }

        //check that balance has changed by proper amount
        val finalDaiBalance = dai.balanceOf(key_two.address).sendAsync().get()

        if ((direction == SwapDirection.BUY && role == ParticipantRole.MAKER) ||
            (direction == SwapDirection.SELL && role == ParticipantRole.TAKER)) {
            assertEquals(initialDaiBalance + BigInteger.valueOf(99), finalDaiBalance)
        } else if ((direction == SwapDirection.SELL && role == ParticipantRole.MAKER) ||
            (direction == SwapDirection.BUY && role == ParticipantRole.TAKER)) {
            assertEquals(initialDaiBalance, finalDaiBalance + BigInteger.valueOf(101))
        }
    }
}