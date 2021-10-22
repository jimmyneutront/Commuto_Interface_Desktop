package com.commuto.interfacedesktop.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver

class DatabaseDriverFactory {
    fun createDriver(): SqlDriver {
        //TODO: Figure out why file database isn't working
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    }
}