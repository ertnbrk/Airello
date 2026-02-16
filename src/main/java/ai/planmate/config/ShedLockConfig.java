package ai.planmate.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * ShedLock Configuration for Distributed Task Locking.
 *
 * <p><b>PURPOSE:</b> Prevents duplicate execution of scheduled tasks in multi-instance deployments.
 *
 * <p><b>PROBLEM:</b>
 *
 * <pre>
 * Instance 1: @Scheduled task runs at 00:00:00
 * Instance 2: @Scheduled task runs at 00:00:00
 * Result: Same task executed twice (e.g., duplicate outbox publishing)
 * </pre>
 *
 * <p><b>SOLUTION:</b>
 *
 * <pre>
 * Instance 1: Acquires lock "outbox-publisher" ✅
 * Instance 2: Tries to acquire lock ❌ (skips execution)
 * </pre>
 *
 * <p><b>CONFIGURATION:</b>
 *
 * <ul>
 *   <li>Provider: JDBC (uses database table "shedlock")
 *   <li>Default lock duration: 10 minutes
 *   <li>Lock expiration: Automatic (prevents deadlock if instance crashes)
 * </ul>
 *
 * <p><b>USAGE:</b>
 *
 * <pre>
 * {@literal @}Scheduled(fixedDelay = 5000)
 * {@literal @}SchedulerLock(name = "outbox-publisher", lockAtMostFor = "30s", lockAtLeastFor = "3s")
 * public void pollAndPublish() {
 *     // Only ONE instance will execute this at a time
 * }
 * </pre>
 *
 * <p><b>DATABASE TABLE:</b> Created by Flyway migration V24
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * Creates JDBC-based lock provider using the application's DataSource.
     *
     * <p><b>TABLE:</b> shedlock (created by V24 migration)
     *
     * @param dataSource The application's data source
     * @return Lock provider instance
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        log.info("🔒 Configuring ShedLock with JDBC provider");
        log.info("   - Provider: JdbcTemplate (PostgreSQL)");
        log.info("   - Table: shedlock");
        log.info("   - Default lock duration: 10 minutes");
        log.info("   - Multi-instance deployment ready");

        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime() // Use database time (important for distributed systems)
                        .build());
    }
}
