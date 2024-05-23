package net.intergamma.stock

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import net.intergamma.stock.store.dto.SetReservationDto
import net.intergamma.stock.store.dto.SetStockDto
import net.intergamma.stock.store.dto.StockReservationDto
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

/**
 * We can put this in a separate module and share it with our consumers.
 *
 * In a production env, we should add more validation
 */
class StockServiceClient(val baseUrl: String) {
    private val http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun getStoreStocks(includeStores: List<String> = emptyList(), includeProducts: List<String> = emptyList()): HttpResponse<String> {
        // for more safety we should url'encode with more time, but for this example it's fine
        val storeQueryParams = includeStores.map { "storeId=${it}" }
        val productQueryParams = includeProducts.map { "productId=${it}" }
        val queryParameters = (storeQueryParams + productQueryParams).joinToString("&")
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/store-product-stock?${queryParameters}"))
                .headers("Accept", "application/json")
                .GET()
                .build(),
            BodyHandlers.ofString()
        )
    }

    fun getStoreStock(storeId: String, productId: String): HttpResponse<String> {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stores/$storeId/products/$productId/stock"))
                .headers("Accept", "application/json")
                .GET()
                .build(),
            BodyHandlers.ofString()
        )
    }

    fun createOrUpdateStock(storeId: String, productId: String, setStockDto: SetStockDto): HttpResponse<String> {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stores/$storeId/products/$productId/stock"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(setStockDto.toJson()))
                .build(),
            BodyHandlers.ofString()
        )
    }

    fun deleteStoreStock(storeId: String, productId: String): HttpResponse<String> {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stores/$storeId/products/$productId/stock"))
                .header("Accept", "application/json")
                .DELETE()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )
    }

    fun getStockReservations(
        includeStores: List<String> = emptyList(),
        includeProducts: List<String> = emptyList(),
        includeUsers: List<String> = emptyList()
    ): HttpResponse<String> {
        // for more safety we should url'encode with more time, but for this example it's fine
        val storeQueryParams = includeStores.map { "storeId=${it}" }
        val productQueryParams = includeProducts.map { "productId=${it}" }
        val usersQueryParams = includeProducts.map { "userId=${it}" }
        val queryParameters = (storeQueryParams + productQueryParams + usersQueryParams).joinToString("&")
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stock-reservations?${queryParameters}"))
                .headers("Accept", "application/json")
                .GET()
                .build(),
            BodyHandlers.ofString()
        )
    }

    fun reserveStock(storeId: String, productId: String, setStockReservation: SetReservationDto): HttpResponse<String> {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stores/$storeId/products/$productId/reservations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(setStockReservation.toJson()))
                .build(),
            BodyHandlers.ofString()
        )
    }

    fun deleteReservation(storeId: String, productId: String, userId: String): HttpResponse<String> {
        return http.send(
            HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/stores/$storeId/products/$productId/reservations/users/$userId"))
                .header("Content-Type", "application/json")
                .DELETE()
                .build(),
            BodyHandlers.ofString()
        )
    }

}

val objectMapper = ObjectMapper()
    .registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())
    .setDateFormat(StdDateFormat())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
    .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    // if someone provides @JsonEnumDefaultValue, this setting makes sure jackson looks at that annotation.
    .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
    .configure(SerializationFeature.INDENT_OUTPUT, true)

inline fun <reified T> HttpResponse<String>.bodyToDto(): T = objectMapper.readValue(this.body())

fun Any.toJson(): String = objectMapper.writeValueAsString(this)
