package be.vives.pizzastore.service;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.mapper.PizzaMapper;
import be.vives.pizzastore.repository.PizzaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit test for PizzaService (NO Spring context).
 * 
 * Uses Mockito's @Mock to create mocks and @InjectMocks to inject them.
 * @ExtendWith(MockitoExtension.class) is REQUIRED for this to work (even in Spring Boot 3).
 * 
 * This is DIFFERENT from @MockBean which is used with Spring test slices like @WebMvcTest.
 * - @Mock = Mockito mock (fast, no Spring)
 * - @MockBean = Spring-managed mock (slower, with Spring context)
 */
@ExtendWith(MockitoExtension.class)  // Required for @Mock and @InjectMocks
class PizzaServiceTest {

    @Mock  // Pure Mockito mock (not @MockBean!)
    private PizzaRepository pizzaRepository;

    @Mock  // Pure Mockito mock (not @MockBean!)
    private PizzaMapper pizzaMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks  // Injects the mocks above
    private PizzaService pizzaService;

    private Pizza testPizza;
    private PizzaResponse testResponse;
    private CreatePizzaRequest createRequest;

    @BeforeEach
    void setup() {
        testPizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato and mozzarella");
        testPizza.setId(1L);

        testResponse = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", null, true, null);

        createRequest = new CreatePizzaRequest("Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", true, null);
    }

    @Test
    void findById_ExistingPizza_ReturnsPizzaResponse() {
        // Given
        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(testPizza));
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        Optional<PizzaResponse> result = pizzaService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        assertThat(result.get().name()).isEqualTo("Margherita");
        assertThat(result.get().price()).isEqualByComparingTo(new BigDecimal("8.50"));

        verify(pizzaRepository).findById(1L);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void findById_NonExistingPizza_ReturnsEmpty() {
        // Given
        when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PizzaResponse> result = pizzaService.findById(999L);

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findById(999L);
        verify(pizzaMapper, never()).toResponse(any());
    }

