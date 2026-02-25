package ai.planmate.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Google user info response DTO */
public record GoogleUserInfo(
        String id,
        String email,
        @JsonProperty("verified_email") Boolean verifiedEmail,
        String name,
        @JsonProperty("given_name") String givenName,
        @JsonProperty("family_name") String familyName,
        String picture,
        String locale) { }
