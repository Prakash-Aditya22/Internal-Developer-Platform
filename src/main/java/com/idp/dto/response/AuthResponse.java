package com.idp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

  private String accessToken;
  private String tokenType;
  private long expiresIn;
  private UserResponse user;

  public static AuthResponse of(String token, long expiresIn, UserResponse user) {
    return AuthResponse.builder()
        .accessToken(token)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .user(user)
        .build();
  }
}
