package ai.planmate.auth.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.auth.dto.AuthResponse;
import ai.planmate.auth.dto.LoginRequest;
import ai.planmate.auth.dto.RegisterRequest;
import ai.planmate.auth.dto.UserDto;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.service.AuthService;
import ai.planmate.auth.service.DemoService;
import ai.planmate.auth.service.GoogleOAuthService;
import ai.planmate.auth.service.JwtService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final DemoService demoService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.email());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("GET /auth/me called without authentication");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.error("GET /auth/me: principal is null despite authenticated status");
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authentication state");
        }

        String userIdStr = authentication.getName();
        if (userIdStr == null || userIdStr.isEmpty()) {
            log.error(
                    "GET /auth/me: authentication.getName() returned null/empty, principal type: {}",
                    principal.getClass().getName());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authentication identifier");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("GET /auth/me: Invalid UUID format in principal: {}", userIdStr);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid user identifier");
        }

        AppUser user =
                authService
                        .findByIdActive(userId)
                        .orElseThrow(
                                () -> {
                                    log.warn(
                                            "GET /auth/me: User not found or inactive for userId:"
                                                    + " {}",
                                            userId);
                                    return new ResponseStatusException(
                                            HttpStatus.UNAUTHORIZED, "User not found");
                                });

        log.debug("GET /auth/me: Returning user data for: {}", user.getEmail());
        UserDto userDto = authService.mapToUserDto(user);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) throws Exception {
        log.info("Redirecting to Google OAuth2 authorization");
        String authUrl = googleOAuthService.getAuthorizationUrl();
        response.sendRedirect(authUrl);
    }

    @GetMapping("/oauth2/google/callback")
    public void googleCallback(@RequestParam String code, HttpServletResponse response)
            throws Exception {
        log.info("Google OAuth2 callback received");
        String redirectUrl = googleOAuthService.handleCallback(code);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/demo")
    public ResponseEntity<AuthResponse> createDemo() {
        log.info("Demo session creation request");
        AuthResponse response = demoService.createDemoSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register-upgrade")
    public ResponseEntity<AuthResponse> registerUpgrade(
            @Valid @RequestBody RegisterRequest request, Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String userIdStr = authentication.getName();
        UUID anonymousUserId;
        try {
            anonymousUserId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID in authentication for upgrade: {}", userIdStr);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user session");
        }

        log.info("Lazy login: upgrading anonymous user {} to {}", anonymousUserId, request.email());

        AuthResponse response = authService.register(request, anonymousUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
