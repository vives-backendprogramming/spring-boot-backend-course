package be.vives.pizzastore.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An admin, or the customer whose account it is. The annotated method must have a parameter named {@code id}
 * holding the customer id: {@code #id} in the expression refers to it.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @customerSecurity.isOwner(#id, authentication)")
public @interface IsAdminOrOwner {
}
