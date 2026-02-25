package ai.planmate.ai.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Worker Token Authentication Filter
 *
 * <p>Protects the /v1/ai/callback endpoint from unauthorized access.
 *
 * <p><b>AUTHENTICATION:</b> Uses a shared secret (X-Worker-Token header) between Java API and
 * Python AI worker.
 *
 * <p><b>SECURITY NOTES:</b>
 *
 * <ul>
 *   <li>Token should be a strong random string (min 32 characters)
 *   <li>Token must match between WORKER_TOKEN env var (Java) and Python worker
 *   <li>Callback endpoint should NOT be exposed to public internet (use internal network only)
 *   <li>Consider using HMAC signatures for production (future enhancement)
 * </ul>
 */
@Component
@Slf4j
public class WorkerTokenFilter extends OncePerRequestFilter {

    @Value("${ai.worker.token:changeme-worker-secret-token}")
    private String workerToken;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply to callback endpoint
        if (!path.equals("/v1/ai/callback")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token from header
        String providedToken = request.getHeader("X-Worker-Token");

        // Validate token
        if (providedToken == null || !providedToken.equals(workerToken)) {
            log.warn(
                    "❌ Unauthorized callback attempt from IP: { }, provided token: { }",
                    request.getRemoteAddr(),
                    providedToken != null ? "***" : "null");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing"
                                    + " X-Worker-Token\"}");
            return;
        }

        log.debug("✅ Worker authentication successful for callback");
        filterChain.doFilter(request, response);
    }
}
