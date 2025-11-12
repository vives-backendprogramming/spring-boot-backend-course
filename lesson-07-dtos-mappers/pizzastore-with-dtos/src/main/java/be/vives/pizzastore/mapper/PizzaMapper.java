package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PizzaMapper {

    PizzaResponse toResponse(Pizza pizza);

    List<PizzaResponse> toResponseList(List<Pizza> pizzas);

    @Mapping(target = "id", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}
