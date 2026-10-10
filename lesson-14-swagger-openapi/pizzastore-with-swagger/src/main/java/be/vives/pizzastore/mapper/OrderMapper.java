package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderLine;
import be.vives.pizzastore.dto.response.OrderLineResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    @Mapping(source = "pizza.id", target = "pizzaId")
    @Mapping(source = "pizza.name", target = "pizzaName")
    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
