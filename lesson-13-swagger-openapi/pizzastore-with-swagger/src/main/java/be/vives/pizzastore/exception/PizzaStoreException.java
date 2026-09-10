package be.vives.pizzastore.exception;

public class PizzaStoreException extends RuntimeException {

    public PizzaStoreException(String message) {
        super(message);
    }

    public PizzaStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
