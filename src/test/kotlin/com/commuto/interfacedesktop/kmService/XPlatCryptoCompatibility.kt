package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.kmTypes.*
import java.nio.charset.Charset
import java.security.KeyPair as JavaSecKeyPair
import java.security.PrivateKey
import java.security.PublicKey as JavaSecPubKey
import java.util.Arrays
import java.util.Base64
import kotlin.test.Test

/**
 * Prints keys in B64 String format for compatibility testing, and tests the re-creation of key objects given keys in
 * B64 String format saved on other platforms.
 */
class XPlatCryptoCompatibility {

    private var driver = DatabaseDriverFactory()
    private var dbService = DBService(driver)
    private val kmService = KMService(dbService)

    /**
     * Prints a key, encrypted data, and an initialization vector in B64 format
     */
    @Test
    fun testPrintSymmetricallyEncryptedMsg() {
        val key = newSymmetricKey()
        val encoder = Base64.getEncoder()
        println("Key B64:\n" + encoder.encodeToString(key.keyBytes))
        val encryptedData: SymmetricallyEncryptedData = key.encrypt("test".toByteArray())
        println("Encrypted Data B64:\n" + encoder.encodeToString(encryptedData.encryptedData))
        println("Initialization Vector B64:\n" + encoder.encodeToString(encryptedData.initializationVector))
    }

    /**
     * Decrypts encrypted data given a key, encrypted data, and an initialization vector, all in B64 format.
     */
    @Test
    fun testSymmetricDecryption() {
        val keyB64 = "9u0BOAMTiKXHV0BAX1snHZsy9fOmfkDIQ9sIGwTiYDE="
        val encryptedDataB64 = "yS1fv7cQpuxwUEX8YgXuMw=="
        val initializationVectorB64 = "nsz76c4yhx/boD1c4KnbYw=="
        val decoder = Base64.getDecoder()
        val keyData = decoder.decode(keyB64)
        val encryptedData = decoder.decode(encryptedDataB64)
        val initializationVector = decoder.decode(initializationVectorB64)
        val key = SymmetricKey(keyData)
        val symmEncryptedData = SymmetricallyEncryptedData(encryptedData, initializationVector)
        assert(Arrays.equals("test".toByteArray(), key.decrypt(symmEncryptedData)))
    }

