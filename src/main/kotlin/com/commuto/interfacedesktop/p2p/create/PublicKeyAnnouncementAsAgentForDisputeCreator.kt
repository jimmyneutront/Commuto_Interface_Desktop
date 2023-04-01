package com.commuto.interfacedesktop.p2p.create

import com.commuto.interfacedesktop.dispute.DisputeRole
import com.commuto.interfacedesktop.key.keys.KeyPair
import com.commuto.interfacedesktop.p2p.serializable.messages.SerializablePublicKeyAnnouncementAsAgentForDisputeMessage
import com.commuto.interfacedesktop.p2p.serializable.payloads.SerializablePublicKeyAnnouncementAsAgentForDisputePayload
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

/**
 * Creates a Public Key Announcement as a dispute agent for a dispute in which the sender of this message has been
 * selected as a dispute agent.
 *
 * @param keyPair The [KeyPair] containing the public key to be announced.
 * @param swapId The ID of the disputed swap, for which the user has been selected as a dispute agent.
 * @param disputeRole The role of the user in the dispute.
 * @param ethereumKeyPair The Ethereum key pair of the user/dispute agent.
 *
 * @return A JSON [String] that is the Public Key Announcement.
 */
fun createPublicKeyAnnouncementAsAgentForDispute(
    keyPair: KeyPair,
    swapId: String,
    disputeRole: DisputeRole,
    ethereumKeyPair: Credentials,
): String {
    val encoder = Base64.getEncoder()
    val publicKeyString = encoder.encodeToString(keyPair.pubKeyToPkcs1Bytes())
    val payload = SerializablePublicKeyAnnouncementAsAgentForDisputePayload(
        pubKey = publicKeyString,
        swapId = swapId,
    )
    val payloadUTF8Bytes = Json.encodeToString(payload).toByteArray(Charset.forName("UTF-8"))
    val payloadDataHash = MessageDigest.getInstance("SHA-256").digest(payloadUTF8Bytes)
    val payloadDataSignatureWithKeyPair = keyPair.sign(payloadDataHash)
    val payloadDataSignatureBytesWithEthereumKey = ByteArray(65).apply {
        val ethSignature = Sign.signPrefixedMessage(payloadDataHash, ethereumKeyPair.ecKeyPair)
        this + ethSignature.r + ethSignature.s + ethSignature.v
    }
    val message = SerializablePublicKeyAnnouncementAsAgentForDisputeMessage(
        sender = encoder.encodeToString(keyPair.interfaceId),
        senderEthAddr = ethereumKeyPair.address,
        msgType = "${disputeRole.asString}Pka",
        payload = encoder.encodeToString(payloadUTF8Bytes),
        signature = encoder.encodeToString(payloadDataSignatureWithKeyPair),
        signatureEth = encoder.encodeToString(payloadDataSignatureBytesWithEthereumKey)
    )
    return Json.encodeToString(message)
}