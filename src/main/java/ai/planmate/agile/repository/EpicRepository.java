package ai.planmate.agile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.agile.entity.Epic;
import ai.planmate.agile.entity.IssueStatus;

@Repository
public interface EpicRepository extends JpaRepository<Epic, UUID> {

    @Query(
            "SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.deletedAt IS NULL ORDER BY"
                    + " e.createdAt DESC")
    List<Epic> findByProjectId(UUID projectId);

    @Query(
            "SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.status = :status AND"
                    + " e.deletedAt IS NULL")
    List<Epic> findByProjectIdAndStatus(UUID projectId, IssueStatus status);

    @Query("SELECT e FROM Epic e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Epic> findByIdAndNotDeleted(UUID id);
}
