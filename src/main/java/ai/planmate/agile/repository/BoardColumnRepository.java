package ai.planmate.agile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.agile.entity.BoardColumn;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    @Query("SELECT bc FROM BoardColumn bc WHERE bc.project.id = :projectId ORDER BY bc.position")
    List<BoardColumn> findByProjectIdOrderByPosition(UUID projectId);

    @Query(
            "SELECT bc FROM BoardColumn bc WHERE bc.project.id = :projectId AND bc.isDefault = true"
                    + " ORDER BY bc.position LIMIT 1")
    Optional<BoardColumn> findDefaultByProjectId(UUID projectId);

    @Query("SELECT COUNT(bc) FROM BoardColumn bc WHERE bc.project.id = :projectId")
    long countByProjectId(UUID projectId);

    @Query(
            "SELECT bc FROM BoardColumn bc WHERE bc.project.id = :projectId ORDER BY bc.position"
                    + " DESC LIMIT 1")
    Optional<BoardColumn> findLastByProjectId(UUID projectId);
}
