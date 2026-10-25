package be.vives.pizzastore.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the e-mail address of the logged-in user into a controller parameter: {@code @CurrentUser String email}.
 * The principal is the {@code UserDetails} that JwtAuthenticationFilter put in the SecurityContext; its username is the e-mail.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal(expression = "username")
public @interface CurrentUser {
}
