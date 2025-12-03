package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.NutritionalInfo;
import be.vives.pizzastore.dto.request.NutritionalInfoRequest;
import be.vives.pizzastore.dto.response.NutritionalInfoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NutritionalInfoMapper {

    NutritionalInfoResponse toResponse(NutritionalInfo nutritionalInfo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pizza", ignore = true)
    NutritionalInfo toEntity(NutritionalInfoRequest request);
}
