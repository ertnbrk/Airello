package ai.planmate.agile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.agile.entity.Sprint;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, UUID> {

    @Query(
            "SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.deletedAt IS NULL ORDER"
                    + " BY s.startDate DESC")
    List<Sprint> findByProjectId(UUID projectId);

    @Query(
            "SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = :status AND"
                    + " s.deletedAt IS NULL")
    List<Sprint> findByProjectIdAndStatus(UUID projectId, String status);

    @Query("SELECT s FROM Sprint s WHERE s.id = :id AND s.deletedAt IS NULL")
    Optional<Sprint> findByIdAndNotDeleted(UUID id);
}
