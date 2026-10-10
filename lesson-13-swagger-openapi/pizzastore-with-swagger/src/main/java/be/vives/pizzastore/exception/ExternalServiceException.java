package be.vives.pizzastore.exception;

/** An API that PizzaStore depends on failed or could not be reached: not the client's fault, so 502 Bad Gateway. */
public class ExternalServiceException extends PizzaStoreException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
