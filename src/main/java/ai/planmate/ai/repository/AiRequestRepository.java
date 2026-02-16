package ai.planmate.ai.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.ai.entity.AiRequest;
import ai.planmate.ai.entity.AiRequestStatus;

@Repository
public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {

    Optional<AiRequest> findByCorrelationId(String correlationId);

    @Query(
            "SELECT ar FROM AiRequest ar WHERE ar.project.id = :projectId ORDER BY ar.startedAt"
                    + " DESC")
    List<AiRequest> findByProjectId(UUID projectId);

    @Query(
            "SELECT ar FROM AiRequest ar WHERE ar.status IN :statuses AND ar.startedAt <"
                    + " :before")
    List<AiRequest> findTimedOutRequests(List<AiRequestStatus> statuses, Instant before);
}
