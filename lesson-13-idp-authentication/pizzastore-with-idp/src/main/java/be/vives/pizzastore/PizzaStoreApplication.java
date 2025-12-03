package be.vives.pizzastore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PizzaStoreApplication {

    private static final Logger log = LoggerFactory.getLogger(PizzaStoreApplication.class);

    public static void main(String[] args) {
        log.info("Starting PizzaStore with IdP Authentication...");
        SpringApplication.run(PizzaStoreApplication.class, args);
        log.info("PizzaStore with IdP Authentication started successfully!");
    }

}
