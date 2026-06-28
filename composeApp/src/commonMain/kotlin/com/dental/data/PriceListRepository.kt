package com.dental.data

import com.dental.data.db.DentalDatabase
import com.dental.data.db.PriceListItemEntity
import com.dental.model.PriceListItem

class PriceListRepository(private val db: DentalDatabase) {

    private val queries = db.priceListQueries

    fun getAll(): List<PriceListItem> {
        return queries.getAll().executeAsList().map { it.toPriceListItem() }
    }

    fun getById(id: Long): PriceListItem? {
        return queries.getById(id).executeAsOneOrNull()?.toPriceListItem()
    }

    fun getByCategory(category: String): List<PriceListItem> {
        return queries.getByCategory(category).executeAsList().map { it.toPriceListItem() }
    }

    fun create(category: String, name: String, defaultPrice: Long, sortOrder: Int = 0): Long {
        queries.insert(
            category = category,
            name = name,
            defaultPrice = defaultPrice,
            sortOrder = sortOrder.toLong()
        )
        return queries.getLastInsertId().executeAsOne()
    }

    fun update(id: Long, category: String, name: String, defaultPrice: Long) {
        queries.updateItem(
            category = category,
            name = name,
            defaultPrice = defaultPrice,
            id = id
        )
    }

    fun delete(id: Long) {
        queries.deleteItem(id)
    }

    fun count(): Long {
        return queries.count().executeAsOne()
    }

    fun search(query: String): List<PriceListItem> {
        if (query.isBlank()) return getAll()
        val lower = query.lowercase()
        return getAll().filter {
            it.name.lowercase().contains(lower) ||
            it.category.lowercase().contains(lower)
        }
    }

    fun getGroupedByCategory(): Map<String, List<PriceListItem>> {
        return getAll().groupBy { it.category }
    }

    private fun PriceListItemEntity.toPriceListItem(): PriceListItem {
        return PriceListItem(
            id = id,
            name = name,
            category = category,
            defaultPrice = defaultPrice
        )
    }
}
