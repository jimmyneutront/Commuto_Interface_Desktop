package com.commuto.interfacedesktop.oldcontractwrapper;

import org.web3j.abi.DefaultFunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.utils.Numeric;
import org.web3j.utils.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommutoFunctionReturnDecoder extends DefaultFunctionReturnDecoder {

    static final int MAX_BYTE_LENGTH_FOR_HEX_STRING = Type.MAX_BYTE_LENGTH << 1;

    /**
     * Decode ABI encoded return values from smart contract function call.
     *
     * @param rawInput ABI encoded input
     * @param outputParameters list of return types as {@link TypeReference}
     * @return {@link List} of values returned by function, {@link Collections#emptyList()} if
     *     invalid response
     */
    public static List<Type> decode(String rawInput, List<TypeReference<Type>> outputParameters) {
        return decoder().decodeFunctionResult(rawInput, outputParameters);
    }

    private static CommutoFunctionReturnDecoder decoder() {
        return defaultDecoder();
    }

    private static CommutoFunctionReturnDecoder defaultDecoder() {
        return new CommutoFunctionReturnDecoder();
    }

    public List<Type> decodeFunctionResult(
            String rawInput, List<TypeReference<Type>> outputParameters) {

        String input = Numeric.cleanHexPrefix(rawInput);

        if (Strings.isEmpty(input)) {
            return Collections.emptyList();
        } else {
            return build(input, outputParameters);
        }
    }

    private static List<Type> build(String input, List<TypeReference<Type>> outputParameters) {
        List<Type> results = new ArrayList<>(outputParameters.size());
        try {
            if (outputParameters.size() == 1 && DynamicStruct.class.isAssignableFrom((Class<Type>) outputParameters.get(0).getClassType())) {
                int offset = 0;
                int hexStringDataOffset = getDataOffset(input, offset, outputParameters.get(0));
                Type result = CommutoTypeDecoder.decodeDynamicStruct(input, hexStringDataOffset, outputParameters.get(0));
                offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;
                results.add(result);
                return results;
            } else {
                return new DefaultFunctionReturnDecoder().decodeFunctionResult(input, outputParameters);
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Invalid class reference provided", e);
        }
    }

}
