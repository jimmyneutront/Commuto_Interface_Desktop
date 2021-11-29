package com.commuto.interfacedesktop.kmService

import com.commuto.interfacedesktop.dbService.DBService
import com.commuto.interfacedesktop.kmService.kmTypes.KeyPair
import com.commuto.interfacedesktop.kmService.kmTypes.PublicKey
import org.bouncycastle.asn1.*
import java.security.KeyFactory
import java.security.KeyPair as JavaSecKeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey as JavaSecPublicKey
import java.security.spec.RSAPrivateKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers
import org.bouncycastle.asn1.pkcs.RSAPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo

/**
 * The Key Manager Service Class.
 *
 * This is responsible for generating, storing, and retrieving key pairs, and for storing and retrieving public keys.
 *
 * @property dbService the Database Service used to store and retrieve data
 */
class KMService(var dbService: DBService) {

    /**
     * Generates a new KeyPair, and optionally stores it in the database
     *
     * @param storeResult indicate whether the generated key pair should be stored in the database. This will primarily
     * be left as true, but testing code may set as false to generate a key pair for testing duplicate key protection.
     * If true, each key pair is stored in the database as Base64 encoded Strings of byte representations of the public
     * and private keys in PKCS#1 format.
     *
     * @return a new KeyPair object
     */
    fun generateKeyPair(storeResult: Boolean = true): KeyPair {
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = KeyPair(keyPairGenerator.generateKeyPair())
        val encoder = Base64.getEncoder()
        if (storeResult) {
            dbService.storeKeyPair(encoder.encodeToString(keyPair.interfaceId),
                encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes()),
                encoder.encodeToString(keyPair.privKeyToPkcs1Bytes()))
        }
        return keyPair
    }

    /**
     * Retrieves the persistently stored KeyPair associated with the given interface id, or returns null if not
     * present.
     *
     * @param interfaceId the interface id of the key pair, a SHA-256 hash of the PKCS#1 byte array encoded
     * representation of its public key.
     *
     * @return KeyPair?
     * @return null if no KeyPair is found with the given interface id
     */
    fun getKeyPair(interfaceId: ByteArray): KeyPair? {
        val encoder = Base64.getEncoder()
        val dbKeyPair: com.commuto.interfacedesktop.db.KeyPair? = dbService
            .getKeyPair(encoder.encodeToString(interfaceId))
        val decoder = Base64.getDecoder()
        if (dbKeyPair != null) {
            val pubKey: JavaSecPublicKey = pubKeyFromPkcs1Bytes(decoder.decode(dbKeyPair.publicKey))
            val privKey: PrivateKey = privKeyFromPkcs1Bytes(decoder.decode(dbKeyPair.privateKey))
            val keyPair = JavaSecKeyPair(pubKey, privKey)
            return KeyPair(keyPair)
        } else {
            return null
        }
    }

    /**
     * Persistently stores a PublicKey associated with an interface id. This will generate a PKCS#1 byte array
     * representation of the public key, encode it as a Base64 String and store it in the database.
     *
     * @param pubKey the public key to be stored
     */
    fun storePublicKey(pubKey: PublicKey) {
        val interfaceId: ByteArray = MessageDigest.getInstance("SHA-256")
            .digest(pubKey.toPkcs1Bytes())
        val encoder = Base64.getEncoder()
        dbService.storePublicKey(encoder.encodeToString(interfaceId),
            encoder.encodeToString(pubKey.toPkcs1Bytes()))
    }

    /**
     * Retrieves the persistently stored PublicKey associated with the given interface id, or returns null if not
     * present
     *
     * @param interfaceId the interface id of the public key, a SHA-256 hash of its PKCS#1 byte array encoded
     * representation
     *
     * @return PublicKey?
     * @return null if no PublicKey is found with the given interface id
     */
    fun getPublicKey(interfaceId: ByteArray): PublicKey? {
        val encoder = Base64.getEncoder()
        val dbPubKey: com.commuto.interfacedesktop.db.PublicKey? = dbService
            .getPublicKey(encoder.encodeToString(interfaceId))
        val decoder = Base64.getDecoder()
        if (dbPubKey != null) {
            val pubKey: JavaSecPublicKey = pubKeyFromPkcs1Bytes(decoder.decode(dbPubKey.publicKey))
            return PublicKey(pubKey)
        } else {
            return null
        }
    }

    /**
     * Encodes an RSA PublicKey to a PKCS1 formatted byte representation
     *
     * @param pubKey the RSA public key from which the PKCS1 formatted byte representation will be derived
     *
     * @return a ByteArray containing the PKCS1 representation of the passed RSA public key
     */
    fun pubKeyToPkcs1Bytes(pubKey: JavaSecPublicKey): ByteArray {
        val pubKeyX509Bytes = pubKey.encoded
        val sPubKeyInfo: SubjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pubKeyX509Bytes)
        val pubKeyPrimitive: ASN1Primitive = sPubKeyInfo.parsePublicKey()
        val pubKeyPkcs1Bytes: ByteArray = pubKeyPrimitive.encoded
        return pubKeyPkcs1Bytes
    }

    /**
     * Restores a RSA PublicKey from a PKCS1 formatted byte representation
     *
     * @param pubKeyPkcs1Bytes the PKCS1 representation of the public RSA key to be restored
     *
     * @return the restored PublicKey described in by pubKeyPkcs1Bytes
     */
    fun pubKeyFromPkcs1Bytes(pubKeyPkcs1Bytes: ByteArray): JavaSecPublicKey {
        val algorithmIdentifier: AlgorithmIdentifier = AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
        val pubKeyX509Bytes: ByteArray = SubjectPublicKeyInfo(algorithmIdentifier, pubKeyPkcs1Bytes).encoded
        val pubKey: JavaSecPublicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(pubKeyX509Bytes))
        return pubKey
    }

    /**
     * Encodes a 2048-bit RSA PrivateKey to a PKCS1 formatted byte representation
     *
     * @param privKey the 2048-bit RSA private key from which the PKCS1 formatted byte representation will be derived
     *
     * @return a ByteArray containing the PKCS1 representation of the passed RSA private key
     */
    fun privKeyToPkcs1Bytes(privKey: PrivateKey): ByteArray {
        val privKeyInfo = PrivateKeyInfo.getInstance(privKey.encoded)
        val privKeyAsn1Encodable: ASN1Encodable = privKeyInfo.parsePrivateKey()
        val privKeyAsn1Primitive: ASN1Primitive = privKeyAsn1Encodable.toASN1Primitive()
        val privKeyPkcs1Bytes: ByteArray = privKeyAsn1Primitive.getEncoded()
        return privKeyPkcs1Bytes
    }

    /**
     * Restores a 2048-bit RSA PrivateKey from a PKCS1 formatted byte representation
     *
     * @param privKeyPkcs1Bytes the PKCS1 representation of the private RSA key to be restored
     *
     * @return the restored PrivateKey described in privKeyPkcs1Bytes
     */
    fun privKeyFromPkcs1Bytes(privKeyPkcs1Bytes: ByteArray): PrivateKey {
        val rsaPrivKey: RSAPrivateKey = RSAPrivateKey.getInstance(ASN1Sequence.fromByteArray(privKeyPkcs1Bytes))
        val privKeySpec: RSAPrivateKeySpec = RSAPrivateCrtKeySpec(rsaPrivKey.modulus,
            rsaPrivKey.publicExponent,
            rsaPrivKey.privateExponent,
            rsaPrivKey.prime1,
            rsaPrivKey.prime2,
            rsaPrivKey.exponent1,
            rsaPrivKey.exponent2,
            rsaPrivKey.coefficient
        )
        val privKey: PrivateKey = KeyFactory.getInstance("RSA").generatePrivate(privKeySpec)
        return privKey
    }

}