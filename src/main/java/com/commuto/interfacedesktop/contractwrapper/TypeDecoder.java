package com.commuto.interfacedesktop.contractwrapper;

import static org.web3j.abi.DefaultFunctionReturnDecoder.getDataOffset;
import static org.web3j.abi.Utils.staticStructNestedPublicFieldsFlatList;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.AbiTypes;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Array;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Bytes;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Fixed;
import org.web3j.abi.datatypes.FixedPointType;
import org.web3j.abi.datatypes.Int;
import org.web3j.abi.datatypes.IntType;
import org.web3j.abi.datatypes.NumericType;
import org.web3j.abi.datatypes.StaticArray;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.StructType;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Ufixed;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint160;
import org.web3j.utils.Numeric;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/*
Added to work around Web3j's inability to decode DynamicArrays properly. The code in this class is
taken almost entirely from Web3j's TypeDecoder, and it is a temporary workaround.
 */
public class TypeDecoder extends org.web3j.abi.TypeDecoder {
    static final int MAX_BYTE_LENGTH_FOR_HEX_STRING = Type.MAX_BYTE_LENGTH << 1;

    static <T extends Type> T decode(String input, int offset, Class<T> type) {
        if (NumericType.class.isAssignableFrom(type)) {
            return (T) decodeNumeric(input.substring(offset), (Class<NumericType>) type);
        } else if (Address.class.isAssignableFrom(type)) {
            return (T) decodeAddress(input.substring(offset));
        } else if (Bool.class.isAssignableFrom(type)) {
            return (T) decodeBool(input, offset);
        } else if (Bytes.class.isAssignableFrom(type)) {
            return (T) decodeBytes(input, offset, (Class<Bytes>) type);
        } else if (DynamicBytes.class.isAssignableFrom(type)) {
            return (T) decodeDynamicBytes(input, offset);
        } else if (Utf8String.class.isAssignableFrom(type)) {
            return (T) decodeUtf8String(input, offset);
        } else if (Array.class.isAssignableFrom(type)) {
            throw new UnsupportedOperationException(
                    "Array types must be wrapped in a TypeReference");
        } else {
            throw new UnsupportedOperationException("Type cannot be encoded: " + type.getClass());
        }
    }

    static <T extends Type> T decode(String input, Class<T> type) {
        return decode(input, 0, type);
    }

    static Address decodeAddress(String input) {
        return new Address(decodeNumeric(input, Uint160.class));
    }

    static <T extends NumericType> T decodeNumeric(String input, Class<T> type) {
        try {
            byte[] inputByteArray = Numeric.hexStringToByteArray(input);
            int typeLengthAsBytes = getTypeLengthInBytes(type);

            byte[] resultByteArray = new byte[typeLengthAsBytes + 1];

            if (Int.class.isAssignableFrom(type) || Fixed.class.isAssignableFrom(type)) {
                resultByteArray[0] = inputByteArray[0]; // take MSB as sign bit
            }

            int valueOffset = Type.MAX_BYTE_LENGTH - typeLengthAsBytes;
            System.arraycopy(inputByteArray, valueOffset, resultByteArray, 1, typeLengthAsBytes);

            BigInteger numericValue = new BigInteger(resultByteArray);
            return type.getConstructor(BigInteger.class).newInstance(numericValue);

        } catch (NoSuchMethodException
                 | SecurityException
                 | InstantiationException
                 | IllegalAccessException
                 | IllegalArgumentException
                 | InvocationTargetException e) {
            throw new UnsupportedOperationException(
                    "Unable to create instance of " + type.getName(), e);
        }
    }

    static <T extends NumericType> int getTypeLengthInBytes(Class<T> type) {
        return getTypeLength(type) >> 3; // divide by 8
    }

    static <T extends NumericType> int getTypeLength(Class<T> type) {
        if (IntType.class.isAssignableFrom(type)) {
            String regex = "(" + Uint.class.getSimpleName() + "|" + Int.class.getSimpleName() + ")";
            String[] splitName = type.getSimpleName().split(regex);
            if (splitName.length == 2) {
                return Integer.parseInt(splitName[1]);
            }
        } else if (FixedPointType.class.isAssignableFrom(type)) {
            String regex =
                    "(" + Ufixed.class.getSimpleName() + "|" + Fixed.class.getSimpleName() + ")";
            String[] splitName = type.getSimpleName().split(regex);
            if (splitName.length == 2) {
                String[] bitsCounts = splitName[1].split("x");
                return Integer.parseInt(bitsCounts[0]) + Integer.parseInt(bitsCounts[1]);
            }
        }
        return Type.MAX_BIT_LENGTH;
    }

