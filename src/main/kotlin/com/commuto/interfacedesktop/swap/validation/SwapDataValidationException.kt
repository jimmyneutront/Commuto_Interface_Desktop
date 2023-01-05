package com.commuto.interfacedesktop.swap.validation

/**
 * An [Exception] thrown if a problem is encountered while validating data for a swap.
 *
 * @param desc A description providing information about the context in which the exception was thrown.
 */
class SwapDataValidationException(desc: String): Exception(desc)