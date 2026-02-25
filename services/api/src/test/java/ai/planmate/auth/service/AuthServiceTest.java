package ai.planmate.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ai.planmate.auth.dto.AuthResponse;
import ai.planmate.auth.dto.RegisterRequest;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserPlan;
import ai.planmate.auth.entity.UserType;
import ai.planmate.auth.repository.AppUserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AppUserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
    }

    @Test
    void createAnonymousUserShouldCreateDemoUser() {
        AppUser savedUser = new AppUser();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUserType(UserType.ANONYMOUS);
        savedUser.setPlan(UserPlan.DEMO);

        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        AppUser result = authService.createAnonymousUser();

        assertNotNull(result);
        assertEquals(UserType.ANONYMOUS, result.getUserType());
        assertEquals(UserPlan.DEMO, result.getPlan());
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    void registerWithAnonymousUserIdShouldUpgradeUser() {
        UUID anonymousId = UUID.randomUUID();
        AppUser anonymousUser = new AppUser();
        anonymousUser.setId(anonymousId);
        anonymousUser.setUserType(UserType.ANONYMOUS);
        anonymousUser.setPlan(UserPlan.DEMO);

        when(userRepository.findById(anonymousId)).thenReturn(Optional.of(anonymousUser));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenReturn(anonymousUser);

        RegisterRequest request = new RegisterRequest("test@example.com", "password", "Test User");

        AuthResponse response = authService.register(request, anonymousId);

        assertNotNull(response);
        assertEquals(UserType.REGISTERED, anonymousUser.getUserType());
        assertEquals(UserPlan.FREE, anonymousUser.getPlan());
        assertEquals("test@example.com", anonymousUser.getEmail());
        verify(userRepository).save(anonymousUser);
    }

    @Test
    void registerNewUserShouldCreateRegisteredUser() {
        AppUser savedUser = new AppUser();
        savedUser.setId(UUID.randomUUID());
        savedUser.setUserType(UserType.REGISTERED);
        savedUser.setPlan(UserPlan.FREE);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenReturn(savedUser);

        RegisterRequest request = new RegisterRequest("new@example.com", "password", "New User");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.token());
        verify(userRepository).save(any(AppUser.class));
    }
}
