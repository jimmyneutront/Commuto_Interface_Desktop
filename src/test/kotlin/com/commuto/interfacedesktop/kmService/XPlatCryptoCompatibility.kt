package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.db.DatabaseDriverFactory
import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.kmTypes.KeyPair
import java.security.KeyPair as JavaSecKeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
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
        val pubKey: PublicKey = kmService.pubKeyFromPkcs1Bytes(decoder.decode(pubKeyB64))
        val privKey: PrivateKey = kmService.privKeyFromPkcs1Bytes(decoder.decode(privKeyB64))
    }
}