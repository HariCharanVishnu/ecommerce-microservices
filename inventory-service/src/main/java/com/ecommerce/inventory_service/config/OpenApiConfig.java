
package com.ecommerce.inventory_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce Inventory Service API")
                        .description(
                                "REST APIs for inventory management, " +
                                        "stock reservation and stock release."
                        )
                        .version("1.0.0"));
    }
}
