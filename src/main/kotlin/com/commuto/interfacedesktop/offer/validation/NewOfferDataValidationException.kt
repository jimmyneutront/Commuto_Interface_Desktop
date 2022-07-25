package com.commuto.interfacedesktop.offer.validation

/**
 * An [Exception] thrown if a problem is encountered while validating data for a new offer.
 *
 * @param desc A description providing information about the context in which the error was thrown.
 */
class NewOfferDataValidationException(desc: String): Exception(desc)