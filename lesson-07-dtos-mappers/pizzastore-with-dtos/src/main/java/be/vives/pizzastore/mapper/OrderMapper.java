package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Order;
import be.vives.pizzastore.domain.OrderLine;
import be.vives.pizzastore.dto.response.OrderLineResponse;
import be.vives.pizzastore.dto.response.OrderResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class, PizzaMapper.class})
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toResponseList(List<Order> orders);

    OrderLineResponse toOrderLineResponse(OrderLine orderLine);

    List<OrderLineResponse> toOrderLineResponseList(List<OrderLine> orderLines);
}
