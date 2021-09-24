package com.commuto.interfacedesktop.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

class DatabaseDriverFactory {
    fun createDriver(): SqlDriver {
        return JdbcSqliteDriver("test.db")
    }
}