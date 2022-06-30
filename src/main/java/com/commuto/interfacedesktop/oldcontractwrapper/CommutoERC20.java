package com.commuto.interfacedesktop.oldcontractwrapper;

import kotlin.Pair;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.contracts.eip20.generated.ERC20;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CommutoERC20 extends ERC20 {

    private CommutoTransactionManager transactionManager;

    protected CommutoERC20(String contractAddress, Web3j web3j, CommutoTransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(contractAddress, web3j, transactionManager, contractGasProvider);
        this.transactionManager = transactionManager;
    }

    public Pair<String, CompletableFuture<TransactionReceipt>> approveAndGetTXID(String _spender, BigInteger _value) throws IOException {
        final Function function = new Function(
                FUNC_APPROVE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_spender),
                        new org.web3j.abi.datatypes.generated.Uint256(_value)),
                Collections.<TypeReference<?>>emptyList());
        String txHash = transactionManager.getTransactionHash(this.gasProvider.getGasPrice(function.getName()), this.gasProvider.getGasLimit(function.getName()), this.contractAddress, CommutoFunctionEncoder.encode(function), BigInteger.ZERO, false);
        return new Pair<>(txHash, executeRemoteCallTransaction(function).sendAsync());
    }

    public static CommutoERC20 load(String contractAddress, Web3j web3j, CommutoTransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CommutoERC20(contractAddress, web3j, transactionManager, contractGasProvider);
    }

}
