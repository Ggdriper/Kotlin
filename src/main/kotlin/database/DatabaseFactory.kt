package com.example.database

import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.h2.Driver"
        val jdbcURL = "jdbc:h2:file:./build/db"

        Database.connect(jdbcURL, driverClassName)
    }
}