package be.vives.pizzastore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// Required for save(): entities use @CreatedDate on a non-null column. No AuditorAwareImpl here (needs Spring Security), so createdBy/updatedBy stay null.
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
