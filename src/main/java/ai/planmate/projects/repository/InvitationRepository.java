package ai.planmate.projects.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.projects.entity.Invitation;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query(
            "SELECT i FROM Invitation i "
                    + "JOIN FETCH i.project p "
                    + "WHERE i.token = :token "
                    + "AND i.revokedAt IS NULL")
    Optional<Invitation> findByTokenAndNotRevoked(String token);

    @Query(
            "SELECT i FROM Invitation i "
                    + "WHERE i.project.id = :projectId "
                    + "AND i.revokedAt IS NULL "
                    + "ORDER BY i.createdAt DESC")
    List<Invitation> findByProjectIdAndNotRevoked(UUID projectId);

    @Query(
            "SELECT i FROM Invitation i "
                    + "WHERE i.email = :email "
                    + "AND i.project.id = :projectId "
                    + "AND i.revokedAt IS NULL "
                    + "AND (i.expiresAt IS NULL OR i.expiresAt > :now) "
                    + "AND i.acceptedAt IS NULL")
    Optional<Invitation> findPendingInvitationByEmailAndProject(
            String email, UUID projectId, Instant now);

    @Query(
            "SELECT i FROM Invitation i "
                    + "WHERE i.expiresAt < :now "
                    + "AND i.revokedAt IS NULL "
                    + "AND i.acceptedAt IS NULL")
    List<Invitation> findExpiredInvitations(Instant now);
}
