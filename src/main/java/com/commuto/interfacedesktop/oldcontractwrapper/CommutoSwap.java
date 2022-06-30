package com.commuto.interfacedesktop.oldcontractwrapper;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes16;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.8.7.
 */
@SuppressWarnings("rawtypes")
public class CommutoSwap extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_BUSDADDRESS = "busdAddress";

    public static final String FUNC_CANCELOFFER = "cancelOffer";

    public static final String FUNC_CLOSESWAP = "closeSwap";

    public static final String FUNC_DAIADDRESS = "daiAddress";

    public static final String FUNC_EDITOFFER = "editOffer";

    public static final String FUNC_FILLSWAP = "fillSwap";

    public static final String FUNC_GETOFFER = "getOffer";

    public static final String FUNC_GETSUPPORTEDSETTLEMENTMETHODS = "getSupportedSettlementMethods";

    public static final String FUNC_GETSUPPORTEDSTABLECOINS = "getSupportedStablecoins";

    public static final String FUNC_GETSWAP = "getSwap";

    public static final String FUNC_OPENOFFER = "openOffer";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PROTOCOLVERSION = "protocolVersion";

    public static final String FUNC_REPORTPAYMENTRECEIVED = "reportPaymentReceived";

    public static final String FUNC_REPORTPAYMENTSENT = "reportPaymentSent";

    public static final String FUNC_SERVICEFEEPOOL = "serviceFeePool";

    public static final String FUNC_SETSETTLEMENTMETHODSUPPORT = "setSettlementMethodSupport";

    public static final String FUNC_SETSTABLECOINSUPPORT = "setStablecoinSupport";

    public static final String FUNC_TAKEOFFER = "takeOffer";

    public static final String FUNC_USDCADDRESS = "usdcAddress";

    public static final String FUNC_USDTADDRESS = "usdtAddress";

    public static final Event BUYERCLOSED_EVENT = new Event("BuyerClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event OFFERCANCELED_EVENT = new Event("OfferCanceled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event OFFEROPENED_EVENT = new Event("OfferOpened",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<DynamicBytes>() {}));
    ;

    public static final Event OFFERTAKEN_EVENT = new Event("OfferTaken",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<DynamicBytes>() {}));
    ;

    public static final Event PAYMENTRECEIVED_EVENT = new Event("PaymentReceived",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event PAYMENTSENT_EVENT = new Event("PaymentSent",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event PRICECHANGED_EVENT = new Event("PriceChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event SELLERCLOSED_EVENT = new Event("SellerClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event SWAPFILLED_EVENT = new Event("SwapFilled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static ArrayList<DynamicBytes> buildDynamicBytesArrList(List<byte[]> byteList) {
        ArrayList<DynamicBytes> dynamicBytesList = new ArrayList<DynamicBytes>();
        for (int i = 0; i < byteList.size(); i++) {
            dynamicBytesList.add(new DynamicBytes(byteList.get(i)));
        }
        return dynamicBytesList;
    }

    public static ArrayList<byte[]> buildByteArrList(DynamicArray<DynamicBytes> dynamicBytesArray) {
        ArrayList<byte[]> byteList = new ArrayList<byte[]>();
        for (int i = 0; i < dynamicBytesArray.getValue().size(); i++) {
            byteList.add(dynamicBytesArray.getValue().get(i).getValue());
        }
        return byteList;
    }

    @Deprecated
    protected CommutoSwap(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CommutoSwap(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CommutoSwap(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CommutoSwap(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<BuyerClosedEventResponse> getBuyerClosedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(BUYERCLOSED_EVENT, transactionReceipt);
        ArrayList<BuyerClosedEventResponse> responses = new ArrayList<BuyerClosedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            BuyerClosedEventResponse typedResponse = new BuyerClosedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<BuyerClosedEventResponse> buyerClosedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, BuyerClosedEventResponse>() {
            @Override
            public BuyerClosedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(BUYERCLOSED_EVENT, log);
                BuyerClosedEventResponse typedResponse = new BuyerClosedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<BuyerClosedEventResponse> buyerClosedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BUYERCLOSED_EVENT));
        return buyerClosedEventFlowable(filter);
    }

    public List<OfferCanceledEventResponse> getOfferCanceledEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(OFFERCANCELED_EVENT, transactionReceipt);
        ArrayList<OfferCanceledEventResponse> responses = new ArrayList<OfferCanceledEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            OfferCanceledEventResponse typedResponse = new OfferCanceledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OfferCanceledEventResponse> offerCanceledEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OfferCanceledEventResponse>() {
            @Override
            public OfferCanceledEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(OFFERCANCELED_EVENT, log);
                OfferCanceledEventResponse typedResponse = new OfferCanceledEventResponse();
                typedResponse.log = log;
                typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OfferCanceledEventResponse> offerCanceledEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OFFERCANCELED_EVENT));
        return offerCanceledEventFlowable(filter);
    }

    public List<OfferOpenedEventResponse> getOfferOpenedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(OFFEROPENED_EVENT, transactionReceipt);
        ArrayList<OfferOpenedEventResponse> responses = new ArrayList<OfferOpenedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            OfferOpenedEventResponse typedResponse = new OfferOpenedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.interfaceId = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OfferOpenedEventResponse> offerOpenedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OfferOpenedEventResponse>() {
            @Override
            public OfferOpenedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(OFFEROPENED_EVENT, log);
                OfferOpenedEventResponse typedResponse = new OfferOpenedEventResponse();
                typedResponse.log = log;
                typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.interfaceId = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OfferOpenedEventResponse> offerOpenedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OFFEROPENED_EVENT));
        return offerOpenedEventFlowable(filter);
    }

    public List<OfferTakenEventResponse> getOfferTakenEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(OFFERTAKEN_EVENT, transactionReceipt);
        ArrayList<OfferTakenEventResponse> responses = new ArrayList<OfferTakenEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            OfferTakenEventResponse typedResponse = new OfferTakenEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.takerInterfaceId = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OfferTakenEventResponse> offerTakenEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OfferTakenEventResponse>() {
            @Override
            public OfferTakenEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(OFFERTAKEN_EVENT, log);
                OfferTakenEventResponse typedResponse = new OfferTakenEventResponse();
                typedResponse.log = log;
                typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.takerInterfaceId = (byte[]) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OfferTakenEventResponse> offerTakenEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OFFERTAKEN_EVENT));
        return offerTakenEventFlowable(filter);
    }

    public List<PaymentReceivedEventResponse> getPaymentReceivedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(PAYMENTRECEIVED_EVENT, transactionReceipt);
        ArrayList<PaymentReceivedEventResponse> responses = new ArrayList<PaymentReceivedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentReceivedEventResponse typedResponse = new PaymentReceivedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PaymentReceivedEventResponse> paymentReceivedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PaymentReceivedEventResponse>() {
            @Override
            public PaymentReceivedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(PAYMENTRECEIVED_EVENT, log);
                PaymentReceivedEventResponse typedResponse = new PaymentReceivedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PaymentReceivedEventResponse> paymentReceivedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTRECEIVED_EVENT));
        return paymentReceivedEventFlowable(filter);
    }

    public List<PaymentSentEventResponse> getPaymentSentEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(PAYMENTSENT_EVENT, transactionReceipt);
        ArrayList<PaymentSentEventResponse> responses = new ArrayList<PaymentSentEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentSentEventResponse typedResponse = new PaymentSentEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PaymentSentEventResponse> paymentSentEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PaymentSentEventResponse>() {
            @Override
            public PaymentSentEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(PAYMENTSENT_EVENT, log);
                PaymentSentEventResponse typedResponse = new PaymentSentEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PaymentSentEventResponse> paymentSentEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTSENT_EVENT));
        return paymentSentEventFlowable(filter);
    }

    public List<PriceChangedEventResponse> getPriceChangedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(PRICECHANGED_EVENT, transactionReceipt);
        ArrayList<PriceChangedEventResponse> responses = new ArrayList<PriceChangedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PriceChangedEventResponse typedResponse = new PriceChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PriceChangedEventResponse> priceChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PriceChangedEventResponse>() {
            @Override
            public PriceChangedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(PRICECHANGED_EVENT, log);
                PriceChangedEventResponse typedResponse = new PriceChangedEventResponse();
                typedResponse.log = log;
                typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PriceChangedEventResponse> priceChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PRICECHANGED_EVENT));
        return priceChangedEventFlowable(filter);
    }

    public List<SellerClosedEventResponse> getSellerClosedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(SELLERCLOSED_EVENT, transactionReceipt);
        ArrayList<SellerClosedEventResponse> responses = new ArrayList<SellerClosedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SellerClosedEventResponse typedResponse = new SellerClosedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SellerClosedEventResponse> sellerClosedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SellerClosedEventResponse>() {
            @Override
            public SellerClosedEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(SELLERCLOSED_EVENT, log);
                SellerClosedEventResponse typedResponse = new SellerClosedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SellerClosedEventResponse> sellerClosedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SELLERCLOSED_EVENT));
        return sellerClosedEventFlowable(filter);
    }

    public List<SwapFilledEventResponse> getSwapFilledEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = extractEventParametersWithLog(SWAPFILLED_EVENT, transactionReceipt);
        ArrayList<SwapFilledEventResponse> responses = new ArrayList<SwapFilledEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            SwapFilledEventResponse typedResponse = new SwapFilledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<SwapFilledEventResponse> swapFilledEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, SwapFilledEventResponse>() {
            @Override
            public SwapFilledEventResponse apply(Log log) {
                EventValuesWithLog eventValues = extractEventParametersWithLog(SWAPFILLED_EVENT, log);
                SwapFilledEventResponse typedResponse = new SwapFilledEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<SwapFilledEventResponse> swapFilledEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SWAPFILLED_EVENT));
        return swapFilledEventFlowable(filter);
    }

    public RemoteFunctionCall<String> busdAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_BUSDADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> cancelOffer(byte[] offerID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CANCELOFFER, 
                Arrays.<Type>asList(new Bytes16(offerID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> closeSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSESWAP, 
                Arrays.<Type>asList(new Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> daiAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DAIADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> editOffer(byte[] offerID, Offer editedOffer, Boolean editPrice, Boolean editSettlementMethods) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EDITOFFER, 
                Arrays.<Type>asList(new Bytes16(offerID),
                editedOffer, 
                new Bool(editPrice),
                new Bool(editSettlementMethods)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> fillSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_FILLSWAP, 
                Arrays.<Type>asList(new Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Offer> getOffer(byte[] offerID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOFFER, 
                Arrays.<Type>asList(new Bytes16(offerID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Offer>() {}));
        return executeRemoteCallSingleValueReturn(function, Offer.class);
    }

    public RemoteFunctionCall<List> getSupportedSettlementMethods() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSUPPORTEDSETTLEMENTMETHODS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<DynamicBytes>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<List> getSupportedStablecoins() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSUPPORTEDSTABLECOINS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteFunctionCall<Swap> getSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSWAP, 
                Arrays.<Type>asList(new Bytes16(swapID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Swap>() {}));
        return executeRemoteCallSingleValueReturn(function, Swap.class);
    }

    public RemoteFunctionCall<TransactionReceipt> openOffer(byte[] offerID, Offer newOffer) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_OPENOFFER, 
                Arrays.<Type>asList(new Bytes16(offerID),
                newOffer), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> owner() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_OWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> protocolVersion() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROTOCOLVERSION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> reportPaymentReceived(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTRECEIVED, 
                Arrays.<Type>asList(new Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> reportPaymentSent(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTSENT, 
                Arrays.<Type>asList(new Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> serviceFeePool() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SERVICEFEEPOOL, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setSettlementMethodSupport(byte[] settlementMethod, Boolean support) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETSETTLEMENTMETHODSUPPORT, 
                Arrays.<Type>asList(new DynamicBytes(settlementMethod),
                new Bool(support)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setStablecoinSupport(String stablecoin, Boolean support) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETSTABLECOINSUPPORT, 
                Arrays.<Type>asList(new Address(160, stablecoin),
                new Bool(support)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> takeOffer(byte[] offerID, Swap newSwap) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TAKEOFFER, 
                Arrays.<Type>asList(new Bytes16(offerID),
                newSwap), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> usdcAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_USDCADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> usdtAddress() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_USDTADDRESS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    @Deprecated
    public static CommutoSwap load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CommutoSwap(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CommutoSwap load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CommutoSwap(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CommutoSwap load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CommutoSwap(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CommutoSwap load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CommutoSwap(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class Offer extends DynamicStruct {
        public Boolean isCreated;

        public Boolean isTaken;

        public String maker;

        public byte[] interfaceId;

        public String stablecoin;

        public BigInteger amountLowerBound;

        public BigInteger amountUpperBound;

        public BigInteger securityDepositAmount;

        public BigInteger direction;

        public byte[] price;

        public List<byte[]> settlementMethods;

        public BigInteger protocolVersion;

        //TODO: Node the modifications made to this and the constructor below it to deal with DynamicBytes/DynamicArray/byte[] issues

        public Offer(Boolean isCreated, Boolean isTaken, String maker, byte[] interfaceId, String stablecoin, BigInteger amountLowerBound, BigInteger amountUpperBound, BigInteger securityDepositAmount, BigInteger direction, byte[] price, List<byte[]> settlementMethods, BigInteger protocolVersion) {
            super(new org.web3j.abi.datatypes.Bool(isCreated),new org.web3j.abi.datatypes.Bool(isTaken),new org.web3j.abi.datatypes.Address(maker),new org.web3j.abi.datatypes.DynamicBytes(interfaceId),new org.web3j.abi.datatypes.Address(stablecoin),new org.web3j.abi.datatypes.generated.Uint256(amountLowerBound),new org.web3j.abi.datatypes.generated.Uint256(amountUpperBound),new org.web3j.abi.datatypes.generated.Uint256(securityDepositAmount),new org.web3j.abi.datatypes.generated.Uint8(direction),new org.web3j.abi.datatypes.DynamicBytes(price),new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.DynamicBytes>(buildDynamicBytesArrList(settlementMethods)),new org.web3j.abi.datatypes.generated.Uint256(protocolVersion));
            this.isCreated = isCreated;
            this.isTaken = isTaken;
            this.maker = maker;
            this.interfaceId = interfaceId;
            this.stablecoin = stablecoin;
            this.amountLowerBound = amountLowerBound;
            this.amountUpperBound = amountUpperBound;
            this.securityDepositAmount = securityDepositAmount;
            this.direction = direction;
            this.price = price;
            this.settlementMethods = settlementMethods;
            this.protocolVersion = protocolVersion;
        }

        public Offer(Bool isCreated, Bool isTaken, Address maker, DynamicBytes interfaceId, Address stablecoin, Uint256 amountLowerBound, Uint256 amountUpperBound, Uint256 securityDepositAmount, Uint8 direction, DynamicBytes price, DynamicArray<DynamicBytes> settlementMethods, Uint256 protocolVersion) {
            super(isCreated,isTaken,maker,interfaceId,stablecoin,amountLowerBound,amountUpperBound,securityDepositAmount,direction,price,settlementMethods,protocolVersion);
            this.isCreated = isCreated.getValue();
            this.isTaken = isTaken.getValue();
            this.maker = maker.getValue();
            this.interfaceId = interfaceId.getValue();
            this.stablecoin = stablecoin.getValue();
            this.amountLowerBound = amountLowerBound.getValue();
            this.amountUpperBound = amountUpperBound.getValue();
            this.securityDepositAmount = securityDepositAmount.getValue();
            this.direction = direction.getValue();
            this.price = price.getValue();
            this.settlementMethods = buildByteArrList(settlementMethods);
            this.protocolVersion = protocolVersion.getValue();
        }
    }

    public static class Swap extends DynamicStruct {
        public Boolean isCreated;

        public Boolean requiresFill;

        public String maker;

        public byte[] makerInterfaceId;

        public String taker;

        public byte[] takerInterfaceId;

        public String stablecoin;

        public BigInteger amountLowerBound;

        public BigInteger amountUpperBound;

        public BigInteger securityDepositAmount;

        public BigInteger takenSwapAmount;

        public BigInteger serviceFeeAmount;

        public BigInteger direction;

        public byte[] price;

        public byte[] settlementMethod;

        public BigInteger protocolVersion;

        public Boolean isPaymentSent;

        public Boolean isPaymentReceived;

        public Boolean hasBuyerClosed;

        public Boolean hasSellerClosed;

        public Swap(Boolean isCreated, Boolean requiresFill, String maker, byte[] makerInterfaceId, String taker, byte[] takerInterfaceId, String stablecoin, BigInteger amountLowerBound, BigInteger amountUpperBound, BigInteger securityDepositAmount, BigInteger takenSwapAmount, BigInteger serviceFeeAmount, BigInteger direction, byte[] price, byte[] settlementMethod, BigInteger protocolVersion, Boolean isPaymentSent, Boolean isPaymentReceived, Boolean hasBuyerClosed, Boolean hasSellerClosed) {
            super(new Bool(isCreated),new Bool(requiresFill),new Address(maker),new DynamicBytes(makerInterfaceId),new Address(taker),new DynamicBytes(takerInterfaceId),new Address(stablecoin),new Uint256(amountLowerBound),new Uint256(amountUpperBound),new Uint256(securityDepositAmount),new Uint256(takenSwapAmount),new Uint256(serviceFeeAmount),new Uint8(direction),new DynamicBytes(price),new DynamicBytes(settlementMethod),new Uint256(protocolVersion),new Bool(isPaymentSent),new Bool(isPaymentReceived),new Bool(hasBuyerClosed),new Bool(hasSellerClosed));
            this.isCreated = isCreated;
            this.requiresFill = requiresFill;
            this.maker = maker;
            this.makerInterfaceId = makerInterfaceId;
            this.taker = taker;
            this.takerInterfaceId = takerInterfaceId;
            this.stablecoin = stablecoin;
            this.amountLowerBound = amountLowerBound;
            this.amountUpperBound = amountUpperBound;
            this.securityDepositAmount = securityDepositAmount;
            this.takenSwapAmount = takenSwapAmount;
            this.serviceFeeAmount = serviceFeeAmount;
            this.direction = direction;
            this.price = price;
            this.settlementMethod = settlementMethod;
            this.protocolVersion = protocolVersion;
            this.isPaymentSent = isPaymentSent;
            this.isPaymentReceived = isPaymentReceived;
            this.hasBuyerClosed = hasBuyerClosed;
            this.hasSellerClosed = hasSellerClosed;
        }

        public Swap(Bool isCreated, Bool requiresFill, Address maker, DynamicBytes makerInterfaceId, Address taker, DynamicBytes takerInterfaceId, Address stablecoin, Uint256 amountLowerBound, Uint256 amountUpperBound, Uint256 securityDepositAmount, Uint256 takenSwapAmount, Uint256 serviceFeeAmount, Uint8 direction, DynamicBytes price, DynamicBytes settlementMethod, Uint256 protocolVersion, Bool isPaymentSent, Bool isPaymentReceived, Bool hasBuyerClosed, Bool hasSellerClosed) {
            super(isCreated,requiresFill,maker,makerInterfaceId,taker,takerInterfaceId,stablecoin,amountLowerBound,amountUpperBound,securityDepositAmount,takenSwapAmount,serviceFeeAmount,direction,price,settlementMethod,protocolVersion,isPaymentSent,isPaymentReceived,hasBuyerClosed,hasSellerClosed);
            this.isCreated = isCreated.getValue();
            this.requiresFill = requiresFill.getValue();
            this.maker = maker.getValue();
            this.makerInterfaceId = makerInterfaceId.getValue();
            this.taker = taker.getValue();
            this.takerInterfaceId = takerInterfaceId.getValue();
            this.stablecoin = stablecoin.getValue();
            this.amountLowerBound = amountLowerBound.getValue();
            this.amountUpperBound = amountUpperBound.getValue();
            this.securityDepositAmount = securityDepositAmount.getValue();
            this.takenSwapAmount = takenSwapAmount.getValue();
            this.serviceFeeAmount = serviceFeeAmount.getValue();
            this.direction = direction.getValue();
            this.price = price.getValue();
            this.settlementMethod = settlementMethod.getValue();
            this.protocolVersion = protocolVersion.getValue();
            this.isPaymentSent = isPaymentSent.getValue();
            this.isPaymentReceived = isPaymentReceived.getValue();
            this.hasBuyerClosed = hasBuyerClosed.getValue();
            this.hasSellerClosed = hasSellerClosed.getValue();
        }
    }

    public static class BuyerClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class OfferCanceledEventResponse extends BaseEventResponse {
        public byte[] offerID;
    }

    public static class OfferOpenedEventResponse extends BaseEventResponse {
        public byte[] offerID;

        public byte[] interfaceId;
    }

    public static class OfferTakenEventResponse extends BaseEventResponse {
        public byte[] offerID;

        public byte[] takerInterfaceId;
    }

    public static class PaymentReceivedEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class PaymentSentEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class PriceChangedEventResponse extends BaseEventResponse {
        public byte[] offerID;
    }

    public static class SellerClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class SwapFilledEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }
}
