package ai.planmate.ai.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.planmate.ai.entity.AiUsage;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    Optional<AiUsage> findByUserIdAndUsageDate(UUID userId, LocalDate usageDate);
}
