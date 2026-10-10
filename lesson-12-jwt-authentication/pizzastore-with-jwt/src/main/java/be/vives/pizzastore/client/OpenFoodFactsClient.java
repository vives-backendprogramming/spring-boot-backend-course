package be.vives.pizzastore.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for the free <a href="https://openfoodfacts.github.io/openfoodfacts-server/api/">Open Food
 * Facts</a> API: an interface, no implementation. Spring generates the proxy (see {@code HttpClientConfig}).
 * <p>
 * The base URL, the mandatory {@code User-Agent} header and the timeouts are not here but in
 * {@code application.properties} ({@code spring.http.serviceclient.openfoodfacts.*}).
 */
@HttpExchange(url = "/api/v2", accept = "application/json")
public interface OpenFoodFactsClient {

    /** {@code fields} limits the response to what we use: the full product document is huge. */
    @GetExchange("/product/{barcode}.json?fields=code,product_name,nutriments")
    OpenFoodFactsResponse getProduct(@PathVariable String barcode);
}
