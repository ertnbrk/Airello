package ai.planmate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket configuration for real-time communication with RabbitMQ STOMP Relay.
 *
 * <p><b>ARCHITECTURE:</b> External Broker Relay (RabbitMQ) for horizontal scalability. This enables
 * multi-instance deployments where:
 *
 * <ul>
 *   <li>User A connects to Instance 1
 *   <li>User B connects to Instance 2
 *   <li>They still receive each other's real-time updates via RabbitMQ
 * </ul>
 *
 * <p><b>STOMP DESTINATIONS:</b>
 *
 * <ul>
 *   <li>/topic/projects/{projectId}/board - Board updates (drag & drop, column changes)
 *   <li>/topic/threads/{threadId} - Chat messages (real-time collaboration)
 *   <li>/topic/projects/{projectId} - General project events (member joins, issue updates)
 *   <li>/queue/... - Point-to-point messaging (user-specific notifications)
 * </ul>
 *
 * <p><b>CLIENT SEND DESTINATIONS:</b>
 *
 * <ul>
 *   <li>/app/... - Application-specific messages routed to @MessageMapping handlers
 * </ul>
 *
 * <p><b>FALLBACK:</b> If RabbitMQ is unavailable, falls back to in-memory SimpleBroker.
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final boolean rabbitmqEnabled;
    private final String stompRelayHost;
    private final int stompRelayPort;
    private final String stompRelayClientLogin;
    private final String stompRelayClientPasscode;
    private final String stompRelaySystemLogin;
    private final String stompRelaySystemPasscode;

    public WebSocketConfig(
            @Value("${rabbitmq.enabled:false}") boolean rabbitmqEnabled,
            @Value("${stomp.relay.host:localhost}") @Nullable String stompRelayHost,
            @Value("${stomp.relay.port:61613}") int stompRelayPort,
            @Value("${stomp.relay.client.login:guest}") @Nullable String stompRelayClientLogin,
            @Value("${stomp.relay.client.passcode:guest}") @Nullable
                    String stompRelayClientPasscode,
            @Value("${stomp.relay.system.login:guest}") @Nullable String stompRelaySystemLogin,
            @Value("${stomp.relay.system.passcode:guest}") @Nullable
                    String stompRelaySystemPasscode) {
        this.rabbitmqEnabled = rabbitmqEnabled;
        this.stompRelayHost = stompRelayHost;
        this.stompRelayPort = stompRelayPort;
        this.stompRelayClientLogin = stompRelayClientLogin;
        this.stompRelayClientPasscode = stompRelayClientPasscode;
        this.stompRelaySystemLogin = stompRelaySystemLogin;
        this.stompRelaySystemPasscode = stompRelaySystemPasscode;
    }

    /**
     * Configures the message broker for handling subscriptions and broadcasting.
     *
     * <p><b>Production Mode (RabbitMQ Enabled):</b> Uses external RabbitMQ STOMP Broker Relay for
     * horizontal scalability. This allows multiple application instances to share WebSocket state.
     *
     * <p><b>Development/Fallback Mode:</b> Uses in-memory SimpleBroker for single-instance
     * deployments.
     *
     * @param registry Message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        if (rabbitmqEnabled && stompRelayHost != null) {
            log.info(
                    "🚀 Configuring RabbitMQ STOMP Relay: { }:{ }", stompRelayHost, stompRelayPort);

            // External STOMP Broker Relay (RabbitMQ) - Production Mode
            registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(stompRelayHost)
                    .setRelayPort(stompRelayPort)
                    .setClientLogin(stompRelayClientLogin)
                    .setClientPasscode(stompRelayClientPasscode)
                    .setSystemLogin(stompRelaySystemLogin)
                    .setSystemPasscode(stompRelaySystemPasscode)
                    .setSystemHeartbeatSendInterval(10000) // Send heartbeat every 10s
                    .setSystemHeartbeatReceiveInterval(10000) // Expect heartbeat every 10s
                    .setVirtualHost("/"); // Use default RabbitMQ vhost

            log.info("✅ RabbitMQ STOMP Relay enabled - Multi-instance WebSocket ready");
        } else {
            log.warn(
                    "⚠️  Using in-memory SimpleBroker (RabbitMQ disabled) - NOT suitable for"
                            + " multi-instance deployment");

            // In-memory Simple Broker - Development/Fallback Mode
            // Note: Heartbeat disabled as it requires TaskScheduler configuration
            registry.enableSimpleBroker("/topic", "/queue");
        }

        // Prefix for messages sent from client to server
        // Example: Client sends to "/app/board/move" -> routes to @MessageMapping("/board/move")
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific destinations
        // Example: /user/{username}/queue/notifications
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registers STOMP endpoints for WebSocket connections.
     *
     * <p>Endpoint: <code>/ws</code>
     *
     * <ul>
     *   <li>Supports SockJS fallback for browsers without native WebSocket support
     *   <li>Allows cross-origin connections (configure CORS in production)
     * </ul>
     *
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register /ws endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // TODO: Configure CORS in production
                .withSockJS(); // Enable SockJS for browser compatibility

        log.info("✅ WebSocket endpoint registered: /ws (SockJS enabled)");
    }
}