    @Test
    void findByPriceLessThan_MultiplePizzas_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId(2L);

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByPriceLessThan(new BigDecimal("10.00"))).thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByPriceLessThan(new BigDecimal("10.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByPriceLessThan(new BigDecimal("10.00"));
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void create_ValidRequest_ReturnsSavedPizza() {
        // Given
        when(pizzaMapper.toEntity(createRequest)).thenReturn(testPizza);
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.create(createRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Margherita");

        verify(pizzaMapper).toEntity(createRequest);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void update_ExistingPizza_ReturnsUpdatedPizza() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Margherita Special",
                new BigDecimal("9.50"),
                "Updated description",
                true,
                null
        );

        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(testPizza));
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        Optional<PizzaResponse> result = pizzaService.update(1L, updateRequest);

        // Then
        assertThat(result).isPresent();

        verify(pizzaRepository).findById(1L);
        verify(pizzaMapper).updateEntity(updateRequest, testPizza);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void update_NonExistingPizza_ReturnsEmpty() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Updated Name",
                new BigDecimal("10.00"),
                "Updated description",
                true,
                null
        );
        when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PizzaResponse> result = pizzaService.update(999L, updateRequest);

        // Then
        assertThat(result).isEmpty();
        verify(pizzaRepository).findById(999L);
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    void delete_ExistingPizza_ReturnsTrue() {
        // Given
        when(pizzaRepository.existsById(1L)).thenReturn(true);

        // When
        boolean result = pizzaService.delete(1L);

        // Then
        assertThat(result).isTrue();

        verify(pizzaRepository).existsById(1L);
        verify(pizzaRepository).deleteById(1L);
    }

    @Test
    void delete_NonExistingPizza_ReturnsFalse() {
        // Given
        when(pizzaRepository.existsById(999L)).thenReturn(false);

        // When
        boolean result = pizzaService.delete(999L);

        // Then
        assertThat(result).isFalse();

        verify(pizzaRepository).existsById(999L);
        verify(pizzaRepository, never()).deleteById(any());
    }

    @Test
    void findAll_WithPageable_ReturnsPageOfPizzas() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Pizza> pizzas = Arrays.asList(testPizza);
        Page<Pizza> pizzaPage = new PageImpl<>(pizzas, pageable, 1);
        
        when(pizzaRepository.findAll(pageable)).thenReturn(pizzaPage);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);

        // When
        Page<PizzaResponse> result = pizzaService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Margherita");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(pizzaRepository).findAll(pageable);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void findAll_WithPageable_EmptyPage_ReturnsEmptyPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pizza> emptyPage = Page.empty();
        
        when(pizzaRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<PizzaResponse> result = pizzaService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(pizzaRepository).findAll(pageable);
        verify(pizzaMapper, never()).toResponse(any());
    }

    @Test
    void findByPriceBetween_MultipleResults_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId(2L);

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00")))
                .thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByPriceBetween(new BigDecimal("7.00"), new BigDecimal("9.00"));
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void findByPriceBetween_NoResults_ReturnsEmptyList() {
        // Given
        when(pizzaRepository.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00")))
                .thenReturn(List.of());
        when(pizzaMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        List<PizzaResponse> result = pizzaService.findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findByPriceBetween(new BigDecimal("100.00"), new BigDecimal("200.00"));
    }

    @Test
    void findByNameContaining_MultipleResults_ReturnsListOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId(2L);

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        List<PizzaResponse> responses = Arrays.asList(
                testResponse,
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        when(pizzaRepository.findByNameContainingIgnoreCase("mar")).thenReturn(pizzas);
        when(pizzaMapper.toResponseList(pizzas)).thenReturn(responses);

        // When
        List<PizzaResponse> result = pizzaService.findByNameContaining("mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findByNameContainingIgnoreCase("mar");
        verify(pizzaMapper).toResponseList(pizzas);
    }

    @Test
    void findByNameContaining_NoResults_ReturnsEmptyList() {
        // Given
        when(pizzaRepository.findByNameContainingIgnoreCase("xyz")).thenReturn(List.of());
        when(pizzaMapper.toResponseList(List.of())).thenReturn(List.of());

        // When
        List<PizzaResponse> result = pizzaService.findByNameContaining("xyz");

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findByNameContainingIgnoreCase("xyz");
    }

    @Test
    void uploadImage_ExistingPizza_ReturnsUpdatedPizza() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        String imageUrl = "/uploads/pizza-1.jpg";
        
        PizzaResponse updatedResponse = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", imageUrl, true, null);

        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(testPizza));
        when(fileStorageService.storeFile(mockFile, 1L)).thenReturn(imageUrl);
        when(pizzaRepository.save(testPizza)).thenReturn(testPizza);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(updatedResponse);

        // When
        Optional<PizzaResponse> result = pizzaService.uploadImage(1L, mockFile);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().imageUrl()).isEqualTo(imageUrl);

        verify(pizzaRepository).findById(1L);
        verify(fileStorageService).storeFile(mockFile, 1L);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void uploadImage_NonExistingPizza_ReturnsEmpty() {
        // Given
        MultipartFile mockFile = mock(MultipartFile.class);
        when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PizzaResponse> result = pizzaService.uploadImage(999L, mockFile);

        // Then
        assertThat(result).isEmpty();

        verify(pizzaRepository).findById(999L);
        verify(fileStorageService, never()).storeFile(any(), any());
        verify(pizzaRepository, never()).save(any());
    }

    @Test
    void create_WithNutritionalInfo_SetsBidirectionalRelationship() {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Margherita", 
                new BigDecimal("8.50"), 
                "Classic tomato and mozzarella", 
                true, 
                null
        );
        
        Pizza pizzaWithNutritionalInfo = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        be.vives.pizzastore.domain.NutritionalInfo nutritionalInfo = new be.vives.pizzastore.domain.NutritionalInfo();
        pizzaWithNutritionalInfo.setNutritionalInfo(nutritionalInfo);
        
        when(pizzaMapper.toEntity(request)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaRepository.save(pizzaWithNutritionalInfo)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaMapper.toResponse(pizzaWithNutritionalInfo)).thenReturn(testResponse);

        // When
        PizzaResponse result = pizzaService.create(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(nutritionalInfo.getPizza()).isEqualTo(pizzaWithNutritionalInfo);
        
        verify(pizzaMapper).toEntity(request);
        verify(pizzaRepository).save(pizzaWithNutritionalInfo);
        verify(pizzaMapper).toResponse(pizzaWithNutritionalInfo);
    }

    @Test
    void update_WithNutritionalInfo_SetsBidirectionalRelationship() {
        // Given
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Margherita Special",
                new BigDecimal("9.50"),
                "Updated description",
                true,
                null
        );

        Pizza pizzaWithNutritionalInfo = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
        pizzaWithNutritionalInfo.setId(1L);
        be.vives.pizzastore.domain.NutritionalInfo nutritionalInfo = new be.vives.pizzastore.domain.NutritionalInfo();
        pizzaWithNutritionalInfo.setNutritionalInfo(nutritionalInfo);

        when(pizzaRepository.findById(1L)).thenReturn(Optional.of(pizzaWithNutritionalInfo));
        when(pizzaRepository.save(pizzaWithNutritionalInfo)).thenReturn(pizzaWithNutritionalInfo);
        when(pizzaMapper.toResponse(pizzaWithNutritionalInfo)).thenReturn(testResponse);

        // When
        Optional<PizzaResponse> result = pizzaService.update(1L, updateRequest);

        // Then
        assertThat(result).isPresent();
        assertThat(nutritionalInfo.getPizza()).isEqualTo(pizzaWithNutritionalInfo);

        verify(pizzaRepository).findById(1L);
        verify(pizzaMapper).updateEntity(updateRequest, pizzaWithNutritionalInfo);
        verify(pizzaRepository).save(pizzaWithNutritionalInfo);
    }
}
