package be.vives.pizzastore.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ProblemDetail;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PizzaStore API",
                version = "1.0.0",
                description = """
                        RESTful API for managing a pizza store, including pizzas, customers, and orders.
                        
                        **Authentication**: This API uses JWT Bearer token authentication.
                        
                        **Authorization**:
                        - **Anonymous users**: Can view available pizzas (GET /api/pizzas)
                        - **CUSTOMER role**: Can place orders and manage their profile
                        - **ADMIN role**: Can manage pizzas (create, update, delete) and view all orders
                        """,
                contact = @Contact(
                        name = "VIVES",
                        email = "yves.seurynck@vives.be",
                        url = "https://www.vives.be"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Development"),
                @Server(url = "https://api.pizzastore.example.com", description = "Production")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication token. Obtain a token by registering or logging in via /api/auth/register or /api/auth/login"
)
public class OpenApiConfig {

    /**
     * Every error response of this API is an RFC 7807 {@link ProblemDetail} (see GlobalExceptionHandler).
     * An {@code @ApiResponse} without {@code content} would otherwise be documented with the method's return type
     * (a {@code PizzaResponse} for a 404, ...). This customizer gives all 4xx/5xx responses the ProblemDetail schema
     * once, instead of repeating {@code content = @Content(...)} on every error response of every endpoint.
     */
    @Bean
    public OpenApiCustomizer problemDetailForErrorResponses() {
        return openApi -> {
            Schema<?> problemDetail = ModelConverters.getInstance().read(ProblemDetail.class).get("ProblemDetail");
            // extension members such as "errors" are written at the top level of the JSON, not inside "properties"
            problemDetail.getProperties().remove("properties");
            problemDetail.setAdditionalProperties(true);
            openApi.getComponents().addSchemas("ProblemDetail", problemDetail);
            Content problemJson = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")));

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation ->
                    operation.getResponses().forEach((code, response) -> {
                        if (code.startsWith("4") || code.startsWith("5")) {
                            response.setContent(problemJson);
                        }
                    })));
        };
    }
}
