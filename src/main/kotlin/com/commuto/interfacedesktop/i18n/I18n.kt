package com.commuto.interfacedesktop.i18n

import java.util.*

/**
 * The Internationalization class, used to fetch localized [String]s from the resource bundle depending on the user's
 * locale. This class should never be instantiated; the properties of its companion object should be used directly.
 */
class I18n {
    /**
     *
     */
    companion object {
        /**
         * The current [ResourceBundle], selected based on the user's current locale.
         */
        private var resources: ResourceBundle? = null

        /**
         * Gets the [String] corresponding to the specified resource key from the proper [ResourceBundle]. If
         * [resources] is null, we attempt to get one for the current locale. If we don't find one, then we use the
         * [ResourceBundle] for the US English locale, and set [resources] as that US English [ResourceBundle].
         */
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