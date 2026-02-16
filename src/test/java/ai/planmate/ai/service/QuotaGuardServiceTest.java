package ai.planmate.ai.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.ai.entity.AiUsage;
import ai.planmate.ai.repository.AiUsageRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserPlan;

@ExtendWith(MockitoExtension.class)
class QuotaGuardServiceTest {

    @Mock private AiUsageRepository aiUsageRepository;

    @InjectMocks private QuotaGuardService quotaGuardService;

    private AppUser demoUser;
    private AppUser freeUser;
    private AppUser proUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(quotaGuardService, "demoLimit", 1);
        ReflectionTestUtils.setField(quotaGuardService, "freeLimit", 20);
        ReflectionTestUtils.setField(quotaGuardService, "proLimit", 200);

        demoUser = new AppUser();
        demoUser.setId(UUID.randomUUID());
        demoUser.setPlan(UserPlan.DEMO);

        freeUser = new AppUser();
        freeUser.setId(UUID.randomUUID());
        freeUser.setPlan(UserPlan.FREE);

        proUser = new AppUser();
        proUser.setId(UUID.randomUUID());
        proUser.setPlan(UserPlan.PRO);
    }

    @Test
    void checkQuotaDemoUserExhaustedShouldThrow() {
        AiUsage usage = new AiUsage();
        usage.setUser(demoUser);
        usage.setUsageDate(LocalDate.now());
        usage.setCallsUsed(1);

        when(aiUsageRepository.findByUserIdAndUsageDate(demoUser.getId(), LocalDate.now()))
                .thenReturn(Optional.of(usage));

        assertThrows(ResponseStatusException.class, () -> quotaGuardService.checkQuota(demoUser));
    }

    @Test
    void checkQuotaFreeUserWithinLimitShouldPass() {
        AiUsage usage = new AiUsage();
        usage.setUser(freeUser);
        usage.setUsageDate(LocalDate.now());
        usage.setCallsUsed(10);

        when(aiUsageRepository.findByUserIdAndUsageDate(freeUser.getId(), LocalDate.now()))
                .thenReturn(Optional.of(usage));

        assertDoesNotThrow(() -> quotaGuardService.checkQuota(freeUser));
    }

    @Test
    void checkQuotaFreeUserExhaustedShouldThrow() {
        AiUsage usage = new AiUsage();
        usage.setUser(freeUser);
        usage.setUsageDate(LocalDate.now());
        usage.setCallsUsed(20);

        when(aiUsageRepository.findByUserIdAndUsageDate(freeUser.getId(), LocalDate.now()))
                .thenReturn(Optional.of(usage));

        assertThrows(ResponseStatusException.class, () -> quotaGuardService.checkQuota(freeUser));
    }

    @Test
    void recordUsageShouldIncrementCalls() {
        AiUsage usage = new AiUsage();
        usage.setUser(proUser);
        usage.setUsageDate(LocalDate.now());
        usage.setCallsUsed(5);
        usage.setTokensUsed(100);

        when(aiUsageRepository.findByUserIdAndUsageDate(proUser.getId(), LocalDate.now()))
                .thenReturn(Optional.of(usage));
        when(aiUsageRepository.save(any(AiUsage.class))).thenReturn(usage);

        quotaGuardService.recordUsage(proUser, 50);

        assertEquals(6, usage.getCallsUsed());
        assertEquals(150, usage.getTokensUsed());
        verify(aiUsageRepository).save(usage);
    }
}
