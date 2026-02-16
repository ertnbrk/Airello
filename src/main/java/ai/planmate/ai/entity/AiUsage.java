package ai.planmate.ai.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import ai.planmate.auth.entity.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "ai_usage",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "usage_date"}))
@Getter
@Setter
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate = LocalDate.now();

    @Column(name = "calls_used", nullable = false)
    private Integer callsUsed = 0;

    @Column(name = "tokens_used", nullable = false)
    private Integer tokensUsed = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
