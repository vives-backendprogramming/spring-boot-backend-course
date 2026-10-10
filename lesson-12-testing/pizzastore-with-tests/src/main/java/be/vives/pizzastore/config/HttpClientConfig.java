package be.vives.pizzastore.config;

import be.vives.pizzastore.client.OpenFoodFactsClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registers the declarative HTTP client. Spring Boot creates a {@code RestClient} for the group
 * {@code openfoodfacts}, configured by the {@code spring.http.serviceclient.openfoodfacts.*} properties,
 * and a proxy bean that implements {@link OpenFoodFactsClient} on top of it.
 */
@Configuration
@ImportHttpServices(group = "openfoodfacts", types = OpenFoodFactsClient.class)
public class HttpClientConfig {
}
