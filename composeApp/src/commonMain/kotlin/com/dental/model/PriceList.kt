package com.dental.model

data class PriceListItem(
    val id: Long = 0,
    val name: String,
    val category: String,
    val defaultPrice: Long
)
