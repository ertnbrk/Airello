package ai.planmate.projects.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.WorkspaceMember;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    @Query(
            "SELECT wm FROM WorkspaceMember wm "
                    + "JOIN FETCH wm.user u "
                    + "WHERE wm.workspace.id = :workspaceId "
                    + "AND u.deletedAt IS NULL "
                    + "ORDER BY wm.joinedAt DESC")
    List<WorkspaceMember> findByWorkspaceIdWithUser(UUID workspaceId);

    @Query(
            "SELECT wm FROM WorkspaceMember wm "
                    + "WHERE wm.workspace.id = :workspaceId "
                    + "AND wm.user.id = :userId")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    @Query(
            "SELECT wm FROM WorkspaceMember wm "
                    + "JOIN FETCH wm.workspace w "
                    + "WHERE wm.user.id = :userId "
                    + "AND w.deletedAt IS NULL "
                    + "ORDER BY wm.joinedAt DESC")
    List<WorkspaceMember> findByUserIdWithWorkspace(UUID userId);

    @Query(
            "SELECT CASE WHEN COUNT(wm) > 0 THEN true ELSE false END "
                    + "FROM WorkspaceMember wm "
                    + "WHERE wm.workspace.id = :workspaceId "
                    + "AND wm.user.id = :userId")
    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
