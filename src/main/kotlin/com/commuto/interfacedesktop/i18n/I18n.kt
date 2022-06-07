package com.commuto.interfacedesktop.i18n

import java.util.*

class I18n {
    companion object {
        private var resources: ResourceBundle? = null

        fun get(key: String): String {
            if (resources == null) {
                try {
                    resources = ResourceBundle.getBundle("messages")
                } catch (e: MissingResourceException) {
                    Locale.setDefault(Locale("en", "US"))
                    resources = ResourceBundle.getBundle("messages")
                }
            }
            return resources!!.getString(key)
        }

    }
}