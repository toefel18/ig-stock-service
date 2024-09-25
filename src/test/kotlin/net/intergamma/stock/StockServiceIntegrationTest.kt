package net.intergamma.stock

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import net.intergamma.stock.db.Public
import net.intergamma.stock.db.Public.Companion.PUBLIC
import net.intergamma.stock.store.StockService
import net.intergamma.stock.store.dto.SetReservationDto
import net.intergamma.stock.store.dto.SetStockDto
import net.intergamma.stock.store.dto.StockDto
import net.intergamma.stock.store.dto.StockReservationDto
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@Tag("integration")
@ActiveProfiles(profiles = ["integration"])
@SpringBootTest(
    properties = ["spring.main.allow-bean-definition-overriding=true"],
    classes = [StockServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class StockServiceIntegrationTest {

    @LocalServerPort
    var restApiPort: Int = 0

    lateinit var client: StockServiceClient

    @Autowired
    lateinit var stockService: StockService

    @Autowired
    lateinit var jooq: DSLContext

    @BeforeEach
    fun clearPreviousDataAndCreateClient() {
        client = StockServiceClient("http://localhost:$restApiPort")
        PUBLIC.tables.forEach {
            runCatching {
                println("Truncating table ${it.name}")
                jooq.truncate(it).cascade().execute()
            }
        }
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun `Create update and delete stock`() {
        // non-existing stock should 404
        client.getStoreStock("store-1", "product-1").statusCode() shouldBe 404

        // creating stock should succeed
        client.createOrUpdateStock("store-1", "product-1", SetStockDto(50)).statusCode() shouldBe 204

        // getting stock should now return the stock
        client.getStoreStock("store-1", "product-1").run {
            statusCode() shouldBe 200
            bodyToDto<StockDto>().run {
                storeId shouldBe "store-1"
                productId shouldBe "product-1"
                stock shouldBe 50
            }
        }

        // updating the stock again should overwrite the value
        client.createOrUpdateStock("store-1", "product-1", SetStockDto(126)).statusCode() shouldBe 204

        // getting stock should now return the updated stock
        client.getStoreStock("store-1", "product-1").run {
            statusCode() shouldBe 200
            bodyToDto<StockDto>().run {
                stock shouldBe 126
            }
        }

        // delete the stock, first call should be 204, indicating successful deletion
        client.deleteStoreStock("store-1", "product-1").statusCode() shouldBe 204

        // delete the stock, first call should be 404, indicating that the resource was not found, but still idempotent
        client.deleteStoreStock("store-1", "product-1").statusCode() shouldBe 404
    }

    @Test
    fun `Filter on multiple stores and stocks`() {
        // creating stock should succeed
        client.createOrUpdateStock("store-1", "product-10", SetStockDto(50)).statusCode() shouldBe 204
        client.createOrUpdateStock("store-2", "product-10", SetStockDto(73)).statusCode() shouldBe 204
        client.createOrUpdateStock("store-3", "product-10", SetStockDto(21)).statusCode() shouldBe 204
        client.createOrUpdateStock("store-1", "product-20", SetStockDto(99)).statusCode() shouldBe 204
        client.createOrUpdateStock("store-3", "product-20", SetStockDto(5)).statusCode() shouldBe 204
        client.createOrUpdateStock("store-2", "product-30", SetStockDto(12)).statusCode() shouldBe 204

        // all products should equal 6
        client.getStoreStocks().run {
            statusCode() shouldBe 200
            bodyToDto<List<StockDto>>().run {
                size shouldBe 6
            }
        }

        // only store-3 should have 2 products
        client.getStoreStocks(includeStores = listOf("store-3")).run {
            statusCode() shouldBe 200
            bodyToDto<List<StockDto>>().run {
                size shouldBe 2
                map { it.storeId }.distinct() shouldContainExactly listOf("store-3")
                find { it.productId == "product-10" }?.stock shouldBe 21
                find { it.productId == "product-20" }?.stock shouldBe 5
            }
        }

        // if we combine store-3 and product-10 then we should have only 1 result
        client.getStoreStocks(includeStores = listOf("store-3"), includeProducts = listOf("product-10")).run {
            statusCode() shouldBe 200
            bodyToDto<List<StockDto>>().run {
                size shouldBe 1
                first().run {
                    storeId shouldBe "store-3"
                    productId shouldBe "product-10"
                    stock shouldBe 21
                }
            }
        }


        // if we combine 2 products then we should have 3 results
        client.getStoreStocks(includeProducts = listOf("product-30", "product-20")).run {
            statusCode() shouldBe 200
            bodyToDto<List<StockDto>>().run {
                size shouldBe 3
                map { it.storeId } shouldContainExactlyInAnyOrder listOf("store-1", "store-2", "store-3")
            }
        }
    }


    @Test
    fun `reserve stock`() {
        client.createOrUpdateStock("gamma-1", "accuboor", SetStockDto(25)).statusCode() shouldBe 204

        client.reserveStock("gamma-1", "accuboor", SetReservationDto("christophe", 5)).statusCode() shouldBe 204

        client.reserveStock("gamma-1", "accuboor", SetReservationDto("joost", 15)).statusCode() shouldBe 204

        client.getStockReservations(includeProducts = listOf("accuboor")).run {
            statusCode() shouldBe 200
            bodyToDto<List<StockReservationDto>>().run {
                size shouldBe 2
                find { it.userId == "christophe" }?.stock shouldBe 5
                find { it.userId == "joost" }?.stock shouldBe 15
            }
        }

        // not enough stock for arie, should fail
        client.reserveStock("gamma-1", "accuboor", SetReservationDto("arie", 10)).statusCode() shouldBe 422

        // we delete a reservation
        client.deleteReservation("gamma-1", "accuboor", "joost").statusCode() shouldBe 204

        client.reserveStock("gamma-1", "accuboor", SetReservationDto("arie", 10)).statusCode() shouldBe 204

        client.getStockReservations(includeProducts = listOf("accuboor")).run {
            statusCode() shouldBe 200
            bodyToDto<List<StockReservationDto>>().run {
                size shouldBe 2
                find { it.userId == "christophe" }?.stock shouldBe 5
                find { it.userId == "joost" }?.stock shouldBe null
            }
        }
    }
}
