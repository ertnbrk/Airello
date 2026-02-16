package ai.planmate.chat.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.chat.entity.ChatThread;

@Repository
public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    @Query(
            "SELECT t FROM ChatThread t WHERE t.project.id = :projectId AND t.deletedAt IS NULL"
                    + " ORDER BY t.createdAt")
    List<ChatThread> findByProjectId(UUID projectId);

    @Query(
            "SELECT t FROM ChatThread t WHERE t.project.id = :projectId AND t.isDefault = true AND"
                    + " t.deletedAt IS NULL")
    Optional<ChatThread> findDefaultByProjectId(UUID projectId);
}
