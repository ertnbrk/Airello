# Security Architecture Decisions

## Overview

This document explains the security architecture for Spring Boot 3 with JWT-based authentication for both REST APIs and WebSocket (STOMP) connections.

## Critical Decisions

### 1. Why Authorities Must Not Be Empty

**Problem:**
```
User authenticated: { } with authorities { }
```

Empty authorities in Spring Security cause several issues:

- **Authorization Failures**: Spring Security's `@PreAuthorize`, `@Secured`, and access control expressions rely on granted authorities
- **Inconsistent Behavior**: Anonymous users and authenticated users become indistinguishable
- **Security Risks**: Role-based access control (RBAC) fails silently

**Solution:**

Every authenticated user receives at minimum:
- `ROLE_USER` - Universal role for all authenticated users
- `ROLE_{userType}` - Specific role based on user type (e.g., `ROLE_REGISTERED`, `ROLE_ANONYMOUS`)

**Implementation:** `JwtAuthenticationFilter:62-66`

```java
List<SimpleGrantedAuthority> authorities =
        List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
```

**Benefits:**
- Enables `@PreAuthorize("hasRole('USER')")` checks
- Supports fine-grained permissions via user type
- Maintains Spring Security contract

---

### 2. Why WebSocket Handshake Needs a Dedicated Interceptor

**Problem:**
```
/ws/info constantly returns 403 Forbidden
WebSocketSession[0 current WS(0)]
```

**Root Cause:**

HTTP filters (like `JwtAuthenticationFilter`) execute **before** the WebSocket handshake completes. However:

1. WebSocket handshake uses HTTP upgrade protocol
2. SockJS uses multiple fallback transports (WebSocket, XHR streaming, polling)
3. The `/ws/info` endpoint is called BEFORE authentication headers can be sent
4. Spring Security blocks unauthenticated handshakes by default

**Why HTTP Filters Are Insufficient:**

- WebSocket connections persist after the HTTP upgrade
- Authentication must be validated during handshake, not per-message
- User identity must be stored in WebSocket session attributes for message routing

**Solution: `JwtHandshakeInterceptor`**

A `HandshakeInterceptor` executes during the WebSocket upgrade process and:

1. Extracts JWT from query parameter (`?token=JWT`)
2. Validates token before connection establishment
3. Stores `userId` and `userEmail` in WebSocket session attributes
4. Rejects handshake if authentication fails (returns `false`)

**Implementation:** `JwtHandshakeInterceptor:31-75`

**Benefits:**
- Prevents unauthorized WebSocket connections
- Enables user-specific message routing (`/user/{userId}/queue/...`)
- Supports message authentication via session attributes
- Works with SockJS fallback transports

---

### 3. Why Query Parameter Token is Supported (WebSocket Only)

**Security Principle:**

Standard practice is to send JWTs via the `Authorization: Bearer` header to prevent:
- Token exposure in server logs
- Token leakage via browser history
- Referrer header leaks

**WebSocket Exception:**

WebSocket and SockJS have technical limitations:

1. **Native WebSocket API**: Does not support custom headers during initial handshake
   ```javascript
   // ❌ Not possible in browser WebSocket API
   const ws = new WebSocket('wss://example.com/ws', {
       headers: { 'Authorization': 'Bearer TOKEN' }
   });
   ```

2. **SockJS Limitations**: Fallback transports (especially JSONP) cannot send headers

**Solution: Conditional Query Parameter Support**

```java
// JwtAuthenticationFilter:96-102
String path = request.getRequestURI();
if (path.startsWith("/ws")) {
    String tokenParam = request.getParameter("token");
    if (tokenParam != null && !tokenParam.isEmpty()) {
        return tokenParam;
    }
}
```

**Security Measures:**

- Query parameter auth is **ONLY** allowed for `/ws/**` endpoints
- REST API endpoints (`/v1/**`) reject query parameter tokens
- HTTPS is mandatory in production (prevents MITM)
- Tokens should be short-lived (15 minutes recommended)

**Recommended Client Usage:**

```javascript
const token = localStorage.getItem('accessToken');
const socket = new SockJS(`https://api.example.com/ws?token=${token}`);
const stompClient = Stomp.over(socket);
```

---

### 4. Security Filter Chain Configuration

**Stateless Session Management:**

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

- No HTTP sessions created
- No JSESSIONID cookies
- Reduces memory footprint
- Enables horizontal scaling

**CSRF Disabled:**

```java
.csrf(AbstractHttpConfigurer::disable)
```

- CSRF protection is not needed for stateless JWT authentication
- Tokens are not stored in cookies (no automatic browser transmission)
- CORS handles cross-origin protection

**Permitted Endpoints:**

```java
auth.requestMatchers("/auth/**").permitAll();    // Authentication endpoints
auth.requestMatchers("/ws/**").permitAll();      // WebSocket handshake
auth.requestMatchers("/actuator/**").permitAll(); // Health checks
```

- `/auth/**`: Login, registration, token refresh
- `/ws/**`: WebSocket handshake (authenticated by `JwtHandshakeInterceptor`)
- `/actuator/**`: Monitoring (restrict in production)

**Protected Endpoints:**

```java
auth.requestMatchers("/v1/**").authenticated();
auth.anyRequest().authenticated();
```

All API endpoints require valid JWT authentication.

---

## Security Tradeoffs

### Accepting Query Parameters for WebSocket

**Tradeoff:**
- ✅ Enables WebSocket authentication with standard clients
- ❌ Tokens may appear in server access logs
- ❌ Potential token exposure via browser history

**Mitigation:**
1. Use short-lived access tokens (15 minutes)
2. Implement token refresh mechanism
3. Configure web servers to exclude `/ws` from access logs
4. Mandate HTTPS in production
5. Educate users to clear browser history after logout

### Permitting All /ws/** Endpoints in SecurityFilterChain

**Tradeoff:**
- ✅ Allows handshake to proceed
- ❌ Spring Security does not block unauthenticated /ws requests

**Mitigation:**
- `JwtHandshakeInterceptor` performs authentication
- Invalid tokens result in handshake rejection (HTTP 401 equivalent)
- STOMP message handlers should verify session attributes

---

## Production Checklist

- [ ] Set `jwt.access-token-expiration-ms=900000` (15 minutes)
- [ ] Enable HTTPS/TLS (mandatory for production)
- [ ] Configure CORS with specific allowed origins (remove `*`)
- [ ] Restrict `/actuator/**` to internal network
- [ ] Configure log masking for `/ws?token=` patterns
- [ ] Implement token refresh flow for long-lived sessions
- [ ] Add rate limiting on `/auth/**` endpoints
- [ ] Monitor failed handshake attempts for brute force attacks

---

## File Reference

- **JwtAuthenticationFilter**: `ai.planmate.auth.filter.JwtAuthenticationFilter`
- **JwtHandshakeInterceptor**: `ai.planmate.realtime.JwtHandshakeInterceptor`
- **SecurityConfig**: `ai.planmate.config.SecurityConfig`
- **WebSocketConfig**: `ai.planmate.config.WebSocketConfig`
