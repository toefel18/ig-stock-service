package net.intergamma.stock.store.dto

import net.intergamma.stock.db.tables.records.StoreStockRecord

data class StockDto(
    val storeId: String,
    val productId: String,
    val stock: Long
)

fun StoreStockRecord.recordToStoreStockDto() = StockDto(
    storeId = this.storeId ?: "",
    productId = this.productId ?: "",
    stock = this.stock ?: 0
)