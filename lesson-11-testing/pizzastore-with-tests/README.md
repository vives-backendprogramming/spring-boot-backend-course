# PizzaStore with Tests

Extends Lesson 9 project with **comprehensive test suite**.

## What's New?

- ✅ **Repository Tests** (@DataJpaTest)
- ✅ **Service Tests** (Mockito)
- ✅ **Controller Tests** (@WebMvcTest)
- ✅ **Integration Tests** (@SpringBootTest)
- ✅ **Validation Testing**
- ✅ **Exception Handling Testing**

## Run the Tests

```bash
cd pizzastore-with-tests
mvn test
```

## Test Coverage

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

## Test Examples

### Repository Test
```bash
mvn test -Dtest=PizzaRepositoryTest
```

### Service Test
```bash
mvn test -Dtest=PizzaServiceTest
```

### Controller Test
```bash
mvn test -Dtest=PizzaControllerTest
```

### Integration Test
```bash
mvn test -Dtest=PizzaIntegrationTest
```

### All Tests
```bash
mvn test
```

## Test Structure

```
src/test/java/be/vives/pizzastore/
├── repository/
│   └── PizzaRepositoryTest.java
├── service/
│   └── PizzaServiceTest.java
├── controller/
│   └── PizzaControllerTest.java
└── integration/
    └── PizzaIntegrationTest.java
```

## Key Testing Concepts

### Unit Tests (Fast)
- Test single units in isolation
- Use mocks for dependencies
- 70% of your tests

### Integration Tests (Moderate)
- Test multiple components together
- Use real database
- 20% of your tests

### Example: Service Test with Mockito

```java
@ExtendWith(MockitoExtension.class)
class PizzaServiceTest {
    @Mock
    private PizzaRepository pizzaRepository;
    
    @InjectMocks
    private PizzaService pizzaService;
    
    @Test
    void findById_ExistingPizza_ReturnsPizzaResponse() {
        // Given
        when(pizzaRepository.findById(1L))
            .thenReturn(Optional.of(testPizza));
        
        // When
        PizzaResponse result = pizzaService.findById(1L);
        
        // Then
        assertThat(result).isNotNull();
        verify(pizzaRepository).findById(1L);
    }
}
```

### Example: Controller Test with MockMvc

```java
@WebMvcTest(PizzaController.class)
class PizzaControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PizzaService pizzaService;
    
    @Test
    void getPizza_ReturnsJson() throws Exception {
        mockMvc.perform(get("/api/pizzas/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name", is("Margherita")));
    }
}
```

See [Lesson 11 README](../README.md) for complete testing guide.
