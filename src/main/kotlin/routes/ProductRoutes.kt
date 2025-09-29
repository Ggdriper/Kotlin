package com.example.routes

import com.example.database.Product
import com.example.database.Products
import com.example.models.ProductRequest
import com.example.models.ProductResponse
import com.example.models.ProductListResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.productRoutes() {

    get("/products") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10
        val minPrice = call.request.queryParameters["minPrice"]?.toDoubleOrNull()
        val maxPrice = call.request.queryParameters["maxPrice"]?.toDoubleOrNull()
        val nameContains = call.request.queryParameters["nameContains"]

        val (products, totalCount) = transaction {
            // Build query with filters
            val query = when {
                minPrice != null && maxPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.price lessEq maxPrice) and
                                (Products.name like "%$nameContains%")
                    }
                }
                minPrice != null && maxPrice != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.price lessEq maxPrice)
                    }
                }
                minPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.name like "%$nameContains%")
                    }
                }
                maxPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price lessEq maxPrice) and
                                (Products.name like "%$nameContains%")
                    }
                }
                minPrice != null -> {
                    Product.find { Products.price greaterEq minPrice }
                }
                maxPrice != null -> {
                    Product.find { Products.price lessEq maxPrice }
                }
                nameContains != null -> {
                    Product.find { Products.name like "%$nameContains%" }
                }
                else -> {
                    Product.all()
                }
            }

            // Apply pagination and get results
            val productsList = query
                .orderBy(Products.id to SortOrder.ASC)
                .limit(pageSize, ((page - 1) * pageSize).toLong())
                .map { ProductResponse(it.id.value, it.name, it.description, it.price, it.stock) }

            // Get total count with same filters
            val total = when {
                minPrice != null && maxPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.price lessEq maxPrice) and
                                (Products.name like "%$nameContains%")
                    }.count()
                }
                minPrice != null && maxPrice != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.price lessEq maxPrice)
                    }.count()
                }
                minPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price greaterEq minPrice) and
                                (Products.name like "%$nameContains%")
                    }.count()
                }
                maxPrice != null && nameContains != null -> {
                    Product.find {
                        (Products.price lessEq maxPrice) and
                                (Products.name like "%$nameContains%")
                    }.count()
                }
                minPrice != null -> {
                    Product.find { Products.price greaterEq minPrice }.count()
                }
                maxPrice != null -> {
                    Product.find { Products.price lessEq maxPrice }.count()
                }
                nameContains != null -> {
                    Product.find { Products.name like "%$nameContains%" }.count()
                }
                else -> {
                    Product.all().count()
                }
            }

            Pair(productsList, total)
        }

        call.respond(ProductListResponse(products, totalCount.toInt(), page, pageSize))
    }

    authenticate("auth-jwt") {
        post("/products") {
            val principal = call.principal<JWTPrincipal>()
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@post
            }

            val request = call.receive<ProductRequest>()

            val product = transaction {
                Product.new {
                    name = request.name
                    description = request.description
                    price = request.price
                    stock = request.stock
                }
            }

            call.respond(HttpStatusCode.Created, ProductResponse(product.id.value, product.name, product.description, product.price, product.stock))
        }

        put("/products/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userRole = principal?.getClaim("role", String::class)

            if (userRole != "ADMIN") {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
                return@put
            }

            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid product ID"))
                return@put
            }

            val product = transaction { Product.findById(id) }
            if (product == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Product not found"))
                return@put
            }

            val request = call.receive<ProductRequest>()

            transaction {
                product.name = request.name
                product.description = request.description
                product.price = request.price
                product.stock = request.stock
            }

            call.respond(ProductResponse(product.id.value, product.name, product.description, product.price, product.stock))
        }
    }
}