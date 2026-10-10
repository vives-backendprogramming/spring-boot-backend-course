package be.vives.pizzastore.integration;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.FileStorageService;
import be.vives.pizzastore.service.PizzaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring Boot 4 bean overriding inside a full-stack test.
 * <ul>
 *   <li>{@code @MockitoBean}: replaces the real bean by a mock (here: no files are written to disk)</li>
 *   <li>{@code @MockitoSpyBean}: wraps the real bean, so it keeps working but calls can be verified</li>
 * </ul>
 * Both live in {@code org.springframework.test.context.bean.override.mockito} (Spring Framework 7) and replace
 * the deprecated {@code @MockBean} / {@code @SpyBean}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:bean-override-test")
@AutoConfigureRestTestClient
class BeanOverrideIntegrationTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoSpyBean
    private PizzaService pizzaService;

    @Test
    void uploadImage_WithMockedStorage_StoresReturnedUrlOnThePizza() {
        PizzaResponse pizza = client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePizzaRequest("Upload test", new BigDecimal("9.00"), "desc", true, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PizzaResponse.class)
                .returnResult().getResponseBody();

        // the mock decides what the "storage" returns; the real PizzaService, mapper and repository do the rest
        when(fileStorageService.storeFile(any(), eq(pizza.id()))).thenReturn("http://localhost/images/test.png");

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("image", new ByteArrayResource("fake image bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "test.png";
            }
        });

        client.post().uri("/api/pizzas/{id}/image", pizza.id())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.imageUrl").isEqualTo("http://localhost/images/test.png");

        // the image URL really is persisted: read the pizza again through the API
        client.get().uri("/api/pizzas/{id}", pizza.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.imageUrl").isEqualTo("http://localhost/images/test.png");
    }

    @Test
    void createPizza_ThroughTheApi_CallsTheRealServiceExactlyOnce() {
        client.post().uri("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePizzaRequest("Spy test", new BigDecimal("9.00"), "desc", true, null))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(PizzaResponse.class)
                .value(created -> assertThat(created.id()).isNotNull());   // the spy delegated to the real method

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }
}
