package com.commuto.interfacedesktop

import com.commuto.interfacedesktop.contractwrapper.CommutoERC20
import com.commuto.interfacedesktop.contractwrapper.CommutoSwap
import com.commuto.interfacedesktop.contractwrapper.CommutoTransactionManager
import com.commuto.interfacedesktop.contractwrapper.WorkingCommutoSwap
import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.KMService
import com.commuto.interfacedesktop.kmService.kmTypes.*
import io.ktor.http.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.web3j.codegen.SolidityFunctionWrapperGenerator
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
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import net.folivo.trixnity.client.api.MatrixApiClient
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.nio.charset.Charset
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.X509EncodedKeySpec

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
    fun testSwapProcess() { runBlocking {
        //Specify swap direction and participant roles
        val direction = SwapDirection.BUY
        val role = ParticipantRole.TAKER

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

        //Setup DBService and KMService
        val driver = DatabaseDriverFactory()
        val dbService = DBService(driver)
        dbService.createTables()
        val kmService = KMService(dbService)

        //Create key pair and encoder
        val keyPair: KeyPair = kmService.generateKeyPair()
        val encoder = Base64.getEncoder()

        //Setup mxSession
        val matrixRestClient = MatrixApiClient(
            baseUrl = Url("http://matrix.org"),
        ).apply { accessToken.value = "" }
        val CINRoomId = RoomId("!WEuJJHaRpDvkbSveLu:matrix.org")

        //Get initial Dai balance
        val initialDaiBalance = dai.balanceOf(key_two.address).sendAsync().get()

        var functionCallResult: Pair<String, CompletableFuture<TransactionReceipt>>? = null
        var offer: CommutoSwap.Offer? = null
        var offerId: ByteArray? = null
        var swap: CommutoSwap.Swap? = null

        /*
        //Prepare this interface's payment method details JSON string
        val swiftDetails = USD_SWIFT_Details(
            "Jeff Roberts",
            "392649254057",
            "JEFROB38"
        )
        val ownPaymentDetails = Json.encodeToString(mapOf("USD-SWIFT" to swiftDetails)).toByteArray(Charset.forName("UTF-8"))
         */
        val ownPaymentDetails = "deets_here".toByteArray()

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
                keyPair.interfaceId,
                dummyDaiContractAddress,
                BigInteger.valueOf(100),
                BigInteger.valueOf(100),
                BigInteger.valueOf(10),
                directionInt!!,
                "a price here".toByteArray(Charsets.UTF_8),
                settlementMethods,
                BigInteger.valueOf(1),
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

            //Prepare public key announcement message
            val pkaPayload = PublicKeyAnnouncementPayload(
                encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()),
                encoder.encodeToString(offerId)
            )
            val payloadData = Json.encodeToString(pkaPayload).toByteArray(Charset.forName("UTF-8"))
            var payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadData)
            val payloadSignature = keyPair.sign(payloadDataHash)
            val pkaMessage = DecodedPublicKeyAnnouncement(
                encoder.encodeToString(keyPair.interfaceId),
                encoder.encodeToString(payloadData),
                encoder.encodeToString(payloadSignature)
            )
            val pkaMessageString = Json.encodeToString(pkaMessage)

            //Send PKA message to CIN Matrix Room
            matrixRestClient.rooms.sendMessageEvent(
                roomId = CINRoomId,
                eventContent = RoomMessageEventContent.TextMessageEventContent(pkaMessageString)
            ).getOrThrow()

            //Wait for offer to be taken
            var isOfferTaken = false
            var takerInterfaceId: ByteArray? = null
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
                            takerInterfaceId = events.get(0).takerInterfaceId
                        }
                    }
                    lastParsedBlockNumber += BigInteger.valueOf(1)
                }
                if (!isOfferTaken) {
                    Thread.sleep(1000)
                }
            }

            //Listen to CIN Matrix Room for taker's TakerInfo message, and handle it
            val takerInfoCF = CompletableFuture<TakerInfo>()
            coroutineScope {
                matrixRestClient.sync.start(scope =  this)
                //TODO: Parse last 20 or so messages too
                matrixRestClient.sync.subscribe<RoomMessageEventContent.TextMessageEventContent> {
                    println("Parsing message:")
                    println(it.content.body)
                    val takerInfoOptional = parseTakerInfoMessage(it.content.body, keyPair, kmService)
                    if (takerInfoOptional != null) {
                        takerInfoCF.complete(takerInfoOptional)
                    }
                }
            }
            val takerInfo: TakerInfo = takerInfoCF.get()

            //TODO: Save taker's public key locally (parseTakerInfoMessage already does this)

            //Prepare maker info message
            val makerInfoMessageKey = newSymmetricKey()
            val symmetricallyEncryptedPayload = makerInfoMessageKey.encrypt(ownPaymentDetails)
            val takerPublicKey = kmService.getPublicKey(takerInterfaceId!!)
            val encryptedKey = takerPublicKey!!.encrypt(makerInfoMessageKey.keyBytes)
            val encryptedIv = takerPublicKey!!.encrypt(symmetricallyEncryptedPayload.initializationVector)
            payloadDataHash = MessageDigest.getInstance("SHA-256").digest(symmetricallyEncryptedPayload.encryptedData)
            val makerInfoMsgObject = MakerInfoMessage(
                encoder.encodeToString(keyPair.interfaceId),
                encoder.encodeToString(takerInterfaceId),
                encoder.encodeToString(encryptedKey),
                encoder.encodeToString(encryptedIv),
                encoder.encodeToString(symmetricallyEncryptedPayload.encryptedData),
                encoder.encodeToString(keyPair.sign(payloadDataHash))
            )
            val makerInfoMsgString = Json.encodeToString(makerInfoMsgObject)

            //Send maker info message to CIN Matrix Room
            matrixRestClient.rooms.sendMessageEvent(
                roomId = CINRoomId,
                eventContent = RoomMessageEventContent.TextMessageEventContent(makerInfoMsgString)
            ).getOrThrow()

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

            //TODO: Listen for maker's PKA message in CIN Matrix room and handle it

            //TODO: Save maker's public key locally

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
                keyPair.interfaceId, //takerInterfaceId
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

            //Prepare taker info message
            //TODO: Encrypt encryptedKey and encryptedIV with taker's public key
            val takerInfoPayloadObject = TakerInfoMessagePayload(
                "takerInfo",
                encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()),
                encoder.encodeToString(offerId),
                ownPaymentDetails.toString(Charset.forName("UTF-8"))
            )
            val takerInfoPayload = Json.encodeToString(takerInfoPayloadObject).toByteArray(Charset.forName("UTF-8"))
            val takerInfoMessageKey = newSymmetricKey()
            val symmetricallyEncryptedPayload = takerInfoMessageKey.encrypt(takerInfoPayload)
            val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(symmetricallyEncryptedPayload.encryptedData)
            val takerInfoMsgObject = TakerInfoMessage(
                encoder.encodeToString(keyPair.interfaceId),
                encoder.encodeToString(offer.interfaceId),
                "",
                "",
                encoder.encodeToString(symmetricallyEncryptedPayload.encryptedData),
                encoder.encodeToString(keyPair.sign(payloadDataHash))
            )
            val takerInfoMsgString = Json.encodeToString(takerInfoMsgObject)

            //TODO: Send taker info message to CIN Matrix room
            matrixRestClient.rooms.sendMessageEvent(
                roomId = CINRoomId,
                eventContent = RoomMessageEventContent.TextMessageEventContent(takerInfoMsgString)
            ).getOrThrow()

            //TODO: Listen to CIN for maker's info message

            //TODO: Store maker's payment information locally

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
    } }

    fun createPublicKeyAnnouncement(keyPair: KeyPair, offerId: ByteArray): String {
        //Setup encoder
        val encoder = Base64.getEncoder()

        //Create message object
        val message = SerializablePublicKeyAnnouncementMessage(
            encoder.encodeToString(keyPair.interfaceId),
            "pka",
            "",
            ""
        )

        //Create Base64-encoded string of public key in PKCS#1 bytes
        val pubKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

        //Create Base-64 encoded string of offer id
        val offerIdString = encoder.encodeToString(offerId)

        //Create payload object
        val payload = SerializablePublicKeyAnnouncementPayload(
            pubKeyString,
            offerIdString
        )

        //Create payload UTF-8 bytes and their Base64-encoded string
        val payloadString = Json.encodeToString(payload)
        val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

        //Set "payload" field of message
        message.payload = encoder.encodeToString(payloadUTF8Bytes)

        //Create signature of payload
        val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadUTF8Bytes)
        val signature = keyPair.sign(payloadDataHash)

        //Set signature field of message
        message.signature = encoder.encodeToString(signature)

        //Prepare and return message string
        val messageString = Json.encodeToString(message)
        return messageString
    }

    fun parsePublicKeyAnnouncement(messageString: String, makerInterfaceId: ByteArray, offerId: ByteArray): PublicKey? {
        //setup decoder
        val decoder = Base64.getDecoder()

        //Restore message object
        val message = try {
            Json.decodeFromString<SerializablePublicKeyAnnouncementMessage>(messageString)
        } catch (e: Exception) {
            return null
        }

        //Ensure that the message is a Public Key announcement message
        if (message.msgType != "pka") {
            return null
        }

        //Ensure that the sender is the maker
        try {
            if (!Arrays.equals(decoder.decode(message.sender), makerInterfaceId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Restore payload object
        val payloadBytes = try {
            decoder.decode(message.payload)
        } catch (e: Exception) {
            return null
        }
        val payload = try {
            val payloadString = payloadBytes.toString(Charset.forName("UTF-8"))
            Json.decodeFromString<SerializablePublicKeyAnnouncementPayload>(payloadString)
        } catch (e: Exception) {
            return null
        }

        //Ensure that the offer id in the PKA matches the offer in question
        try {
            if (!Arrays.equals(decoder.decode(payload.offerId), offerId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Re-create maker's public key
        val publicKey = try {
            PublicKey(decoder.decode(payload.pubKey))
        } catch (e: Exception) {
            return null
        }

        //Check that interface id of taker's key matches value in "sender" field of message
        try {
            if (!Arrays.equals(decoder.decode(message.sender), publicKey.interfaceId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Create hash of payload
        val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadBytes)

        //Verify signature
        try {
            if (publicKey.verifySignature(payloadDataHash, decoder.decode(message.signature))) {
                return publicKey
            } else {
                return null
            }
        } catch (e: Exception) {
            return null
        }
    }

    @Test
    fun testPKAParsing() {
        //Restore the maker's public key
        val pubKeyB64 = "MIIBCgKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQAB"
        val privKeyB64 = "MIIEogIBAAKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQABAoIBACWe/ZLfS4DG144x0lUNedhUPsuvXzl5NAj8DBXtcQ6TkZ51VN8TgsHrQ2WKwkKdVnZAzPnkEMxy/0oj5xG8tBL43RM/tXFUsUHJhpe3G9Xb7JprG/3T2aEZP/Sviy16QvvFWJWtZHq1knOIy3Fy/lGTJM/ymVciJpc0TGGtccDyeQDBxaoQrr1r4Q9q5CMED/kEXq5KNLmzbfB1WInQZJ7wQhtyyAJiXJxKIeR3hVGR1dfBJGSbIIgYA5sYv8HPnXrorU7XEgDWLkILjSNgCvaGOgC5B4sgTB1pmwPQ173ee3gbn+PCai6saU9lciXeCteQp9YRBBWfwl+DDy5oGsUCgYEA0TB+kXbUgFyatxI46LLYRFGYTHgOPZz6Reu2ZKRaVNWC75NHyFTQdLSxvYLnQTnKGmjLapCTUwapiEAB50tLSko/uVcf4bG44EhCfL4S8hmfS3uCczokhhBjR/tZxnamXb/T1Wn2X06QsPSYQQmZB7EoQ6G0u/K792YgGn/qh+cCgYEAweUWInTK5nIAGyA/k0v0BNOefNTvfgV25wfR6nvXM3SJamHUTuO8wZntekD/epd4EewTP57rEb9kCzwdQnMkAaT1ejr7pQE4RFAZcL86o2C998QS0k25fw5xUhRiOIxSMqK7RLkAlRsThel+6BzHQ+jHxB06te3yyIjxnqP576UCgYA7tvAqbhVzHvw7TkRYiNUbi39CNPM7u1fmJcdHK3NtzBU4dn6DPVLUPdCPHJMPF4QNzeRjYynrBXfXoQ3qDKBNcKyIJ8q+DpGL1JTGLywRWCcU0QkIA4zxiDQPFD0oXi5XjK7XuQvPYQoEuY3M4wSAIZ4w0DRbgosNsGVxqxoz+QKBgClYh3LLguTHFHy0ULpBLQTGd3pZEcTGt4cmZL3isI4ZYKAdwl8cMwj5oOk76P6kRAdWVvhvE+NR86xtojOkR95N5catwzF5ZB01E2e2b3OdUoT9+6F6z35nfwSoshUq3vBLQTGzXYtuHaillNk8IcW6YrbQIM/gsK/Qe+1/O/G9AoGAYJhKegiRuasxY7ig1viAdYmhnCbtKhOa6qsq4cvI4avDL+Qfcgq6E8V5xgUsPsl2QUGz4DkBDw+E0D1Z4uT60y2TTTPbK7xmDs7KZy6Tvb+UKQNYlxL++DKbjFvxz6VJg17btqid8sP+LMhT3oqfRSakyGS74Bn3NBpLUeonYkQ="
        val decoder = Base64.getDecoder()
        val keyPair = KeyPair(decoder.decode(pubKeyB64), decoder.decode(privKeyB64))

        //Restore offer id
        val offerId = decoder.decode("9tGMGTr0SbuySqE0QOsAMQ==")

        //Create Public Key Announcement message string
        val jvmPkaMessageString = createPublicKeyAnnouncement(keyPair, offerId)!!

        //A Public Key Announcement message string generated by Swift code
        val swiftPkaMessageString = "{\"msgType\":\"pka\",\"sender\":\"gXE4i2ZrzX+QK5AdNalVTpU1tJoIA9sEMca6uRfiRSE=\",\"payload\":\"eyJwdWJLZXkiOiJNSUlCQ2dLQ0FRRUFubkRCNHpWMmxsRXd3TEh3N2M5MzRlVjd0NjlPbTUyZHBMY3VjdFh0T3RqR3NhS3lPQVY5NmVnbXhYNitDK01wdEZTVDN5WDR3TzZxSzNcL05TdU9IV0JYSUhraFFHWkVkVEhPbjRIRTloSGR3MmF4SjBGOUdRS1plVDh0OGt3KzU4K24rbmxiUVVhRkhVdzVpeXBsM1dpSTFLN0VuNFhWMmVnZlhHazl1akVsTXFYWk9cL2VGdW4zZUFNK2FzVDFnN29cL2syeXNPcFk1WCtzcWVzTHNKMGd6YUdINGpmRFZ1V2lmUzVZaGRnRktrQmkxaTNVMXRmUGRjM3NONTN1TkNQRWh4amp1T3VZSDVJM1dJOVZ6anBKZXpZb1Nsd3pJNGhZTk9qWTBjV3paTTlrcWVVdDkzS3pUcHZYNEZlUWlnVDlVTzIwY3MyM001TmJJVzRxN2xBNHdJREFRQUIiLCJvZmZlcklkIjoiOXRHTUdUcjBTYnV5U3FFMFFPc0FNUT09In0=\",\"signature\":\"kuIv+16HwhowvPq330\\/gIW\\/XuCwoztG25b8yvFA0H0CJ\\/aYKEzkHMxAIwlGSEdhlFecW8agB2VKj1gVdPQeRxvmbhrrkH3vPjlr7cObAqcpqJYEPT2IHgSaMyLhEQCGAxOS6BDts4VlApfeAgjhqLzU8602XPTD5E9\\/ilISaa6mPMUbOL8H0pWQMzykMlMxEfmi5pJrB9awnhnU98nJuVaUj7KiPRqjbjcnwcdGXI5ruiytXc+qyusuZA5FApjxeup+EVLIioPyOzI7\\/O0XIfpuGqyR4GOe1MATAzGR5n9sZjTzONANd+HwCo1YCtgA9D6vq9XoIJ8wiURp2bM1YBA==\"}"

        //Attempt to parse Public Key Announcement message string generated by JVM code
        val restoredJvmPublicKey = parsePublicKeyAnnouncement(jvmPkaMessageString, keyPair.interfaceId, offerId)!!

        //Attempt to parse Public Key Announcement message string generated by Swift code
        val restoredSwiftPublicKey = parsePublicKeyAnnouncement(swiftPkaMessageString, keyPair.interfaceId, offerId)!!

        //Check that the original and restored interface ids (and thus keys) are identical
        assert(Arrays.equals(keyPair.interfaceId, restoredJvmPublicKey.interfaceId))
        assert(Arrays.equals(keyPair.interfaceId, restoredSwiftPublicKey.interfaceId))
    }

    fun createTakerInfoMessage(keyPair: KeyPair, makerPubKey: PublicKey, swapId: ByteArray,
                               paymentDetails: SerializablePaymentMethodDetails): String {
        //Setup encoder
        val encoder = Base64.getEncoder()

        //Create message object
        val message = SerializableTakerInfoMessage(
            encoder.encodeToString(keyPair.interfaceId),
            encoder.encodeToString(makerPubKey.interfaceId),
            "",
            "",
            "",
            ""
        )

        //Create Base64-encoded string of taker's public key in PKCS#1 bytes
        val pubKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())

        //Create Base-64 encoded string of swap id
        val swapIdString = encoder.encodeToString(swapId)

        //Create payload object
        val payload = SerializableTakerInfoPayload(
            "takerInfo",
            pubKeyString,
            swapIdString,
            ""
        )

        //Create payment details UTF-8 bytes and their Base64-encoded string
        val paymentDetailsString = Json.encodeToString(paymentDetails)
        val paymentDetailsUTFBytes = paymentDetailsString.toByteArray(Charset.forName("UTF-8"))

        //Set "paymentDetails" field of payload
        payload.paymentDetails = encoder.encodeToString(paymentDetailsUTFBytes)

        //Create payload UTF-8 bytes and their Base64-encoded string
        val payloadString = Json.encodeToString(payload)
        val payloadUTF8Bytes = payloadString.toByteArray(Charset.forName("UTF-8"))

        //Generate a new symmetric key and initialization vector, and encrypt the payload bytes
        val symmetricKey = newSymmetricKey()
        val encryptedPayload = symmetricKey.encrypt(payloadUTF8Bytes)

        //Set "payload" field of message
        message.payload = encoder.encodeToString(encryptedPayload.encryptedData)

        //Create signature of encrypted payload
        val encryptedPayloadHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayload.encryptedData)
        val signature = keyPair.sign(encryptedPayloadHash)

        //Set signature field of message
        message.signature = encoder.encodeToString(signature)

        //Encrypt symmetric key and initialization vector with maker's public key
        val encryptedKey = makerPubKey.encrypt(symmetricKey.keyBytes)
        val encryptedIV = makerPubKey.encrypt(encryptedPayload.initializationVector)

        //Set "encryptedKey" and "encryptedIV" fields of message
        message.encryptedKey = encoder.encodeToString(encryptedKey)
        message.encryptedIV = encoder.encodeToString(encryptedIV)

        //Prepare and return message string
        val messageString = Json.encodeToString(message)
        return messageString
    }

    fun parseTakerInfoMessage(messageString: String, keyPair: KeyPair, takerInterfaceId: ByteArray,
                              swapId: ByteArray): Pair<PublicKey, SerializablePaymentMethodDetails>? {
        //setup decoder
        val decoder = Base64.getDecoder()

        //Restore message object
        val message = try {
            Json.decodeFromString<SerializableTakerInfoMessage>(messageString)
        } catch (e: Exception) {
            return null
        }

        //Ensure that the sender is the taker and the recipient is the maker
        try {
            if (!Arrays.equals(decoder.decode(message.sender), takerInterfaceId) ||
                !Arrays.equals(decoder.decode(message.recipient), keyPair.interfaceId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Decrypt symmetric key, initialization vector and encrypted payload
        val symmetricKey = try {
            val encryptedKey = decoder.decode(message.encryptedKey)
            val decryptedKeyBytes = keyPair.decrypt(encryptedKey)
            SymmetricKey(decryptedKeyBytes)
        } catch (e: Exception) {
            return null
        }
        val encryptedPayloadBytes = try {
            decoder.decode(message.payload)
        } catch (e: Exception) {
            return null
        }
        val decryptedPayloadBytes = try {
            val encryptedIV = decoder.decode(message.encryptedIV)
            val decryptedIV = keyPair.decrypt(encryptedIV)
            val encryptedPayload = SymmetricallyEncryptedData(encryptedPayloadBytes, decryptedIV)
            symmetricKey.decrypt(encryptedPayload)
        } catch (e: Exception) {
            return null
        }

        //Restore payload object
        val payload = try {
            val payloadString = decryptedPayloadBytes.toString(Charset.forName("UTF-8"))
            Json.decodeFromString<SerializableTakerInfoPayload>(payloadString)
        } catch (e: Exception) {
            return null
        }

        //Ensure the message is a taker info message
        if (payload.msgType != "takerInfo") {
            return null
        }

        //Ensure that the swap id in the takerInfo message matches the swap in qustion
        try {
            if (!Arrays.equals(decoder.decode(payload.swapId), swapId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Re-create taker's public key
        val publicKey = try {
            PublicKey(decoder.decode(payload.pubKey))
        } catch (e: Exception) {
            return null
        }

        //Check that interface id of taker's key matches value in "sender" field of message
        try {
            if (!Arrays.equals(decoder.decode(message.sender), publicKey.interfaceId)) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Create hash of encrypted payload
        val encryptedPayloadDataHash = MessageDigest.getInstance("SHA-256").digest(encryptedPayloadBytes)

        //Verify signature
        try {
            if (!publicKey.verifySignature(encryptedPayloadDataHash, decoder.decode(message.signature))) {
                return null
            }
        } catch (e: Exception) {
            return null
        }

        //Restore payment details object
        /*
        TODO: In production, we should know the sort of payment info we are looking for, try to deserialize that type
        exactly, and return null if the payment info is for a different payment method
         */
        val paymentDetails = try {
            val paymentDetailsBytes = decoder.decode(payload.paymentDetails)
            val paymentDetailsString = paymentDetailsBytes.toString(Charset.forName("UTF-8"))
            /*
            Ignore unknown keys, because for some reason kotlinx.serialization can't decode the "type" key it put into
            the message in the first place
             */
            Json { ignoreUnknownKeys = true} .decodeFromString<SerializableUSD_SWIFT_Details>(paymentDetailsString)
        } catch (e: Exception) {
            return null
        }

        return Pair(publicKey, paymentDetails)
    }

    @Test
    fun testTakerInfoParsing() {
        //Setup decoder
        val decoder = Base64.getDecoder()

        //Restore the maker's public and private key
        val makerPubKeyB64 = "MIIBCgKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQAB"
        val makerPrivKeyB64 = "MIIEogIBAAKCAQEAnnDB4zV2llEwwLHw7c934eV7t69Om52dpLcuctXtOtjGsaKyOAV96egmxX6+C+MptFST3yX4wO6qK3/NSuOHWBXIHkhQGZEdTHOn4HE9hHdw2axJ0F9GQKZeT8t8kw+58+n+nlbQUaFHUw5iypl3WiI1K7En4XV2egfXGk9ujElMqXZO/eFun3eAM+asT1g7o/k2ysOpY5X+sqesLsJ0gzaGH4jfDVuWifS5YhdgFKkBi1i3U1tfPdc3sN53uNCPEhxjjuOuYH5I3WI9VzjpJezYoSlwzI4hYNOjY0cWzZM9kqeUt93KzTpvX4FeQigT9UO20cs23M5NbIW4q7lA4wIDAQABAoIBACWe/ZLfS4DG144x0lUNedhUPsuvXzl5NAj8DBXtcQ6TkZ51VN8TgsHrQ2WKwkKdVnZAzPnkEMxy/0oj5xG8tBL43RM/tXFUsUHJhpe3G9Xb7JprG/3T2aEZP/Sviy16QvvFWJWtZHq1knOIy3Fy/lGTJM/ymVciJpc0TGGtccDyeQDBxaoQrr1r4Q9q5CMED/kEXq5KNLmzbfB1WInQZJ7wQhtyyAJiXJxKIeR3hVGR1dfBJGSbIIgYA5sYv8HPnXrorU7XEgDWLkILjSNgCvaGOgC5B4sgTB1pmwPQ173ee3gbn+PCai6saU9lciXeCteQp9YRBBWfwl+DDy5oGsUCgYEA0TB+kXbUgFyatxI46LLYRFGYTHgOPZz6Reu2ZKRaVNWC75NHyFTQdLSxvYLnQTnKGmjLapCTUwapiEAB50tLSko/uVcf4bG44EhCfL4S8hmfS3uCczokhhBjR/tZxnamXb/T1Wn2X06QsPSYQQmZB7EoQ6G0u/K792YgGn/qh+cCgYEAweUWInTK5nIAGyA/k0v0BNOefNTvfgV25wfR6nvXM3SJamHUTuO8wZntekD/epd4EewTP57rEb9kCzwdQnMkAaT1ejr7pQE4RFAZcL86o2C998QS0k25fw5xUhRiOIxSMqK7RLkAlRsThel+6BzHQ+jHxB06te3yyIjxnqP576UCgYA7tvAqbhVzHvw7TkRYiNUbi39CNPM7u1fmJcdHK3NtzBU4dn6DPVLUPdCPHJMPF4QNzeRjYynrBXfXoQ3qDKBNcKyIJ8q+DpGL1JTGLywRWCcU0QkIA4zxiDQPFD0oXi5XjK7XuQvPYQoEuY3M4wSAIZ4w0DRbgosNsGVxqxoz+QKBgClYh3LLguTHFHy0ULpBLQTGd3pZEcTGt4cmZL3isI4ZYKAdwl8cMwj5oOk76P6kRAdWVvhvE+NR86xtojOkR95N5catwzF5ZB01E2e2b3OdUoT9+6F6z35nfwSoshUq3vBLQTGzXYtuHaillNk8IcW6YrbQIM/gsK/Qe+1/O/G9AoGAYJhKegiRuasxY7ig1viAdYmhnCbtKhOa6qsq4cvI4avDL+Qfcgq6E8V5xgUsPsl2QUGz4DkBDw+E0D1Z4uT60y2TTTPbK7xmDs7KZy6Tvb+UKQNYlxL++DKbjFvxz6VJg17btqid8sP+LMhT3oqfRSakyGS74Bn3NBpLUeonYkQ="
        val makerKeyPair = KeyPair(decoder.decode(makerPubKeyB64), decoder.decode(makerPrivKeyB64))
        val makerPublicKey = PublicKey(decoder.decode(makerPubKeyB64))

        //Restore the taker's public and private key
        val takerPubKeyB64 = "MIIBCgKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQAB"
        val takerPrivKeyB64 = "MIIEowIBAAKCAQEAstQwQCanMBPJIEj1Mjc1m80sL3eJ/y1SDM3iVoDk2oNN6WOZly0GWbv1xjNMM94U8GLnYrzEGUek2IKcicBAVYhwsegeVo2DHOts72g6GpVWOPKndpT87raKCqSkd+IqR2OWAo+olGWmjWgAbesH/ojqJPNHaKlhi4b0JSwNAMfTP2HqcN2lXLXnSbR7F7MnrvjHbUxEUulthmX1mLId/7bznQ2hjyUP2yOQY92C7DFwVl/J33YV2F1GJbx5xGqB/cRRB+0hTRoqQvHscZAlGykWIVgvrdPw2JOsadQVePUhDBU5jvS5qyD6JxAlRWgN7FZsMTFLVM2XNW40N3jMIwIDAQABAoIBADez/Kue3qkNILMbxrSzmdFEIaVPeP6xYUN3xi7ny2F9UQGH8smyTq4Y7D+mru/hF2ihhi2tWu/87w458QS/i8qYy3G/OeQABH03oCEauC6bodXvT9aSJg89cNZL3qcxHbZLAOkfUoWW/EBDyw5yDXVttHF6Dh491JKfoOELTamWD4KxIScR/Nf6ih6UqB/SwmLz1X5+fZpW4iGZXIRsPzOzDtDmoSGajNXoi0Ln2x9DkUeXpx9r7TTT9DBT0jTLbCUiB3LYU4I/VR6upm0bDUKKRi9VTkQjOAV5rD3qdoraPVRCSzjUVqCwL7jqfunXsG/hhRccD+Di5pXaCuPeOsECgYEA3p4LLVHDzLhF269oUcvflMoBUVKo9UNHL/wmyujdV+RwFi5J2sxVLgKHsdKHCy7FdrDmxax7Mrgh57KS3+zfdDhs98w181JLwgFxzEAxIP2PnHd4P3NEbxCxnxhILW4fEotUVzJWjjhEHXe5QhOW2z2yIZIOEqBzFfRx33kWrbMCgYEAzaUrDMaTkIkOoVI7BbNS7n5CBWL/DaPOID1UiL4eHWngeoOwaeI+CB0cxSrxngykue0xM3aI3KVFaeIYSdn7DZAxWAS3U143VApgLxgLyxZBtVX18HYiTZQx/PiTczMH6kFA5z0L7iNlf0uQrQQJgDzM6QY0kKasufoss+Baj9ECgYA1BjvvTXxvtKyfCQa2BPN6QytRLXklAiNgoJS03AZsuvKfteLNhMH9NYkQp+6WkUtjW/t7tfuaNxWMVJJ7V7ZZvl7mHvPywvVcfm+WkOuiygJ86E/x/Qid08Ia/POkLoikKB+srUbElU5UHoI35OaXzfgx2tITSbhf0FuXOQZX1QKBgAj7A4xFR8ByG89ztdwj3qVHoj51+klwM9o4k259Tvdd3k27XoLhPHBCRTVfELokNzVfZFyo+oUYOpXLJ+BhwpLvDxiW7CKZ5LSo11Z3KFywFiKDJIBhyFG2/Q/dEyNewSO7wcfXZKP7q70JYcIMgRW2kgRDHxyKCtT8VeNtEsdhAoGBAJHzNruW/ZS31o0rvQxHu8tBcd8osTsPNZBhuHs60mbPFRHwBaU8JSofl4XjR8B7K9vjYtxVYMEsIX6NqNf1JMXGDva/cTCHXyPuiCnuUMbHkK0YpsFxQABwYA+sOSlujwJwMNPu4ylzHL1HDyv9m4x74/NM20zDFW6MB/zD6G0c"
        val takerKeyPair = KeyPair(decoder.decode(takerPubKeyB64), decoder.decode(takerPrivKeyB64))
        val takerPublicKey = PublicKey(decoder.decode(takerPubKeyB64))

        //Restore swap id
        val swapId = decoder.decode("9tGMGTr0SbuySqE0QOsAMQ==")

        //Create payment details
        val paymentDetails = SerializableUSD_SWIFT_Details(
            "USD-SWIFT",
            "Take Ker",
            "2039482",
            "TAK3940"
        )

        //Create taker info message string
        val jvmTakerInfoMessageString = createTakerInfoMessage(takerKeyPair, makerPublicKey, swapId, paymentDetails)

        //A taker info message string generated by Swift code
        val swiftTakerInfoMessageString = "{\"payload\":\"1S2NePK2idq0EfrWEa3lvamx03oqmKLT0Y\\/6Lx7xL6M5cmCKAsQW5CRGOo\\/fsmyYr5rVzjjDJsdHdj5JIbam\\/Fo5FIud0jLdMBnK3kkG98T62BZH492zdH4pP7vO+9\\/yGUgALHNo06hRBmzSvVpygCrP7+LvzMTYba42jYgYwihwdVdXjIdhv9E1ziCEbvBRrGWGvOUWAEC6PO8AMUasvrenW\\/f6v5+++01TgHrAzSLvE+gWiR\\/InuuJbqRCvKgN8OpJzAKkDehjp+acN0e5K6cMeRnLeD4vFGgywFn3Vg9OCKl+OyUac5YHsFwskMrVjSmf1z7UJZIdJRVwpRThugnhSRY3Lg+SxjpDZGIsun+jPOW\\/VuV7zMXMEimDy1n5ykgSEYDcj+YK4d4xCcpt2WsHS8ZqdTvkbfLpOabQfarYece5WpHWRX1b3iyMjkz7ZSne+dxbL4fkxC0Adl8LlUoFUXJy+VPooCjvsTbLF4jF9T92grpIYeAo16elIN13JZJfPctStZ9RYloDFSg5FfkIFuUgNXKGES3QixI9odnHsMTWafXgqZF9ZceHZcvgFfIi34I23Bl233d6SdxdR8Swk1T5PjUZzEvPNIiAdjBfG5s6kyoHapVpW837U1u0h7iUufOk2Z69IuO\\/tbUaCNGMIaPx9hS2yzpNUYgDLrlWLgWxL51w9xz3NK2m9li60hba8HFP+28GXlgbcVVD9zwMf3QmrK75OKdMj5WJoegc51FI9V0nhOf9Mhxek4G6\",\"sender\":\"HpIWD\\/7nBJ3VP+yQ2WYfh2lq5\\/uCLAkbkNIJ3FFJ2oc=\",\"signature\":\"bXvGF\\/HHoW+Y7kQuKkdTtTL4epWGp7K4rE2Va1uhzZxTE+Pj4snOfuk+hnM2HGnf5lR3Yp02+tn9UB9zkDAYavh\\/u5G8YExVkUim+QinbJv1cTHDqtdKiKHH4cHZhz+e8ErqEpWTpB2\\/rtXod+4jfI4DHvGhJhydRq9DFZV+wKuKe5GK0PMhYucs22cz6QOt0KGXNGq+LR2dcaljR67JKcr9Kp\\/fCkMdni42kbIY6Nry5AiOaVUs86F4ycmnnW+PbK1v+gOeA\\/sHCkM9veYy59dLbDqnTY0sDEMwqK3WvjW8eXVmKhk\\/nGGK1jBWp\\/fbyLLmZeNczxug1AtLQieCdA==\",\"encryptedKey\":\"dW4FUMxgv+6YbBer0MP3p2EYlaJ\\/4gkjYdhZi3lCvEftfPgZ4C1M3VHlaAKjdC8KA0PgtcDTWUEK\\/PYR2kyBfw9IkLvQUi3qYnNKCzcOmcBjanCfOszFF2PJ\\/aMKkLrwosxFVyVfTi5tjJa2GNCTa9XaFQKW4aoV+ujYqFoSgb081yOzEmVPjsyXgSfl159HhLuMZ1GaU5iaw2EmQcRnt\\/C1c6V5+OF1NOioOUO+Fgy31dJEDFl3Dd8NT8bVGRwfeZ7ecf3JBG5GC0yO1Zt95rw2SokRz6av4C7zs8bfSsbm\\/PcnVwfHUCfq0TtC8pU\\/ZuknL+k5G\\/4S+9Ev+kQm4g==\",\"recipient\":\"gXE4i2ZrzX+QK5AdNalVTpU1tJoIA9sEMca6uRfiRSE=\",\"encryptedIV\":\"l10XV+uHKK+zhynhB4hDyoQBMyqu+XkyJBL8eh4nylF3cVELw1jN+8CV8mjdqIk+2RAYhyepV0YtVd3baJK9H5Xker5MfqJCtG1b05vvp9wBp8ROLxz0HBPDqsSB2wXOz7lA+6km9xB8I0XiGv2\\/1Ml4gLz8nFEk\\/DWiHKXKKpcJ02QDSXsIEZt0sU4f1eKRouPUVs9n\\/RzgPIOMmBZycvaZj3eHaOiJGx5\\/\\/Qe8yJDsVptySAGwWOoD5Etkq64RMUWLkCCTVFwwJgg5qqtXWGtuyxx2kBPsebzU4r6HP\\/CPWkcicGOMiw64W0DSfvBuiFdA+\\/MuNmu+G0xfkvoZOg==\"}"

        //Attempt to parse taker info message string generated by JVM code
        val jvmParsingResults = parseTakerInfoMessage(jvmTakerInfoMessageString, makerKeyPair, takerPublicKey.interfaceId,
            swapId)

        //Attempt to parse taker info message string generated by Swift code
        val swiftParsingResults = parseTakerInfoMessage(swiftTakerInfoMessageString, makerKeyPair, takerPublicKey.interfaceId,
            swapId)

        //Check that the original and restored interface ids (and thus keys) are identical
        assert(Arrays.equals(takerKeyPair.interfaceId, jvmParsingResults!!.first.interfaceId))
        assert(Arrays.equals(takerKeyPair.interfaceId, swiftParsingResults!!.first.interfaceId))

        //Check that original and restored payment details are identical
        assert(paymentDetails.equals(jvmParsingResults.second as SerializableUSD_SWIFT_Details))
        assert(paymentDetails.equals(swiftParsingResults.second as SerializableUSD_SWIFT_Details))
    }

    fun parseTakerInfoMessage(message: String, keyPair: KeyPair, kmService: KMService) : TakerInfo? {
        val decoder = Base64.getDecoder()
        //Decode the message from JSON
        val decodedMessage = try {
            Json.decodeFromString<TakerInfoMessage>(message)
        } catch (e: Exception) {
            return null
        }
        //Check to make sure the message is adressed to us
        if (!Arrays.equals(keyPair.interfaceId, decoder.decode(decodedMessage.recipient))) {
            return null
        }
        //Get the encrypted key and IV
        val encryptedKeyBytes = decoder.decode(decodedMessage.encryptedKey)
        val encryptedIvBytes = decoder.decode(decodedMessage.encryptedIV)
        //Decrypt and restore the key and IV
        val decryptedKeyBytes = keyPair.decrypt(encryptedKeyBytes)
        val decryptedIvBytes = keyPair.decrypt(encryptedIvBytes)
        //Create encrypted payload object
        val encryptedPayload = SymmetricallyEncryptedData(
            decoder.decode(decodedMessage.payload),
            decryptedIvBytes
        )
        //Restore symmetric key object
        val symmetricKey = SymmetricKey(decryptedKeyBytes)
        //Decrypt payload to get UTF-8 JSON string
        val decryptedPayload = symmetricKey.decrypt(encryptedPayload)
        val decryptedPayloadString = decryptedPayload.toString(Charset.forName("UTF-8"))
        //Restore payload object from JSON string
        val decodedPayload = try {
            Json.decodeFromString<TakerInfoMessagePayload>(decryptedPayloadString)
        } catch (e: Exception) {
            return null
        }
        val takerKeyBytes = decoder.decode(decodedPayload.pubKey)
        val takerKey = PublicKey(takerKeyBytes)
        kmService.storePublicKey(takerKey)
        val signature = decoder.decode(decodedMessage.signature)
        if (!takerKey.verifySignature(encryptedPayload.encryptedData, signature)) {
            return null
        }
        //TODO: Check that the swapId matches the swap in question
        val swapId = decoder.decode(decodedPayload.swapId)
        val paymentDetails = decodedPayload.paymentDetails
        return TakerInfo(
            decoder.decode(decodedMessage.sender),
            decoder.decode(decodedMessage.recipient),
            symmetricKey,
            encryptedIvBytes,
            decodedPayload.msgType,
            takerKey,
            swapId,
            paymentDetails,
            signature,
        )
    }
}