package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.Pizza;
import be.vives.pizzastore.dto.request.CreatePizzaRequest;
import be.vives.pizzastore.dto.request.UpdatePizzaRequest;
import be.vives.pizzastore.dto.response.PizzaResponse;
import be.vives.pizzastore.dto.response.PizzaSummaryResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PizzaMapper {

    PizzaResponse toPizzaResponse(Pizza pizza);

    PizzaSummaryResponse toPizzaSummaryResponse(Pizza pizza);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "nutritionalInfo", ignore = true)
    @Mapping(target = "favoritedByUsers", ignore = true)
    Pizza toEntity(CreatePizzaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "nutritionalInfo", ignore = true)
    @Mapping(target = "favoritedByUsers", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(UpdatePizzaRequest request, @MappingTarget Pizza pizza);
}
