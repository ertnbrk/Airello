package ai.planmate.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for AI Job Queue and Session Management.
 *
 * <p><b>USE CASES:</b>
 *
 * <ul>
 *   <li>AI Job Queue: LPUSH/BRPOP for async AI worker communication
 *   <li>Session Store: Spring Session with Redis backend
 *   <li>Rate Limiting: Distributed rate limiting across API instances
 *   <li>Semantic Cache: Future enhancement for AI response caching
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "planmate.features.redis-enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializers for simplicity and compatibility
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
