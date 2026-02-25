package ai.planmate.ai.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ai.planmate.ai.entity.AiUsage;
import ai.planmate.ai.repository.AiUsageRepository;
import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.entity.UserPlan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaGuardService {

    private final AiUsageRepository aiUsageRepository;

    @Value("${planmate.limits.ai-requests-per-day-demo:1}")
    private int demoLimit;

    @Value("${planmate.limits.ai-requests-per-day-free:20}")
    private int freeLimit;

    @Value("${planmate.limits.ai-requests-per-day-pro:200}")
    private int proLimit;

    public void checkQuota(AppUser user) {
        int limit = getDailyLimit(user.getPlan());
        AiUsage usage = getOrCreateUsage(user);

        if (usage.getCallsUsed() >= limit) {
            log.warn(
                    "AI quota exceeded for user { } (plan={ }, used={ }, limit={ })",
                    user.getId(),
                    user.getPlan(),
                    usage.getCallsUsed(),
                    limit);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "AI_QUOTA_EXCEEDED: Your AI quota is exhausted. Upgrade to continue.");
        }
    }

    @Transactional
    public void recordUsage(AppUser user, int tokensUsed) {
        AiUsage usage = getOrCreateUsage(user);
        usage.setCallsUsed(usage.getCallsUsed() + 1);
        usage.setTokensUsed(usage.getTokensUsed() + tokensUsed);
        usage.setUpdatedAt(Instant.now());
        aiUsageRepository.save(usage);
        log.debug(
                "Recorded AI usage for user { }: calls={ }, tokens={ }",
                user.getId(),
                usage.getCallsUsed(),
                usage.getTokensUsed());
    }

    public AiUsage getOrCreateUsage(AppUser user) {
        LocalDate today = LocalDate.now();
        return aiUsageRepository
                .findByUserIdAndUsageDate(user.getId(), today)
                .orElseGet(
                        () -> {
                            AiUsage newUsage = new AiUsage();
                            newUsage.setUser(user);
                            newUsage.setUsageDate(today);
                            newUsage.setCallsUsed(0);
                            newUsage.setTokensUsed(0);
                            return aiUsageRepository.save(newUsage);
                        });
    }

    public int getDailyLimit(UserPlan plan) {
        return switch (plan) {
            case DEMO -> demoLimit;
            case FREE -> freeLimit;
            case PRO -> proLimit;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    public QuotaStatus getQuotaStatus(AppUser user) {
        int limit = getDailyLimit(user.getPlan());
        AiUsage usage = getOrCreateUsage(user);
        return new QuotaStatus(
                user.getPlan().name(),
                usage.getCallsUsed(),
                limit,
                usage.getTokensUsed(),
                limit - usage.getCallsUsed());
    }

    public record QuotaStatus(
            String plan, int callsUsed, int dailyLimit, int tokensUsed, int remaining) { }
}
