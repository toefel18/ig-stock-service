package net.intergamma.stock.store.dto

import net.intergamma.stock.db.tables.records.StoreStockReservationRecord
import java.time.OffsetDateTime

data class StockReservationDto(
    val storeId: String,
    val productId: String,
    val userId: String,
    val stock: Long,
    val expiresAt: OffsetDateTime
)

fun StoreStockReservationRecord.recordToStockReservationDto() = StockReservationDto(
    storeId = this.storeId ?: "",
    productId = this.productId ?: "",
    userId = this.userId ?: "",
    stock = this.reservedStock ?: 0,
    expiresAt = this.expiresAt ?: OffsetDateTime.now()
)