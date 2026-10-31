package com.unifsa.confeitaria.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI confeitariaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API da Confeitaria")
                .version("1.0.0")
                .description("API REST para gerenciar clientes, bolos e pedidos de uma confeitaria."));
    }
}
