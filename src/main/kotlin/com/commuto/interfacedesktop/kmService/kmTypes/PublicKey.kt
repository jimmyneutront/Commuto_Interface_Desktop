package com.commuto.interfacedesktop.kmService.kmTypes

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
 * The PublicKey class is a wrapper around the java.security.PublicKey class, with support for Commuto Interface IDs and
 * several methods for encoding the wrapped public key. The wrapped public key shall be that corresponding to a 2048-bit
 * RSA private key, and interfaceId is the SHA-256 hash of the public key encoded in PKCS#1 byte format.
 *
 * @property publicKey the java.security.PublicKey object around which this class is wrapped.
 * @property interfaceId the interface id derived from the public key
 */
class PublicKey {

    /**
     * Creates a PublicKey object using the PKCS#1 byte format of an RSA public key
     *
     * @param publicKeyBytes: the PKCS#1 byte encoded representation of the public key to be restored
     */
    constructor(publicKeyBytes: ByteArray) {
        val algorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        val pubKeyX509Bytes = SubjectPublicKeyInfo(algorithmIdentifier, publicKeyBytes).encoded
        val publicKey: JavaSecPublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(pubKeyX509Bytes))
        this.publicKey = publicKey
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(toPkcs1Bytes())
    }

    /**
     * Creates a PublicKey object using an RSA JavaSecPublicKey
     *
     * @param publicKey: the JavaSecPublicKey to be wrapped in a PublicKey
     */
    constructor(publicKey: JavaSecPublicKey) {
        this.publicKey = publicKey
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(toPkcs1Bytes())
    }

    val interfaceId: ByteArray
    val publicKey: JavaSecPublicKey

    /**
     * Verifies a signature using this PublicKey's public key
     *
     * @param signedData: the data that was signed
     * @param signature: the signature
     *
     * @return Boolean: indicating the success of the verification
     */
    fun verifySignature(signedData: ByteArray, signature: ByteArray): Boolean {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initVerify(this.publicKey)
        signatureObj.update(signedData)
        return signatureObj.verify(signature)
    }

    /**
     * Encrypt the passed data using this KeyPair's RSA public key, using OEAP SHA-256 padding.
     *
     * @param clearData: the data to be encrypted
     * @return ByteArray: the encrypted data
     */
    fun encrypt(clearData: ByteArray): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)
        encryptCipher.init(Cipher.ENCRYPT_MODE, this.publicKey, oaepParams)
        return encryptCipher.doFinal(clearData)
    }

    /**
     * Encodes the RSA PublicKey to a PKCS1 formatted byte representation
     *
     * @return a ByteArray containing the PKCS1 representation of the PublicKey object
     */
    fun toPkcs1Bytes(): ByteArray {
        val pubKeyX509Bytes = this.publicKey.encoded
        val sPubKeyInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pubKeyX509Bytes)
        val pubKeyPrimitive: ASN1Primitive = sPubKeyInfo.parsePublicKey()
        val pubKeyPkcs1Bytes: ByteArray = pubKeyPrimitive.encoded
        return pubKeyPkcs1Bytes
    }
}