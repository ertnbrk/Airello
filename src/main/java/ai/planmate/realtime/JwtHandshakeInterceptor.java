package ai.planmate.realtime;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import ai.planmate.auth.entity.AppUser;
import ai.planmate.auth.service.AuthService;
import ai.planmate.auth.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final AuthService authService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes)
            throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token == null || token.isEmpty()) {
                log.warn("WebSocket handshake rejected: missing token parameter");
                return false;
            }

            try {
                if (!jwtService.validateToken(token)) {
                    log.warn("WebSocket handshake rejected: invalid token");
                    return false;
                }

                UUID userId = jwtService.extractUserId(token);
                AppUser user = authService.getUserById(userId);

                if (!user.getActive()) {
                    log.warn("WebSocket handshake rejected: inactive user {}", userId);
                    return false;
                }

                attributes.put("userId", userId);
                attributes.put("userEmail", user.getEmail());

                log.info("WebSocket handshake accepted for user: {}", user.getEmail());
                return true;

            } catch (Exception e) {
                log.error("WebSocket handshake failed: {}", e.getMessage());
                return false;
            }
        }

        log.warn("WebSocket handshake rejected: not a servlet request");
        return false;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {

        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        }
    }
}
