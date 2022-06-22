package com.commuto.interfacedesktop.keymanager.types

import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.PublicKey as JavaSecPublicKey
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * This is a wrapper around the [JavaSecPublicKey] class with added support for Commuto Interface
 * IDs and several methods for encoding the wrapped public key. The wrapped key is the public key of
 * a 2048-bit RSA private key, and the interface ID of a public key is the SHA-256 hash of its
 * PKCS#1 formatted byte representation.
 *
 * @property interfaceId The interface ID of this [PublicKey], which is the SHA-256 hash of its
 * PKCS#1 byte representation.
 * @property publicKey the [JavaSecPublicKey] that this class wraps.
 */
class PublicKey {

    /**
     * Creates a [PublicKey] using the PKCS#1-formatted byte representation of an RSA public key.
     *
     * @param publicKeyBytes The PKCS#1 bytes of the public key to be created, as a [ByteArray].
     */
    constructor(publicKeyBytes: ByteArray) {
        val algorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
        val pubKeyX509Bytes = SubjectPublicKeyInfo(algorithmIdentifier, publicKeyBytes).encoded
        val publicKey: JavaSecPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(pubKeyX509Bytes))
        this.publicKey = publicKey
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(toPkcs1Bytes())
    }

    /**
     * Creates a [PublicKey] to wrap an RSA [JavaSecPublicKey].
     *
     * @param publicKey The [JavaSecPublicKey] to be wrapped in a [PublicKey].
     */
    constructor(publicKey: JavaSecPublicKey) {
        this.publicKey = publicKey
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(toPkcs1Bytes())
    }

    val interfaceId: ByteArray
    val publicKey: JavaSecPublicKey

    /**
     * Verifies a signature using this [PublicKey].
     *
     * @param signedData The [ByteArray] that has been signed.
     * @param signature The signature of [signedData].
     *
     * @return A [Boolean] indicating whether or not verification was successful.
     */
    fun verifySignature(signedData: ByteArray, signature: ByteArray): Boolean {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initVerify(this.publicKey)
        signatureObj.update(signedData)
        return signatureObj.verify(signature)
    }

    /**
     * Encrypts the given bytes using this [PublicKey], using OEAP SHA-256 padding.
     *
     * @param clearData The bytes to be encrypted, as a [ByteArray].
     *
     * @return [clearData] encrypted with this [PublicKey].
     */
    fun encrypt(clearData: ByteArray): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec("SHA-256"),
            PSource.PSpecified.DEFAULT
        )
        encryptCipher.init(Cipher.ENCRYPT_MODE, this.publicKey, oaepParams)
        return encryptCipher.doFinal(clearData)
    }

    /**
     * Encodes this [PublicKey] to its PKCS#1-formatted byte representation.
     *
     * @return The PKCS#1 byte representation of this [PublicKey], as a [ByteArray].
     */
    fun toPkcs1Bytes(): ByteArray {
        val pubKeyX509Bytes = this.publicKey.encoded
        val sPubKeyInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pubKeyX509Bytes)
        val pubKeyPrimitive: ASN1Primitive = sPubKeyInfo.parsePublicKey()
        val pubKeyPkcs1Bytes: ByteArray = pubKeyPrimitive.encoded
        return pubKeyPkcs1Bytes
    }
}