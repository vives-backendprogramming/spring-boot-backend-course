package be.vives.pizzastore.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        // For now, return "system" as the auditor
        // In future lessons with security, this will return the authenticated user
        return Optional.of("system");
    }
}
