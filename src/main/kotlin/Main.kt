package org.example

object AppConfig {
    val databasePassword: String by lazy {
        println("Загружаем пароль из файла...")
        "mySecretPassword123"

    }
}

fun main() {


    var count = 10
    count += 5
    println(count)

    println("Обращаемся к паролю впервые:")
    println("Пароль: ${AppConfig.databasePassword}")
    println("Пароль: ${AppConfig.databasePassword}")// загружает уже из памяти

    lateinit var username: String
    username = "Иван"
    println("Добро пожаловать, $username!")

    var name: String = "Kotlin"

    var nullableName: String? = null

    fun printLength(str: String?) {
        println(str?.length ?: 0)
    }

    printLength(name)         // 6
    printLength(nullableName) // Нет данных

    fun sum(a: Int, b: Int): Int {
        return a + b
    }

//    data class Product(var id: Int, var name: String, var price: Double)
//    var pr1 = Product(1, "cocos" , 20.0)
//    var pr2 = pr1.copy( name = "dundu")
//
//    println(pr1)
//    println(pr2)


    data class Product(val id: Int, val name: String, val price: Double)

    val products = listOf(
        Product(1, "Телефон", 29999.0),
        Product(2, "Ноутбук", 89999.0),
        Product(3, "Наушники", 4999.0),
        Product(4, "Мышь", 1999.0)
    )

    // Преобразуем в список названий
    val productNames = products.map { it.name }
    println("Названия товаров: $productNames")
    // Названия товаров: [Телефон, Ноутбук, Наушники, Мышь]

    // Можно добавить дополнительную логику
    val formattedNames = products.map { "${it.name} - ${it.price}₽" }
    println("Форматированные названия: $formattedNames")










}