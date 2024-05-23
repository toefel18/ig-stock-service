package net.intergamma.stock.store

import net.intergamma.stock.store.dto.SetReservationDto
import net.intergamma.stock.store.dto.StockReservationDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class StockReservationController(val stockService: StockService) {

    @GetMapping("/stock-reservations")
    fun getActiveReservations(
        @RequestParam(required = false) storeId: List<String>?,
        @RequestParam(required = false) productId: List<String>?,
        @RequestParam(required = false) user: List<String>?,
    ): ResponseEntity<List<StockReservationDto>> {
        return ResponseEntity.ok(stockService.getStockReservations(storeId.orEmpty(), productId.orEmpty(), user.orEmpty()))
    }

    /** Create or update the stock. I could separate create and update, but no need here.*/
    @PostMapping("/stores/{storeId}/products/{productId}/reservations")
    fun createReservation(
        @PathVariable storeId: String,
        @PathVariable productId: String,
        @RequestBody reservationDto: SetReservationDto,
    ): ResponseEntity<Void> {
        if (reservationDto.amountToReserve < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot reserve a negative amount")
        }
        return if (stockService.createReservation(storeId, productId, reservationDto.userId, reservationDto.amountToReserve) > 0) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.unprocessableEntity().build()
        }
    }

    /** The URL's can be better here, but no time left to make big changes now */
    @DeleteMapping("/stores/{storeId}/products/{productId}/reservations/users/{userId}")
    fun deleteReservation(
        @PathVariable storeId: String,
        @PathVariable productId: String,
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        return if (stockService.deleteReservation(storeId, productId, userId) > 0) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.unprocessableEntity().build()
        }
    }

    @Scheduled(fixedRate = 60000)
    fun cleanUpExpiredReservations() {
        stockService.cleanUpExpiredReservations()
    }

}