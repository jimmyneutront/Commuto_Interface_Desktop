package com.commuto.interfacedesktop.oldcontractwrapper;

import kotlin.Pair;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.exceptions.ContractCallException;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.RevertReasonExtractor;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
A subclass of CommutoSwap with write function wrappers that return a pair containing the transaction hash and a
CompletableFuture that will return the TransactionReceipt.
Note that all of these functions quickly block the calling thread in order to get the caller's account nonce.
Note that there is a short time interval between the calculation of the transaction hash and the actual construction
and sending of the transaction. If another transaction is sent from the same account during this short interval, the
nonce of the transaction built for hash calculation and that of the actual transaction broadcast to the network will be
different.
 */
public class WorkingCommutoSwap extends CommutoSwap {

    private CommutoTransactionManager transactionManager;

    protected WorkingCommutoSwap(String contractAddress, Web3j web3j, CommutoTransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(contractAddress, web3j, transactionManager, contractGasProvider);
        this.transactionManager = transactionManager;
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> openOfferAndGetTXID(byte[] offerID, Offer newOffer) throws IOException {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_OPENOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID),
                        newOffer),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, CommutoFunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    @Override
    public RemoteFunctionCall<Offer> getOffer(byte[] offerID) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Offer>() {}));
        return executeRemoteCallSingleValueReturn(function, Offer.class);
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> takeOfferAndGetTXID(byte[] offerID, Swap newSwap) throws IOException{
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_TAKEOFFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(offerID),
                        newSwap),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> fillSwapAndGetTXID(byte[] swapID) throws IOException {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_FILLSWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> reportPaymentSentAndGetTXID(byte[] swapID) throws IOException {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTSENT,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> reportPaymentReceivedAndGetTXID(byte[] swapID) throws IOException {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_REPORTPAYMENTRECEIVED,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> closeSwapAndGetTXID(byte[] swapID) throws IOException {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CLOSESWAP,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes16(swapID)),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, FunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public static WorkingCommutoSwap load(String contractAddress, Web3j web3j, CommutoTransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new WorkingCommutoSwap(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    protected <T> RemoteFunctionCall<T> executeRemoteCallSingleValueReturn(Function function, Class<T> returnType) {
        return new RemoteFunctionCall(function, () -> {
            return this.executeCallSingleValueReturn(function, returnType);
        });
    }

    protected <T extends Type, R> R executeCallSingleValueReturn(Function function, Class<R> returnType) throws IOException {
        T result = this.executeCallSingleValueReturn(function);
        if (result == null) {
            throw new ContractCallException("Empty value (0x) returned from contract");
        } else {
            Object value = result.getValue();
            if (returnType.isAssignableFrom(result.getClass())) {
                return (R)result;
            } else if (returnType.isAssignableFrom(value.getClass())) {
                return (R)value;
            } else if (result.getClass().equals(Address.class) && returnType.equals(String.class)) {
                return (R)result.toString();
            } else {
                throw new ContractCallException("Unable to convert response: " + value + " to expected type: " + returnType.getSimpleName());
            }
        }
    }

    protected <T extends Type> T executeCallSingleValueReturn(Function function) throws IOException {
        List<Type> values = this.executeCall(function);
        return !values.isEmpty() ? (T)values.get(0) : null;
    }

    private List<Type> executeCall(Function function) throws IOException {
        String encodedFunction = FunctionEncoder.encode(function);
        String value = this.call(this.contractAddress, encodedFunction, this.defaultBlockParameter);
        return CommutoFunctionReturnDecoder.decode(value, function.getOutputParameters());
    }

    protected RemoteFunctionCall<TransactionReceipt> executeRemoteCallTransaction(Function function) {
        return new RemoteFunctionCall(function, () -> {
            return this.executeTransaction(function);
        });
    }

    protected TransactionReceipt executeTransaction(Function function) throws IOException, TransactionException {
        return this.executeTransaction(function, BigInteger.ZERO);
    }

    private TransactionReceipt executeTransaction(Function function, BigInteger weiValue) throws IOException, TransactionException {
        return this.executeTransaction(CommutoFunctionEncoder.encode(function), weiValue, function.getName());
    }

    TransactionReceipt executeTransaction(String data, BigInteger weiValue, String funcName) throws TransactionException, IOException {
        return this.executeTransaction(data, weiValue, funcName, false);
    }

    TransactionReceipt executeTransaction(String data, BigInteger weiValue, String funcName, boolean constructor) throws TransactionException, IOException {
        TransactionReceipt receipt = this.send(this.contractAddress, data, weiValue, this.gasProvider.getGasPrice(funcName), this.gasProvider.getGasLimit(funcName), constructor);
        if (!receipt.isStatusOK()) {
            throw new TransactionException(String.format("Transaction %s has failed with status: %s. Gas used: %s. Revert reason: '%s'.", receipt.getTransactionHash(), receipt.getStatus(), receipt.getGasUsedRaw() != null ? receipt.getGasUsed().toString() : "unknown", RevertReasonExtractor.extractRevertReason(receipt, data, this.web3j, true)), receipt);
        } else {
            return receipt;
        }
    }
}
