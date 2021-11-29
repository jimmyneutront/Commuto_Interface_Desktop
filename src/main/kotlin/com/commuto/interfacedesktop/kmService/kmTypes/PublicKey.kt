package com.commuto.interfacedesktop.kmService.kmTypes

import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.security.MessageDigest
import java.security.PublicKey as JavaSecPublicKey

/**
 * The PublicKey class is a wrapper around the java.security.PublicKey class, with support for Commuto Interface IDs and
 * several methods for encoding the wrapped public key. The wrapped public key shall be that corresponding to a 2048-bit
 * RSA private key key, and interfaceId is the SHA-256 hash of the public key encoded in PKCS#1 byte format.
 *
 * @property publicKey the java.security.PublicKey object around which this class is wrapped.
 * @property interfaceId the interface id derived from the public key
 */
class PublicKey(publicKey: JavaSecPublicKey) {

    val interfaceId: ByteArray
    val publicKey: JavaSecPublicKey

    init {
        this.publicKey = publicKey
        this.interfaceId = MessageDigest.getInstance("SHA-256")
            .digest(toPkcs1Bytes())
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