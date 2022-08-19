package com.commuto.interfacedesktop.offer.validation

/**
 * An [Exception] thrown if a problem is encountered while validating settlement methods for an offer.
 *
 * @param desc A description providing information about the context in which the exception was thrown.
 */
class EditedSettlementMethodValidationException(desc: String): Exception(desc)