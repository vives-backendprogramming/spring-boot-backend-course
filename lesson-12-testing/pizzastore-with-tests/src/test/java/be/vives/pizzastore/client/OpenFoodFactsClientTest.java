package be.vives.pizzastore.client;

import be.vives.pizzastore.config.HttpClientConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Client-side test: the class under test CALLS another API, it is not called by one.
 * <p>
 * {@code @RestClientTest} loads only the infrastructure for outgoing calls and replaces the HTTP transport by a
 * {@link MockRestServiceServer}. The declarative client is an interface, so there is no class to name in the
 * annotation: {@code @Import(HttpClientConfig.class)} makes Spring generate the {@link OpenFoodFactsClient} proxy,
 * exactly as in the real application. The test first tells the mock server which request to expect and which
 * response to return, then calls the client and asserts what comes out. No real HTTP call is ever made, so the
 * test works offline, never depends on someone else's server and cannot exhaust Open Food Facts' rate limit.
 * <p>
 * The {@code spring.http.serviceclient.openfoodfacts.*} properties are the same ones the application uses,
 * pointed to a fake host.
 * <p>
 * Why the {@code @ImportAutoConfiguration}? The {@code @RestClientTest} slice does not include the auto-configuration
 * for declarative clients. Without it the generated proxy would use a plain {@code RestClient} that ignores the
 * {@code spring.http.serviceclient.*} properties and that the mock server cannot intercept
 * ("Unable to use auto-configured MockRestServiceServer since a mock server customizer has not been bound").
 */
@RestClientTest(properties = {
        "spring.http.serviceclient.openfoodfacts.base-url=https://off.test",
        "spring.http.serviceclient.openfoodfacts.default-header.User-Agent=PizzaStoreTest/1.0 (test@example.com)"
})
@Import(HttpClientConfig.class)
@ImportAutoConfiguration({HttpServiceClientAutoConfiguration.class, HttpServiceClientPropertiesAutoConfiguration.class})
class OpenFoodFactsClientTest {

    @Autowired
    private OpenFoodFactsClient client;

    @Autowired
    private MockRestServiceServer server;

    @Test
    void getProduct_KnownBarcode_MapsTheNutrimentsPer100g() {
        server.expect(requestTo("https://off.test/api/v2/product/3017620422003.json?fields=code,product_name,nutriments"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("User-Agent", "PizzaStoreTest/1.0 (test@example.com)"))   // Open Food Facts requires one
                .andRespond(withSuccess("""
                        {
                          "code": "3017620422003",
                          "status": 1,
                          "product": {
                            "product_name": "Nutella",
                            "nutriments": {
                              "energy-kcal_100g": 539,
                              "proteins_100g": 6.3,
                              "carbohydrates_100g": 57.5,
                              "fat_100g": 30.9,
                              "salt_100g": 0.107
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        OpenFoodFactsResponse response = client.getProduct("3017620422003");

        assertThat(response.status()).isEqualTo(1);
        assertThat(response.product().productName()).isEqualTo("Nutella");
        assertThat(response.product().nutriments().energyKcal100g()).isEqualByComparingTo("539");
        assertThat(response.product().nutriments().proteins100g()).isEqualByComparingTo("6.3");
        assertThat(response.product().nutriments().carbohydrates100g()).isEqualByComparingTo("57.5");
        assertThat(response.product().nutriments().fat100g()).isEqualByComparingTo("30.9");   // "salt_100g" is simply ignored
        server.verify();                                                                       // the expected request really happened
    }

    @Test
    void getProduct_UnknownBarcodeWithStatus200_ReturnsStatusZeroAndNoProduct() {
        // Open Food Facts sometimes answers 200 for an unknown product: the body says "status": 0
        server.expect(requestTo("https://off.test/api/v2/product/0000000000017.json?fields=code,product_name,nutriments"))
                .andRespond(withSuccess("""
                        {"code": "0000000000017", "status": 0, "status_verbose": "product not found"}
                        """, MediaType.APPLICATION_JSON));

        OpenFoodFactsResponse response = client.getProduct("0000000000017");

        assertThat(response.status()).isZero();
        assertThat(response.product()).isNull();
    }

    @Test
    void getProduct_UnknownBarcodeWithStatus404_ThrowsNotFound() {
        server.expect(requestTo("https://off.test/api/v2/product/0000000000017.json?fields=code,product_name,nutriments"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": \"0000000000017\", \"status\": 0, \"status_verbose\": \"product not found\"}"));

        assertThatThrownBy(() -> client.getProduct("0000000000017"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    void getProduct_ServerError_ThrowsServiceUnavailable() {
        server.expect(requestTo("https://off.test/api/v2/product/3017620422003.json?fields=code,product_name,nutriments"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getProduct("3017620422003"))
                .isInstanceOf(HttpServerErrorException.ServiceUnavailable.class);
    }

    @Test
    void getProduct_RateLimited_ThrowsTooManyRequests() {
        server.expect(requestTo("https://off.test/api/v2/product/3017620422003.json?fields=code,product_name,nutriments"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.getProduct("3017620422003"))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);
    }
}
