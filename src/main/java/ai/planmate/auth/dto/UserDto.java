package ai.planmate.auth.dto;

import java.util.UUID;

import ai.planmate.auth.entity.AuthProvider;

/** User data transfer object */
public record UserDto(
        UUID id, String email, String name, AuthProvider provider, String userType, String plan) {

    /** Backward-compatible constructor */
    public UserDto(UUID id, String email, String name, AuthProvider provider) {
        this(id, email, name, provider, null, null);
    }
}
