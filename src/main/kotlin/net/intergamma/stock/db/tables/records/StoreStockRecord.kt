/*
 * This file is generated by jOOQ.
 */
package net.intergamma.stock.db.tables.records


import java.time.OffsetDateTime

import net.intergamma.stock.db.tables.StoreStock

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * Stock of a product in a store
 */
@Suppress("warnings")
open class StoreStockRecord private constructor() : UpdatableRecordImpl<StoreStockRecord>(StoreStock.STORE_STOCK) {

    open var id: Long?
        set(value): Unit = set(0, value)
        get(): Long? = get(0) as Long?

    open var storeId: String
        set(value): Unit = set(1, value)
        get(): String = get(1) as String

    open var productId: String
        set(value): Unit = set(2, value)
        get(): String = get(2) as String

    open var stock: Long?
        set(value): Unit = set(3, value)
        get(): Long? = get(3) as Long?

    open var createdTimestampUtc: OffsetDateTime?
        set(value): Unit = set(4, value)
        get(): OffsetDateTime? = get(4) as OffsetDateTime?

    open var modifiedTimestampUtc: OffsetDateTime?
        set(value): Unit = set(5, value)
        get(): OffsetDateTime? = get(5) as OffsetDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Long?> = super.key() as Record1<Long?>

    /**
     * Create a detached, initialised StoreStockRecord
     */
    constructor(id: Long? = null, storeId: String, productId: String, stock: Long? = null, createdTimestampUtc: OffsetDateTime? = null, modifiedTimestampUtc: OffsetDateTime? = null): this() {
        this.id = id
        this.storeId = storeId
        this.productId = productId
        this.stock = stock
        this.createdTimestampUtc = createdTimestampUtc
        this.modifiedTimestampUtc = modifiedTimestampUtc
        resetTouchedOnNotNull()
    }
}
