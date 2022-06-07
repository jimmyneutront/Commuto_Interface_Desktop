package com.commuto.interfacedesktop.i18n

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class I18nTest {
    @Test
    fun i18nTest() {
        Locale.setDefault(Locale.GERMAN)
        val string = I18n.get("Offers")
        assertEquals("Offers", string)
    }
}