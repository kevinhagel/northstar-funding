package com.northstar.funding.rest.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for REST API documentation.
 *
 * <p>Swagger UI available at: http://localhost:8090/swagger-ui.html
 * <p>OpenAPI JSON available at: http://localhost:8090/v3/api-docs
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NorthStar Funding Discovery API")
                        .version("1.0.0")
                        .description("""
                                Event-driven search workflow API for discovering funding opportunities.

                                **Workflow:**
                                1. POST /api/search/execute - Initiates search with categories
                                2. System generates AI-powered search queries
                                3. Queries published to Kafka topic `search-requests`
                                4. Kafka consumers execute searches across multiple engines
                                5. Results validated, scored, and stored as FundingSourceCandidates

                                **Use sessionId from response to track search progress in database.**
                                """)
                        .license(new License()
                                .name("Proprietary")
                                .url("https://github.com/kevinhagel/northstar-funding"))
                );
    }
}
