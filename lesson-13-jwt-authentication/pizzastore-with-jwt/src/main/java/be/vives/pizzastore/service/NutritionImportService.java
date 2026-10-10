package be.vives.pizzastore.service;

import be.vives.pizzastore.client.OpenFoodFactsClient;
import be.vives.pizzastore.client.OpenFoodFactsResponse;
import be.vives.pizzastore.dto.request.NutritionalInfoRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fills the {@code NutritionalInfo} of a pizza with data from Open Food Facts.
 * <p>
 * Deliberately NOT {@code @Transactional}: a database transaction (and its connection) must not stay open while
 * we wait for somebody else's server. The two database steps are short transactions in {@link PizzaService}.
 */
@Service
public class NutritionImportService {

    private static final Logger log = LoggerFactory.getLogger(NutritionImportService.class);

    private final OpenFoodFactsClient openFoodFactsClient;
    private final PizzaService pizzaService;

    public NutritionImportService(OpenFoodFactsClient openFoodFactsClient, PizzaService pizzaService) {
        this.openFoodFactsClient = openFoodFactsClient;
        this.pizzaService = pizzaService;
    }

    public PizzaResponse importFromBarcode(Long pizzaId, String barcode) {
        pizzaService.findById(pizzaId);   // 404 for an unknown pizza BEFORE we spend a request of our limited quota

        OpenFoodFactsResponse response = fetchProduct(barcode);
        NutritionalInfoRequest nutrition = toNutritionalInfo(barcode, response);

        log.info("Importing nutrition of '{}' ({}) into pizza {}", response.product().productName(), barcode, pizzaId);
        return pizzaService.updateNutritionalInfo(pizzaId, nutrition);
    }

    private OpenFoodFactsResponse fetchProduct(String barcode) {
        try {
            return openFoodFactsClient.getProduct(barcode);
        } catch (HttpClientErrorException.NotFound e) {
            throw unknownBarcode(barcode);
        } catch (RestClientException e) {
            // 5xx, 429 (rate limit), timeouts, connection refused, unreadable body, ...
            log.error("Open Food Facts call failed for barcode {}", barcode, e);
            throw new ExternalServiceException("Open Food Facts is currently unavailable, please try again later", e);
        }
    }

    private NutritionalInfoRequest toNutritionalInfo(String barcode, OpenFoodFactsResponse response) {
        // Open Food Facts can answer HTTP 200 with status 0 for an unknown barcode
        if (response == null || response.product() == null || Integer.valueOf(0).equals(response.status())) {
            throw unknownBarcode(barcode);
        }
        OpenFoodFactsResponse.Nutriments n = response.product().nutriments();
        if (n == null || n.energyKcal100g() == null || n.proteins100g() == null
                || n.carbohydrates100g() == null || n.fat100g() == null) {
            // The data is crowd-sourced: many products miss values
            throw new BusinessException("Open Food Facts has no complete nutritional data (kcal, protein, carbohydrates, fat) for barcode " + barcode);
        }
        return new NutritionalInfoRequest(
                n.energyKcal100g().setScale(0, RoundingMode.HALF_UP).intValueExact(),
                scale(n.proteins100g()),
                scale(n.carbohydrates100g()),
                scale(n.fat100g()));
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException unknownBarcode(String barcode) {
        return new BusinessException("Open Food Facts does not know a product with barcode " + barcode);
    }
}
