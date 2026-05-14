package com.cleanifyai.api.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cleanifyai.api.domain.entity.AuditLog;
import com.cleanifyai.api.domain.entity.User;
import com.cleanifyai.api.repository.AuditLogRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogFilter.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogFilter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (deveAuditar(request)) {
                registrar(request, response, System.currentTimeMillis() - start);
            }
        }
    }

    private boolean deveAuditar(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            return false;
        }
        if (path.equals("/api/auth/login") || path.equals("/api/ping")) {
            return false;
        }

        return method.equals("POST") || method.equals("PUT") || method.equals("PATCH") || method.equals("DELETE");
    }

    private void registrar(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        try {
            AuditLog log = new AuditLog();
            log.setEmpresaId(resolverEmpresaId());

            User user = resolverUsuario();
            if (user != null) {
                log.setUserId(user.getId());
                log.setUserEmail(user.getEmail());
                if (log.getEmpresaId() == null) {
                    log.setEmpresaId(user.getEmpresaId());
                }
            }

            String path = limitar(request.getRequestURI(), 500);
            log.setAction(actionFromMethod(request.getMethod()));
            log.setResourceType(resourceTypeFromPath(path));
            log.setResourceId(resourceIdFromPath(path));
            log.setHttpMethod(request.getMethod().toUpperCase(Locale.ROOT));
            log.setPath(path);
            log.setStatusCode(response.getStatus());
            log.setIpAddress(limitar(clientIp(request), 80));
            log.setUserAgent(limitar(request.getHeader("User-Agent"), 500));
            log.setDurationMs(durationMs);
            log.setOccurredAt(Instant.now());

            auditLogRepository.save(log);
        } catch (RuntimeException ex) {
            LOGGER.warn("Falha ao registrar audit_log para {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
    }

    private Long resolverEmpresaId() {
        return TenantContext.getEmpresaId();
    }

    private User resolverUsuario() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            return null;
        }
        return authenticatedUser.getUser();
    }

    private String actionFromMethod(String method) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "PATCH" -> "PATCH";
            case "DELETE" -> "DELETE";
            default -> method.toUpperCase(Locale.ROOT);
        };
    }

    private String resourceTypeFromPath(String path) {
        String[] parts = path.split("/");
        if (parts.length <= 2) {
            return "api";
        }
        if (parts.length > 3 && "financeiro".equals(parts[2])) {
            return parts[2] + "/" + parts[3];
        }
        if (parts.length > 3 && "auth".equals(parts[2])) {
            return parts[2] + "/" + parts[3];
        }
        return parts[2];
    }

    private String resourceIdFromPath(String path) {
        String[] parts = path.split("/");
        for (int i = 3; i < parts.length; i++) {
            if (parts[i].matches("\\d+")) {
                return parts[i];
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String limitar(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
