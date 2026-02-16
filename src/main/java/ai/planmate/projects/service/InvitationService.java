package ai.planmate.projects.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.repository.AppUserRepository;
import ai.planmate.projects.InvitationType;
import ai.planmate.projects.dto.CreateInvitationRequest;
import ai.planmate.projects.dto.InvitationResponse;
import ai.planmate.projects.dto.ProjectMemberResponse;
import ai.planmate.projects.entity.Invitation;
import ai.planmate.projects.entity.Project;
import ai.planmate.projects.entity.ProjectMember;
import ai.planmate.projects.repository.InvitationRepository;
import ai.planmate.projects.repository.ProjectMemberRepository;
import ai.planmate.projects.repository.ProjectRepository;
import ai.planmate.projects.repository.WorkspaceMemberRepository;
import ai.planmate.shared.exception.BadRequestException;
import ai.planmate.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AppUserRepository appUserRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Transactional
    public InvitationResponse createInvitation(
            UUID projectId, CreateInvitationRequest request, UUID inviterId) {
        Project project =
                projectRepository
                        .findByIdAndNotDeleted(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        AppUser inviter =
                appUserRepository
                        .findById(inviterId)
                        .orElseThrow(() -> new ResourceNotFoundException("Inviter not found"));

        // Verify inviter has access to project
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, inviterId)) {
            throw new BadRequestException("You don't have permission to create invitations");
        }

        // Validate email is provided for EMAIL type
        if (request.getType() == InvitationType.EMAIL && request.getEmail() == null) {
            throw new BadRequestException("Email is required for EMAIL type invitations");
        }

        // Check for existing pending invitation for the same email
        if (request.getType() == InvitationType.EMAIL) {
            invitationRepository
                    .findPendingInvitationByEmailAndProject(
                            request.getEmail(), projectId, Instant.now())
                    .ifPresent(
                            inv -> {
                                throw new BadRequestException(
                                        "An active invitation already exists for this email");
                            });
        }

        Invitation invitation = new Invitation();
        invitation.setProject(project);
        invitation.setType(request.getType());
        invitation.setToken(generateSecureToken());
        invitation.setEmail(request.getEmail());
        invitation.setRole(request.getRole());
        invitation.setInvitedBy(inviter);

        if (request.getExpiresInDays() != null) {
            invitation.setExpiresAt(
                    Instant.now().plus(request.getExpiresInDays(), ChronoUnit.DAYS));
        }

        if (request.getMaxUses() != null) {
            invitation.setMaxUses(request.getMaxUses());
        }

        invitation = invitationRepository.save(invitation);

        return toResponse(invitation);
    }

    @Transactional(readOnly = true)
    public InvitationResponse getInvitation(String token) {
        Invitation invitation =
                invitationRepository
                        .findByTokenAndNotRevoked(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        return toResponse(invitation);
    }

    @Transactional
    public ProjectMemberResponse acceptInvitation(String token, UUID userId) {
        Invitation invitation =
                invitationRepository
                        .findByTokenAndNotRevoked(token)
                        .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        AppUser user =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate invitation
        if (!invitation.isValid()) {
            if (invitation.isExpired()) {
                throw new BadRequestException("Invitation has expired");
            }
            if (invitation.isRevoked()) {
                throw new BadRequestException("Invitation has been revoked");
            }
            if (invitation.getCurrentUses() >= invitation.getMaxUses()) {
                throw new BadRequestException("Invitation has reached maximum uses");
            }
        }

        // For EMAIL type, verify email matches
        if (invitation.getType() == InvitationType.EMAIL
                && !user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new BadRequestException("This invitation is for a different email address");
        }

        Project project = invitation.getProject();

        // Check if user is already a project member
        if (projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw new BadRequestException("You are already a member of this project");
        }

        // User must be a workspace member first
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                project.getWorkspace().getId(), userId)) {
            throw new BadRequestException(
                    "You must be a workspace member before joining this project");
        }

        // Add user as project member
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        member.setRole(invitation.getRole());
        member.setAddedBy(invitation.getInvitedBy());

        member = projectMemberRepository.save(member);

        // Update invitation
        if (invitation.getType() == InvitationType.EMAIL) {
            invitation.setAcceptedAt(Instant.now());
            invitation.setAcceptedBy(user);
        }
        invitation.incrementUses();
        invitationRepository.save(invitation);

        return toMemberResponse(member);
    }

    @Transactional
    public void revokeInvitation(UUID invitationId, UUID revokerId) {
        Invitation invitation =
                invitationRepository
                        .findById(invitationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Verify revoker has access to project
        if (!projectMemberRepository.existsByProjectIdAndUserId(
                invitation.getProject().getId(), revokerId)) {
            throw new BadRequestException("You don't have permission to revoke invitations");
        }

        invitation.revoke();
        invitationRepository.save(invitation);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private InvitationResponse toResponse(Invitation invitation) {
        InvitationResponse response = new InvitationResponse();
        response.setId(invitation.getId());
        response.setProjectId(invitation.getProject().getId());
        response.setProjectName(invitation.getProject().getName());
        response.setType(invitation.getType());
        response.setToken(invitation.getToken());
        response.setEmail(invitation.getEmail());
        response.setRole(invitation.getRole());
        response.setInvitedById(invitation.getInvitedBy().getId());
        response.setInvitedByName(invitation.getInvitedBy().getFullName());
        response.setCreatedAt(invitation.getCreatedAt());
        response.setExpiresAt(invitation.getExpiresAt());
        response.setAcceptedAt(invitation.getAcceptedAt());
        if (invitation.getAcceptedBy() != null) {
            response.setAcceptedById(invitation.getAcceptedBy().getId());
            response.setAcceptedByName(invitation.getAcceptedBy().getFullName());
        }
        response.setMaxUses(invitation.getMaxUses());
        response.setCurrentUses(invitation.getCurrentUses());
        response.setExpired(invitation.isExpired());
        response.setRevoked(invitation.isRevoked());
        response.setValid(invitation.isValid());
        return response;
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        ProjectMemberResponse response = new ProjectMemberResponse();
        response.setId(member.getId());
        response.setUserId(member.getUser().getId());
        response.setUserName(member.getUser().getFullName());
        response.setUserEmail(member.getUser().getEmail());
        response.setRole(member.getRole());
        if (member.getAddedBy() != null) {
            response.setAddedById(member.getAddedBy().getId());
            response.setAddedByName(member.getAddedBy().getFullName());
        }
        response.setJoinedAt(member.getJoinedAt());
        return response;
    }
}
