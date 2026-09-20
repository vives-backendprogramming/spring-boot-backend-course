package be.vives.pizzastore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

// Mongo equivalent of the JPA lesson's JpaConfig. Unlike JPA, entities don't need an
// @EntityListeners annotation for this to work - @EnableMongoAuditing alone activates
// @CreatedDate/@LastModifiedDate on every @Document.
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
