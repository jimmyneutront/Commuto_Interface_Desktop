package com.commuto.interfacedesktop.settlement.privatedata

import kotlinx.serialization.Serializable

/**
 * Private data for a SEPA settlement method, corresponding to a bank account that can receive SEPA transfers.
 *
 * @property accountHolder The legal name of the holder of the bank account.
 * @property bic The Business Identifier Code identifying the bank in which the account exists.
 * @property iban The International Bank Account Number of the account.
 * @property address The address associated with the account.
 */
@Serializable
data class PrivateSEPAData(
    val accountHolder: String,
    val bic: String,
    val iban: String,
    val address: String,
): PrivateData
