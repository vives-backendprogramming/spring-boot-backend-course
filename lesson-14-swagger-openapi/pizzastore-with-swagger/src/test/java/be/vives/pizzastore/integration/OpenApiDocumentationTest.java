package be.vives.pizzastore.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The OpenAPI document is part of the API: front-end developers generate clients from it and read it in Swagger UI.
 * These tests check the generated /v3/api-docs, without a token, the way a client developer sees it:
 * public documentation, a lock only on secured endpoints, ProblemDetail for errors, and no parameters
 * that the server takes from the JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:openapi-documentation-test")
@AutoConfigureRestTestClient
class OpenApiDocumentationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void apiDocs_ArePublic_AndDescribeTheJwtScheme() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("PizzaStore API")
                .jsonPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
                .jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").isEqualTo("JWT");
    }

    @Test
    void server_IsTheUrlTheDocumentWasRequestedOn(@LocalServerPort int port) {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                .jsonPath("$.openapi").isEqualTo("3.1.0")
                .jsonPath("$.servers[0].url").isEqualTo("http://localhost:" + port);
    }

    @Test
    void yamlAndSwaggerUi_ArePublic() {
        client.get().uri("/v3/api-docs.yaml").exchange().expectStatus().isOk();
        client.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
    }

    @Test
    void onlySecuredEndpoints_HaveALock() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                // no global security requirement: it would also put a lock on the public endpoints
                .jsonPath("$.security").doesNotExist()
                .jsonPath("$.paths['/api/pizzas'].get.security").doesNotExist()
                .jsonPath("$.paths['/api/pizzas/{id}'].get.security").doesNotExist()
                .jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist()
                .jsonPath("$.paths['/api/auth/register'].post.security").doesNotExist()
                .jsonPath("$.paths['/api/pizzas'].post.security[0].bearerAuth").exists()
                .jsonPath("$.paths['/api/orders'].post.security[0].bearerAuth").exists()
                .jsonPath("$.paths['/api/customers'].get.security[0].bearerAuth").exists()
                .jsonPath("$.paths['/api/customers/me'].get.security[0].bearerAuth").exists();
    }

    @Test
    void errorResponses_AreDocumentedAsProblemDetail() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                .jsonPath("$.components.schemas.ProblemDetail.properties.status").exists()
                .jsonPath("$.paths['/api/pizzas/{id}'].get.responses['404'].content['application/problem+json'].schema['$ref']")
                .isEqualTo("#/components/schemas/ProblemDetail")
                .jsonPath("$.paths['/api/orders'].post.responses['422'].content['application/problem+json']").exists()
                .jsonPath("$.paths['/api/pizzas/{id}'].get.responses['200'].content['application/json'].schema['$ref']")
                .isEqualTo("#/components/schemas/PizzaResponse");
    }

    @Test
    void currentUser_IsNotAParameter() {
        // the e-mail address comes from the JWT (@CurrentUser), so Swagger UI must not ask for it
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                .jsonPath("$.paths['/api/customers/me'].get.parameters").doesNotExist()
                .jsonPath("$.paths['/api/orders'].post.parameters").doesNotExist();
    }

    @Test
    void pageable_IsDocumentedAsSeparateQueryParameters() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                .jsonPath("$.paths['/api/pizzas'].get.parameters[?(@.name == 'page')].in").isEqualTo("query")
                .jsonPath("$.paths['/api/pizzas'].get.parameters[?(@.name == 'size')].in").isEqualTo("query")
                .jsonPath("$.paths['/api/pizzas'].get.parameters[?(@.name == 'pageable')]").isEmpty();
    }

    @Test
    void validationAnnotations_EndUpInTheSchemas() {
        client.get().uri("/v3/api-docs")
                .exchange()
                .expectBody()
                .jsonPath("$.components.schemas.RegisterRequest.required[?(@ == 'name')]").isNotEmpty()
                .jsonPath("$.components.schemas.RegisterRequest.required[?(@ == 'email')]").isNotEmpty()
                .jsonPath("$.components.schemas.RegisterRequest.required[?(@ == 'password')]").isNotEmpty()
                .jsonPath("$.components.schemas.RegisterRequest.required[?(@ == 'phone')]").isEmpty()
                .jsonPath("$.components.schemas.RegisterRequest.properties.password.minLength").isEqualTo(6)
                .jsonPath("$.components.schemas.RegisterRequest.properties.email.format").isEqualTo("email")
                .jsonPath("$.components.schemas.ImportNutritionRequest.properties.barcode.pattern").exists()
                // @PositiveOrZero is not translated by springdoc: NutritionalInfoRequest documents it with @Schema(minimum = "0")
                .jsonPath("$.components.schemas.NutritionalInfoRequest.properties.calories.minimum").isEqualTo(0);
    }
}
