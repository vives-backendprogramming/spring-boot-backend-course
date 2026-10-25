package be.vives.pizzastore.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An admin, or the customer who placed the order. A meta-annotation: the SpEL expression is written once, here,
 * instead of being repeated as a string on every method. The annotated method must have a parameter named
 * {@code id} holding the order id: {@code #id} in the expression refers to it.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOwner(#id, authentication)")
public @interface IsAdminOrOrderOwner {
}
