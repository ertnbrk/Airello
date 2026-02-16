package ai.planmate.projects.entity;

import java.time.Instant;
import java.util.UUID;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.InvitationType;
import ai.planmate.projects.ProjectRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invitation")
@Getter
@Setter
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull(message = "Invitation type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationType type;

    @NotBlank(message = "Token is required")
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Email(message = "Must be a valid email address")
    @Column(length = 255)
    private String email;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectRole role = ProjectRole.MEMBER;

    @NotNull(message = "Invited by is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private AppUser invitedBy;

    @NotNull(message = "Created at is required")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_by")
    private AppUser acceptedBy;

    @NotNull(message = "Max uses is required")
    @Column(name = "max_uses", nullable = false)
    private Integer maxUses = 1;

    @NotNull(message = "Current uses is required")
    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked() && currentUses < maxUses;
    }

    public boolean canBeUsed() {
        return isValid() && (type == InvitationType.LINK || acceptedAt == null);
    }

    public void incrementUses() {
        this.currentUses++;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
