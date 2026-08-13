package com.aegis.product_service.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * <p>Entry point for handling authentication failures.</p>
 * <p>This class is responsible for handling authentication failures by returning a JSON
 * response with appropriate error details.</p>
 * <p>UnAuthorized  Exception</p>
 */
@Component
@RequiredArgsConstructor

public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         @NonNull AuthenticationException authException)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, Object> body = Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", "Authentication is required to access this resource",
                "path", request.getRequestURI(),
                "timestamp", Instant.now()
                        .toString()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
