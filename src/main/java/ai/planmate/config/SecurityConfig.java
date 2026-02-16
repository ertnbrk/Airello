package ai.planmate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import ai.planmate.ai.filter.WorkerTokenFilter;
import ai.planmate.auth.filter.JwtAuthenticationFilter;
import ai.planmate.auth.service.AuthService;
import ai.planmate.auth.service.JwtService;

/** Security configuration for JWT authentication and authorization */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.enable-auth:true}")
    private boolean enableAuth;

    /** JWT authentication filter bean */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, AuthService authService) {
        return new JwtAuthenticationFilter(jwtService, authService);
    }

    /** Configure HTTP security with JWT authentication */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            WorkerTokenFilter workerTokenFilter)
            throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Disable CORS (handled by WebConfig)
                .cors(AbstractHttpConfigurer::disable)

                // Session management - stateless
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(
                        auth -> {
                            // Public endpoints - no authentication required
                            auth.requestMatchers("/auth/**").permitAll();
                            auth.requestMatchers("/actuator/**").permitAll();
                            auth.requestMatchers("/error").permitAll();
                            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                            // AI callback endpoint - protected by WorkerTokenFilter
                            auth.requestMatchers("/v1/ai/callback").permitAll();

                            if (enableAuth) {
                                // Protected endpoints - JWT authentication required
                                auth.requestMatchers("/v1/**").authenticated();
                                auth.anyRequest().authenticated();
                            } else {
                                // Dev mode - allow all requests (for development/testing)
                                auth.anyRequest().permitAll();
                            }
                        })

                // Add Worker Token filter first (for callback endpoint)
                .addFilterBefore(workerTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Password encoder bean for hashing passwords */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
