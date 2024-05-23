package net.intergamma.stock.store

import net.intergamma.stock.logger
import net.intergamma.stock.store.dto.SetStockDto
import net.intergamma.stock.store.dto.StockDto
import net.intergamma.stock.store.dto.StockReservationDto
import net.intergamma.stock.store.dto.recordToStockReservationDto
import net.intergamma.stock.store.dto.recordToStoreStockDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * In a bigger application, we could add another level of abstraction between the DTO's and the database records, no need now.
 */
@Repository
class StockService(val stockRepository: StockRepository) {
    val log = logger()

    @Transactional(readOnly = true)
    fun getStockFiltered(storeId: List<String>, productId: List<String>): List<StockDto> {
        return stockRepository.getStockFilters(storeId, productId).map { it.recordToStoreStockDto() }
    }

    @Transactional(readOnly = true)
    fun getStock(storeId: String, productId: String): StockDto? {
        return stockRepository.getStock(storeId, productId)?.recordToStoreStockDto()
    }

    @Transactional(readOnly = false)
    fun setStock(storeId: String, productId: String, stockDto: SetStockDto): Int {
        return stockRepository.setStock(storeId, productId, stockDto.stock).also { rowsAffected ->
            if (rowsAffected > 0) {
                log.info("Updated or created stock for store $storeId and product $productId to ${stockDto.stock}")
            } else {
                log.warn("Failed to update stock for store $storeId and product $productId, no rows affected")
            }
        }
    }

    @Transactional(readOnly = false)
    fun deleteStock(storeId: String, productId: String): Int {
        return stockRepository.deleteStock(storeId, productId).also {
            if (it > 0) {
                log.info("Deleted stock level for store $storeId and product $productId")
            } else {
                log.warn("Tried to delete stock level for store $storeId and product $productId but it didn't exist")
            }
        }
    }

    @Transactional(readOnly = true)
    fun getStockReservations(storeId: List<String>, productId: List<String>, user: List<String>): List<StockReservationDto> {
        return stockRepository.getStockReservations(storeId, productId, user).map { it.recordToStockReservationDto() }
    }

    @Transactional(readOnly = false)
    fun createReservation(storeId: String, productId: String, userId: String, amountToReserve: Long): Int {
        verifyStoreCarriesProductAndHasStock(storeId, productId)
        verifyUserDoesntHaveReservationForProduct(storeId, productId, userId)

        return stockRepository.createReservation(storeId, productId, userId, amountToReserve).also { rowsAffected ->
            if (rowsAffected > 0) {
                log.info("created reservation for store $storeId and product $productId to $amountToReserve for user $userId")
            } else {
                log.warn("Failed to update reservation for store $storeId and product $productId to $amountToReserve for user $userId, conditions not met")
            }
        }
    }

    @Transactional(readOnly = false)
    fun deleteReservation(storeId: String, productId: String, userId: String): Int {
        return stockRepository.deleteReservation(storeId, productId, userId).also { rowsAffected ->
            if (rowsAffected > 0) {
                log.info("Deleted reservation for store $storeId and product $productId for user $userId")
            } else {
                log.warn("Tried to delete reservation for store $storeId and product $productId for user $userId but it didn't exist")
            }
        }
    }

    @Transactional(readOnly = false)
    fun cleanUpExpiredReservations() {
        val pruned = stockRepository.pruneExpiredReservations()
        if (pruned > 0) {
            log.info("Pruned $pruned expired reservations")
        }
    }

    private fun verifyUserDoesntHaveReservationForProduct(storeId: String, productId: String, userId: String) {
        val existingReservation = stockRepository.getStockReservation(storeId, productId, userId)
        if (existingReservation != null) {
            log.warn("User $userId already has already reserved products $productId in $storeId for ${existingReservation.reservedStock} units")
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Already ${existingReservation.reservedStock} units reserved")
        }
    }

    private fun verifyStoreCarriesProductAndHasStock(storeId: String, productId: String) {
        val stock = stockRepository.getStock(storeId, productId)
        if ( stock == null ) {
            log.warn("Reservation failed, store $storeId does not carry product $productId")
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Store $storeId does not carry product $productId")
        }
        if ((stock.stock ?: 0) <= 0) {
            log.warn("Reservation failed, store $storeId is out of stock for product $productId")
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Store $storeId is out of stock for product $productId")
        }
    }

}