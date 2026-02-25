package ai.planmate.projects.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query(
            "SELECT p FROM Project p WHERE p.workspace.id = :workspaceId AND p.key = :key AND"
                    + " p.deletedAt IS NULL")
    Optional<Project> findByWorkspaceIdAndKey(UUID workspaceId, String key);

    @Query(
            "SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE"
                    + " p.workspace.id = :workspaceId AND p.key = :key AND p.deletedAt IS NULL")
    boolean existsByWorkspaceIdAndKey(UUID workspaceId, String key);

    @Query(
            "SELECT p FROM Project p WHERE p.workspace.id = :workspaceId AND p.deletedAt IS NULL"
                    + " ORDER BY p.createdAt DESC")
    List<Project> findByWorkspaceId(UUID workspaceId);

    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdAndNotDeleted(UUID id);
}
