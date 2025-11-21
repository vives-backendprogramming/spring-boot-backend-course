package be.vives.pizzastore.dto.response;

import be.vives.pizzastore.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime orderDate,
        UserSummaryResponse user,
        List<OrderLineResponse> orderLines
) {
}
