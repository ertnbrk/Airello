package ai.planmate.projects.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.ProjectMember;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    @Query(
            "SELECT pm FROM ProjectMember pm "
                    + "JOIN FETCH pm.user u "
                    + "WHERE pm.project.id = :projectId "
                    + "AND pm.deletedAt IS NULL "
                    + "AND u.deletedAt IS NULL "
                    + "ORDER BY pm.joinedAt DESC")
    List<ProjectMember> findByProjectIdWithUser(UUID projectId);

    @Query(
            "SELECT pm FROM ProjectMember pm "
                    + "WHERE pm.project.id = :projectId "
                    + "AND pm.user.id = :userId "
                    + "AND pm.deletedAt IS NULL")
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT pm FROM ProjectMember pm " + "WHERE pm.id = :id " + "AND pm.deletedAt IS NULL")
    Optional<ProjectMember> findByIdAndNotDeleted(UUID id);

    @Query(
            "SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END "
                    + "FROM ProjectMember pm "
                    + "WHERE pm.project.id = :projectId "
                    + "AND pm.user.id = :userId "
                    + "AND pm.deletedAt IS NULL")
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query(
            "SELECT pm FROM ProjectMember pm "
                    + "JOIN FETCH pm.project p "
                    + "WHERE pm.user.id = :userId "
                    + "AND pm.deletedAt IS NULL "
                    + "AND p.deletedAt IS NULL "
                    + "ORDER BY pm.joinedAt DESC")
    List<ProjectMember> findByUserIdWithProject(UUID userId);
}
