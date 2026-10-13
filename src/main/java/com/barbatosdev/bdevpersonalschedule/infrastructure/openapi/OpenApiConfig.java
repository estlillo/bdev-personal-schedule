package com.barbatosdev.bdevpersonalschedule.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI rehabSchedulerOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Rehab Scheduler API")
            .version("v1")
            .description("API local para gestion de profesionales y agenda auxiliar."));
    }
}

