package ai.planmate.projects.dto;

import ai.planmate.projects.InvitationType;
import ai.planmate.projects.ProjectRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateInvitationRequest {

    @NotNull(message = "Invitation type is required")
    private InvitationType type;

    @Email(message = "Must be a valid email address")
    private String email;

    @NotNull(message = "Role is required")
    private ProjectRole role;

    @Min(value = 1, message = "Expires in days must be at least 1")
    private Integer expiresInDays; // null = no expiration

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses = 1;
}
