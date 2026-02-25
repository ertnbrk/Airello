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

    @Query(
            "SELECT bc FROM BoardColumn bc WHERE bc.project.id = :projectId AND bc.deletedAt IS"
                    + " NULL ORDER BY bc.position")
    List<BoardColumn> findByProjectIdOrderByPosition(UUID projectId);

    @Query(
            "SELECT bc FROM BoardColumn bc WHERE bc.id = :id AND bc.deletedAt IS NULL ORDER BY"
                    + " bc.position LIMIT 1")
    Optional<BoardColumn> findByIdAndNotDeleted(UUID id);

    @Query(
            "SELECT bc FROM BoardColumn bc WHERE bc.project.id = :projectId AND bc.isDefault = true"
                    + " AND bc.deletedAt IS NULL ORDER BY bc.position LIMIT 1")
    Optional<BoardColumn> findDefaultByProjectId(UUID projectId);

    @Query(
            "SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END FROM BoardColumn bc WHERE"
                    + " bc.project.id = :projectId AND bc.isDefault = true AND bc.deletedAt IS"
                    + " NULL")
    boolean existsDefaultByProjectId(UUID projectId);

    @Query(
            "SELECT COUNT(bc) FROM BoardColumn bc WHERE bc.project.id = :projectId AND bc.deletedAt"
                    + " IS NULL")
    long countByProjectId(UUID projectId);

    @Query(
            "SELECT COALESCE(MAX(bc.position), -1) FROM BoardColumn bc WHERE bc.project.id ="
                    + " :projectId AND bc.deletedAt IS NULL")
    Integer findMaxPositionByProjectId(UUID projectId);

    @Query(
            "SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END FROM BoardColumn bc WHERE"
                    + " bc.project.id = :projectId AND LOWER(bc.name) = LOWER(:name) AND"
                    + " bc.deletedAt IS NULL")
    boolean existsByProjectIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID projectId, String name);

    @Query(
            "SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END FROM BoardColumn bc WHERE"
                    + " bc.project.id = :projectId AND LOWER(bc.name) = LOWER(:name) AND bc.id !="
                    + " :excludeId AND bc.deletedAt IS NULL")
    boolean existsByProjectIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(
            UUID projectId, String name, UUID excludeId);
}
