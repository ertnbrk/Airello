package ai.planmate.agile.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.agile.entity.Issue;
import ai.planmate.agile.entity.IssueStatus;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {

    @Query(
            "SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.deletedAt IS NULL ORDER BY"
                    + " i.createdAt DESC")
    List<Issue> findByProjectId(UUID projectId);

    @Query(
            "SELECT i FROM Issue i WHERE i.project.id = :projectId AND i.status = :status AND"
                    + " i.deletedAt IS NULL")
    List<Issue> findByProjectIdAndStatus(UUID projectId, IssueStatus status);

    @Query(
            "SELECT i FROM Issue i WHERE i.assignee.id = :assigneeId AND i.status IN :statuses AND"
                    + " i.deletedAt IS NULL")
    List<Issue> findByAssigneeIdAndStatusIn(UUID assigneeId, List<IssueStatus> statuses);

    @Query("SELECT i FROM Issue i WHERE i.id = :id AND i.deletedAt IS NULL")
    Optional<Issue> findByIdAndNotDeleted(UUID id);
}
