package net.intergamma.stock.store

import net.intergamma.stock.store.dto.SetStockDto
import net.intergamma.stock.store.dto.StockDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class StockController(val stockService: StockService) {

    @GetMapping("/stores/{storeId}/products/{productId}/stock")
    fun getStoreStockLevel(
        @PathVariable storeId: String,
        @PathVariable productId: String
    ): ResponseEntity<StockDto> {
        val stock: StockDto? = stockService.getStock(storeId, productId)
        return stock?.let { return ResponseEntity.ok(it) } ?: ResponseEntity.notFound().build()
    }

    /** Create or update the stock. I could separate create and update, but no need here.*/
    @PutMapping("/stores/{storeId}/products/{productId}/stock")
    fun setStoreStockLevel(
        @PathVariable storeId: String,
        @PathVariable productId: String,
        @RequestBody stockDto: SetStockDto,
    ): ResponseEntity<Void> {
        if (stockDto.stock < 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock level must be a positive number")
        }
        return if (stockService.setStock(storeId, productId, stockDto) > 0) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.unprocessableEntity().build()
        }
    }

    @DeleteMapping("/stores/{storeId}/products/{productId}/stock")
    fun deleteStoreStockLevel(
        @PathVariable storeId: String,
        @PathVariable productId: String,
    ): ResponseEntity<Void> {
        return if (stockService.deleteStock(storeId, productId) > 0) {
            ResponseEntity.noContent().build() // successful delete
        } else {
            ResponseEntity.notFound().build() // still idempotent, but indicates that the resource was not found
        }
    }

    /** Endpoint to quickly navigate the stock of a complete store, or to see what the stock is in all the stores for product*/
    @GetMapping("/store-product-stock")
    fun getStoreProductStock(
        @RequestParam(required = false) storeId: List<String>?,
        @RequestParam(required = false) productId: List<String>?
    ): ResponseEntity<List<StockDto>> {
        return ResponseEntity.ok(
            stockService.getStockFiltered(storeId.orEmpty(), productId.orEmpty())
        )
    }
}