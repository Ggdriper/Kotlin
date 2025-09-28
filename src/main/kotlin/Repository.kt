package com.example

import java.util.concurrent.ConcurrentHashMap

class ItemRepository {
    private val storage = ConcurrentHashMap<String, Item>()

    fun list(limit: Int? = null, nameFilter: String? = null): List<Item> {
        val seq = storage.values
            .filter { nameFilter == null || it.name.contains(nameFilter, ignoreCase = true) }
            .sortedBy { it.name }
        return limit?.let { seq.take(it) } ?: seq.toList()
    }

    fun get(id: String): Item? = storage[id]



    fun delete(id: String): Boolean = storage.remove(id) != null
    fun add(item: Item): Item {
        storage[item.id] = item
        return item
    }

    fun create(name: String, description: String?): Item {
        val id = java.util.UUID.randomUUID().toString()
        val item = Item(id = id, name = name, description = description)
        storage[id] = item
        return item
    }
}