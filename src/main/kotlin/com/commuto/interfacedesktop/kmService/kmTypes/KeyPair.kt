package com.commuto.interfacedesktop.kmService.kmTypes

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
 * The KeyPair class is a wrapper around the java.security.KeyPair class, with support for Commuto Interface IDs and
 * several methods for encoding the wrapped public and private key. The wrapped key pair shall be a 2048-bit RSA key
 * pair, and interfaceId is the SHA-256 hash of the key pair's public key encoded in PKCS#1 byte format.
 *
 * @property keyPair the java.security.KeyPair object around which this class is wrapped.
 * @property interfaceId the interface id derived from the key pair's public key
 */
class KeyPair {

    /**
     * Creates a KeyPair object using the PKCS#1 byte formats of a 2048-bit RSA private key and its public key
     *
     * @param publicKeyBytes: the PKCS#1 byte encoded representation of the public key to be restored
     * @param privateKeyBytes: the PKCS#1 byte encoded representation of the 2048-bit RSA private key to be restored
     */
    constructor(publicKeyBytes: ByteArray, privateKeyBytes: ByteArray) {
        //Restore public key
        val algorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
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
     * Creates a KeyPair object using an RSA JavaSecKeyPair
     *
     * @param keyPair: the JavaSecKeyPair to be wrapped in a KeyPair
     */
    constructor(keyPair: JavaSecKeyPair) {
        this.keyPair = keyPair
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(pubKeyToPkcs1Bytes())
    }

    val interfaceId: ByteArray
    val keyPair: JavaSecKeyPair

    /**
     * Create a signature from the passed ByteArray using this KeyPair's private key
     *
     * @param data: the data to be signed
     * @return ByteArray: the signature
     */
    fun sign(data: ByteArray): ByteArray {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initSign(this.keyPair.private)
        signatureObj.update(data)
        return signatureObj.sign()
    }

    /**
     * Verifies a signature using this KeyPair's public key
     *
     * @param signedData: the data that was signed
     * @param signature: the signature
     *
     * @return Boolean: indicating the success of the verification
     */
    fun verifySignature(signedData: ByteArray, signature: ByteArray): Boolean {
        val signatureObj = Signature.getInstance("SHA256withRSA")
        signatureObj.initVerify(this.keyPair.public)
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
        encryptCipher.init(Cipher.ENCRYPT_MODE, this.keyPair.public, oaepParams)
        return encryptCipher.doFinal(clearData)
    }

    /**
     * Decrypt the passed data using this KeyPair's RSA private key, using OEAP SHA-256 padding.
     *
     * @param cipherData: the data to be decrypted
     * @return ByteArray: the decrypted data
     */
    fun decrypt(cipherData: ByteArray): ByteArray {
        val encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-256"), PSource.PSpecified.DEFAULT)
        encryptCipher.init(Cipher.DECRYPT_MODE, this.keyPair.private, oaepParams)
        return encryptCipher.doFinal(cipherData)
    }

    /**
     * Encodes the RSA PublicKey to a PKCS1 formatted byte representation
     *
     * @return a ByteArray containing the PKCS1 representation of the KeyPair's RSA public key
     */
    fun pubKeyToPkcs1Bytes(): ByteArray {
        val pubKeyX509Bytes = this.keyPair.public.encoded
        val sPubKeyInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pubKeyX509Bytes)
        val pubKeyPrimitive: ASN1Primitive = sPubKeyInfo.parsePublicKey()
        val pubKeyPkcs1Bytes: ByteArray = pubKeyPrimitive.encoded
        return pubKeyPkcs1Bytes
    }

    /**
     * Encodes the 2048-bit RSA PrivateKey to a PKCS1 formatted byte representation
     *
     * @return a ByteArray containing the PKCS1 representation of the  RSA private key
     */
    fun privKeyToPkcs1Bytes(): ByteArray {
        val privKeyInfo = PrivateKeyInfo.getInstance(this.keyPair.private.encoded)
        val privKeyAsn1Encodable: ASN1Encodable = privKeyInfo.parsePrivateKey()
        val privKeyAsn1Primitive: ASN1Primitive = privKeyAsn1Encodable.toASN1Primitive()
        val privKeyPkcs1Bytes: ByteArray = privKeyAsn1Primitive.getEncoded()
        return privKeyPkcs1Bytes
    }
}