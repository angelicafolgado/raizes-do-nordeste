package br.com.raizesdonordeste.config;

import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;

import java.io.IOException;

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {
    @Bean
    OpenApiCustomizer documentedContract() throws IOException {
        OpenAPI contract;
        try (var input = OpenApiConfiguration.class.getResourceAsStream("/openapi-contract.json")) {
            if (input == null) throw new IOException("Contrato OpenAPI não encontrado.");
            contract = Json.mapper().readValue(input, OpenAPI.class);
        }
        return generated -> {
            // Keep Spring's discovered routes; fail if documentation names an absent operation.
            contract.getPaths().forEach((path, documented) -> {
                var discovered = generated.getPaths().get(path);
                if (discovered == null) throw new IllegalStateException("Rota documentada ausente: " + path);
                documented.readOperationsMap().forEach((method, operation) -> {
                    if (!discovered.readOperationsMap().containsKey(method))
                        throw new IllegalStateException("Operação documentada ausente: " + method + " " + path);
                    discovered.operation(method, operation);
                });
            });
            generated.setInfo(contract.getInfo());
            generated.setComponents(contract.getComponents());
            generated.setTags(contract.getTags());
        };
    }
}