    /**
     * Prints a signature, original message and public key in Base64 format
     */
    @Test
    fun testPrintSignature() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val encoder = Base64.getEncoder()
        println("Signed Data B64:\n" + encoder.encodeToString("test".toByteArray(Charset.forName("UTF-16"))))
        println("Public Key B64:")
        println(encoder.encodeToString(kmService
            .pubKeyToPkcs1Bytes(keyPair.keyPair.public)))
        val signature = keyPair.sign("test".toByteArray(Charset.forName("UTF-16")))
        println("Signature:\n" + encoder.encodeToString(signature))
    }

    /**
     * Verifies a signature using an original message and public key, all given in Base64 format
     */
    @Test
    fun testVerifySignature() {
        val decoder = Base64.getDecoder()
        var signedData = decoder.decode("//50AGUAcwB0AA==")
        val pubKeyB64 = "MIIBCgKCAQEAnvuNlBeukUvDiF76DoNoNuhV8llOq6R2fdECyNCC1MrDmXZlShyn2NrPq6/W0tJi3PsSNopSv6He12d7Le4I0/umKpxJxEg5iJ4zm3Xthd6EaFkwPyqa+7E0ZeFOo0wcDThdRZ/5UzQQmnBwua8xCFZrxQVLY8h/eoucQQ6MJknF6iAk4fFSic5Pa8SeIJeQBjO+dmje/+POCmlMBFmhLBVJA5QG+eI7WEa274On7bxIHeMK0zx673kfLADwBTHHS10oKDaGi5Pn2hGUf4Ksihv6yI+GDJJI0uemESi1J95e2NXXlPHqqteJKUzEAENh0QXHg97XJhhJtNV7gh/60wIDAQAB"
        var signatureB64 = "D74DCCZFlsjRifmyKmGYo7qpYyxoGOEgNJ8+f0E+lg7DEI5TLFurk1Z+bwlS8tj9M6FxNjJED6iVMiHGGJREmFYrgvIJZbl+bgurDcHL2hO2xR6GOoOoBpmAoi7UiPeC5eGN2+lK9IiNKWx3XbSsGiBW0ysuZzcPyVmDE2jK1j3ZXi26BMFvkXBCQbmrQaJ9ApxKF6hkZtT4eKBvcu0K6kNAYTRrA90af5yt0TFf2aPhp9XFMlYBF1nSjG4CBQrrXIR/qK7MC+VfUJ4tvRjF0W7pwCHjIUxczfUtx1uldY7aMnYmpcfEuKi4O/QGEMWMr/JGEgpWnKl2zCjTqNYXzw=="
        val pubKey: JavaSecPubKey = kmService
            .pubKeyFromPkcs1Bytes(decoder.decode(pubKeyB64))
        val wrappedPubKey = PublicKey(pubKey)
        var signature = decoder.decode(signatureB64)
        assert(wrappedPubKey.verifySignature(signedData, signature))
    }

    /**
     * Prints keys according to KMService's specification in B64 format, as well as an original
     * message string and the encrypted message, also in B64 format, for cross platform
     * compatibility testing
     */
    @Test
    fun testPrintEncryptedMessage() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val encoder = Base64.getEncoder()
        println("Public Key B64:")
        println(encoder.encodeToString(kmService
            .pubKeyToPkcs1Bytes(keyPair.keyPair.public)))
        println("Private Key B64:")
        println(encoder.encodeToString(kmService
            .privKeyToPkcs1Bytes(keyPair.keyPair.private)))
        val originalMessage = "test"
        println("Original Message:\n" + originalMessage)
        println("Encrypted Message:")
        println(encoder.encodeToString(keyPair.encrypt(originalMessage
            .toByteArray(Charset.forName("UTF-16")))))
    }

    /**
     * Uses the given public and private key strings to restore a KeyPair object and decrypt the
     * given message, ensuring it matches the original message.
     */
    @Test
    fun testDecryptMessage() {
        val originalMessage = "test"
        val pubKeyB64 = "MIIBCgKCAQEAuK0QR2ducqtMCWFYcK92lIJu/WPu4BYbbBGeMoGK17C+unRHEmOlfdvaM5JpiZ7RBuxd3IlG9RbuCxZ/pTYqG4PEVDCWTBftrCNnV+ogauKQfcsDlj/NaCEKUrzHUCIdENdEXUw6QWkpOcS7wejlBP4Im2ez4HDkcDIJZjXxhY7CCoNCE4YOdvelJEwN0/+2MIXNNwcpuUng4Sbv9z054+7nvF1139XDsyaZa5U2a6L5TmdY5RE1+qnP9zmAZcwLx4JlnekEpYs+wlpPP139NCLm9bIqoMbbvAVH71gApjylna0HKpCN/NYHHM+lXQC/FCBeng/xIMLMUDTFMu0LqQIDAQAB"
        val privKeyB64 = "MIIEogIBAAKCAQEAuK0QR2ducqtMCWFYcK92lIJu/WPu4BYbbBGeMoGK17C+unRHEmOlfdvaM5JpiZ7RBuxd3IlG9RbuCxZ/pTYqG4PEVDCWTBftrCNnV+ogauKQfcsDlj/NaCEKUrzHUCIdENdEXUw6QWkpOcS7wejlBP4Im2ez4HDkcDIJZjXxhY7CCoNCE4YOdvelJEwN0/+2MIXNNwcpuUng4Sbv9z054+7nvF1139XDsyaZa5U2a6L5TmdY5RE1+qnP9zmAZcwLx4JlnekEpYs+wlpPP139NCLm9bIqoMbbvAVH71gApjylna0HKpCN/NYHHM+lXQC/FCBeng/xIMLMUDTFMu0LqQIDAQABAoIBABZrBtH+La+MmQo9944VVLoeM9I9dHjeIiXOJTO5G0VH6r1IYnzKXSNwQfaW+EoJHhN+s72y6erUZeDxch0YfEfTCO0V6VbTJEpMUW4gCR+kgXkNwRGXDOyuLxpAWZsiCwB5e05uk5dPaF5fghHd31hJ7LpNvk2ZcremcVLnqzoktyjnQaHumbQcAzyiCjEkWSNkRPXKUNYJTvGo+8Z1xIwadiscUmc/5rxTSoTRn3h0Mt59PcqzEuxR3YURJijCsLneCl6oeoyEHaGXvzkarmg35m0rrdfZ79js2z1Fty5VRc3OSsf6FOTBElPxmjBNACo+YiFvr9bDOfSoJSbvpsECgYEA+I4iLORgsGnlxj3HaCxbqG+lJ+2LbvHzUySdGTbcpROTKF160oWKd4+6YmNForS8iPt5VxS+ow+20vj/dJRJU34Q1FLcVO2g44mGFznSPCxe7vz3S40IG95tM5J3/9OOj2i94T1A6A6symYJ45Dtn8NWgK9X4FcqhzTSoUkoMMECgYEAvjUeXx7IpAU8RVP00qqtJ73tmSZaA7lZDmEJhnOtr0wDEirnwQfylv+UO+NMCDbWHScQF1XEXXsTTPVz4j0YoLjq3FrsJbR9vsNyHddZRg4v8mslLj5Hs+llomwzk4b0Fi4b90mlz2v1wcmglzMyVzCPazKmT269olwmwctXrOkCgYA7VcroxpgiZRVaLtNUlgpHemeF0ZpQoOfCeGIca9Fegv7FSxOQABsfEauf4yzze4vqc4Xy+NvNl2nAkXqCPQgIK3cfCKzahWO8Dc956e67OKhtCuyKF5/Q71dIUXXeF4XXzFxP0cyV2TL8mkFQFv/y/LHAxJsIziYz4rNJl2pFwQKBgFDa3b1LpbjrrNI/vTvsZ80UFLNctTkOCkhtgZIRDI0O/+MyL/BDg6Eipg3LMp/vR5d+6n0w7VdboTm+wXMzy4tO8C+ZyvbAQg/cn18GEyIPl9wyJc0BlpNpLNYdrtMQtCPVl/fH698/ommtX0HG9qhPsTe9gSsVBTHGgIcy/GM5AoGARGacIww9Cf5m+QfFdOeuc6q0Hz9faYPZQmCGSfzt7GtziaNGQJFwowVORuSNnXzo4bIreuu15NlhnCCu1JLVNKKXn8AttEtJHWPNmhAwdxCqXofvppsVmhelCsypEs8vxSE5hTh1DTRcf7iIzpsroGNmc75/qIVBBFGlCa96awQ="
        val encryptedMessageB64 = "kFIGAM64UGug6uDPtVAX0+rOS2FoYQ69VLqfNkO0j6+QfOV8iw5u9aCDSHw3zw04fw7yAw7tiTgc0S1WuSZYUJNJJiotXlFHU/03oW5n369Q34k7I32tu1IA5a9DJp5rhOasaLcZuY/cNsBV509mmVl4A5VOxMYZloMHWCK1fsganrp9sdEeAbqfBm31gwcD0OSsASSHB4OvGzwXKGkl9Z4TYkoBgZ399EtRaKYLBL3m9TTwKlDoIdj1MjmldfEgAJc5vAmWophTZY+Q8GZ2HzS0srr7cvqto4lehyYRzQ5uPZSBuhs8FRx5I0dihFzU6BgzSZPtztzrhzQlhFkeZQ=="
        val decoder = Base64.getDecoder()
        val pubKey: JavaSecPubKey = kmService
            .pubKeyFromPkcs1Bytes(decoder.decode(pubKeyB64))
        val privKey: PrivateKey = kmService
            .privKeyFromPkcs1Bytes(decoder.decode(privKeyB64))
        val javaSecKeyPair = JavaSecKeyPair(pubKey, privKey)
        val keyPair = KeyPair(javaSecKeyPair)
        val encryptedMessageBytes = decoder.decode(encryptedMessageB64)
        val decryptedMessageBytes = keyPair.decrypt(encryptedMessageBytes)
        val decryptedMessage = String(decryptedMessageBytes, Charset.forName("UTF-16"))
        assert(originalMessage.equals(decryptedMessage))
    }

    /**
     * Prints keys according to KMService's specification in B64 format, so that they can be pasted into
     * testRestoreRSAKeysFromB64() on other platforms, to ensure that keys saved on any platform can be read on any
     * other.
     */
    @Test
    fun testGenB64RSAKeys() {
        val keyPair: KeyPair = kmService.generateKeyPair(storeResult = false)
        val encoder = Base64.getEncoder()
        println("Public Key B64:")
        println(encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()))
        println("Private Key B64:")
        println(encoder.encodeToString(keyPair.privKeyToPkcs1Bytes()))
    }

    /**
     * Attempts to restore keys according to KMService's specification given in B64 format, to ensure that keys saved on
     * other platforms can be restored to key objects on this platform.
     */
    @Test
    fun testRestoreRSAKeysFromB64() {
        val pubKeyB64 = "MIIBCgKCAQEA6nRs5aMeFg2TIxbhH6MMh946mZ35sP5d27J+Fm1ha1IZlz9+Z7Knw7FOJ827BhLLcaeHCSI3/opnWc1TKPD6exArNZQsJLSWXJeLfBOm/zseKJyCr9PHahhQYBD3X6HfuZYqzUV4B/BZSu5cJbcvgqTsSbnR7tlRscwW32m1l/jWixLrsmE3zBg0SWBDtZ8ouOMJJ5ZSS6MXJeQUfzyrEIhc8cABpI8MFsVL82UYPdXxGvQ86AUnNMnEDGWtGvUvYqzcWCuLNpcSkx5y9+VKdnC7zevLJbuj0fyjjBadAkMc6jNcgeaGJSsWxbtKC11CawMWPuXuckzIRsAJUYFnhQIDAQAB"
        val privKeyB64 = "MIIEogIBAAKCAQEA6nRs5aMeFg2TIxbhH6MMh946mZ35sP5d27J+Fm1ha1IZlz9+Z7Knw7FOJ827BhLLcaeHCSI3/opnWc1TKPD6exArNZQsJLSWXJeLfBOm/zseKJyCr9PHahhQYBD3X6HfuZYqzUV4B/BZSu5cJbcvgqTsSbnR7tlRscwW32m1l/jWixLrsmE3zBg0SWBDtZ8ouOMJJ5ZSS6MXJeQUfzyrEIhc8cABpI8MFsVL82UYPdXxGvQ86AUnNMnEDGWtGvUvYqzcWCuLNpcSkx5y9+VKdnC7zevLJbuj0fyjjBadAkMc6jNcgeaGJSsWxbtKC11CawMWPuXuckzIRsAJUYFnhQIDAQABAoIBAA9uB5pU/J58w2LzDHBUVEy0eFH9UVOPvgC+3mjz8bjKKlh89iugGJs6q0HX2o0zsJ4Y0CcupcA+VBRsfKKsZKkI30sd/AoTX3TrB/lBDYPCcdbq4U1DMwkdXgbLKbiVEuqoF8YyzPY6yr7xcRuN/YC2gWgjDvj45j/nsQFVjT0EeWednEDhqPlo1FWMFrH0woLOTRVRhzemlrMxsxhFxc/Tvr4kJtnmacOT9+am82Fn5eOelg+Dafms6G6z+8r/EQmaiLUeXwzQYinQWpLL5jEbVEbRz4fZhBXaz++D8uK9lzF/N1Quv31V5OtHLtBAGe17OibarP9bPwLbg2knilkCgYEA9R8cYI8E6umr2TEyCUNxfpFv1XkdyTTZNv0H/ZIRh5qUVg84r/VCUguPaIq4nbmOngskgpaiwx1kYGAmJCabHV3L20yEztLAOtltBQFoTodaz6Apkws4gyharg7dQ1Z75RqZTfdHSiKZwO0Ihu4EkTEPTdY6/+wzZnt31Kzf2O0CgYEA9Nwgfra6QHw1AN5QkfYUxHqRKuPWrJoYwupwl3g+VGW7NfbrCOS/VcezdBzIgvuz5AOIEQxkNPf61kjKS6/t+/V2XE76qGV5tQ5B0Mdgm8eViAT6IoTNl2YPFZhgJizEMJ5ybGWC2Q+8hnPOBmvh1u+pQ/mG8nWGXAF9qHVE7fkCgYBUbmjp4ZmCCQcGgumHQ1HelN3+m/9khO2lATc1YpDjMp2RnyCZi1NSy2SUT+QTgAzd51ymFpjtuDwQ7k10+k9HqD1Fxm+ghftsyePBa6CwG/NtvO9VFPJcSxQhDEGupiV63tSbhGdr48suJvde8rFkCZAJ8ZbU/FkgHbtC6GEaaQKBgB2z7kUwyVs1NgDK9x8dqNtEuwNm7A24C7TpV4soTPdT9+fN8ij8BrHTLdOyAijRe7r3KrRWunkqc8U2w0N3LflYh2kfM4zl8mOiPR2kcfWzulHruKQjVAU/nijSeSdoWsxDDEJV9g96tzXgKmfhAl5eaDwUsugKlafnjmS3BQuRAoGAZp6D6LIndvr6t4025GgepDYXjcFU/irPHi6WR3LOimJouQHIB56gWRxEVmiS4/k02GmgZIpAtEpHEHcz6svCzgI8ZKzk5+P91wqNpO8TfS1O2Nl9RO+/zBxIAnbkf6TxCbo5JhVtClWCFLIXOqzE7baQOi4aMjXk1rzOQNeu9vQ="
        val decoder = Base64.getDecoder()
        val pubKey: JavaSecPubKey = kmService.pubKeyFromPkcs1Bytes(decoder.decode(pubKeyB64))
        val privKey: PrivateKey = kmService.privKeyFromPkcs1Bytes(decoder.decode(privKeyB64))
    }
}