package com.commuto.interfacedesktop.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * A class containing utility methods for formatting dates.
 */
class DateFormatter {

    companion object {
        /**
         * Creates a [String] representation of [Date] in the form "yyyy-MM-dd'T'HH:mm'Z'" where Z indicates that this
         * date and time stamp is in the UTC time zone.
         */
        fun createDateString(date: Date): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return dateFormat.format(date)
        }
    }

}