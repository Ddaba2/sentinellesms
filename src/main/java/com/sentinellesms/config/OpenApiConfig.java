package com.sentinellesms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI sentinelleSmsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sentinelle Mali API")
                        .description("API Sentinelle Mali — analyse assistée, vérification liens/numéros, "
                                + "signalements anonymisés, moteur de règles, contenus FR et back-office "
                                + "(rôles, modération, statistiques, audit). Bambara/audio hors scope.")
                        .version("v2"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
