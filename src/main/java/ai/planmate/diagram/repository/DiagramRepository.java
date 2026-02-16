package ai.planmate.diagram.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.diagram.entity.Diagram;

@Repository
public interface DiagramRepository extends JpaRepository<Diagram, UUID> {

    @Query("SELECT d FROM Diagram d WHERE d.project.id = :projectId ORDER BY d.createdAt DESC")
    List<Diagram> findByProjectId(UUID projectId);

    @Query(
            "SELECT d FROM Diagram d WHERE d.project.id = :projectId AND d.type = :type ORDER BY"
                    + " d.version DESC")
    List<Diagram> findByProjectIdAndType(UUID projectId, String type);
}
