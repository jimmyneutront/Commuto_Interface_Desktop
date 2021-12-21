package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import okhttp3.internal.wait
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.DynamicBytes
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
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.Transfer
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.tx.response.NoOpProcessor
import org.web3j.utils.Convert
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.Test

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

    @Test
    fun testSwapProcess() {
        //Restore Hardhat account #2
        val key_two = Credentials.create("5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a")

        //Establish connection to Hardhat node
        val endpoint = "http://192.168.1.12:8545"
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
        /*
        Using a noOpProcessor will immediately return an empty tx receipt with just a hash when calling .send() instead
        waiting for tx confirmation, so Commuto must handle polling for tx confirmation itself.
         */
        val noOpProcessor = NoOpProcessor(web3)
        /*
        TODO: Two different contract objects: one normal one to broadcast transactions, and another to immediately
        return a transaction hash for tracking
         */

        val txManager = RawTransactionManager(web3, key_two, ChainIdLong.NONE, noOpProcessor)
        val commutoSwap = CommutoSwap.load(commutoSwapContractAddress, web3, key_two, gasProvider)

        //Don't parse any blocks earlier than that with this block number looking for events emitted by Commuto, because there won't be any
        var lastParsedBlockNumber: BigInteger = BigInteger.valueOf(0)//web3.ethBlockNumber().send().blockNumber

        //Start listening for OfferOpened event emission

        //Setup dummy Dai contract interface
        val dummyDaiContractAddress = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
        val dai = ERC20.load(dummyDaiContractAddress, web3, key_two, gasProvider)

        //Find OfferOpened events
        /*
        Note: As of now, this will try to take the first OfferOpened event that it finds, even if the offer is closed
        or there exist more than one open offer
         */
        var foundOpenOfferEvent = false
        var offerId: ByteArray = ByteArray(32)
        var interfaceId: ByteArray = ByteArray(32)
        while (!foundOpenOfferEvent) {
            if (web3.ethBlockNumber().send().blockNumber > lastParsedBlockNumber) {
                val blockToParseNumber = DefaultBlockParameter
                    .valueOf(lastParsedBlockNumber + BigInteger.valueOf(1))
                /*
                Get transaction hashes in next block, get the transaction receipt for each hash, pick all the ones that
                interact with Commuto, and then scan each one events, and then analyze the results
                */
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
                    val events = commutoSwap.getOfferOpenedEvents(txnReceipt)
                    if (events.count() > 0) {
                        foundOpenOfferEvent = true
                        offerId = events.elementAt(0).offerID
                        interfaceId = events.elementAt(0).offerID
                    }
                }
                lastParsedBlockNumber += BigInteger.valueOf(1)
            }
        }

        //TODO: Fix issue parsing DynamicArray<DynamicBytes> in returned Offer struct
        //Note: Right now, this only works if custom JARs which support decoding DynamicArray<DynamicBytes> are used
        val offer = commutoSwap.getOffer(offerId).send()

        //Approve DAI swap
        dai.approve(commutoSwapContractAddress, BigInteger.valueOf(11)).sendAsync().get()

        //Create Swap object and take offer
        val swap = CommutoSwap.Swap(
            true,
            true,
            offer.maker,
            offer.interfaceId,
            key_two.address,
            "maker's interface id here".toByteArray(),
            offer.stablecoin,
            offer.amountLowerBound,
            offer.amountUpperBound,
            offer.securityDepositAmount,
            BigInteger.valueOf(100),
            BigInteger.valueOf(1),
            offer.direction,
            offer.price,
            offer.settlementMethods[0],
            BigInteger.valueOf(1),
            offer.extraData,
            MessageDigest.getInstance("SHA-256").digest("taker's extra data in here".toByteArray()),
            false,
            false,
            false,
            false
        )
        commutoSwap.takeOffer(offerId, swap).sendAsync().get()

        //Start listening for SwapFilled event

        //call confirmPaymentSent

        //Start listening for confirmPaymentReceived

        //call closeswap

        //check that balance has changed by proper amount
    }

}