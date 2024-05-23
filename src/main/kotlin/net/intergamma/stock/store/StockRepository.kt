package net.intergamma.stock.store

import net.intergamma.stock.db.keys.STORE_STOCK_UKEY
import net.intergamma.stock.db.tables.records.StoreStockRecord
import net.intergamma.stock.db.tables.records.StoreStockReservationRecord
import net.intergamma.stock.db.tables.references.STORE_STOCK
import net.intergamma.stock.db.tables.references.STORE_STOCK_RESERVATION
import org.jooq.DSLContext
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.select
import org.jooq.impl.DSL.sum
import org.jooq.impl.DSL.trueCondition
import org.jooq.impl.DSL.value
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class StockRepository(val jooq: DSLContext) {

    companion object {
        val DEFAULT_STOCK_RESERVATION_EXPIRATION: Duration = Duration.ofMinutes(5)
    }

    fun getStockFilters(storeId: List<String>, productId: List<String>): List<StoreStockRecord> {
        return jooq.selectFrom(STORE_STOCK)
            .where(
                // if stores or products are empty, we want to return all records, trueCondition matches all
                if (storeId.isNotEmpty()) STORE_STOCK.STORE_ID.`in`(storeId) else trueCondition(),
                if (productId.isNotEmpty()) STORE_STOCK.PRODUCT_ID.`in`(productId) else trueCondition(),
            ).fetch()
    }

    fun getStock(storeId: String, productId: String): StoreStockRecord? {
        return jooq.selectFrom(STORE_STOCK)
            .where(
                STORE_STOCK.STORE_ID.eq(storeId),
                STORE_STOCK.PRODUCT_ID.eq(productId)
            ).fetchOne()
    }

    fun setStock(storeId: String, productId: String, stock: Long): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return jooq.insertInto(STORE_STOCK)
            .set(STORE_STOCK.STORE_ID, storeId)
            .set(STORE_STOCK.PRODUCT_ID, productId)
            .set(STORE_STOCK.STOCK, stock)
            .set(STORE_STOCK.CREATED_TIMESTAMP_UTC, now)
            .set(STORE_STOCK.MODIFIED_TIMESTAMP_UTC, now)
            .onConflictOnConstraint(STORE_STOCK_UKEY).doUpdate()
            .set(STORE_STOCK.STOCK, stock)
            .set(STORE_STOCK.MODIFIED_TIMESTAMP_UTC, now)
            .execute()
    }

    fun deleteStock(storeId: String, productId: String): Int {
        return jooq.deleteFrom(STORE_STOCK)
            .where(
                STORE_STOCK.STORE_ID.eq(storeId),
                STORE_STOCK.PRODUCT_ID.eq(productId)
            ).execute()
    }

    fun getStockReservations(storeId: List<String>, productId: List<String>, user: List<String>): List<StoreStockReservationRecord> {
        return jooq.selectFrom(STORE_STOCK_RESERVATION)
            .where(
                if (storeId.isNotEmpty()) STORE_STOCK_RESERVATION.STORE_ID.`in`(storeId) else trueCondition(),
                if (productId.isNotEmpty()) STORE_STOCK_RESERVATION.PRODUCT_ID.`in`(productId) else trueCondition(),
                if (user.isNotEmpty()) STORE_STOCK_RESERVATION.USER_ID.`in`(user) else trueCondition(),
                STORE_STOCK_RESERVATION.EXPIRES_AT.gt(OffsetDateTime.now(ZoneOffset.UTC))
            ).fetch()
    }

    /**
     * This is a more complicated query but ensures correct behaviour in a concurrent environment.
     * It will insert a reservation if there is enough stock available AND there aren't other reservations
     * that would make the total reserved stock exceed the total stock.
     */
    fun createReservation(storeId: String, productId: String, userId: String, amountToReserve: Long): Int {
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        val totalStockSubQuery = select(sum(STORE_STOCK.STOCK))
            .from(STORE_STOCK)
            .where(
                STORE_STOCK.STORE_ID.eq(storeId),
                STORE_STOCK.PRODUCT_ID.eq(productId),
            ).asField<Long>()

        val reservedStockSubQuery = select(coalesce(sum(STORE_STOCK_RESERVATION.RESERVED_STOCK), 0))
            .from(STORE_STOCK_RESERVATION)
            .where(
                STORE_STOCK_RESERVATION.STORE_ID.eq(storeId),
                STORE_STOCK_RESERVATION.PRODUCT_ID.eq(productId),
                STORE_STOCK_RESERVATION.EXPIRES_AT.gt(now)
            ).asField<Long>()

        return jooq.insertInto(
            STORE_STOCK_RESERVATION,
            STORE_STOCK_RESERVATION.STORE_ID,
            STORE_STOCK_RESERVATION.PRODUCT_ID,
            STORE_STOCK_RESERVATION.USER_ID,
            STORE_STOCK_RESERVATION.RESERVED_STOCK,
            STORE_STOCK_RESERVATION.EXPIRES_AT,
            STORE_STOCK_RESERVATION.CREATED_TIMESTAMP_UTC,
            STORE_STOCK_RESERVATION.MODIFIED_TIMESTAMP_UTC,
        ).select(
            select(
                value(storeId),
                value(productId),
                value(userId),
                value(amountToReserve),
                value(now.plus(DEFAULT_STOCK_RESERVATION_EXPIRATION)),
                value(now),
                value(now),
            ).where(
                totalStockSubQuery.gt(reservedStockSubQuery.plus(amountToReserve))
            )
        ).execute()
    }

    fun getStockReservation(storeId: String, productId: String, userId: String): StoreStockReservationRecord? {
        return jooq.selectFrom(STORE_STOCK_RESERVATION)
            .where(
                STORE_STOCK_RESERVATION.STORE_ID.eq(storeId),
                STORE_STOCK_RESERVATION.PRODUCT_ID.eq(productId),
                STORE_STOCK_RESERVATION.USER_ID.eq(userId),
                STORE_STOCK_RESERVATION.EXPIRES_AT.gt(OffsetDateTime.now(ZoneOffset.UTC))
            ).fetchOne()
    }

    fun deleteReservation(storeId: String, productId: String, userId: String): Int {
        return jooq.deleteFrom(STORE_STOCK_RESERVATION)
            .where(
                STORE_STOCK_RESERVATION.STORE_ID.eq(storeId),
                STORE_STOCK_RESERVATION.PRODUCT_ID.eq(productId),
                STORE_STOCK_RESERVATION.USER_ID.eq(userId),
            ).execute()
    }

    /**
     * Deletes expired reservations and returns the number of deleted records
     */
    fun pruneExpiredReservations(): Int {
        return jooq.deleteFrom(STORE_STOCK_RESERVATION)
            .where(STORE_STOCK_RESERVATION.EXPIRES_AT.lt(OffsetDateTime.now(ZoneOffset.UTC)))
            .execute()
    }
}
