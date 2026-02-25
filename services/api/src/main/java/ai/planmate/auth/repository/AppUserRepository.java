package ai.planmate.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.planmate.auth.entity.AppUser;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("SELECT u FROM AppUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<AppUser> findByEmail(String email);

    @Query("SELECT u FROM AppUser u WHERE u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<AppUser> findByProviderId(String providerId);

    @Query(
            "SELECT u FROM AppUser u WHERE u.keycloakSubject = :keycloakSubject AND u.deletedAt IS"
                    + " NULL")
    Optional<AppUser> findByKeycloakSubject(String keycloakSubject);

    @Query(
            "SELECT u FROM AppUser u WHERE u.stripeCustomerId = :stripeCustomerId AND u.deletedAt"
                    + " IS NULL")
    Optional<AppUser> findByStripeCustomerId(String stripeCustomerId);

    @Query("SELECT COUNT(u) > 0 FROM AppUser u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(String email);
}
