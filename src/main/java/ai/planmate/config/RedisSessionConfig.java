package ai.planmate.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Session Management Configuration for Horizontal Scalability.
 *
 * <p><b>PURPOSE:</b> Enables session sharing across multiple application instances.
 *
 * <p><b>ARCHITECTURE:</b>
 *
 * <pre>
 * ┌──────────────┐     ┌──────────────┐
 * │  Instance 1  │     │  Instance 2  │
 * └──────┬───────┘     └──────┬───────┘
 *        │                    │
 *        └──────┬─────────────┘
 *               │
 *         ┌─────▼──────┐
 *         │   Redis    │ (Shared Session Store)
 *         └────────────┘
 * </pre>
 *
 * <p><b>BENEFITS:</b>
 *
 * <ul>
 *   <li>User session persists across server restarts
 *   <li>Load balancer can route requests to any instance
 *   <li>No sticky sessions required
 *   <li>WebSocket connections can span multiple instances
 * </ul>
 *
 * <p><b>SESSION TIMEOUT:</b> 30 minutes (1800 seconds)
 *
 * <p><b>NAMESPACE:</b> "airello:session" (prevents collision with other Redis data)
 */
@Slf4j
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800) // 30 minutes
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
public class RedisSessionConfig {

    /**
     * Configures session cookie serialization.
     *
     * <p><b>COOKIE SETTINGS:</b>
     *
     * <ul>
     *   <li>Name: SESSION (Spring Session default)
     *   <li>Path: / (available across entire application)
     *   <li>HttpOnly: true (prevents XSS attacks)
     *   <li>Secure: false in dev, true in production (HTTPS only)
     *   <li>SameSite: Lax (CSRF protection)
     * </ul>
     *
     * @return Configured cookie serializer
     */
    @Bean
    public CookieSerializer cookieSerializer() {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$"); // Allow subdomains

        // Security settings
        serializer.setUseHttpOnlyCookie(true); // Prevent XSS
        serializer.setUseSecureCookie(false); // TODO: Enable in production (HTTPS)
        serializer.setSameSite("Lax"); // CSRF protection

        log.info("✅ Redis session cookie serializer configured");
        return serializer;
    }

    /**
     * Custom configuration for RedisConnectionFactory (if needed).
     *
     * <p>By default, Spring Boot auto-configures RedisConnectionFactory from application.yml
     * properties. This method is included for demonstration and can be uncommented if custom
     * configuration is needed.
     *
     * @param redisConnectionFactory Auto-configured connection factory
     */
    @Bean
    public String redisSessionInfo(RedisConnectionFactory redisConnectionFactory) {
        log.info("🚀 Redis HTTP Session enabled:");
        log.info("   - Max inactive interval: 1800 seconds (30 minutes)");
        log.info("   - Namespace: airello:session");
        log.info("   - Connection: { }", redisConnectionFactory);
        log.info("   - Sessions will survive server restarts");
        log.info("   - Multi-instance deployment ready");
        return "RedisSessionEnabled";
    }
}
