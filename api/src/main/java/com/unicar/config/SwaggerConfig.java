package com.unicar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // Acessar por http://localhost:8080/swagger-ui.html
    @Bean
    public OpenAPI unicarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("UniCar API")
                        .description("API para gerenciamento de caronas universitárias")
                        .version("1.0.0"));
    }

}
