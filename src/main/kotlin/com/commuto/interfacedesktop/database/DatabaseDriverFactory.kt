package com.commuto.interfacedesktop.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import javax.inject.Inject

/**
 * Provides [SqlDriver]s.
 */
class DatabaseDriverFactory @Inject constructor() {
    /**
     * Creates and returns a new [JdbcSqliteDriver] connected to an in-memory database.
     * @return A new [JdbcSqliteDriver] connected to an in-memory database.
     */
    fun createDriver(): SqlDriver {
        //TODO: Figure out why file database isn't working
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    }
}