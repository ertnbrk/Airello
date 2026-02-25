package ai.planmate.auth.entity;

import java.time.Instant;

import ai.planmate.shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser extends BaseEntity {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 255, message = "Password hash must not exceed 255 characters")
    @Column(name = "password_hash")
    private String passwordHash;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotNull(message = "User type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20, nullable = false)
    private UserType userType = UserType.REGISTERED;

    @NotNull(message = "Authentication provider is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 32, nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Size(max = 255, message = "Provider ID must not exceed 255 characters")
    @Column(name = "provider_id", unique = true)
    private String providerId;

    @Size(max = 255, message = "Keycloak subject must not exceed 255 characters")
    @Column(name = "keycloak_subject", unique = true)
    private String keycloakSubject;

    @NotNull(message = "User plan is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", length = 32, nullable = false)
    private UserPlan plan = UserPlan.FREE;

    @Size(max = 255, message = "Stripe customer ID must not exceed 255 characters")
    @Column(name = "stripe_customer_id", unique = true)
    private String stripeCustomerId;

    @NotNull(message = "Email verified status is required")
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @NotNull(message = "Active status is required")
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
