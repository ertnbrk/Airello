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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.enable-auth:true}")
    private boolean enableAuth;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService, AuthService authService) {
        return new JwtAuthenticationFilter(jwtService, authService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            WorkerTokenFilter workerTokenFilter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> {
                            auth.requestMatchers("/auth/**").permitAll();
                            auth.requestMatchers("/ws/**").permitAll();
                            auth.requestMatchers("/actuator/**").permitAll();
                            auth.requestMatchers("/error").permitAll();
                            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                            auth.requestMatchers("/v1/ai/callback").permitAll();

                            if (enableAuth) {
                                auth.requestMatchers("/v1/**").authenticated();
                                auth.anyRequest().authenticated();
                            } else {
                                auth.anyRequest().permitAll();
                            }
                        })
                .addFilterBefore(workerTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
