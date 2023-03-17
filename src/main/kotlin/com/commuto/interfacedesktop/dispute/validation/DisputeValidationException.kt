package com.commuto.interfacedesktop.dispute.validation

/**
 * An [Exception] thrown if a problem is encountered while validating dispute-related data.
 *
 * @param desc A description providing information about the context in which the exception was thrown.
 */
class DisputeValidationException(desc: String): Exception(desc)