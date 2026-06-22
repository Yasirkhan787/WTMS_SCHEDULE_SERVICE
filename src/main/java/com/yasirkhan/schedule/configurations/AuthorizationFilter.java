package com.yasirkhan.schedule.configurations;

import com.yasirkhan.schedule.exceptions.UnauthorizedException;
import com.yasirkhan.schedule.models.UserPrincipal;
import com.yasirkhan.schedule.services.implementation.DownstreamJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

@Component
public class AuthorizationFilter extends OncePerRequestFilter {

    // Used for normal user traffic hitting via API Gateway
    @Value("${app.security.internal-secret}")
    private String GATEWAY_SECRET = "";

    // Used strictly for server-to-server signature validation
    @Value("${app.security.internal-secret:my-super-secret-service-key}")
    private String INTERNAL_SECRET = "";

    private final DownstreamJwtService jwtService;
    private final HandlerExceptionResolver exceptionResolver;

    public AuthorizationFilter(DownstreamJwtService jwtService,
                               @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtService = jwtService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // =================================================================================
            // 1. INTERNAL MICROSERVICE COMMUNICATION (HMAC SIGNATURE VERIFICATION)
            // =================================================================================
            String serviceName = request.getHeader("X-Service-Name");
            String timestampStr = request.getHeader("X-Timestamp");
            String incomingSignature = request.getHeader("X-Signature");

            // If these headers exist, the request claims to be an internal microservice
            if (serviceName != null && timestampStr != null && incomingSignature != null) {
                long timestamp = Long.parseLong(timestampStr);
                long currentTime = System.currentTimeMillis();

                // REPLAY PROTECTION: Reject the request if it is older than 60 seconds
                if (currentTime - timestamp > 60000) {
                    throw new UnauthorizedException("Internal Request Expired (Replay Attack Prevented)");
                }

                // RECREATE THE SIGNATURE using the hidden server secret
                String rawData = serviceName + timestamp + INTERNAL_SECRET;
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));
                String expectedSignature = Base64.getEncoder().encodeToString(hash);

                // VERIFY THE SIGNATURE
                if (expectedSignature.equals(incomingSignature)) {
                    // Grant the internal service access without a JWT
                    UserPrincipal systemPrincipal = new UserPrincipal("system-uuid", "internal_service", "SYSTEM_SERVICE");
                    UsernamePasswordAuthenticationToken systemAuth = new UsernamePasswordAuthenticationToken(
                            systemPrincipal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SYSTEM_SERVICE"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(systemAuth);

                    // Let the request proceed directly to the controller!
                    filterChain.doFilter(request, response);
                    return;
                } else {
                    throw new UnauthorizedException("Invalid Internal Service Signature");
                }
            }


            // =================================================================================
            // 2. NORMAL EXTERNAL USER TRAFFIC (GATEWAY + JWT VERIFICATION)
            // =================================================================================
            String incomingGatewaySecret = request.getHeader("X-Gateway-Secret");

            if (incomingGatewaySecret == null || !incomingGatewaySecret.equals(GATEWAY_SECRET)) {
                throw new UnauthorizedException("Direct access blocked: Request must come through API Gateway");
            }

            // Extract Token
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Missing Authorization Token");
            }

            String token = authHeader.substring(7);

            // Mathematical Validation (Extract claims using Public Key)
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            String userId = jwtService.extractUserId(token);

            // Note: Put your Redis Revocation Check back here if you re-enable it

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            // Authenticate in Spring Context
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String authorityRole = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorityRole));

                UserPrincipal customPrincipal = new UserPrincipal(userId, username, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(customPrincipal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Bridge any JWT or Auth errors to your GlobalExceptionHandler!
            exceptionResolver.resolveException(request, response, null, e);
        }
    }
}