package ai.planmate.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.auth.dto.AuthResponse;
import ai.planmate.auth.dto.LoginRequest;
import ai.planmate.auth.dto.RegisterRequest;
import ai.planmate.auth.dto.UserDto;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.AuthProvider;
import ai.planmate.auth.entity.UserPlan;
import ai.planmate.auth.entity.UserType;
import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for authentication operations (register, login, demo) */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Register a new user with email and password */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    /**
     * Register a new user. If anonymousUserId is provided, upgrade the anonymous user instead of
     * creating a new one.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, UUID anonymousUserId) {
        log.info("Registering user with email: { }", request.email());

        if (anonymousUserId != null) {
            // Lazy login: upgrade anonymous user to registered
            return upgradeAnonymousUser(anonymousUserId, request);
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: Email already exists: { }", request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Create new user
        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.name());
        user.setProvider(AuthProvider.LOCAL);
        user.setUserType(UserType.REGISTERED);
        user.setPlan(UserPlan.FREE);
        user.setEmailVerified(false);
        user.setActive(true);

        user = userRepository.save(user);
        log.info("User registered successfully with ID: { }", user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, mapToUserDto(user));
    }

    /** Upgrade an anonymous user to a registered user */
    private AuthResponse upgradeAnonymousUser(UUID anonymousUserId, RegisterRequest request) {
        AppUser user =
                userRepository
                        .findById(anonymousUserId)
                        .filter(u -> u.getUserType() == UserType.ANONYMOUS)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Invalid anonymous user"));

        // Check if email is taken by another user
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.name());
        user.setUserType(UserType.REGISTERED);
        user.setPlan(UserPlan.FREE);
        user.setProvider(AuthProvider.LOCAL);
        user = userRepository.save(user);

        log.info("Anonymous user { } upgraded to registered", user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, mapToUserDto(user));
    }

    /** Login user with email and password */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: { }", request.email());

        AppUser user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(
                                () -> {
                                    log.warn("Login failed: User not found: { }", request.email());
                                    return new ResponseStatusException(
                                            HttpStatus.UNAUTHORIZED, "Invalid credentials");
                                });

        if (!user.getActive()) {
            log.warn("Login failed: User is inactive: { }", request.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is inactive");
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: Invalid password for: { }", request.email());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: { }", user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, mapToUserDto(user));
    }

    /** Create an anonymous demo user */
    @Transactional
    public AppUser createAnonymousUser() {
        AppUser user = new AppUser();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        user.setEmail("demo-" + uniqueId + "@anonymous.planmate.ai");
        user.setFullName("Demo User");
        user.setProvider(AuthProvider.LOCAL);
        user.setUserType(UserType.ANONYMOUS);
        user.setPlan(UserPlan.DEMO);
        user.setEmailVerified(false);
        user.setActive(true);

        user = userRepository.save(user);
        log.info("Anonymous demo user created: { }", user.getId());
        return user;
    }

    /** Get user by ID for authenticated requests */
    public AppUser getUserById(UUID userId) {
        return userRepository
                .findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /** Find user by ID only if active (not deleted, not inactive) */
    public java.util.Optional<AppUser> findByIdActive(UUID userId) {
        return userRepository.findById(userId).filter(user -> !user.isDeleted() && user.getActive());
    }

    /** Map AppUser entity to UserDto */
    public UserDto mapToUserDto(AppUser user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getProvider(),
                user.getUserType().name(),
                user.getPlan().name());
    }
}