    static int decodeUintAsInt(String rawInput, int offset) {
        String input = rawInput.substring(offset, offset + MAX_BYTE_LENGTH_FOR_HEX_STRING);
        return decode(input, 0, Uint.class).getValue().intValue();
    }

    static Bool decodeBool(String rawInput, int offset) {
        String input = rawInput.substring(offset, offset + MAX_BYTE_LENGTH_FOR_HEX_STRING);
        BigInteger numericValue = Numeric.toBigInt(input);
        boolean value = numericValue.equals(BigInteger.ONE);
        return new Bool(value);
    }

    static <T extends Bytes> T decodeBytes(String input, Class<T> type) {
        return decodeBytes(input, 0, type);
    }

    static <T extends Bytes> T decodeBytes(String input, int offset, Class<T> type) {
        try {
            String simpleName = type.getSimpleName();
            String[] splitName = simpleName.split(Bytes.class.getSimpleName());
            int length = Integer.parseInt(splitName[1]);
            int hexStringLength = length << 1;

            byte[] bytes =
                    Numeric.hexStringToByteArray(input.substring(offset, offset + hexStringLength));
            return type.getConstructor(byte[].class).newInstance(bytes);
        } catch (NoSuchMethodException
                 | SecurityException
                 | InstantiationException
                 | IllegalAccessException
                 | IllegalArgumentException
                 | InvocationTargetException e) {
            throw new UnsupportedOperationException(
                    "Unable to create instance of " + type.getName(), e);
        }
    }

    static DynamicBytes decodeDynamicBytes(String input, int offset) {
        int encodedLength = decodeUintAsInt(input, offset);
        int hexStringEncodedLength = encodedLength << 1;

        int valueOffset = offset + MAX_BYTE_LENGTH_FOR_HEX_STRING;

        String data = input.substring(valueOffset, valueOffset + hexStringEncodedLength);
        byte[] bytes = Numeric.hexStringToByteArray(data);

        return new DynamicBytes(bytes);
    }

    static <T extends Type> T decodeDynamicStruct(
            String input, int offset, TypeReference<T> typeReference) {

        BiFunction<List<T>, String, T> function =
                (elements, typeName) -> {
                    if (elements.isEmpty()) {
                        throw new UnsupportedOperationException(
                                "Zero length fixed array is invalid type");
                    } else {
                        return instantiateStruct(typeReference, elements);
                    }
                };

        return decodeDynamicStructElements(input, offset, typeReference, function);
    }

    static Utf8String decodeUtf8String(String input, int offset) {
        DynamicBytes dynamicBytesResult = decodeDynamicBytes(input, offset);
        byte[] bytes = dynamicBytesResult.getValue();

        return new Utf8String(new String(bytes, StandardCharsets.UTF_8));
    }

