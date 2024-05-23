package net.intergamma.stock

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*

@SpringBootApplication
class StockServiceApplication

fun main(args: Array<String>) {
	runApplication<StockServiceApplication>(*args)
}

/**
 * This class exists here because lib-swagger doesn't support spring boot 3.
 */
@Configuration
class SwaggerWebMVCConfig : WebMvcConfigurer {
	override fun addViewControllers(registry: ViewControllerRegistry) {
		registry.addViewController("/").setViewName("redirect:/swagger-ui.html")
	}

	override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/")
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/")
	}
}

@Configuration
class SwaggerConfig {
	@Bean
	fun customOpenAPI(): OpenAPI? {
		return OpenAPI()
			.info(
				Info()
					.title("IG stock service")
					.description("Stock service")
			)
	}
}