package com.commuto.interfacedesktop.p2p.messages

import com.commuto.interfacedesktop.keymanager.types.PublicKey
import java.util.*

data class PublicKeyAnnouncement constructor(val id: UUID, val publicKey: PublicKey)