    static <T extends Type> T decodeStaticArray(
            String input, int offset, TypeReference<T> typeReference, int length) {

        BiFunction<List<T>, String, T> function =
                (elements, typeName) -> {
                    if (elements.isEmpty()) {
                        throw new UnsupportedOperationException(
                                "Zero length fixed array is invalid type");
                    } else {
                        return instantiateStaticArray(elements, length);
                    }
                };

        return decodeArrayElements(input, offset, typeReference, length, function);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Type> T instantiateStruct(
            final TypeReference<T> typeReference, final List<T> parameters) {
        try {
            Constructor ctor =
                    Arrays.stream(typeReference.getClassType().getDeclaredConstructors())
                            .filter(
                                    declaredConstructor ->
                                            Arrays.stream(declaredConstructor.getParameterTypes())
                                                    .allMatch(Type.class::isAssignableFrom))
                            .findAny()
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "TypeReference struct must contain a constructor with types that extend Type"));
            ;
            ctor.setAccessible(true);
            return (T) ctor.newInstance(parameters.toArray());
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(
                    "Constructor cannot accept" + Arrays.toString(parameters.toArray()), e);
        }
    }

    private static <T extends Type> T instantiateStaticArray(List<T> elements, int length) {
        try {
            Class<? extends StaticArray> arrayClass =
                    (Class<? extends StaticArray>)
                            Class.forName("org.web3j.abi.datatypes.generated.StaticArray" + length);
            return (T) arrayClass.getConstructor(List.class).newInstance(elements);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Type> int getSingleElementLength(String input, int offset, Class<T> type) {
        if (input.length() == offset) {
            return 0;
        } else if (DynamicBytes.class.isAssignableFrom(type)
                || Utf8String.class.isAssignableFrom(type)) {
            // length field + data value
            return (decodeUintAsInt(input, offset) / Type.MAX_BYTE_LENGTH) + 2;
        } else if (StaticStruct.class.isAssignableFrom(type)) {
            return staticStructNestedPublicFieldsFlatList((Class<Type>) type).size();
        } else {
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends Type> T decodeDynamicArray(
            String input, int offset, TypeReference<T> typeReference) {

        int length = decodeUintAsInt(input, offset);

        BiFunction<List<T>, String, T> function =
                (elements, typeName) -> (T) new DynamicArray(AbiTypes.getType(typeName), elements);

        int valueOffset = offset + MAX_BYTE_LENGTH_FOR_HEX_STRING;

        return decodeArrayElements(input, valueOffset, typeReference, length, function);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Type> T decodeDynamicStructElements(
            final String input,
            final int offset,
            final TypeReference<T> typeReference,
            final BiFunction<List<T>, String, T> consumer) {
        try {
            final Class<T> classType = typeReference.getClassType();
            Constructor<?> constructor =
                    Arrays.stream(classType.getDeclaredConstructors())
                            .filter(
                                    declaredConstructor ->
                                            Arrays.stream(declaredConstructor.getParameterTypes())
                                                    .allMatch(Type.class::isAssignableFrom))
                            .findAny()
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "TypeReferenced struct must contain a constructor with types that extend Type"));
            final int length = constructor.getParameterCount();
            final Map<Integer, T> parameters = new HashMap<>();
            int staticOffset = 0;
            final List<Integer> parameterOffsets = new ArrayList<>();
            for (int i = 0; i < length; ++i) {
                final Class<T> declaredField = (Class<T>) constructor.getParameterTypes()[i];
                final T value;
                final int beginIndex = offset + staticOffset;
                if (isDynamic(declaredField)) {
                    final boolean isOnlyParameterInStruct = length == 1;
                    final int parameterOffset =
                            isOnlyParameterInStruct
                                    ? offset
                                    : (decodeDynamicStructDynamicParameterOffset(
                                    input.substring(beginIndex, beginIndex + 64)))
                                    + offset;
                    parameterOffsets.add(parameterOffset);
                    staticOffset += 64;
                } else {
                    if (StaticStruct.class.isAssignableFrom(declaredField)) {
                        value =
                                decodeStaticStruct(
                                        input.substring(beginIndex),
                                        0,
                                        TypeReference.create(declaredField));
                        staticOffset +=
                                staticStructNestedPublicFieldsFlatList((Class<Type>) classType)
                                        .size()
                                        * MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    } else {
                        value = decode(input.substring(beginIndex), 0, declaredField);
                        staticOffset += value.bytes32PaddedLength() * 2;
                    }
                    parameters.put(i, value);
                }
            }
            int dynamicParametersProcessed = 0;
            int dynamicParametersToProcess =
                    getDynamicStructDynamicParametersCount(constructor.getParameterTypes());
            for (int i = 0; i < length; ++i) {
                final Class<T> declaredField = (Class<T>) constructor.getParameterTypes()[i];
                if (isDynamic(declaredField)) {
                    final boolean isLastParameterInStruct =
                            dynamicParametersProcessed == (dynamicParametersToProcess - 1);
                    final int parameterLength =
                            isLastParameterInStruct
                                    ? input.length()
                                    - parameterOffsets.get(dynamicParametersProcessed)
                                    : parameterOffsets.get(dynamicParametersProcessed + 1)
                                    - parameterOffsets.get(dynamicParametersProcessed);
                    /*
                    Here we assume all DynamicArrays contain DynamicBytes. Due to the implementation
                    of DynamicArrays and Java's type erasure behavior, we can't possibly know what
                    a DynamicArray should hold. This is a temporary workaround.
                     */
                    if (isDynamicArray(declaredField)) {
                        parameters.put(
                                i,
                                (T)
                                        decodeDynamicArray(
                                                input,
                                                parameterOffsets.get(dynamicParametersProcessed),
                                                TypeReference.makeTypeReference("bytes[]")));
                    } else {
                        parameters.put(
                                i,
                                decodeDynamicParameterFromStruct(
                                        input,
                                        parameterOffsets.get(dynamicParametersProcessed),
                                        parameterLength,
                                        declaredField));
                    }
                    dynamicParametersProcessed++;
                }
            }

            String typeName = Utils.getSimpleTypeName(classType);

            final List<T> elements = new ArrayList<>();
            for (int i = 0; i < length; ++i) {
                elements.add(parameters.get(i));
            }

            return consumer.apply(elements, typeName);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Unable to access parameterized type " + typeReference.getType().getTypeName(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Type> int getDynamicStructDynamicParametersCount(
            final Class<?>[] cls) {
        return (int) Arrays.stream(cls).filter(c -> isDynamic((Class<T>) c)).count();
    }

    private static <T extends Type> T decodeDynamicParameterFromStruct(
            final String input,
            final int parameterOffset,
            final int parameterLength,
            final Class<T> declaredField) {
        final String dynamicElementData =
                input.substring(parameterOffset, parameterOffset + parameterLength);

        final T value;
        if (DynamicStruct.class.isAssignableFrom(declaredField)) {
            value =
                    decodeDynamicStruct(
                            dynamicElementData, 64, TypeReference.create(declaredField));
        } else {
            value = decode(dynamicElementData, declaredField);
        }
        return value;
    }

    private static int decodeDynamicStructDynamicParameterOffset(final String input) {
        return (decodeUintAsInt(input, 0) * 2);
    }

    static <T extends Type> boolean isDynamic(Class<T> parameter) {
        return DynamicBytes.class.isAssignableFrom(parameter)
                || Utf8String.class.isAssignableFrom(parameter)
                || DynamicArray.class.isAssignableFrom(parameter);
    }

    static <T extends Type> boolean isDynamicArray(Class<T> parameter) {
        return DynamicArray.class == parameter;
    }

    private static <T extends Type> T decodeArrayElements(
            String input,
            int offset,
            TypeReference<T> typeReference,
            int length,
            BiFunction<List<T>, String, T> consumer) {

        try {
            Class<T> cls = Utils.getParameterizedTypeFromArray(typeReference);
            if (StructType.class.isAssignableFrom(cls)) {
                List<T> elements = new ArrayList<>(length);
                for (int i = 0, currOffset = offset;
                     i < length;
                     i++,
                             currOffset +=
                                     getSingleElementLength(input, currOffset, cls)
                                             * MAX_BYTE_LENGTH_FOR_HEX_STRING) {
                    T value;
                    if (DynamicStruct.class.isAssignableFrom(cls)) {
                        value =
                                TypeDecoder.decodeDynamicStruct(
                                        input,
                                        offset + getDataOffset(input, currOffset, typeReference),
                                        TypeReference.create(cls));
                    } else {
                        value =
                                org.web3j.abi.TypeDecoder.decodeStaticStruct(
                                        input, currOffset, TypeReference.create(cls));
                    }
                    elements.add(value);
                }

                String typeName = Utils.getSimpleTypeName(cls);

                return consumer.apply(elements, typeName);
            } else if (Array.class.isAssignableFrom(cls)) {
                throw new UnsupportedOperationException(
                        "Arrays of arrays are not currently supported for external functions, see"
                                + "http://solidity.readthedocs.io/en/develop/types.html#members");
            } else {
                List<T> elements = new ArrayList<>(length);
                int currOffset = offset;
                for (int i = 0; i < length; i++) {
                    T value;
                    if (isDynamic(cls)) {
                        int hexStringDataOffset = getDataOffset(input, currOffset, typeReference);
                        value = decode(input, offset + hexStringDataOffset, cls);
                        currOffset += MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    } else {
                        value = decode(input, currOffset, cls);
                        currOffset +=
                                getSingleElementLength(input, currOffset, cls)
                                        * MAX_BYTE_LENGTH_FOR_HEX_STRING;
                    }
                    elements.add(value);
                }

                String typeName = Utils.getSimpleTypeName(cls);

                return consumer.apply(elements, typeName);
            }
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException(
                    "Unable to access parameterized type " + typeReference.getType().getTypeName(),
                    e);
        }
    }
}
