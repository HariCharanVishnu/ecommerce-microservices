
package com.ecommerce.product_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("E-Commerce Product Service API")
                        .description("REST APIs for managing products in the E-Commerce Microservices Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Development Team")
                                .email("support@example.com")));
    }
}
