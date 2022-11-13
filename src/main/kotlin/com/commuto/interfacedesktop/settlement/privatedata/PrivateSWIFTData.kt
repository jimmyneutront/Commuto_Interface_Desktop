package com.commuto.interfacedesktop.settlement.privatedata

import kotlinx.serialization.Serializable

/**
 * Private data for a SWIFT settlement method, corresponding to a bank account that can receive SWIFT transfers.
 *
 * @property accountHolder The legal name of the holder of the bank account.
 * @property bic The SWIFT code or Business Identifier Code identifying the bank in which the account exists.
 * @property accountNumber The number of the account.
 */
@Serializable
data class PrivateSWIFTData(
    val accountHolder: String,
    val bic: String,
    val accountNumber: String,
): PrivateData