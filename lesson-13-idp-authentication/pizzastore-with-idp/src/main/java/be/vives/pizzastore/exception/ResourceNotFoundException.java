package be.vives.pizzastore.exception;

public class ResourceNotFoundException extends PizzaStoreException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s with id %d not found", resourceName, id));
    }
}
