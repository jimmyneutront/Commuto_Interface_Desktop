package com.commuto.interfacedesktop.contractwrapper;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.spi.FunctionEncoderProvider;

import java.math.BigInteger;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.web3j.abi.Utils.staticStructNestedPublicFieldsFlatList;

public class CommutoFunctionEncoder extends FunctionEncoder {

    private static CommutoFunctionEncoder DEFAULT_ENCODER;

    private static final ServiceLoader<FunctionEncoderProvider> loader =
            ServiceLoader.load(FunctionEncoderProvider.class);

    public static String encode(final Function function) {
        return encoder().encodeFunction(function);
    }

    @Override
    protected String encodeFunction(Function function) {
        final List<Type> parameters = function.getInputParameters();

        final String methodSignature = buildMethodSignature(function.getName(), parameters);
        final String methodId = buildMethodId(methodSignature);

        final StringBuilder result = new StringBuilder();
        result.append(methodId);

        return encodeParameters(parameters, result);
    }

    @Override
    protected String encodeParameters(List<Type> parameters) {
        return encodeParameters(parameters, new StringBuilder());
    }

    private static String encodeParameters(
            final List<Type> parameters, final StringBuilder result) {

        int dynamicDataOffset = getLength(parameters) * Type.MAX_BYTE_LENGTH;
        final StringBuilder dynamicData = new StringBuilder();

        for (Type parameter : parameters) {
            final String encodedValue = TypeEncoder.encode(parameter);

            if (CommutoTypeEncoder.isDynamic(parameter)) {
                final String encodedDataOffset =
                        CommutoTypeEncoder.encodeNumeric(new Uint(BigInteger.valueOf(dynamicDataOffset)));
                result.append(encodedDataOffset);
                dynamicData.append(encodedValue);
                dynamicDataOffset += encodedValue.length() >> 1;
            } else {
                result.append(encodedValue);
            }
        }
        result.append(dynamicData);

        return result.toString();
    }

    /**
     * Required by [encodeParameters], copied from [DefaultFunctionEncoder]
     */
    private static int getLength(final List<Type> parameters) {
        int count = 0;
        for (final Type type : parameters) {
            if (type instanceof StaticArray
                    && StaticStruct.class.isAssignableFrom(
                    ((StaticArray) type).getComponentType())) {
                count +=
                        staticStructNestedPublicFieldsFlatList(
                                ((StaticArray) type).getComponentType())
                                .size()
                                * ((StaticArray) type).getValue().size();
            } else if (type instanceof StaticArray
                    && DynamicStruct.class.isAssignableFrom(
                    ((StaticArray) type).getComponentType())) {
                count++;
            } else if (type instanceof StaticArray) {
                count += ((StaticArray) type).getValue().size();
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Required because the default method signature builder method doesn't work properly when the parameters
     * include arrays of arrays (For example, bytes[] incorrectly becomes dynamicarray). Mostly copied from
     * [DefaultFunctionEncoder].
     */
    protected static String buildMethodSignature(String methodName, List<Type> parameters) {
        if (methodName == "openOffer") {
            return "openOffer(bytes16,(bool,bool,address,bytes,address,uint256,uint256,uint256,uint256,uint8,bytes[],uint256))";
        } else {
            StringBuilder result = new StringBuilder();
            result.append(methodName);
            result.append("(");
            String params = (String)parameters.stream().map(Type::getTypeAsString).collect(Collectors.joining(","));
            result.append(params);
            result.append(")");
            return result.toString();
        }
    }

    private static CommutoFunctionEncoder encoder() {
        return defaultEncoder();
    }

    private static CommutoFunctionEncoder defaultEncoder() {
        if (DEFAULT_ENCODER == null) {
            DEFAULT_ENCODER = new CommutoFunctionEncoder();
        }
        return DEFAULT_ENCODER;
    }

}
