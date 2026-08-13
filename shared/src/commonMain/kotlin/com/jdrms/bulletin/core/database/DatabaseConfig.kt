package com.jdrms.bulletin.core.database

data class DatabaseConfig(
    val dbName: String = "bulletin_local.db",
    val version: Int = 1
)
