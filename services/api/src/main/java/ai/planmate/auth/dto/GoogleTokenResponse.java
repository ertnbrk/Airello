package ai.planmate.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Google OAuth2 token response DTO */
public record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        String scope,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("id_token") String idToken) { }
