package be.vives.pizzastore.mapper;

import be.vives.pizzastore.domain.User;
import be.vives.pizzastore.dto.response.UserResponse;
import be.vives.pizzastore.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    UserSummaryResponse toUserSummaryResponse(User user);
}
