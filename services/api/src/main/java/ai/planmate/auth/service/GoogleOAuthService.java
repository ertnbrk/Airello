package ai.planmate.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.auth.dto.AuthResponse;
import ai.planmate.auth.dto.GoogleTokenResponse;
import ai.planmate.auth.dto.GoogleUserInfo;
import ai.planmate.auth.dto.UserDto;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.AuthProvider;
import ai.planmate.auth.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for Google OAuth2 authentication */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthService {

    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    @Value("${google.frontend-redirect-uri:http://localhost:3000/auth/callback}")
    private String frontendRedirectUri;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";

    /** Generate Google OAuth2 authorization URL */
    public String getAuthorizationUrl() {
        String scope = "openid email profile";
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8);

        return String.format(
                "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                GOOGLE_AUTH_URL, clientId, encodedRedirectUri, encodedScope);
    }

    /** Handle Google OAuth2 callback and authenticate user */
    @Transactional
    public String handleCallback(String code) {
        try {
            log.info("Handling Google OAuth callback");

            // Exchange authorization code for access token
            GoogleTokenResponse tokenResponse = exchangeCodeForToken(code);

            // Get user info from Google
            GoogleUserInfo googleUser = getUserInfo(tokenResponse.accessToken());

            log.info("Google user info retrieved: { }", googleUser.email());

            // Find or create user
            AppUser user = findOrCreateUser(googleUser);

            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            // Generate JWT token
            String jwtToken = jwtService.generateAccessToken(user);

            log.info("User authenticated successfully via Google: { }", user.getId());

            // Redirect to frontend with token
            return String.format("%s?token=%s", frontendRedirectUri, jwtToken);

        } catch (Exception e) {
            log.error("Error during Google OAuth callback", e);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Google authentication failed");
        }
    }

    /** Exchange authorization code for access token */
    private GoogleTokenResponse exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponse> response =
                restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, GoogleTokenResponse.class);

        if (response.getBody() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Failed to get Google access token");
        }

        return response.getBody();
    }

    /** Get user info from Google */
    private GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfo> response =
                restTemplate.exchange(
                        GOOGLE_USERINFO_URL, HttpMethod.GET, entity, GoogleUserInfo.class);

        if (response.getBody() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Failed to get Google user info");
        }

        return response.getBody();
    }

    /** Find existing user or create new one from Google user info */
    private AppUser findOrCreateUser(GoogleUserInfo googleUser) {
        // Try to find by provider ID (Google user ID)
        return userRepository
                .findByProviderId(googleUser.id())
                .orElseGet(
                        () -> {
                            // Check if email already exists with different provider
                            userRepository
                                    .findByEmail(googleUser.email())
                                    .ifPresent(
                                            existingUser -> {
                                                if (existingUser.getProvider()
                                                        != AuthProvider.GOOGLE) {
                                                    log.warn(
                                                            "Email { } already registered with"
                                                                    + " different provider",
                                                            googleUser.email());
                                                    throw new ResponseStatusException(
                                                            HttpStatus.CONFLICT,
                                                            "Email already registered with"
                                                                    + " different provider");
                                                }
                                            });

                            // Create new user
                            AppUser newUser = new AppUser();
                            newUser.setEmail(googleUser.email());
                            newUser.setFullName(
                                    googleUser.name() != null
                                            ? googleUser.name()
                                            : googleUser.email());
                            newUser.setProvider(AuthProvider.GOOGLE);
                            newUser.setProviderId(googleUser.id());
                            newUser.setEmailVerified(googleUser.verifiedEmail());
                            newUser.setActive(true);

                            log.info("Creating new user from Google: { }", googleUser.email());
                            return userRepository.save(newUser);
                        });
    }

    /** Authenticate user and return AuthResponse (for direct API calls) */
    @Transactional
    public AuthResponse authenticateWithGoogle(String code) {
        GoogleTokenResponse tokenResponse = exchangeCodeForToken(code);
        GoogleUserInfo googleUser = getUserInfo(tokenResponse.accessToken());
        AppUser user = findOrCreateUser(googleUser);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        UserDto userDto = authService.mapToUserDto(user);

        return new AuthResponse(accessToken, refreshToken, userDto);
    }
}
