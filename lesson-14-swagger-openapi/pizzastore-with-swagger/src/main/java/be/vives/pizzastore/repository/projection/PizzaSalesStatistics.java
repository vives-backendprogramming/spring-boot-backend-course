package be.vives.pizzastore.repository.projection;

import java.math.BigDecimal;

/**
 * Aggregated sales figures for one pizza, computed directly by a JPQL query
 * (see {@code OrderRepository.findPizzaSalesStatistics()}) rather than assembled
 * in Java from a list of {@code Order}/{@code OrderLine} entities. Not an entity itself.
 */
public record PizzaSalesStatistics(
        String pizzaName,
        Long timesOrdered,
        Long totalQuantitySold,
        BigDecimal totalRevenue
) {
}
