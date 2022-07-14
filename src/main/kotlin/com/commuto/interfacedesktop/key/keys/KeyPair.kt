package com.commuto.interfacedesktop.key.keys

import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.security.*
import java.security.PublicKey
import java.security.KeyPair as JavaSecKeyPair
import java.security.spec.MGF1ParameterSpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * This is a wrapper around [JavaSecKeyPair] class with added support for Commuto Interface IDs and
 * several methods for encoding the wrapped public and private key. The wrapped keys are a 2048-bit
 * RSA private key and its corresponding public key, and the interface ID of a key pair is the
 * SHA-256 hash of the public key encoded in PKCS#1 byte format.
 *
 * @property interfaceId The interface ID of this [KeyPair], which is the SHA-256 hash of the public
 * key encoded in PKCS#1 byte format.
 * @property keyPair The [JavaSecKeyPair] that this class wraps.
 */
class KeyPair {

    /**
     * Creates a [KeyPair] wrapped around a new 2058-bit RSA private key and its public key
     */
    constructor() {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        this.keyPair = keyPairGenerator.generateKeyPair()
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(pubKeyToPkcs1Bytes())
    }

    /**
     * Creates a [KeyPair] using the PKCS#1-formatted byte representation of a 2048-bit RSA private
     * key and its public key.
     *
     * @param publicKeyBytes The PKCS#1 bytes of the public key of the key pair, as a [ByteArray].
     * @param privateKeyBytes: The PKCS#1 bytes of the private key of the key pair, as a
     * [ByteArray].
     */
    constructor(publicKeyBytes: ByteArray, privateKeyBytes: ByteArray) {
        //Restore public key
        val algorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
        val pubKeyX509Bytes = SubjectPublicKeyInfo(algorithmIdentifier, publicKeyBytes).encoded
        val publicKey: PublicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(pubKeyX509Bytes))

        //Restore private key
        val rsaPrivKey: RSAPrivateKey = RSAPrivateKey.getInstance(ASN1Sequence.fromByteArray(privateKeyBytes))
        val privKeySpec: RSAPrivateKeySpec = RSAPrivateCrtKeySpec(rsaPrivKey.modulus,
            rsaPrivKey.publicExponent,
            rsaPrivKey.privateExponent,
            rsaPrivKey.prime1,
            rsaPrivKey.prime2,
            rsaPrivKey.exponent1,
            rsaPrivKey.exponent2,
            rsaPrivKey.coefficient
        )
        val privateKey: PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(privKeySpec)

        this.keyPair = JavaSecKeyPair(publicKey, privateKey)
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(pubKeyToPkcs1Bytes())
    }

    /**
     * Creates a [KeyPair] to wrap an RSA [JavaSecKeyPair]
     *
     * @param keyPair The [JavaSecKeyPair] to be wrapped in a [KeyPair].
     */
    constructor(keyPair: JavaSecKeyPair) {
        this.keyPair = keyPair
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(pubKeyToPkcs1Bytes())
    }

    val interfaceId: ByteArray
    val keyPair: JavaSecKeyPair

    /**
     * Signs a [ByteArray] using this [KeyPair]'s private key.
     *
     * @param data The [ByteArray] to be signed.
     *
     * @return The signature, as a [ByteArray].
     */
    fun sign(data: ByteArray): ByteArray {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initSign(this.keyPair.private)
        signatureObj.update(data)
        return signatureObj.sign()
    }

    /**
     * Verifies a signature using this [KeyPair]'s public key.
     *
     * @param signedData The [ByteArray] that has been signed.
     * @param signature The signature of [signedData].
     *
     * @return A [Boolean] indicating whether or not verification was successful.
     */
    fun verifySignature(signedData: ByteArray, signature: ByteArray): Boolean {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initVerify(this.keyPair.public)
        signatureObj.update(signedData)
        return signatureObj.verify(signature)
    }

    /**
     * Encrypts the given bytes using this [KeyPair]'s public key, using OEAP SHA-256 padding.
     *
     * @param clearData The bytes to be encrypted, as a [ByteArray].
     *
     * @return [clearData] encrypted with this [KeyPair]'s public key.
     */
    fun encrypt(clearData: ByteArray): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)
        encryptCipher.init(Cipher.ENCRYPT_MODE, this.keyPair.public, oaepParams)
        return encryptCipher.doFinal(clearData)
    }

    /**
     * Decrypts the given bytes using this [KeyPair]'s private key, using OEAP SHA-256 padding.
     *
     * @param cipherData The bytes to be decrypted, as a [ByteArray].
     *
     * @return [cipherData] decrypted with this [KeyPair]'s private key.
     */
    fun decrypt(cipherData: ByteArray): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)
        encryptCipher.init(Cipher.DECRYPT_MODE, this.keyPair.private, oaepParams)
        return encryptCipher.doFinal(cipherData)
    }

    /**
     * Encodes this [KeyPair]'s RSA public key to its PKCS#1-formatted byte representation.
     *
     * @return The PKCS#1 byte representation of this [KeyPair]'s public key, as a [ByteArray].
     */
    fun pubKeyToPkcs1Bytes(): ByteArray {
        val pubKeyX509Bytes = this.keyPair.public.encoded
        val sPubKeyInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pubKeyX509Bytes)
        val pubKeyPrimitive: ASN1Primitive = sPubKeyInfo.parsePublicKey()
        val pubKeyPkcs1Bytes: ByteArray = pubKeyPrimitive.encoded
        return pubKeyPkcs1Bytes
    }

    /**
     * Encodes this [KeyPair]'s 2048-bit RSA private key to its PKCS#1-formatted byte
     * representation.
     *
     * @return The PKCS#1 byte representation of this [KeyPair]'s private key, as a [ByteArray].
     */
    fun privKeyToPkcs1Bytes(): ByteArray {
        val privKeyInfo = PrivateKeyInfo.getInstance(this.keyPair.private.encoded)
        val privKeyAsn1Encodable: ASN1Encodable = privKeyInfo.parsePrivateKey()
        val privKeyAsn1Primitive: ASN1Primitive = privKeyAsn1Encodable.toASN1Primitive()
        val privKeyPkcs1Bytes: ByteArray = privKeyAsn1Primitive.getEncoded()
        return privKeyPkcs1Bytes
    }
}