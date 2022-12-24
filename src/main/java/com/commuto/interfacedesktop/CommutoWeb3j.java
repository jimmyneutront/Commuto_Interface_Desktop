package com.commuto.interfacedesktop;

import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.JsonRpc2_0Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthFeeHistory;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * A Web3jService implementation that extends JsonRpc2_0Web3j but overrides JsonRpc2_0Web3j.ethFeeHistory in
 * order to serialize the newest block value correctly as specified
 * <a href="https://ethereum.github.io/execution-apis/api-documentation/">here</a>
 */
public class CommutoWeb3j extends JsonRpc2_0Web3j {
    public CommutoWeb3j(Web3jService web3jService) {
        super(web3jService);
    }

    /**
     * Overrides JsonRpc2_0Web3j's default ethFeeHistory method, and correctly serializes the newest block value.
     */
    @Override
    public org.web3j.protocol.core.Request<?, EthFeeHistory> ethFeeHistory(
            int blockCount,
            DefaultBlockParameter newestBlock,
            List<Double> rewardPercentiles
    ) {
        return new Request<>(
            "eth_feeHistory",
            Arrays.asList(
                    Numeric.encodeQuantity(BigInteger.valueOf(blockCount)),
                    newestBlock.getValue(),
                    rewardPercentiles
            ),
            this.web3jService,
            EthFeeHistory.class
        );
    }
}
