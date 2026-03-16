package com.idp.mapper;

import com.idp.domain.entity.User;
import com.idp.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMapper {

  public UserResponse toResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .fullName(user.getFullName())
        .enabled(user.isEnabled())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet()))
        .build();
  }
}
