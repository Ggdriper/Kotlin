package com.example.config

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init(
        url: String = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/shop",
        user: String = System.getenv("DB_USER") ?: "postgres",
        password: String = System.getenv("DB_PASSWORD") ?: "1234"
    ) {
        println("🔧 Настройка подключения к БД: $url")

        // Flyway миграции
        val flyway = Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .baselineOnMigrate(true)
            .load()

        println("🔄 Применение миграций Flyway...")
        val migrations = flyway.info().all()
        println("📊 Найдено миграций: ${migrations.size}")

        migrations.forEach { migration ->
            println("   • ${migration.version}: ${migration.description} - ${migration.state}")
        }

        flyway.migrate()
        println("✅ Миграции Flyway применены")

        // Exposed
        Database.connect(
            url = url,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )

        println("✅ Подключение к базе данных установлено")
    }
}