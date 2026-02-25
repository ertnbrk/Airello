package ai.planmate.projects.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.Workspace;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    @Query("SELECT w FROM Workspace w WHERE w.slug = :slug AND w.deletedAt IS NULL")
    Optional<Workspace> findBySlug(String slug);

    @Query("SELECT w FROM Workspace w WHERE w.owner.id = :ownerId AND w.deletedAt IS NULL")
    List<Workspace> findByOwnerId(UUID ownerId);
}
