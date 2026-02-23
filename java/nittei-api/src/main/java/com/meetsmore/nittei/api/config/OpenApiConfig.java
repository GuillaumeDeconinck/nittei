package com.meetsmore.nittei.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nitteiOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Nittei API")
                .version("1.0.0")
                .description("OpenAPI documentation for the Nittei API"))
            .components(new Components().addSecuritySchemes(
                "api_key",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("x-api-key")
            ));
    }
}
