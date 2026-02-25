package ai.planmate.projects.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.Artifact;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    @Query(
            "SELECT a FROM Artifact a WHERE a.project.id = :projectId AND a.deletedAt IS NULL"
                    + " ORDER BY a.createdAt DESC")
    List<Artifact> findByProjectId(UUID projectId);

    @Query("SELECT a FROM Artifact a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Artifact> findByIdAndNotDeleted(UUID id);
}
