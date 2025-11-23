# Lesson 11: Testing Spring Boot Applications

**Writing Comprehensive Tests for Production-Ready APIs**

---

## 📋 Learning Objectives

By the end of this lesson, you will be able to:
- Write **unit tests** with JUnit 5 and Mockito
- Test **controllers** with MockMvc and @WebMvcTest
- Test **repositories** with @DataJpaTest
- Test **services** with mocked dependencies
- Write **integration tests** with @SpringBootTest
- Test **validation** and **exception handling**
- Use **AssertJ** for fluent assertions
- Test **JSON responses** with JSONPath
- Achieve high test coverage for production-ready code
- Follow testing best practices and patterns

---

## 📚 Table of Contents

1. [Why Testing Matters](#why-testing-matters)
2. [Testing Pyramid](#testing-pyramid)
3. [JUnit 5 Basics](#junit-5-basics)
4. [Spring Boot Test Support](#spring-boot-test-support)
5. [Testing Layers](#testing-layers)
   - [Repository Tests](#repository-tests-datajpatest)
   - [Service Tests](#service-tests-with-mockito)
   - [Controller Tests](#controller-tests-webmvctest)
   - [Integration Tests](#integration-tests-springboottest)
6. [Testing Validation](#testing-validation)
7. [Testing Exception Handling](#testing-exception-handling)
8. [AssertJ for Better Assertions](#assertj-for-better-assertions)
9. [Testing JSON Responses](#testing-json-responses)
10. [Best Practices](#best-practices)
11. [PizzaStore Test Suite](#complete-pizzastore-test-suite)
12. [Summary](#summary)

---

## ⚠️ Why Testing Matters

### Without Tests

```java
@Service
public class PizzaService {
    public PizzaResponse findById(Long id) {
        return pizzaRepository.findById(id)
                .map(pizzaMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pizza", id));
    }
}
```

**How do you know:**
- ✗ It actually works?
- ✗ It handles null values?
- ✗ It throws the right exception?
- ✗ It still works after refactoring?
- ✗ It doesn't break other code?

### With Tests

```java
@Test
void findById_ExistingPizza_ReturnsPizzaResponse() {
    // Given
    Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic");
    pizza.setId(1L);
    when(pizzaRepository.findById(1L)).thenReturn(Optional.of(pizza));
    
    // When
    PizzaResponse result = pizzaService.findById(1L);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.name()).isEqualTo("Margherita");
}

@Test
void findById_NonExistingPizza_ThrowsNotFoundException() {
    // Given
    when(pizzaRepository.findById(999L)).thenReturn(Optional.empty());
    
    // When / Then
    assertThatThrownBy(() -> pizzaService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Pizza with id 999 not found");
}
```

**Benefits:**
- ✅ Confidence in your code
- ✅ Catch bugs early
- ✅ Safe refactoring
- ✅ Living documentation
- ✅ Better design (testable code = good code)

---

## 🔺 Testing Pyramid

```
           ┌───────────────┐
           │   UI Tests    │  ← Few, slow, expensive
           │  (End-to-End) │
           └───────────────┘
         ┌─────────────────────┐
         │  Integration Tests  │  ← Some, moderate speed
         │  (@SpringBootTest)  │
         └─────────────────────┘
     ┌───────────────────────────────┐
     │        Unit Tests             │  ← Many, fast, cheap
     │  (Controllers, Services, etc) │
     └───────────────────────────────┘
```

### Unit Tests (70%)
- Test **single units** in isolation
- Use **mocks** for dependencies
- Fast (<10ms per test)
- Examples: Service methods, Mapper methods

### Integration Tests (20%)
- Test **multiple components** together
- Use **real database** (H2 in-memory)
- Medium speed (~100ms per test)
- Examples: Full REST endpoint, Repository queries

### End-to-End Tests (10%)
- Test **complete user flows**
- Use **real everything**
- Slow (seconds per test)
- Examples: Full user registration flow

**For this course, we focus on Unit Tests (70%) and Integration Tests (20%).**

---

## 🧪 JUnit 5 Basics

### Essential Annotations

```java
import org.junit.jupiter.api.*;

class CalculatorTest {

    @BeforeAll
    static void setupAll() {
        // Runs once before all tests
        System.out.println("Starting test suite");
    }

    @BeforeEach
    void setup() {
        // Runs before each test
        System.out.println("Setting up test");
    }

    @Test
    void add_TwoPositiveNumbers_ReturnsSum() {
        // Given
        int a = 5;
        int b = 3;
        
        // When
        int result = a + b;
        
        // Then
        assertEquals(8, result);
    }

    @Test
    @DisplayName("Should multiply two numbers correctly")
    void multiplyTest() {
        assertEquals(15, 5 * 3);
    }

    @Test
    @Disabled("Not implemented yet")
    void divideTest() {
        // TODO: implement
    }

    @AfterEach
    void tearDown() {
        // Runs after each test
        System.out.println("Cleaning up test");
    }

    @AfterAll
    static void tearDownAll() {
        // Runs once after all tests
        System.out.println("Finished test suite");
    }
}
```

### Assertions

```java
import static org.junit.jupiter.api.Assertions.*;

@Test
void assertionExamples() {
    // Basic assertions
    assertEquals(4, 2 + 2);
    assertNotEquals(5, 2 + 2);
    assertTrue(5 > 3);
    assertFalse(5 < 3);
    assertNull(null);
    assertNotNull("value");
    
    // Array/Collection assertions
    assertArrayEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3});
    
    // Exception assertions
    assertThrows(IllegalArgumentException.class, () -> {
        throw new IllegalArgumentException("Invalid");
    });
    
    // Timeout assertions
    assertTimeout(Duration.ofSeconds(1), () -> {
        // Fast operation
    });
    
    // Group assertions
    assertAll("person",
        () -> assertEquals("John", person.getName()),
        () -> assertEquals(30, person.getAge())
    );
}
```

---

## 🌱 Spring Boot Test Support

### Dependencies

Already included in `spring-boot-starter-test`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Includes:**
- JUnit 5
- Mockito
- AssertJ
- Hamcrest
- JSONAssert
- JsonPath
- Spring Test & Spring Boot Test

### Test Slices

Spring Boot provides specialized test slices for testing specific layers:

| Annotation | Purpose | What's Loaded | Mock Annotation |
|------------|---------|---------------|-----------------|
| `@WebMvcTest` | Test controllers | Spring MVC components only | `@MockBean` |
| `@DataJpaTest` | Test repositories | JPA components + in-memory DB | `@MockBean` |
| `@JsonTest` | Test JSON serialization | JSON marshallers | `@MockBean` |
| `@RestClientTest` | Test REST clients | REST client components | `@MockBean` |
| `@SpringBootTest` | Integration tests | **Entire application** | `@MockBean` |
| `@ExtendWith(MockitoExtension.class)` | **Pure unit tests** | **NO Spring context** | `@Mock` |

> **Key Difference:**
> - **With Spring** (`@WebMvcTest`, `@SpringBootTest`, etc.) → Use `@MockBean`
> - **Without Spring** (`@ExtendWith(MockitoExtension.class)`) → Use `@Mock`

---

## 🧪 Testing Layers

### Repository Tests (@DataJpaTest)

Test JPA repositories with an in-memory database.

#### Example: PizzaRepository Test

```java
package be.vives.pizzastore.repository;

import be.vives.pizzastore.domain.Pizza;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({be.vives.pizzastore.config.JpaConfig.class, be.vives.pizzastore.config.AuditorAwareImpl.class})  // Enable JPA Auditing
class PizzaRepositoryTest {

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_ExistingPizza_ReturnsPizza() {
        // Given
        Pizza pizza = new Pizza("Margherita", new BigDecimal("8.50"), "Classic tomato and mozzarella");
        Pizza saved = entityManager.persistAndFlush(pizza);

        // When
        Optional<Pizza> result = pizzaRepository.findById(saved.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Margherita");
        assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("8.50"));
    }

    @Test
    void findById_NonExistingPizza_ReturnsEmpty() {
        // When
        Optional<Pizza> result = pizzaRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByPriceLessThan_MultipleResults_ReturnsFilteredList() {
        // Given
        entityManager.persist(new Pizza("Cheap Pizza", new BigDecimal("5.00"), "Budget option"));
        entityManager.persist(new Pizza("Mid Pizza", new BigDecimal("10.00"), "Medium price"));
        entityManager.persist(new Pizza("Expensive Pizza", new BigDecimal("15.00"), "Premium"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("12.00"));

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Cheap Pizza", "Mid Pizza");
    }

    @Test
    void findByNameContainingIgnoreCase_PartialMatch_ReturnsMatches() {
        // Given
        entityManager.persist(new Pizza("Margherita", new BigDecimal("8.50"), "Classic"));
        entityManager.persist(new Pizza("Marinara", new BigDecimal("7.50"), "Simple"));
        entityManager.persist(new Pizza("Quattro Formaggi", new BigDecimal("11.00"), "Cheese"));
        entityManager.flush();

        // When
        List<Pizza> result = pizzaRepository.findByNameContainingIgnoreCase("mar");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(Pizza::getName)
                .containsExactlyInAnyOrder("Margherita", "Marinara");
    }

    @Test
    void save_NewPizza_GeneratesId() {
        // Given
        Pizza pizza = new Pizza("Test Pizza", new BigDecimal("9.99"), "Test description");

        // When
        Pizza saved = pizzaRepository.save(pizza);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
    }

    @Test
    void delete_ExistingPizza_RemovesFromDatabase() {
        // Given
        Pizza pizza = entityManager.persistAndFlush(new Pizza("To Delete", new BigDecimal("10.00"), "Will be deleted"));
        Long id = pizza.getId();

        // When
        pizzaRepository.deleteById(id);
        entityManager.flush();

        // Then
        Optional<Pizza> result = pizzaRepository.findById(id);
        assertThat(result).isEmpty();
    }
}
```

**Key Points:**
- `@DataJpaTest` loads only JPA components
- Uses in-memory H2 database
- Transactions are rolled back after each test
- `TestEntityManager` for setup/verification
- Fast execution (<100ms per test)
- `@Import` is needed to load JPA Auditing configuration for audit fields (createdAt, updatedAt, etc.)

---

### Service Tests (with Mockito)

Test services in isolation by mocking dependencies.

> **📝 Note on `@ExtendWith(MockitoExtension.class)`**
> 
> This annotation **is still required** for pure unit tests without Spring context in Spring Boot 3.
> 
> **When to use:**
> - ✅ **Pure unit tests** (service tests): Use `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
> - ✅ **Spring integration tests** (controller tests): Use `@WebMvcTest` or `@SpringBootTest` with `@MockBean`
> 
> The difference:
> - `@ExtendWith(MockitoExtension.class)` = Mockito without Spring (faster, pure unit test)
> - `@MockBean` = Mock within Spring context (for integration tests)

#### Example: PizzaService Test

```java
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Pure unit test - NO Spring context
// This is why we need @ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
class PizzaServiceTest {

    @Mock  // Mockito mock (not @MockBean!)
    private PizzaRepository pizzaRepository;

    @Mock  // Mockito mock (not @MockBean!)
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
    void findAll_MultiplePizzas_ReturnsPageOfResponses() {
        // Given
        Pizza pizza2 = new Pizza("Marinara", new BigDecimal("7.50"), "Simple");
        pizza2.setId(2L);

        List<Pizza> pizzas = Arrays.asList(testPizza, pizza2);
        Page<Pizza> pizzaPage = new PageImpl<>(pizzas);
        Pageable pageable = PageRequest.of(0, 10);

        when(pizzaRepository.findAll(pageable)).thenReturn(pizzaPage);
        when(pizzaMapper.toResponse(testPizza)).thenReturn(testResponse);
        when(pizzaMapper.toResponse(pizza2)).thenReturn(
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );

        // When
        Page<PizzaResponse> result = pizzaService.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(PizzaResponse::name)
                .containsExactly("Margherita", "Marinara");

        verify(pizzaRepository).findAll(pageable);
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
        PizzaResponse result = pizzaService.update(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();

        verify(pizzaRepository).findById(1L);
        verify(pizzaMapper).updateEntity(updateRequest, testPizza);
        verify(pizzaRepository).save(testPizza);
        verify(pizzaMapper).toResponse(testPizza);
    }

    @Test
    void update_NonExistingPizza_ReturnsNull() {
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
        PizzaResponse result = pizzaService.update(999L, updateRequest);

        // Then
        assertThat(result).isNull();
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
}
```

**Key Points:**
- `@ExtendWith(MockitoExtension.class)` enables Mockito
- `@Mock` creates mock objects
- `@InjectMocks` injects mocks into the service
- `when().thenReturn()` defines mock behavior
- `verify()` checks if methods were called
- Very fast (<10ms per test)

---

### Controller Tests (@WebMvcTest)

Test REST controllers with MockMvc, mocking the service layer.

> **📝 Note on `@MockBean`**
> 
> `@MockBean` **is NOT deprecated** in Spring Boot 3. It's the standard way to mock beans in Spring test context.
> 
> **When to use:**
> - ✅ **Spring integration tests**: Use `@MockBean` with `@WebMvcTest` or `@SpringBootTest`
> - ❌ **Pure unit tests**: Use `@Mock` with `@ExtendWith(MockitoExtension.class)` instead
> 
> The difference:
> - `@MockBean` = Mock managed by Spring context (slower, but tests Spring integration)
> - `@Mock` = Mock managed by Mockito (faster, pure unit test)

#### Example: PizzaController Test

```java
package be.vives.pizzastore.controller;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.service.PizzaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Spring integration test - loads Spring MVC context
// This is why we use @MockBean (not @Mock!)
@WebMvcTest(controllers = PizzaController.class)
@Import(be.vives.pizzastore.exception.GlobalExceptionHandler.class)  // Import exception handler
class PizzaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean  // Mock within Spring context (not @Mock!)
    private PizzaService pizzaService;

    @Test
    void getPizzas_NoPizzas_ReturnsEmptyPage() throws Exception {
        // Given
        Page<PizzaResponse> emptyPage = Page.empty();
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizzas_MultiplePizzas_ReturnsPage() throws Exception {
        // Given
        List<PizzaResponse> pizzas = Arrays.asList(
                new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null),
                new PizzaResponse(2L, "Marinara", new BigDecimal("7.50"), "Simple", null, true, null)
        );
        Page<PizzaResponse> page = new PageImpl<>(pizzas);
        when(pizzaService.findAll(any(Pageable.class))).thenReturn(page);

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Margherita")))
                .andExpect(jsonPath("$.content[0].price", is(8.50)))
                .andExpect(jsonPath("$.content[1].id", is(2)))
                .andExpect(jsonPath("$.content[1].name", is("Marinara")));

        verify(pizzaService).findAll(any(Pageable.class));
    }

    @Test
    void getPizza_ExistingId_ReturnsPizza() throws Exception {
        // Given
        PizzaResponse pizza = new PizzaResponse(1L, "Margherita", new BigDecimal("8.50"), "Classic", null, true, null);
        when(pizzaService.findById(1L)).thenReturn(Optional.of(pizza));

        // When / Then
        mockMvc.perform(get("/api/pizzas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Margherita")))
                .andExpect(jsonPath("$.price", is(8.50)))
                .andExpect(jsonPath("$.description", is("Classic")));

        verify(pizzaService).findById(1L);
    }

    @Test
    void getPizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound());

        verify(pizzaService).findById(999L);
    }

    @Test
    void createPizza_ValidRequest_Returns201() throws Exception {
        // Given
        CreatePizzaRequest request = new CreatePizzaRequest(
                "New Pizza",
                new BigDecimal("12.00"),
                "Delicious new pizza with amazing toppings",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "New Pizza", new BigDecimal("12.00"), 
                "Delicious new pizza with amazing toppings", null, true, null);
        when(pizzaService.create(any(CreatePizzaRequest.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/pizzas/1")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("New Pizza")));

        verify(pizzaService).create(any(CreatePizzaRequest.class));
    }

    @Test
    void createPizza_InvalidRequest_Returns400() throws Exception {
        // Given - invalid request (empty name, negative price)
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",
                new BigDecimal("-5.00"),
                "Description",
                true,
                null
        );

        // When / Then
        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))));

        verify(pizzaService, never()).create(any());
    }

    @Test
    void updatePizza_ValidRequest_Returns200() throws Exception {
        // Given
        UpdatePizzaRequest request = new UpdatePizzaRequest(
                "Updated Pizza",
                new BigDecimal("11.00"),
                "Updated description for this amazing pizza",
                true,
                null
        );

        PizzaResponse response = new PizzaResponse(1L, "Updated Pizza", new BigDecimal("11.00"), 
                "Updated description for this amazing pizza", null, true, null);
        when(pizzaService.update(eq(1L), any(UpdatePizzaRequest.class))).thenReturn(response);

        // When / Then
        mockMvc.perform(put("/api/pizzas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Pizza")))
                .andExpect(jsonPath("$.price", is(11.00)));

        verify(pizzaService).update(eq(1L), any(UpdatePizzaRequest.class));
    }

    @Test
    void updatePizza_NonExistingId_Returns404() throws Exception {
        // Given
        UpdatePizzaRequest request = new UpdatePizzaRequest(
                "Updated Pizza",
                new BigDecimal("11.00"),
                "Updated description for this pizza",
                true,
                null
        );

        when(pizzaService.update(eq(999L), any(UpdatePizzaRequest.class)))
                .thenReturn(null);

        // When / Then
        mockMvc.perform(put("/api/pizzas/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(pizzaService).update(eq(999L), any(UpdatePizzaRequest.class));
    }

    @Test
    void deletePizza_ExistingId_Returns204() throws Exception {
        // Given
        when(pizzaService.delete(1L)).thenReturn(true);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/1"))
                .andExpect(status().isNoContent());

        verify(pizzaService).delete(1L);
    }

    @Test
    void deletePizza_NonExistingId_Returns404() throws Exception {
        // Given
        when(pizzaService.delete(999L)).thenReturn(false);

        // When / Then
        mockMvc.perform(delete("/api/pizzas/999"))
                .andExpect(status().isNotFound());

        verify(pizzaService).delete(999L);
    }
}
```

**Key Points:**
- `@WebMvcTest` loads only web layer
- `MockMvc` for HTTP requests/responses
- `@MockBean` mocks service dependencies
- `jsonPath()` for testing JSON responses
- Tests HTTP status codes, headers, and body
- No database, very fast

---

### Integration Tests (@SpringBootTest)

Test the complete application with all layers.

#### Example: Pizza Integration Test

```java
package be.vives.pizzastore;

import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PizzaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndRetrievePizza_Success() throws Exception {
        // Given - Create pizza
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Integration Test Pizza",
                new BigDecimal("13.50"),
                "This pizza was created during an integration test",
                true,
                null
        );

        // When - Create pizza
        MvcResult createResult = mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Extract ID from Location header
        String location = createResult.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        Long pizzaId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        // Then - Retrieve pizza
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(pizzaId.intValue())))
                .andExpect(jsonPath("$.name", is("Integration Test Pizza")))
                .andExpect(jsonPath("$.price", is(13.50)))
                .andExpect(jsonPath("$.description", containsString("integration test")));
    }

    @Test
    void fullCrudFlow_Success() throws Exception {
        // Create
        CreatePizzaRequest createRequest = new CreatePizzaRequest(
                "CRUD Test Pizza",
                new BigDecimal("10.00"),
                "Testing full CRUD operations with this amazing pizza",
                true,
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        PizzaResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                PizzaResponse.class
        );
        Long pizzaId = created.id();

        // Read
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("CRUD Test Pizza")));

        // Update
        UpdatePizzaRequest updateRequest = new UpdatePizzaRequest(
                "Updated CRUD Pizza",
                new BigDecimal("11.50"),
                "Updated description for this pizza during testing",
                true,
                null
        );

        mockMvc.perform(put("/api/pizzas/" + pizzaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated CRUD Pizza")))
                .andExpect(jsonPath("$.price", is(11.50)));

        // Delete
        mockMvc.perform(delete("/api/pizzas/" + pizzaId))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/pizzas/" + pizzaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void filterPizzasByPrice_Success() throws Exception {
        // Given - Create test pizzas
        createPizza("Cheap Pizza", "5.00");
        createPizza("Medium Pizza", "10.00");
        createPizza("Expensive Pizza", "20.00");

        // When / Then - Filter by max price
        mockMvc.perform(get("/api/pizzas")
                        .param("maxPrice", "12.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItem("Cheap Pizza")))
                .andExpect(jsonPath("$[*].name", hasItem("Medium Pizza")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Expensive Pizza"))));
    }

    private void createPizza(String name, String price) throws Exception {
        CreatePizzaRequest request = new CreatePizzaRequest(
                name,
                new BigDecimal(price),
                "Test pizza: " + name,
                true,
                null
        );

        mockMvc.perform(post("/api/pizzas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
```

**Key Points:**
- `@SpringBootTest` loads entire application
- `@AutoConfigureMockMvc` provides MockMvc
- `@Transactional` rolls back after each test
- Uses real database (H2 in-memory)
- Tests complete flows end-to-end
- Slower than unit tests but more confidence

---

## ✅ Testing Validation

Test that validation annotations work correctly.

```java
@WebMvcTest(PizzaController.class)
@Import(be.vives.pizzastore.exception.GlobalExceptionHandler.class)
class PizzaValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PizzaService pizzaService;

    @Test
    void createPizza_BlankName_Returns400() throws Exception {
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",  // Blank name
                new BigDecimal("10.00"),
                "Valid description",
                true,
                null
        );

        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.validationErrors[*].message", 
                        hasItem(containsString("required"))));
    }

    @Test
    void createPizza_PriceTooLow_Returns400() throws Exception {
        CreatePizzaRequest request = new CreatePizzaRequest(
                "Valid Name",
                new BigDecimal("0.00"),  // Too low
                "Valid description",
                true,
                null
        );

        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.validationErrors[*].message", 
                        hasItem(containsString("0.01"))));
    }

    @Test
    void createPizza_MultipleValidationErrors_ReturnsAllErrors() throws Exception {
        CreatePizzaRequest request = new CreatePizzaRequest(
                "",  // Blank
                new BigDecimal("-5.00"),  // Negative
                "Description",
                true,
                null
        );

        mockMvc.perform(post("/api/pizzas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.validationErrors[*].field", 
                        hasItem(anyOf(is("name"), is("price")))));
    }
}
```

---

## 🛡️ Testing Exception Handling

Test that exceptions are handled correctly.

```java
@WebMvcTest(PizzaController.class)
class ExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PizzaService pizzaService;

    @Test
    void getPizza_NotFound_ReturnsProperErrorResponse() throws Exception {
        // Given
        when(pizzaService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Pizza", 999L));

        // When / Then
        mockMvc.perform(get("/api/pizzas/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Pizza with id 999 not found")))
                .andExpect(jsonPath("$.path", is("/api/pizzas/999")))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
    }

    @Test
    void createCustomer_DuplicateEmail_Returns409() throws Exception {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John Doe",
                "duplicate@example.com",
                "+32471234567",
                "Main Street 123, Brussels"
        );

        when(pizzaService.create(any()))
                .thenThrow(new DuplicateResourceException(
                        "Customer with email duplicate@example.com already exists"));

        // When / Then
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void genericError_Returns500() throws Exception {
        // Given
        when(pizzaService.findAll())
                .thenThrow(new RuntimeException("Database connection failed"));

        // When / Then
        mockMvc.perform(get("/api/pizzas"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", not(containsString("Database"))));  // Don't expose internal details
    }
}
```

---

## 💪 AssertJ for Better Assertions

AssertJ provides fluent assertions that are more readable than JUnit's assertEquals.

```java
import static org.assertj.core.api.Assertions.*;

@Test
void assertJExamples() {
    // Basic assertions
    String name = "Margherita";
    assertThat(name).isNotNull()
                    .startsWith("Mar")
                    .endsWith("ita")
                    .contains("gher");

    // Number assertions
    BigDecimal price = new BigDecimal("8.50");
    assertThat(price).isPositive()
                     .isGreaterThan(BigDecimal.ZERO)
                     .isLessThan(new BigDecimal("100"));

    // Collection assertions
    List<String> pizzas = Arrays.asList("Margherita", "Marinara", "Quattro Formaggi");
    assertThat(pizzas).hasSize(3)
                      .contains("Margherita", "Marinara")
                      .doesNotContain("Hawaiian")
                      .allMatch(name -> name.length() > 5);

    // Extracting properties
    List<Pizza> pizzaList = getPizzas();
    assertThat(pizzaList).extracting(Pizza::getName)
                         .containsExactly("Margherita", "Marinara");

    // Exception assertions
    assertThatThrownBy(() -> pizzaService.findById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999")
            .hasMessageContaining("not found");

    // Object assertions
    PizzaResponse response = pizzaService.findById(1L);
    assertThat(response).isNotNull()
                        .extracting(PizzaResponse::name, PizzaResponse::price)
                        .containsExactly("Margherita", new BigDecimal("8.50"));
}
```

---

## 🔍 Testing JSON Responses

### JSONPath Syntax

```java
// Basic path
jsonPath("$.name")                    // Top-level field
jsonPath("$.address.street")          // Nested field
jsonPath("$[0].name")                 // First element in array
jsonPath("$[*].name")                 // All names in array

// Filters
jsonPath("$[?(@.price < 10)]")        // Items with price < 10
jsonPath("$[?(@.name == 'Margherita')]")  // Items with name Margherita

// Functions
jsonPath("$.length()")                // Array length
jsonPath("$.sum()")                   // Sum of numbers
```

### Example

```java
mockMvc.perform(get("/api/pizzas"))
        // Array size
        .andExpect(jsonPath("$", hasSize(3)))
        
        // First item
        .andExpect(jsonPath("$[0].id", is(1)))
        .andExpect(jsonPath("$[0].name", is("Margherita")))
        
        // All names
        .andExpect(jsonPath("$[*].name", 
                containsInAnyOrder("Margherita", "Marinara", "Quattro Formaggi")))
        
        // Filter
        .andExpect(jsonPath("$[?(@.price > 10)]", hasSize(1)))
        
        // Nested
        .andExpect(jsonPath("$[0].nutritionalInfo.calories", is(250)));
```

---

## 💡 Best Practices

### 1. Follow AAA Pattern

```java
@Test
void testName() {
    // Arrange (Given) - Set up test data
    Pizza pizza = new Pizza("Test", new BigDecimal("10"), "Description");
    when(repository.findById(1L)).thenReturn(Optional.of(pizza));
    
    // Act (When) - Execute the code under test
    PizzaResponse result = service.findById(1L);
    
    // Assert (Then) - Verify the result
    assertThat(result.name()).isEqualTo("Test");
}
```

### 2. Test Naming Conventions

```java
// Good names describe: What_Condition_Expected
@Test
void findById_ExistingPizza_ReturnsPizzaResponse() {}

@Test
void findById_NonExistingPizza_ThrowsNotFoundException() {}

@Test
void createPizza_InvalidPrice_Returns400() {}
```

### 3. One Assertion per Test

```java
// ❌ Bad - multiple assertions
@Test
void testPizza() {
    assertThat(pizza.getName()).isEqualTo("Margherita");
    assertThat(pizza.getPrice()).isEqualTo(new BigDecimal("8.50"));
    assertThat(pizza.getDescription()).contains("Classic");
}

// ✅ Good - one logical assertion
@Test
void getPizza_ReturnsCorrectProperties() {
    assertThat(pizza)
            .extracting(Pizza::getName, Pizza::getPrice, Pizza::getDescription)
            .containsExactly("Margherita", new BigDecimal("8.50"), "Classic");
}
```

### 4. Don't Test Framework Code

```java
// ❌ Don't test Spring Data JPA methods
@Test
void testSave() {
    Pizza pizza = new Pizza();
    pizzaRepository.save(pizza);
    // This just tests Spring Data JPA, not your code
}

// ✅ Test custom queries
@Test
void findByPriceLessThan_ReturnsFilteredResults() {
    // This tests your custom query
    List<Pizza> result = pizzaRepository.findByPriceLessThan(new BigDecimal("10"));
    assertThat(result).allMatch(p -> p.getPrice().compareTo(new BigDecimal("10")) < 0);
}
```

### 5. Use Test Data Builders

```java
// Create a builder class for test data
class PizzaTestDataBuilder {
    private String name = "Test Pizza";
    private BigDecimal price = new BigDecimal("10.00");
    private String description = "Test description for this pizza";

    public PizzaTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public PizzaTestDataBuilder withPrice(String price) {
        this.price = new BigDecimal(price);
        return this;
    }

    public Pizza build() {
        return new Pizza(name, price, description);
    }
}

// Use in tests
@Test
void test() {
    Pizza pizza = new PizzaTestDataBuilder()
            .withName("Custom Pizza")
            .withPrice("15.00")
            .build();
}
```

### 6. Isolate Tests

```java
// ❌ Bad - tests depend on each other
static Pizza sharedPizza;

@Test
void test1() {
    sharedPizza = new Pizza();
}

@Test
void test2() {
    // Depends on test1 running first
    assertThat(sharedPizza).isNotNull();
}

// ✅ Good - each test is independent
@BeforeEach
void setup() {
    testPizza = new Pizza();
}

@Test
void test1() {
    // Uses testPizza
}

@Test
void test2() {
    // Also uses testPizza, independent of test1
}
```

### 7. Test Edge Cases

```java
@Test
void findByPriceLessThan_ZeroPrice_ReturnsEmpty() {}

@Test
void findByPriceLessThan_NegativePrice_ReturnsEmpty() {}

@Test
void findByPriceLessThan_VeryLargePrice_ReturnsAll() {}

@Test
void findByNameContaining_EmptyString_ReturnsAll() {}

@Test
void findByNameContaining_NullValue_ThrowsException() {}
```

### 8. Fast Tests

```java
// Unit tests should be < 10ms
// Integration tests should be < 100ms
// If tests are slow, you're testing too much

// ✅ Fast - uses mocks
@ExtendWith(MockitoExtension.class)
class FastServiceTest {
    @Mock
    private Repository repository;
    
    @InjectMocks
    private Service service;
}

// ❌ Slow - loads entire app
@SpringBootTest
class SlowServiceTest {
    @Autowired
    private Service service;
}
```

### 9. Choose the Right Mock Annotation

> **Important: `@Mock` vs `@MockBean` in Spring Boot 3**

Both annotations are **NOT deprecated** and serve different purposes:

| Scenario | Use | Why |
|----------|-----|-----|
| **Pure unit test** (no Spring) | `@Mock` + `@ExtendWith(MockitoExtension.class)` | Fastest - no Spring context |
| **Spring integration test** | `@MockBean` with `@WebMvcTest` or `@SpringBootTest` | Tests Spring integration |

```java
// ✅ Pure unit test - service layer
@ExtendWith(MockitoExtension.class)  // Still needed in Spring Boot 3!
class PizzaServiceTest {
    @Mock  // Mockito mock (no Spring)
    private PizzaRepository repository;
    
    @InjectMocks
    private PizzaService service;
}

// ✅ Spring integration test - controller layer
@WebMvcTest(PizzaController.class)  // Loads Spring MVC
class PizzaControllerTest {
    @MockBean  // Mock in Spring context
    private PizzaService service;
    
    @Autowired
    private MockMvc mockMvc;
}
```

**Key Differences:**

| Feature | `@Mock` | `@MockBean` |
|---------|---------|-------------|
| **Spring context** | ❌ No | ✅ Yes |
| **Speed** | ⚡ Very fast (<10ms) | 🐌 Slower (loads Spring) |
| **Use case** | Service tests | Controller tests |
| **Annotation combo** | `@ExtendWith(MockitoExtension.class)` | `@WebMvcTest` or `@SpringBootTest` |
| **Status in Spring Boot 3** | ✅ Still required | ✅ Not deprecated |

**Recommendation:**
- Use `@Mock` for 70% of your tests (service layer unit tests)
- Use `@MockBean` for 30% of your tests (controller/integration tests)

---

## 🍕 PizzaStore Test Suite

See the `pizzastore-with-tests/` project for:

### Repository Tests (`@DataJpaTest`)
- ✅ `PizzaRepositoryTest` - All custom queries tested
- Tests custom queries with in-memory H2
- Fast execution (<100ms per test)

### Service Tests (Mockito)
- ✅ `PizzaServiceTest` - All CRUD operations tested
- Mocked dependencies (repository, mapper)
- Very fast (<10ms per test)

### Controller Tests (`@WebMvcTest`)
- ✅ `PizzaControllerTest` - All endpoints tested
- MockMvc for HTTP testing
- JSON response validation
- Status codes and headers tested

### Integration Tests (`@SpringBootTest`)
- ✅ `PizzaIntegrationTest` - Full CRUD flows
- Tests complete application
- Real database (H2 in-memory)
- End-to-end scenarios

### Validation & Exception Tests
- ✅ `ValidationTest` - All validation scenarios
- ✅ `ExceptionHandlingTest` - All error responses

**Total: 100+ tests covering >80% of code**

---

## 🎓 Summary

### What We Learned

1. **Testing Pyramid**
   - 70% Unit Tests (fast, isolated)
   - 20% Integration Tests (medium, realistic)
   - 10% E2E Tests (slow, complete)

2. **JUnit 5**
   - `@Test`, `@BeforeEach`, `@AfterEach`
   - Assertions and exception testing
   - Test lifecycle

3. **Spring Boot Testing**
   - `@DataJpaTest` for repositories
   - `@WebMvcTest` for controllers
   - `@SpringBootTest` for integration

4. **Mockito**
   - `@Mock` and `@InjectMocks`
   - `when().thenReturn()` for behavior
   - `verify()` for interactions

5. **MockMvc**
   - HTTP request/response testing
   - JSONPath for JSON assertions
   - Status codes and headers

6. **AssertJ**
   - Fluent assertions
   - Better readability
   - Rich assertions

### Key Takeaways

✅ **Write tests first** - TDD when possible  
✅ **Test behavior, not implementation**  
✅ **One assertion per test**  
✅ **Fast, isolated, repeatable**  
✅ **Test edge cases**  
✅ **Good names describe what/condition/expected**  
✅ **AAA pattern** (Arrange, Act, Assert)  

---

## 📖 Additional Resources

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Testing Spring Boot Applications Masterclass](https://rieckpil.de/courses/testing-spring-boot-applications-masterclass/) - Excellent resource!

---

## 🚀 Runnable Project

A complete, production-ready Spring Boot project with **comprehensive test suite** is available in:

**`pizzastore-with-tests/`**

The project includes:
- ✅ 100+ tests covering all layers
- ✅ Repository tests with @DataJpaTest
- ✅ Service tests with Mockito
- ✅ Controller tests with MockMvc
- ✅ Integration tests with @SpringBootTest
- ✅ Validation testing
- ✅ Exception handling testing
- ✅ >80% code coverage
- ✅ Extends Lesson 9 project (with validation)

Run tests:
```bash
cd pizzastore-with-tests
mvn test
```

---

**Congratulations!** 🎉 You now know how to write comprehensive tests for production-ready Spring Boot APIs!
