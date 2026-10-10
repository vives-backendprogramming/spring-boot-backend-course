package be.vives.pizzastore.service;

import be.vives.pizzastore.client.OpenFoodFactsClient;
import be.vives.pizzastore.client.OpenFoodFactsResponse;
import be.vives.pizzastore.client.OpenFoodFactsResponse.Nutriments;
import be.vives.pizzastore.client.OpenFoodFactsResponse.Product;
import be.vives.pizzastore.dto.request.NutritionalInfoRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.exception.BusinessException;
import be.vives.pizzastore.exception.ExternalServiceException;
import be.vives.pizzastore.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit test (no Spring) for the logic AROUND the external call: which failure of Open Food Facts becomes which
 * PizzaStore exception. Both collaborators are Mockito mocks, so no HTTP is involved at all;
 * the HTTP side is covered by {@code OpenFoodFactsClientTest}.
 */
@ExtendWith(MockitoExtension.class)
class NutritionImportServiceTest {

    private static final String BARCODE = "3017620422003";

    @Mock
    private OpenFoodFactsClient openFoodFactsClient;

    @Mock
    private PizzaService pizzaService;

    @InjectMocks
    private NutritionImportService service;

    private OpenFoodFactsResponse found(BigDecimal kcal, BigDecimal protein, BigDecimal carbs, BigDecimal fat) {
        return new OpenFoodFactsResponse(BARCODE, 1,
                new Product("Nutella", new Nutriments(kcal, protein, carbs, fat)));
    }

    @Test
    void importFromBarcode_CompleteProduct_StoresRoundedValues() {
        PizzaResponse expected = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);
        when(openFoodFactsClient.getProduct(BARCODE)).thenReturn(
                found(new BigDecimal("538.6"), new BigDecimal("6.305"), new BigDecimal("57.5"), new BigDecimal("30.9")));
        when(pizzaService.updateNutritionalInfo(any(), any())).thenReturn(expected);

        PizzaResponse result = service.importFromBarcode(1L, BARCODE);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<NutritionalInfoRequest> captor = ArgumentCaptor.forClass(NutritionalInfoRequest.class);
        verify(pizzaService).updateNutritionalInfo(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().calories()).isEqualTo(539);                    // 538.6 -> Integer column
        assertThat(captor.getValue().protein()).isEqualByComparingTo("6.31");       // 2 decimals, half up
        assertThat(captor.getValue().carbohydrates()).isEqualByComparingTo("57.50");
        assertThat(captor.getValue().fat()).isEqualByComparingTo("30.90");
    }

    @Test
    void importFromBarcode_UnknownPizza_NeverCallsOpenFoodFacts() {
        when(pizzaService.findById(999L)).thenThrow(new ResourceNotFoundException("Pizza", 999L));

        assertThatThrownBy(() -> service.importFromBarcode(999L, BARCODE))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(openFoodFactsClient);   // do not waste a request of the 15/minute quota
    }

    @Test
    void importFromBarcode_Http404_BecomesBusinessException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenThrow(
                HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Open Food Facts does not know a product with barcode " + BARCODE);
        verify(pizzaService, never()).updateNutritionalInfo(any(), any());
    }

    @Test
    void importFromBarcode_Status0With200_BecomesBusinessException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenReturn(new OpenFoodFactsResponse(BARCODE, 0, null));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not know a product");
    }

    @Test
    void importFromBarcode_MissingValue_BecomesBusinessExceptionAndStoresNothing() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenReturn(
                found(new BigDecimal("539"), new BigDecimal("6.3"), null, new BigDecimal("30.9")));   // no carbohydrates

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no complete nutritional data");
        verify(pizzaService, never()).updateNutritionalInfo(any(), any());
    }

    @Test
    void importFromBarcode_ProductWithoutNutriments_BecomesBusinessException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenReturn(
                new OpenFoodFactsResponse(BARCODE, 1, new Product("Mystery", null)));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void importFromBarcode_ServerError_BecomesExternalServiceException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenThrow(
                HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage("Open Food Facts is currently unavailable, please try again later")
                .hasCauseInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void importFromBarcode_RateLimited_BecomesExternalServiceException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenThrow(
                HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void importFromBarcode_Timeout_BecomesExternalServiceException() {
        when(openFoodFactsClient.getProduct(BARCODE)).thenThrow(
                new ResourceAccessException("I/O error: Read timed out"));

        assertThatThrownBy(() -> service.importFromBarcode(1L, BARCODE))
                .isInstanceOf(ExternalServiceException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);
    }
}
