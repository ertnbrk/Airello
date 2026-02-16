package ai.planmate.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

/** Controller for authentication endpoints */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final DemoService demoService;
    private final JwtService jwtService;

    /**
     * Register a new user with email and password
     *
     * <p>POST /auth/register Body: { "email": "user@example.com", "password": "123456", "name":
     * "John Doe" } Response: { "token": "jwt_token", "refreshToken": "refresh_token", "user": {...}
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: { }", request.email());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with email and password
     *
     * <p>POST /auth/login Body: { "email": "user@example.com", "password": "123456" } Response: {
     * "token": "jwt_token", "refreshToken": "refresh_token", "user": {...} }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: { }", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user information
     *
     * <p>GET /auth/me Header: Authorization: Bearer {jwt_token} Response: { "id": "uuid", "email":
     * "user@example.com", "name": "John Doe", "provider": "LOCAL" }
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal AppUser user) {
        log.debug("Get current user request for: { }", user.getEmail());
        UserDto userDto = authService.mapToUserDto(user);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Initiate Google OAuth2 login flow
     *
     * <p>GET /auth/oauth2/google Redirects to Google authorization page
     */
    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) throws Exception {
        log.info("Redirecting to Google OAuth2 authorization");
        String authUrl = googleOAuthService.getAuthorizationUrl();
        response.sendRedirect(authUrl);
    }

    /**
     * Google OAuth2 callback endpoint
     *
     * <p>GET /auth/oauth2/google/callback?code={authorization_code} Redirects to frontend with JWT
     * token
     */
    @GetMapping("/oauth2/google/callback")
    public void googleCallback(@RequestParam String code, HttpServletResponse response)
            throws Exception {
        log.info("Google OAuth2 callback received");
        String redirectUrl = googleOAuthService.handleCallback(code);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Create anonymous demo session
     *
     * <p>POST /auth/demo Response: { "token": "jwt_token", "refreshToken": "refresh_token", "user":
     * {...} } Creates an anonymous user with a demo project.
     */
    @PostMapping("/demo")
    public ResponseEntity<AuthResponse> createDemo() {
        log.info("Demo session creation request");
        AuthResponse response = demoService.createDemoSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register and upgrade anonymous user (lazy login)
     *
     * <p>POST /auth/register?upgradeFromAnonymous={userId} Body: { "email": "user@example.com",
     * "password": "123456", "name": "John Doe" } Response: { "token": "jwt_token", "refreshToken":
     * "refresh_token", "user": {...} }
     */
    @PostMapping("/register-upgrade")
    public ResponseEntity<AuthResponse> registerUpgrade(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal AppUser anonymousUser) {
        log.info(
                "Lazy login: upgrading anonymous user { } to { }",
                anonymousUser.getId(),
                request.email());

        AuthResponse response = authService.register(request, anonymousUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
