package com.commuto.interfacedesktop.oldcontractwrapper;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.tx.RawTransactionManager;

import java.io.IOException;
import java.math.BigInteger;

public class CommutoTransactionManager extends RawTransactionManager {

    public CommutoTransactionManager(Web3j web3j, Credentials credentials, long chainId) {
        super(web3j, credentials, chainId);
    }

    public String getTransactionHash(BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger value, boolean constructor) throws IOException {
        BigInteger nonce = this.getNonce();
        RawTransaction rawTransaction = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data);
        String hexValue = this.sign(rawTransaction);
        return Hash.sha3(hexValue);
    }

}
