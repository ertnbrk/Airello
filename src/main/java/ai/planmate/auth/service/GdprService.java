package ai.planmate.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GdprService {

    private final AppUserRepository appUserRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public String exportUserData() {
        // TODO: Without authentication, implement user identification mechanism (e.g., pass userId
        // as parameter)
        throw new UnsupportedOperationException(
                "GDPR export requires authentication to be enabled");
        /* Original code before removing authentication:
            UUID currentUserId = securityContext.getCurrentUserId();
            AppUser user =
                    appUserRepository
                            .findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Map<String, Object> exportData = new HashMap<>();
            exportData.put("user_id", user.getId().toString());
            exportData.put("email", user.getEmail());
            exportData.put("full_name", user.getFullName());
            exportData.put("plan", user.getPlan().toString());
            exportData.put("email_verified", user.getEmailVerified());
            exportData.put("created_at", user.getCreatedAt().toString());
            exportData.put("last_login_at", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
            exportData.put("export_date", Instant.now().toString());

            // Add more data: projects, issues, etc. (simplified for MVP)

            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);
            } catch (Exception e) {
                throw new RuntimeException("Failed to export data", e);
            }
        }

        @Transactional
        public void deleteUserAccount() {
            UUID currentUserId = securityContext.getCurrentUserId();
            AppUser user =
                    appUserRepository
                            .findById(currentUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Soft delete
            user.softDelete();
            user.setActive(false);
            appUserRepository.save(user);

            log.info("User account deleted (soft): { }", user.getId());

            // TODO: Schedule hard delete after retention period (e.g., 30 days)
            // TODO: Delete associated data (projects, issues, artifacts)
            */
    }

    @Transactional
    public void deleteUserAccount() {
        // TODO: Without authentication, implement user identification mechanism (e.g., pass userId
        // as parameter)
        throw new UnsupportedOperationException(
                "Account deletion requires authentication to be enabled");
        /* Original code before removing authentication:
        UUID currentUserId = securityContext.getCurrentUserId();
        AppUser user =
                appUserRepository
                        .findById(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Soft delete
        user.softDelete();
        user.setActive(false);
        appUserRepository.save(user);

        log.info("User account deleted (soft): { }", user.getId());

        // TODO: Schedule hard delete after retention period (e.g., 30 days)
        // TODO: Delete associated data (projects, issues, artifacts)
        */
    }
}
