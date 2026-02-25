package ai.planmate.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Response DTO for authentication endpoints */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, String refreshToken, UserDto user) { }
