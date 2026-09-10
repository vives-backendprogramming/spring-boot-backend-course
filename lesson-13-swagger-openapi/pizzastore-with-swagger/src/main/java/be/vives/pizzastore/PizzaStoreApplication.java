package be.vives.pizzastore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PizzaStoreApplication {

    private static final Logger log = LoggerFactory.getLogger(PizzaStoreApplication.class);

    public static void main(String[] args) {
        log.info("Starting PizzaStore Complete REST API...");
        SpringApplication.run(PizzaStoreApplication.class, args);
        log.info("PizzaStore Complete REST API started successfully!");
    }

}
