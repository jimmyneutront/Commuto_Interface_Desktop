package com.commuto.interfacedesktop.contractwrapper;

import static org.web3j.abi.DefaultFunctionReturnDecoder.getDataOffset;
import static com.commuto.interfacedesktop.contractwrapper.Utils.
        getParameterizedTypeFromArray;
import static org.web3j.abi.Utils.staticStructNestedPublicFieldsFlatList;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.Bytes;
import org.web3j.abi.datatypes.BytesType;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.utils.Numeric;
import org.web3j.utils.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
Added to work around Web3j's inability to decode DynamicArrays properly. The code in this class is
taken almost entirely from Web3j's DefaultFunctionReturnDecoder, and it is a temporary workaround.
 */
public class FunctionReturnDecoderForDynamicArrayOfBytes extends FunctionReturnDecoder {

    private static final FunctionReturnDecoderForDynamicArrayOfBytes decoder =
            new FunctionReturnDecoderForDynamicArrayOfBytes();

    static final int MAX_BYTE_LENGTH_FOR_HEX_STRING = Type.MAX_BYTE_LENGTH << 1;

    public static List<Type> decode(String rawInput, List<TypeReference<Type>> outputParameters) {
        return decoder.decodeFunctionResult(rawInput, outputParameters);
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

    @SuppressWarnings("unchecked")
    public <T extends Type> Type decodeEventParameter(
            String rawInput, TypeReference<T> typeReference) {

        String input = Numeric.cleanHexPrefix(rawInput);

        try {
            Class<T> type = typeReference.getClassType();

            if (Bytes.class.isAssignableFrom(type)) {
                Class<Bytes> bytesClass = (Class<Bytes>) Class.forName(type.getName());
                return TypeDecoder.decodeBytes(input, bytesClass);
            } else if (Array.class.isAssignableFrom(type)
                    || BytesType.class.isAssignableFrom(type)
                    || Utf8String.class.isAssignableFrom(type)) {
                return TypeDecoder.decodeBytes(input, Bytes32.class);
            } else {
                return TypeDecoder.decode(input, type);
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Invalid class reference provided", e);
        }
    }

    private static List<Type> build(String input, List<TypeReference<Type>> outputParameters) {
        List<Type> results = new ArrayList<>(outputParameters.size());

        int offset = 0;
        for (TypeReference<?> typeReference : outputParameters) {
            try {
                int hexStringDataOffset = getDataOffset(input, offset, typeReference);

                @SuppressWarnings("unchecked")
                Class<Type> classType = (Class<Type>) typeReference.getClassType();

                Type result;
                if (DynamicStruct.class.isAssignableFrom(classType)) {
                    result =
                            TypeDecoder.decodeDynamicStruct(
                                    input, hexStringDataOffset, typeReference);
                    offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;

                } else if (DynamicArray.class.isAssignableFrom(classType)) {
                    result =
                            TypeDecoder.decodeDynamicArray(
                                    input, hexStringDataOffset, typeReference);
                    offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;

                } else if (typeReference instanceof TypeReference.StaticArrayTypeReference) {
                    int length = ((TypeReference.StaticArrayTypeReference) typeReference).getSize();
                    result =
                            TypeDecoder.decodeStaticArray(
                                    input, hexStringDataOffset, typeReference, length);
                    offset += length * MAX_BYTE_LENGTH_FOR_HEX_STRING;

                } else if (StaticStruct.class.isAssignableFrom(classType)) {
                    result =
                            TypeDecoder.decodeStaticStruct(
                                    input, hexStringDataOffset, typeReference);
                    offset +=
                            staticStructNestedPublicFieldsFlatList(classType).size()
                                    * MAX_BYTE_LENGTH_FOR_HEX_STRING;
                } else if (StaticArray.class.isAssignableFrom(classType)) {
                    int length =
                            Integer.parseInt(
                                    classType
                                            .getSimpleName()
                                            .substring(StaticArray.class.getSimpleName().length()));
                    result =
                            TypeDecoder.decodeStaticArray(
                                    input, hexStringDataOffset, typeReference, length);
                    if (DynamicStruct.class.isAssignableFrom(
                            getParameterizedTypeFromArray(typeReference))) {
                        offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    } else if (StaticStruct.class.isAssignableFrom(
                            getParameterizedTypeFromArray(typeReference))) {
                        offset +=
                                staticStructNestedPublicFieldsFlatList(
                                        getParameterizedTypeFromArray(
                                                typeReference))
                                        .size()
                                        * length
                                        * MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    } else if (Utf8String.class.isAssignableFrom(
                            getParameterizedTypeFromArray(typeReference))) {
                        offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    } else {
                        offset += length * MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    }
                } else {
                    result = TypeDecoder.decode(input, hexStringDataOffset, classType);
                    offset += MAX_BYTE_LENGTH_FOR_HEX_STRING;
                }
                results.add(result);

            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("Invalid class reference provided", e);
            }
        }
        return results;
    }

}
