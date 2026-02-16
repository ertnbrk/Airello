package ai.planmate.chat.entity;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.projects.entity.Project;
import ai.planmate.shared.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_thread")
@Getter
@Setter
public class ChatThread extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(length = 255)
    private String title;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
