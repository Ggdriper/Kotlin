package com.example

import com.example.config.DatabaseFactory
import com.example.plugins.configureRouting
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import com.example.service.RabbitMQService
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.config.yaml.*
import java.io.File
import com.example.plugins.configureRouting
import com.example.plugins.configureSecurity
import com.example.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun Application.module() {
    println("🚀 Запуск приложения...")

    // Инициализация БД
    println("🔧 Инициализация базы данных...")
    DatabaseFactory.init()
    println("✅ База данных инициализирована")

    // Инициализация RabbitMQ Service с обработкой ошибок
    println("🔧 Инициализация RabbitMQ Service...")
    val rabbitMQService = try {
        RabbitMQService().also {
            println("✅ RabbitMQ Service инициализирован")
        }
    } catch (e: Exception) {
        println("⚠️ Не удалось инициализировать RabbitMQ Service: ${e.message}")
        println("⚠️ Приложение запустится без RabbitMQ")
        null
    }

    // Запускаем консьюмер в отдельной корутине с задержкой
    runBlocking {
        launch(Dispatchers.IO) {
            try {
                println("⏳ Ожидание запуска RabbitMQ...")
                delay(10000) // Ждем 10 секунд для запуска RabbitMQ в docker-compose

                if (rabbitMQService != null) {
                    println("🚀 Запуск RabbitMQ Consumer...")
                    rabbitMQService.startConsumer()
                    println("✅ RabbitMQ Consumer успешно запущен")
                }

            } catch (e: Exception) {
                println("❌ Не удалось запустить RabbitMQ Consumer: ${e.message}")
                println("⚠️ Приложение продолжит работу без RabbitMQ Consumer")

                // Пробуем перезапустить через 30 секунд
                launch {
                    delay(30000)
                    println("🔄 Попытка перезапуска RabbitMQ Consumer...")
                    try {
                        rabbitMQService?.startConsumer()
                        println("✅ RabbitMQ Consumer перезапущен успешно")
                    } catch (e2: Exception) {
                        println("❌ Не удалось перезапустить RabbitMQ Consumer: ${e2.message}")
                    }
                }
            }
        }
    }

    // Добавляем обработчик завершения работы
    environment.monitor.subscribe(ApplicationStopPreparing) {
        println("\n🛑 Получен сигнал остановки приложения...")
        try {
            rabbitMQService?.close()
            println("✅ RabbitMQ Service корректно закрыт")
        } catch (e: Exception) {
            println("❌ Ошибка при закрытии RabbitMQ Service: ${e.message}")
        }
        println("👋 Приложение завершает работу")
    }

    // Добавляем обработчик старта
    environment.monitor.subscribe(ApplicationStarted) {
        println("\n🎉 Приложение успешно запущено!")
        println("📊 Сервисы:")
        println("   • PostgreSQL: готов")
        println("   • Redis: готов")
        println("   • RabbitMQ: ${if (rabbitMQService != null) "подключен" else "недоступен"}")
        println("   • JWT Auth: готов")
        println("   • Кэширование: активно")
        println("\n🌐 API доступно по адресу: http://localhost:${System.getenv("PORT")?.toIntOrNull() ?: 8080}")
        if (rabbitMQService != null) {
            println("📚 RabbitMQ Management UI: http://localhost:15672 (guest/guest)")
        }
    }

    // Serialization
    println("🔧 Настройка сериализации...")
    configureSerialization()
    println("✅ Сериализация настроена")

    // Security (JWT)
    println("🔧 Настройка безопасности...")
    configureSecurity()
    println("✅ Безопасность настроена")

    // Routing
    println("🔧 Настройка маршрутов...")
    configureRouting()
    println("✅ Маршруты настроены")

    println("\n🎯 Все системы готовы к работе!")
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = "0.0.0.0"

    println("========================================")
    println("        SHOP BACKEND SERVICE")
    println("========================================")
    println("Порт: $port")
    println("Хост: $host")
    println("========================================")

    embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::module
    ).start(wait = true)
}