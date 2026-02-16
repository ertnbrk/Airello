package ai.planmate.ai.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ai.planmate.auth.entity.AppUser;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiRouterService {

    private final Map<String, AiProvider> providers = new HashMap<>();
    private final QuotaGuardService quotaGuardService;
    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    @Value("${ai.primary-provider:openai}")
    private String primaryProviderName;

    @Value("${ai.fallback-provider:mock}")
    private String fallbackProviderName;

    public AiRouterService(List<AiProvider> providerList, QuotaGuardService quotaGuardService) {
        this.quotaGuardService = quotaGuardService;
        for (AiProvider provider : providerList) {
            providers.put(provider.name(), provider);
        }
        log.info("AI Router initialized with providers: { }", providers.keySet());
    }

    public AiProvider.AiResult route(
            AppUser user, String prompt, Map<String, Object> tools, Map<String, Object> context) {

        // Check quota before calling any provider
        quotaGuardService.checkQuota(user);

        // Check cache
        String cacheKey =
                buildCacheKey(
                        user.getId().toString(), context != null ? context.toString() : "", prompt);
        CachedResult cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Cache hit for AI request");
            return cached.result;
        }

        // Try primary provider
        AiProvider primary = providers.get(primaryProviderName);
        if (primary != null) {
            log.debug("Trying primary provider: { }", primary.name());
            AiProvider.AiResult result = primary.invoke(prompt, tools, context);
            if (result.success()) {
                quotaGuardService.recordUsage(user, result.tokensUsed());
                cache.put(cacheKey, new CachedResult(result));
                return result;
            }
            log.warn("Primary provider { } failed: { }", primary.name(), result.error());
        }

        // Fallback
        AiProvider fallback = providers.get(fallbackProviderName);
        if (fallback != null && !fallback.name().equals(primaryProviderName)) {
            log.debug("Trying fallback provider: { }", fallback.name());
            AiProvider.AiResult result = fallback.invoke(prompt, tools, context);
            if (result.success()) {
                quotaGuardService.recordUsage(user, result.tokensUsed());
                cache.put(cacheKey, new CachedResult(result));
                return result;
            }
            log.error("Fallback provider { } also failed: { }", fallback.name(), result.error());
        }

        // All providers failed, use mock as last resort
        AiProvider mock = providers.get("mock");
        if (mock != null) {
            AiProvider.AiResult result = mock.invoke(prompt, tools, context);
            quotaGuardService.recordUsage(user, result.tokensUsed());
            return result;
        }

        return AiProvider.AiResult.failure("All AI providers failed");
    }

    private String buildCacheKey(String userId, String context, String prompt) {
        return String.valueOf((userId + "|" + context + "|" + prompt).hashCode());
    }

    private static class CachedResult {
        final AiProvider.AiResult result;
        final long timestamp;
        static final long TTL_MS = 3600_000; // 1 hour

        CachedResult(AiProvider.AiResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL_MS;
        }
    }
}
