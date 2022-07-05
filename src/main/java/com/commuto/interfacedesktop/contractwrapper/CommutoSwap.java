package com.commuto.interfacedesktop.contractwrapper;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
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
import org.web3j.tx.exceptions.ContractCallException;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class CommutoSwap extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_CANCELOFFER = "cancelOffer";

    public static final String FUNC_CHANGEDISPUTERESOLUTIONTIMELOCK = "changeDisputeResolutionTimelock";

    public static final String FUNC_CHANGEPRIMARYTIMELOCK = "changePrimaryTimelock";

    public static final String FUNC_CLOSEDISPUTEDSWAP = "closeDisputedSwap";

    public static final String FUNC_CLOSEESCALATEDSWAP = "closeEscalatedSwap";

    public static final String FUNC_CLOSESWAP = "closeSwap";

    public static final String FUNC_COMMUTOSWAPCLOSER = "commutoSwapCloser";

    public static final String FUNC_COMMUTOSWAPDISPUTEESCALATOR = "commutoSwapDisputeEscalator";

    public static final String FUNC_COMMUTOSWAPDISPUTERAISER = "commutoSwapDisputeRaiser";

    public static final String FUNC_COMMUTOSWAPFILLER = "commutoSwapFiller";

    public static final String FUNC_COMMUTOSWAPOFFERCANCELER = "commutoSwapOfferCanceler";

    public static final String FUNC_COMMUTOSWAPOFFEREDITOR = "commutoSwapOfferEditor";

    public static final String FUNC_COMMUTOSWAPOFFEROPENER = "commutoSwapOfferOpener";

    public static final String FUNC_COMMUTOSWAPOFFERTAKER = "commutoSwapOfferTaker";

    public static final String FUNC_COMMUTOSWAPPAYMENTREPORTER = "commutoSwapPaymentReporter";

    public static final String FUNC_COMMUTOSWAPRESOLUTIONPROPOSALREACTOR = "commutoSwapResolutionProposalReactor";

    public static final String FUNC_COMMUTOSWAPRESOLUTIONPROPOSER = "commutoSwapResolutionProposer";

    public static final String FUNC_DISPUTERESOLUTIONTIMELOCK = "disputeResolutionTimelock";

    public static final String FUNC_EDITOFFER = "editOffer";

    public static final String FUNC_ESCALATEDISPUTE = "escalateDispute";

    public static final String FUNC_FILLSWAP = "fillSwap";

    public static final String FUNC_GETACTIVEDISPUTEAGENTS = "getActiveDisputeAgents";

    public static final String FUNC_GETDISPUTE = "getDispute";

    public static final String FUNC_GETMINIMUMDISPUTEPERIOD = "getMinimumDisputePeriod";

    public static final String FUNC_GETOFFER = "getOffer";

    public static final String FUNC_GETSERVICEFEERATE = "getServiceFeeRate";

    public static final String FUNC_GETSUPPORTEDSETTLEMENTMETHODS = "getSupportedSettlementMethods";

    public static final String FUNC_GETSUPPORTEDSTABLECOINS = "getSupportedStablecoins";

    public static final String FUNC_GETSWAP = "getSwap";

    public static final String FUNC_MINIMUMDISPUTEPERIOD = "minimumDisputePeriod";

    public static final String FUNC_OPENOFFER = "openOffer";

    public static final String FUNC_PRIMARYTIMELOCK = "primaryTimelock";

    public static final String FUNC_PROPOSERESOLUTION = "proposeResolution";

    public static final String FUNC_PROTOCOLVERSION = "protocolVersion";

    public static final String FUNC_RAISEDISPUTE = "raiseDispute";

    public static final String FUNC_REACTTORESOLUTIONPROPOSAL = "reactToResolutionProposal";

    public static final String FUNC_REPORTPAYMENTRECEIVED = "reportPaymentReceived";

    public static final String FUNC_REPORTPAYMENTSENT = "reportPaymentSent";

    public static final String FUNC_SERVICEFEERATE = "serviceFeeRate";

    public static final String FUNC_SETDISPUTEAGENTACTIVE = "setDisputeAgentActive";

    public static final String FUNC_SETMINIMUMDISPUTEPERIOD = "setMinimumDisputePeriod";

    public static final String FUNC_SETSERVICEFEERATE = "setServiceFeeRate";

    public static final String FUNC_SETSETTLEMENTMETHODSUPPORT = "setSettlementMethodSupport";

    public static final String FUNC_SETSTABLECOINSUPPORT = "setStablecoinSupport";

    public static final String FUNC_TAKEOFFER = "takeOffer";

    public static final Event BUYERCLOSED_EVENT = new Event("BuyerClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event DISPUTEESCALATED_EVENT = new Event("DisputeEscalated",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Address>() {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event DISPUTERAISED_EVENT = new Event("DisputeRaised",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event DISPUTERESOLUTIONTIMELOCKCHANGED_EVENT = new Event("DisputeResolutionTimelockChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event DISPUTEDSWAPCLOSED_EVENT = new Event("DisputedSwapClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event ESCALATEDSWAPCLOSED_EVENT = new Event("EscalatedSwapClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event MINIMUMDISPUTEPERIODCHANGED_EVENT = new Event("MinimumDisputePeriodChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event OFFERCANCELED_EVENT = new Event("OfferCanceled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event OFFEREDITED_EVENT = new Event("OfferEdited",
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

    public static final Event PRIMARYTIMELOCKCHANGED_EVENT = new Event("PrimaryTimelockChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event REACTIONSUBMITTED_EVENT = new Event("ReactionSubmitted",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Address>() {}, new TypeReference<Uint8>() {}));
    ;

    public static final Event RESOLUTIONPROPOSED_EVENT = new Event("ResolutionProposed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event SELLERCLOSED_EVENT = new Event("SellerClosed",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    public static final Event SERVICEFEERATECHANGED_EVENT = new Event("ServiceFeeRateChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    public static final Event SWAPFILLED_EVENT = new Event("SwapFilled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes16>() {}));
    ;

    // Custom function to patch bug in web3j codegen
    public static ArrayList<DynamicBytes> buildDynamicBytesArrList(List<byte[]> byteList) {
        ArrayList<DynamicBytes> dynamicBytesList = new ArrayList<DynamicBytes>();
        for (int i = 0; i < byteList.size(); i++) {
            dynamicBytesList.add(new DynamicBytes(byteList.get(i)));
        }
        return dynamicBytesList;
    }

    // Custom function to patch bug in web3j codegen
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BUYERCLOSED_EVENT, transactionReceipt);
        ArrayList<BuyerClosedEventResponse> responses = new ArrayList<BuyerClosedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BUYERCLOSED_EVENT, log);
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

    public List<DisputeEscalatedEventResponse> getDisputeEscalatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DISPUTEESCALATED_EVENT, transactionReceipt);
        ArrayList<DisputeEscalatedEventResponse> responses = new ArrayList<DisputeEscalatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DisputeEscalatedEventResponse typedResponse = new DisputeEscalatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.escalator = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.reason = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DisputeEscalatedEventResponse> disputeEscalatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DisputeEscalatedEventResponse>() {
            @Override
            public DisputeEscalatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DISPUTEESCALATED_EVENT, log);
                DisputeEscalatedEventResponse typedResponse = new DisputeEscalatedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.escalator = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.reason = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DisputeEscalatedEventResponse> disputeEscalatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DISPUTEESCALATED_EVENT));
        return disputeEscalatedEventFlowable(filter);
    }

    public List<DisputeRaisedEventResponse> getDisputeRaisedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DISPUTERAISED_EVENT, transactionReceipt);
        ArrayList<DisputeRaisedEventResponse> responses = new ArrayList<DisputeRaisedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DisputeRaisedEventResponse typedResponse = new DisputeRaisedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.disputeAgent0 = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.disputeAgent1 = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.disputeAgent2 = (String) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DisputeRaisedEventResponse> disputeRaisedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DisputeRaisedEventResponse>() {
            @Override
            public DisputeRaisedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DISPUTERAISED_EVENT, log);
                DisputeRaisedEventResponse typedResponse = new DisputeRaisedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.disputeAgent0 = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.disputeAgent1 = (String) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.disputeAgent2 = (String) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DisputeRaisedEventResponse> disputeRaisedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DISPUTERAISED_EVENT));
        return disputeRaisedEventFlowable(filter);
    }

    public List<DisputeResolutionTimelockChangedEventResponse> getDisputeResolutionTimelockChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DISPUTERESOLUTIONTIMELOCKCHANGED_EVENT, transactionReceipt);
        ArrayList<DisputeResolutionTimelockChangedEventResponse> responses = new ArrayList<DisputeResolutionTimelockChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DisputeResolutionTimelockChangedEventResponse typedResponse = new DisputeResolutionTimelockChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldDisputeResolutionTimelock = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newDisputeResolutionTimelock = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DisputeResolutionTimelockChangedEventResponse> disputeResolutionTimelockChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DisputeResolutionTimelockChangedEventResponse>() {
            @Override
            public DisputeResolutionTimelockChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DISPUTERESOLUTIONTIMELOCKCHANGED_EVENT, log);
                DisputeResolutionTimelockChangedEventResponse typedResponse = new DisputeResolutionTimelockChangedEventResponse();
                typedResponse.log = log;
                typedResponse.oldDisputeResolutionTimelock = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.newDisputeResolutionTimelock = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DisputeResolutionTimelockChangedEventResponse> disputeResolutionTimelockChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DISPUTERESOLUTIONTIMELOCKCHANGED_EVENT));
        return disputeResolutionTimelockChangedEventFlowable(filter);
    }

    public List<DisputedSwapClosedEventResponse> getDisputedSwapClosedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(DISPUTEDSWAPCLOSED_EVENT, transactionReceipt);
        ArrayList<DisputedSwapClosedEventResponse> responses = new ArrayList<DisputedSwapClosedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DisputedSwapClosedEventResponse typedResponse = new DisputedSwapClosedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.closer = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<DisputedSwapClosedEventResponse> disputedSwapClosedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, DisputedSwapClosedEventResponse>() {
            @Override
            public DisputedSwapClosedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(DISPUTEDSWAPCLOSED_EVENT, log);
                DisputedSwapClosedEventResponse typedResponse = new DisputedSwapClosedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.closer = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<DisputedSwapClosedEventResponse> disputedSwapClosedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DISPUTEDSWAPCLOSED_EVENT));
        return disputedSwapClosedEventFlowable(filter);
    }

    public List<EscalatedSwapClosedEventResponse> getEscalatedSwapClosedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ESCALATEDSWAPCLOSED_EVENT, transactionReceipt);
        ArrayList<EscalatedSwapClosedEventResponse> responses = new ArrayList<EscalatedSwapClosedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            EscalatedSwapClosedEventResponse typedResponse = new EscalatedSwapClosedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.makerPayout = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.takerPayout = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.confiscationPayout = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<EscalatedSwapClosedEventResponse> escalatedSwapClosedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, EscalatedSwapClosedEventResponse>() {
            @Override
            public EscalatedSwapClosedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ESCALATEDSWAPCLOSED_EVENT, log);
                EscalatedSwapClosedEventResponse typedResponse = new EscalatedSwapClosedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.makerPayout = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.takerPayout = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                typedResponse.confiscationPayout = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<EscalatedSwapClosedEventResponse> escalatedSwapClosedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ESCALATEDSWAPCLOSED_EVENT));
        return escalatedSwapClosedEventFlowable(filter);
    }

    public List<MinimumDisputePeriodChangedEventResponse> getMinimumDisputePeriodChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(MINIMUMDISPUTEPERIODCHANGED_EVENT, transactionReceipt);
        ArrayList<MinimumDisputePeriodChangedEventResponse> responses = new ArrayList<MinimumDisputePeriodChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            MinimumDisputePeriodChangedEventResponse typedResponse = new MinimumDisputePeriodChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newMinimumDisputePeriod = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<MinimumDisputePeriodChangedEventResponse> minimumDisputePeriodChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, MinimumDisputePeriodChangedEventResponse>() {
            @Override
            public MinimumDisputePeriodChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(MINIMUMDISPUTEPERIODCHANGED_EVENT, log);
                MinimumDisputePeriodChangedEventResponse typedResponse = new MinimumDisputePeriodChangedEventResponse();
                typedResponse.log = log;
                typedResponse.newMinimumDisputePeriod = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<MinimumDisputePeriodChangedEventResponse> minimumDisputePeriodChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(MINIMUMDISPUTEPERIODCHANGED_EVENT));
        return minimumDisputePeriodChangedEventFlowable(filter);
    }

    public List<OfferCanceledEventResponse> getOfferCanceledEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OFFERCANCELED_EVENT, transactionReceipt);
        ArrayList<OfferCanceledEventResponse> responses = new ArrayList<OfferCanceledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OFFERCANCELED_EVENT, log);
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

    public List<OfferEditedEventResponse> getOfferEditedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OFFEREDITED_EVENT, transactionReceipt);
        ArrayList<OfferEditedEventResponse> responses = new ArrayList<OfferEditedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            OfferEditedEventResponse typedResponse = new OfferEditedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<OfferEditedEventResponse> offerEditedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, OfferEditedEventResponse>() {
            @Override
            public OfferEditedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OFFEREDITED_EVENT, log);
                OfferEditedEventResponse typedResponse = new OfferEditedEventResponse();
                typedResponse.log = log;
                typedResponse.offerID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<OfferEditedEventResponse> offerEditedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(OFFEREDITED_EVENT));
        return offerEditedEventFlowable(filter);
    }

    public List<OfferOpenedEventResponse> getOfferOpenedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OFFEROPENED_EVENT, transactionReceipt);
        ArrayList<OfferOpenedEventResponse> responses = new ArrayList<OfferOpenedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OFFEROPENED_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(OFFERTAKEN_EVENT, transactionReceipt);
        ArrayList<OfferTakenEventResponse> responses = new ArrayList<OfferTakenEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(OFFERTAKEN_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PAYMENTRECEIVED_EVENT, transactionReceipt);
        ArrayList<PaymentReceivedEventResponse> responses = new ArrayList<PaymentReceivedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PAYMENTRECEIVED_EVENT, log);
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
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PAYMENTSENT_EVENT, transactionReceipt);
        ArrayList<PaymentSentEventResponse> responses = new ArrayList<PaymentSentEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PAYMENTSENT_EVENT, log);
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

    public List<PrimaryTimelockChangedEventResponse> getPrimaryTimelockChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PRIMARYTIMELOCKCHANGED_EVENT, transactionReceipt);
        ArrayList<PrimaryTimelockChangedEventResponse> responses = new ArrayList<PrimaryTimelockChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PrimaryTimelockChangedEventResponse typedResponse = new PrimaryTimelockChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.oldPrimaryTimelock = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newPrimaryTimelock = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PrimaryTimelockChangedEventResponse> primaryTimelockChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PrimaryTimelockChangedEventResponse>() {
            @Override
            public PrimaryTimelockChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PRIMARYTIMELOCKCHANGED_EVENT, log);
                PrimaryTimelockChangedEventResponse typedResponse = new PrimaryTimelockChangedEventResponse();
                typedResponse.log = log;
                typedResponse.oldPrimaryTimelock = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.newPrimaryTimelock = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PrimaryTimelockChangedEventResponse> primaryTimelockChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PRIMARYTIMELOCKCHANGED_EVENT));
        return primaryTimelockChangedEventFlowable(filter);
    }

    public List<ReactionSubmittedEventResponse> getReactionSubmittedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(REACTIONSUBMITTED_EVENT, transactionReceipt);
        ArrayList<ReactionSubmittedEventResponse> responses = new ArrayList<ReactionSubmittedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ReactionSubmittedEventResponse typedResponse = new ReactionSubmittedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.addr = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.reaction = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ReactionSubmittedEventResponse> reactionSubmittedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ReactionSubmittedEventResponse>() {
            @Override
            public ReactionSubmittedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(REACTIONSUBMITTED_EVENT, log);
                ReactionSubmittedEventResponse typedResponse = new ReactionSubmittedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.addr = (String) eventValues.getNonIndexedValues().get(1).getValue();
                typedResponse.reaction = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ReactionSubmittedEventResponse> reactionSubmittedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(REACTIONSUBMITTED_EVENT));
        return reactionSubmittedEventFlowable(filter);
    }

    public List<ResolutionProposedEventResponse> getResolutionProposedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(RESOLUTIONPROPOSED_EVENT, transactionReceipt);
        ArrayList<ResolutionProposedEventResponse> responses = new ArrayList<ResolutionProposedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ResolutionProposedEventResponse typedResponse = new ResolutionProposedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.disputeAgent = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ResolutionProposedEventResponse> resolutionProposedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ResolutionProposedEventResponse>() {
            @Override
            public ResolutionProposedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(RESOLUTIONPROPOSED_EVENT, log);
                ResolutionProposedEventResponse typedResponse = new ResolutionProposedEventResponse();
                typedResponse.log = log;
                typedResponse.swapID = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.disputeAgent = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ResolutionProposedEventResponse> resolutionProposedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(RESOLUTIONPROPOSED_EVENT));
        return resolutionProposedEventFlowable(filter);
    }

    public List<SellerClosedEventResponse> getSellerClosedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SELLERCLOSED_EVENT, transactionReceipt);
        ArrayList<SellerClosedEventResponse> responses = new ArrayList<SellerClosedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SELLERCLOSED_EVENT, log);
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

    public List<ServiceFeeRateChangedEventResponse> getServiceFeeRateChangedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SERVICEFEERATECHANGED_EVENT, transactionReceipt);
        ArrayList<ServiceFeeRateChangedEventResponse> responses = new ArrayList<ServiceFeeRateChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ServiceFeeRateChangedEventResponse typedResponse = new ServiceFeeRateChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newServiceFeeRate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ServiceFeeRateChangedEventResponse> serviceFeeRateChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, ServiceFeeRateChangedEventResponse>() {
            @Override
            public ServiceFeeRateChangedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SERVICEFEERATECHANGED_EVENT, log);
                ServiceFeeRateChangedEventResponse typedResponse = new ServiceFeeRateChangedEventResponse();
                typedResponse.log = log;
                typedResponse.newServiceFeeRate = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ServiceFeeRateChangedEventResponse> serviceFeeRateChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(SERVICEFEERATECHANGED_EVENT));
        return serviceFeeRateChangedEventFlowable(filter);
    }

    public List<SwapFilledEventResponse> getSwapFilledEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(SWAPFILLED_EVENT, transactionReceipt);
        ArrayList<SwapFilledEventResponse> responses = new ArrayList<SwapFilledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
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
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(SWAPFILLED_EVENT, log);
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

    public RemoteFunctionCall<TransactionReceipt> cancelOffer(byte[] offerID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CANCELOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeDisputeResolutionTimelock(String newDisputeResolutionTimelock) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CHANGEDISPUTERESOLUTIONTIMELOCK,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newDisputeResolutionTimelock)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changePrimaryTimelock(String newPrimaryTimelock) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CHANGEPRIMARYTIMELOCK,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newPrimaryTimelock)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> closeDisputedSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSEDISPUTEDSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> closeEscalatedSwap(byte[] swapID, BigInteger makerPayout, BigInteger takerPayout, BigInteger confiscationPayout) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSEESCALATEDSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID),
                        new org.web3j.abi.datatypes.generated.Uint256(makerPayout),
                        new org.web3j.abi.datatypes.generated.Uint256(takerPayout),
                        new org.web3j.abi.datatypes.generated.Uint256(confiscationPayout)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> closeSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSESWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> commutoSwapCloser() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPCLOSER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapDisputeEscalator() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPDISPUTEESCALATOR,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapDisputeRaiser() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPDISPUTERAISER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapFiller() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPFILLER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapOfferCanceler() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPOFFERCANCELER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapOfferEditor() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPOFFEREDITOR,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapOfferOpener() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPOFFEROPENER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapOfferTaker() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPOFFERTAKER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapPaymentReporter() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPPAYMENTREPORTER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapResolutionProposalReactor() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPRESOLUTIONPROPOSALREACTOR,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> commutoSwapResolutionProposer() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_COMMUTOSWAPRESOLUTIONPROPOSER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> disputeResolutionTimelock() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_DISPUTERESOLUTIONTIMELOCK,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> editOffer(byte[] offerID, Offer editedOffer) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_EDITOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID),
                        editedOffer),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> escalateDispute(byte[] swapID, BigInteger reason) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_ESCALATEDISPUTE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID),
                        new org.web3j.abi.datatypes.generated.Uint8(reason)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> fillSwap(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_FILLSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<List> getActiveDisputeAgents() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETACTIVEDISPUTEAGENTS,
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

    public RemoteFunctionCall<Dispute> getDispute(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETDISPUTE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Dispute>() {}));
        return executeRemoteCallSingleValueReturn(function, Dispute.class);
    }

    public RemoteFunctionCall<BigInteger> getMinimumDisputePeriod() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETMINIMUMDISPUTEPERIOD,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<Offer> getOffer(byte[] offerID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Offer>() {}));
        /*
        Normally, this would return the result of
        executeRemoteCallSingleValueReturn(function, Offer.class). However, since Web3j can't decode
        dynamically sized arrays properly, we call our own function here.
         */
        return executeRemoteCallSingleValueReturnWithDynamicArrayOfBytes(function, Offer.class);
    }

    // Added to work around Web3j's inability to decode DynamicArrays properly
    protected <T> RemoteFunctionCall<T> executeRemoteCallSingleValueReturnWithDynamicArrayOfBytes(
            org.web3j.abi.datatypes.Function function, Class<T> returnType) {
        return new RemoteFunctionCall<>(
                function, () ->
                executeCallSingleValueReturnWithDynamicArrayOfBytes(function, returnType));
    }

    // Added to work around Web3j's inability to decode DynamicArrays properly
    protected <T extends Type, R> R executeCallSingleValueReturnWithDynamicArrayOfBytes(
            org.web3j.abi.datatypes.Function function, Class<R> returnType) throws IOException {
        T result = executeCallSingleValueReturnWithDynamicArrayOfBytes(function);
        if (result == null) {
            throw new ContractCallException("Empty value (0x) returned from contract");
        }

        Object value = result.getValue();
        if (returnType.isAssignableFrom(result.getClass())) {
            return (R) result;
        } else if (returnType.isAssignableFrom(value.getClass())) {
            return (R) value;
        } else if (result.getClass().equals(Address.class) && returnType.equals(String.class)) {
            return (R) result.toString(); // cast isn't necessary
        } else {
            throw new ContractCallException(
                    "Unable to convert response: "
                            + value
                            + " to expected type: "
                            + returnType.getSimpleName());
        }
    }

    // Added to work around Web3j's inability to decode DynamicArrays properly
    protected <T extends Type> T executeCallSingleValueReturnWithDynamicArrayOfBytes(
            org.web3j.abi.datatypes.Function function
    )
            throws IOException {
        List<Type> values = executeCallForDynamicArrayOfBytes(function);
        if (!values.isEmpty()) {
            return (T) values.get(0);
        } else {
            return null;
        }
    }

    // Added to work around Web3j's inability to decode DynamicArrays properly
    private List<Type> executeCallForDynamicArrayOfBytes(
            org.web3j.abi.datatypes.Function function
    ) throws IOException {
        String encodedFunction = FunctionEncoder.encode(function);

        String value = call(contractAddress, encodedFunction, defaultBlockParameter);
        // Instead of the default function return decoder, we use our modified one
        return FunctionReturnDecoderForDynamicArrayOfBytes
                .decode(value, function.getOutputParameters());
    }

    public RemoteFunctionCall<BigInteger> getServiceFeeRate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETSERVICEFEERATE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
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
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Swap>() {}));
        return executeRemoteCallSingleValueReturn(function, Swap.class);
    }

    public RemoteFunctionCall<BigInteger> minimumDisputePeriod() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_MINIMUMDISPUTEPERIOD,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> openOffer(byte[] offerID, Offer newOffer) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_OPENOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID),
                        newOffer),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> primaryTimelock() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PRIMARYTIMELOCK,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> proposeResolution(byte[] swapID, BigInteger makerPayout, BigInteger takerPayout, BigInteger confiscationPayout) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_PROPOSERESOLUTION,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID),
                        new org.web3j.abi.datatypes.generated.Uint256(makerPayout),
                        new org.web3j.abi.datatypes.generated.Uint256(takerPayout),
                        new org.web3j.abi.datatypes.generated.Uint256(confiscationPayout)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> protocolVersion() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_PROTOCOLVERSION,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> raiseDispute(byte[] swapID, String disputeAgent0, String disputeAgent1, String disputeAgent2) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_RAISEDISPUTE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID),
                        new org.web3j.abi.datatypes.Address(160, disputeAgent0),
                        new org.web3j.abi.datatypes.Address(160, disputeAgent1),
                        new org.web3j.abi.datatypes.Address(160, disputeAgent2)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> reactToResolutionProposal(byte[] swapID, BigInteger reaction) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REACTTORESOLUTIONPROPOSAL,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID),
                        new org.web3j.abi.datatypes.generated.Uint8(reaction)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> reportPaymentReceived(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTRECEIVED,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> reportPaymentSent(byte[] swapID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTSENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<BigInteger> serviceFeeRate() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_SERVICEFEERATE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setDisputeAgentActive(String disputeAgentAddress, Boolean setActive) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETDISPUTEAGENTACTIVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, disputeAgentAddress),
                        new org.web3j.abi.datatypes.Bool(setActive)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setMinimumDisputePeriod(BigInteger newMinimumDisputePeriod) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETMINIMUMDISPUTEPERIOD,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newMinimumDisputePeriod)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setServiceFeeRate(BigInteger newServiceFeeRate) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETSERVICEFEERATE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(newServiceFeeRate)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setSettlementMethodSupport(byte[] settlementMethod, Boolean support) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETSETTLEMENTMETHODSUPPORT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(settlementMethod),
                        new org.web3j.abi.datatypes.Bool(support)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setStablecoinSupport(String stablecoin, Boolean support) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETSTABLECOINSUPPORT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, stablecoin),
                        new org.web3j.abi.datatypes.Bool(support)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> takeOffer(byte[] offerID, Swap newSwap) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TAKEOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID),
                        newSwap),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
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

        public BigInteger serviceFeeRate;

        public BigInteger direction;

        public List<byte[]> settlementMethods;

        public BigInteger protocolVersion;

        public Offer(Boolean isCreated, Boolean isTaken, String maker, byte[] interfaceId, String stablecoin, BigInteger amountLowerBound, BigInteger amountUpperBound, BigInteger securityDepositAmount, BigInteger serviceFeeRate, BigInteger direction, List<byte[]> settlementMethods, BigInteger protocolVersion) {
            /*
            Generated by web3j-cli, but creates a "Cannot resolve constructor" error:
            super(new org.web3j.abi.datatypes.Bool(isCreated),new org.web3j.abi.datatypes.Bool(isTaken),new org.web3j.abi.datatypes.Address(maker),new org.web3j.abi.datatypes.DynamicBytes(interfaceId),new org.web3j.abi.datatypes.Address(stablecoin),new org.web3j.abi.datatypes.generated.Uint256(amountLowerBound),new org.web3j.abi.datatypes.generated.Uint256(amountUpperBound),new org.web3j.abi.datatypes.generated.Uint256(securityDepositAmount),new org.web3j.abi.datatypes.generated.Uint256(serviceFeeRate),new org.web3j.abi.datatypes.generated.Uint8(direction),new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.DynamicBytes>(settlementMethods),new org.web3j.abi.datatypes.generated.Uint256(protocolVersion));
             */
            super(new org.web3j.abi.datatypes.Bool(isCreated),new org.web3j.abi.datatypes.Bool(isTaken),new org.web3j.abi.datatypes.Address(maker),new org.web3j.abi.datatypes.DynamicBytes(interfaceId),new org.web3j.abi.datatypes.Address(stablecoin),new org.web3j.abi.datatypes.generated.Uint256(amountLowerBound),new org.web3j.abi.datatypes.generated.Uint256(amountUpperBound),new org.web3j.abi.datatypes.generated.Uint256(securityDepositAmount),new org.web3j.abi.datatypes.generated.Uint256(serviceFeeRate),new org.web3j.abi.datatypes.generated.Uint8(direction),new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.DynamicBytes>(buildDynamicBytesArrList(settlementMethods)),new org.web3j.abi.datatypes.generated.Uint256(protocolVersion));
            this.isCreated = isCreated;
            this.isTaken = isTaken;
            this.maker = maker;
            this.interfaceId = interfaceId;
            this.stablecoin = stablecoin;
            this.amountLowerBound = amountLowerBound;
            this.amountUpperBound = amountUpperBound;
            this.securityDepositAmount = securityDepositAmount;
            this.serviceFeeRate = serviceFeeRate;
            this.direction = direction;
            this.settlementMethods = settlementMethods;
            this.protocolVersion = protocolVersion;
        }

        public Offer(Bool isCreated, Bool isTaken, Address maker, DynamicBytes interfaceId, Address stablecoin, Uint256 amountLowerBound, Uint256 amountUpperBound, Uint256 securityDepositAmount, Uint256 serviceFeeRate, Uint8 direction, DynamicArray<DynamicBytes> settlementMethods, Uint256 protocolVersion) {
            super(isCreated,isTaken,maker,interfaceId,stablecoin,amountLowerBound,amountUpperBound,securityDepositAmount,serviceFeeRate,direction,settlementMethods,protocolVersion);
            this.isCreated = isCreated.getValue();
            this.isTaken = isTaken.getValue();
            this.maker = maker.getValue();
            this.interfaceId = interfaceId.getValue();
            this.stablecoin = stablecoin.getValue();
            this.amountLowerBound = amountLowerBound.getValue();
            this.amountUpperBound = amountUpperBound.getValue();
            this.securityDepositAmount = securityDepositAmount.getValue();
            this.serviceFeeRate = serviceFeeRate.getValue();
            this.direction = direction.getValue();
            this.settlementMethods = buildByteArrList(settlementMethods);
            this.protocolVersion = protocolVersion.getValue();
        }
    }

    public static class Dispute extends StaticStruct {
        public BigInteger disputeRaisedBlockNum;

        public String disputeAgent0;

        public String disputeAgent1;

        public String disputeAgent2;

        public Boolean hasDA0Proposed;

        public BigInteger dA0MakerPayout;

        public BigInteger dA0TakerPayout;

        public BigInteger dA0ConfiscationPayout;

        public Boolean hasDA1Proposed;

        public BigInteger dA1MakerPayout;

        public BigInteger dA1TakerPayout;

        public BigInteger dA1ConfiscationPayout;

        public Boolean hasDA2Proposed;

        public BigInteger dA2MakerPayout;

        public BigInteger dA2TakerPayout;

        public BigInteger dA2ConfiscationPayout;

        public BigInteger matchingProposals;

        public BigInteger makerReaction;

        public BigInteger takerReaction;

        public BigInteger state;

        public Boolean hasMakerPaidOut;

        public Boolean hasTakerPaidOut;

        public BigInteger totalWithoutSpentServiceFees;

        public Dispute(BigInteger disputeRaisedBlockNum, String disputeAgent0, String disputeAgent1, String disputeAgent2, Boolean hasDA0Proposed, BigInteger dA0MakerPayout, BigInteger dA0TakerPayout, BigInteger dA0ConfiscationPayout, Boolean hasDA1Proposed, BigInteger dA1MakerPayout, BigInteger dA1TakerPayout, BigInteger dA1ConfiscationPayout, Boolean hasDA2Proposed, BigInteger dA2MakerPayout, BigInteger dA2TakerPayout, BigInteger dA2ConfiscationPayout, BigInteger matchingProposals, BigInteger makerReaction, BigInteger takerReaction, BigInteger state, Boolean hasMakerPaidOut, Boolean hasTakerPaidOut, BigInteger totalWithoutSpentServiceFees) {
            super(new org.web3j.abi.datatypes.generated.Uint256(disputeRaisedBlockNum),new org.web3j.abi.datatypes.Address(disputeAgent0),new org.web3j.abi.datatypes.Address(disputeAgent1),new org.web3j.abi.datatypes.Address(disputeAgent2),new org.web3j.abi.datatypes.Bool(hasDA0Proposed),new org.web3j.abi.datatypes.generated.Uint256(dA0MakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA0TakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA0ConfiscationPayout),new org.web3j.abi.datatypes.Bool(hasDA1Proposed),new org.web3j.abi.datatypes.generated.Uint256(dA1MakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA1TakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA1ConfiscationPayout),new org.web3j.abi.datatypes.Bool(hasDA2Proposed),new org.web3j.abi.datatypes.generated.Uint256(dA2MakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA2TakerPayout),new org.web3j.abi.datatypes.generated.Uint256(dA2ConfiscationPayout),new org.web3j.abi.datatypes.generated.Uint8(matchingProposals),new org.web3j.abi.datatypes.generated.Uint8(makerReaction),new org.web3j.abi.datatypes.generated.Uint8(takerReaction),new org.web3j.abi.datatypes.generated.Uint8(state),new org.web3j.abi.datatypes.Bool(hasMakerPaidOut),new org.web3j.abi.datatypes.Bool(hasTakerPaidOut),new org.web3j.abi.datatypes.generated.Uint256(totalWithoutSpentServiceFees));
            this.disputeRaisedBlockNum = disputeRaisedBlockNum;
            this.disputeAgent0 = disputeAgent0;
            this.disputeAgent1 = disputeAgent1;
            this.disputeAgent2 = disputeAgent2;
            this.hasDA0Proposed = hasDA0Proposed;
            this.dA0MakerPayout = dA0MakerPayout;
            this.dA0TakerPayout = dA0TakerPayout;
            this.dA0ConfiscationPayout = dA0ConfiscationPayout;
            this.hasDA1Proposed = hasDA1Proposed;
            this.dA1MakerPayout = dA1MakerPayout;
            this.dA1TakerPayout = dA1TakerPayout;
            this.dA1ConfiscationPayout = dA1ConfiscationPayout;
            this.hasDA2Proposed = hasDA2Proposed;
            this.dA2MakerPayout = dA2MakerPayout;
            this.dA2TakerPayout = dA2TakerPayout;
            this.dA2ConfiscationPayout = dA2ConfiscationPayout;
            this.matchingProposals = matchingProposals;
            this.makerReaction = makerReaction;
            this.takerReaction = takerReaction;
            this.state = state;
            this.hasMakerPaidOut = hasMakerPaidOut;
            this.hasTakerPaidOut = hasTakerPaidOut;
            this.totalWithoutSpentServiceFees = totalWithoutSpentServiceFees;
        }

        public Dispute(Uint256 disputeRaisedBlockNum, Address disputeAgent0, Address disputeAgent1, Address disputeAgent2, Bool hasDA0Proposed, Uint256 dA0MakerPayout, Uint256 dA0TakerPayout, Uint256 dA0ConfiscationPayout, Bool hasDA1Proposed, Uint256 dA1MakerPayout, Uint256 dA1TakerPayout, Uint256 dA1ConfiscationPayout, Bool hasDA2Proposed, Uint256 dA2MakerPayout, Uint256 dA2TakerPayout, Uint256 dA2ConfiscationPayout, Uint8 matchingProposals, Uint8 makerReaction, Uint8 takerReaction, Uint8 state, Bool hasMakerPaidOut, Bool hasTakerPaidOut, Uint256 totalWithoutSpentServiceFees) {
            super(disputeRaisedBlockNum,disputeAgent0,disputeAgent1,disputeAgent2,hasDA0Proposed,dA0MakerPayout,dA0TakerPayout,dA0ConfiscationPayout,hasDA1Proposed,dA1MakerPayout,dA1TakerPayout,dA1ConfiscationPayout,hasDA2Proposed,dA2MakerPayout,dA2TakerPayout,dA2ConfiscationPayout,matchingProposals,makerReaction,takerReaction,state,hasMakerPaidOut,hasTakerPaidOut,totalWithoutSpentServiceFees);
            this.disputeRaisedBlockNum = disputeRaisedBlockNum.getValue();
            this.disputeAgent0 = disputeAgent0.getValue();
            this.disputeAgent1 = disputeAgent1.getValue();
            this.disputeAgent2 = disputeAgent2.getValue();
            this.hasDA0Proposed = hasDA0Proposed.getValue();
            this.dA0MakerPayout = dA0MakerPayout.getValue();
            this.dA0TakerPayout = dA0TakerPayout.getValue();
            this.dA0ConfiscationPayout = dA0ConfiscationPayout.getValue();
            this.hasDA1Proposed = hasDA1Proposed.getValue();
            this.dA1MakerPayout = dA1MakerPayout.getValue();
            this.dA1TakerPayout = dA1TakerPayout.getValue();
            this.dA1ConfiscationPayout = dA1ConfiscationPayout.getValue();
            this.hasDA2Proposed = hasDA2Proposed.getValue();
            this.dA2MakerPayout = dA2MakerPayout.getValue();
            this.dA2TakerPayout = dA2TakerPayout.getValue();
            this.dA2ConfiscationPayout = dA2ConfiscationPayout.getValue();
            this.matchingProposals = matchingProposals.getValue();
            this.makerReaction = makerReaction.getValue();
            this.takerReaction = takerReaction.getValue();
            this.state = state.getValue();
            this.hasMakerPaidOut = hasMakerPaidOut.getValue();
            this.hasTakerPaidOut = hasTakerPaidOut.getValue();
            this.totalWithoutSpentServiceFees = totalWithoutSpentServiceFees.getValue();
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

        public BigInteger serviceFeeRate;

        public BigInteger direction;

        public byte[] settlementMethod;

        public BigInteger protocolVersion;

        public Boolean isPaymentSent;

        public Boolean isPaymentReceived;

        public Boolean hasBuyerClosed;

        public Boolean hasSellerClosed;

        public BigInteger disputeRaiser;

        public Swap(Boolean isCreated, Boolean requiresFill, String maker, byte[] makerInterfaceId, String taker, byte[] takerInterfaceId, String stablecoin, BigInteger amountLowerBound, BigInteger amountUpperBound, BigInteger securityDepositAmount, BigInteger takenSwapAmount, BigInteger serviceFeeAmount, BigInteger serviceFeeRate, BigInteger direction, byte[] settlementMethod, BigInteger protocolVersion, Boolean isPaymentSent, Boolean isPaymentReceived, Boolean hasBuyerClosed, Boolean hasSellerClosed, BigInteger disputeRaiser) {
            super(new org.web3j.abi.datatypes.Bool(isCreated),new org.web3j.abi.datatypes.Bool(requiresFill),new org.web3j.abi.datatypes.Address(maker),new org.web3j.abi.datatypes.DynamicBytes(makerInterfaceId),new org.web3j.abi.datatypes.Address(taker),new org.web3j.abi.datatypes.DynamicBytes(takerInterfaceId),new org.web3j.abi.datatypes.Address(stablecoin),new org.web3j.abi.datatypes.generated.Uint256(amountLowerBound),new org.web3j.abi.datatypes.generated.Uint256(amountUpperBound),new org.web3j.abi.datatypes.generated.Uint256(securityDepositAmount),new org.web3j.abi.datatypes.generated.Uint256(takenSwapAmount),new org.web3j.abi.datatypes.generated.Uint256(serviceFeeAmount),new org.web3j.abi.datatypes.generated.Uint256(serviceFeeRate),new org.web3j.abi.datatypes.generated.Uint8(direction),new org.web3j.abi.datatypes.DynamicBytes(settlementMethod),new org.web3j.abi.datatypes.generated.Uint256(protocolVersion),new org.web3j.abi.datatypes.Bool(isPaymentSent),new org.web3j.abi.datatypes.Bool(isPaymentReceived),new org.web3j.abi.datatypes.Bool(hasBuyerClosed),new org.web3j.abi.datatypes.Bool(hasSellerClosed),new org.web3j.abi.datatypes.generated.Uint8(disputeRaiser));
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
            this.serviceFeeRate = serviceFeeRate;
            this.direction = direction;
            this.settlementMethod = settlementMethod;
            this.protocolVersion = protocolVersion;
            this.isPaymentSent = isPaymentSent;
            this.isPaymentReceived = isPaymentReceived;
            this.hasBuyerClosed = hasBuyerClosed;
            this.hasSellerClosed = hasSellerClosed;
            this.disputeRaiser = disputeRaiser;
        }

        public Swap(Bool isCreated, Bool requiresFill, Address maker, DynamicBytes makerInterfaceId, Address taker, DynamicBytes takerInterfaceId, Address stablecoin, Uint256 amountLowerBound, Uint256 amountUpperBound, Uint256 securityDepositAmount, Uint256 takenSwapAmount, Uint256 serviceFeeAmount, Uint256 serviceFeeRate, Uint8 direction, DynamicBytes settlementMethod, Uint256 protocolVersion, Bool isPaymentSent, Bool isPaymentReceived, Bool hasBuyerClosed, Bool hasSellerClosed, Uint8 disputeRaiser) {
            super(isCreated,requiresFill,maker,makerInterfaceId,taker,takerInterfaceId,stablecoin,amountLowerBound,amountUpperBound,securityDepositAmount,takenSwapAmount,serviceFeeAmount,serviceFeeRate,direction,settlementMethod,protocolVersion,isPaymentSent,isPaymentReceived,hasBuyerClosed,hasSellerClosed,disputeRaiser);
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
            this.serviceFeeRate = serviceFeeRate.getValue();
            this.direction = direction.getValue();
            this.settlementMethod = settlementMethod.getValue();
            this.protocolVersion = protocolVersion.getValue();
            this.isPaymentSent = isPaymentSent.getValue();
            this.isPaymentReceived = isPaymentReceived.getValue();
            this.hasBuyerClosed = hasBuyerClosed.getValue();
            this.hasSellerClosed = hasSellerClosed.getValue();
            this.disputeRaiser = disputeRaiser.getValue();
        }
    }

    public static class BuyerClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class DisputeEscalatedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public String escalator;

        public BigInteger reason;
    }

    public static class DisputeRaisedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public String disputeAgent0;

        public String disputeAgent1;

        public String disputeAgent2;
    }

    public static class DisputeResolutionTimelockChangedEventResponse extends BaseEventResponse {
        public String oldDisputeResolutionTimelock;

        public String newDisputeResolutionTimelock;
    }

    public static class DisputedSwapClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public String closer;
    }

    public static class EscalatedSwapClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public BigInteger makerPayout;

        public BigInteger takerPayout;

        public BigInteger confiscationPayout;
    }

    public static class MinimumDisputePeriodChangedEventResponse extends BaseEventResponse {
        public BigInteger newMinimumDisputePeriod;
    }

    public static class OfferCanceledEventResponse extends BaseEventResponse {
        public byte[] offerID;
    }

    public static class OfferEditedEventResponse extends BaseEventResponse {
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

    public static class PrimaryTimelockChangedEventResponse extends BaseEventResponse {
        public String oldPrimaryTimelock;

        public String newPrimaryTimelock;
    }

    public static class ReactionSubmittedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public String addr;

        public BigInteger reaction;
    }

    public static class ResolutionProposedEventResponse extends BaseEventResponse {
        public byte[] swapID;

        public String disputeAgent;
    }

    public static class SellerClosedEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }

    public static class ServiceFeeRateChangedEventResponse extends BaseEventResponse {
        public BigInteger newServiceFeeRate;
    }

    public static class SwapFilledEventResponse extends BaseEventResponse {
        public byte[] swapID;
    }
}
