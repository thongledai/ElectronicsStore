package com.example.store.common.interceptor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.store.common.annotation.AuditAction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "AUDIT_START_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        String actionName = "UNKNOWN_ACTION";
        if (handler instanceof HandlerMethod handlerMethod) {
            AuditAction auditAction = handlerMethod.getMethodAnnotation(AuditAction.class);
            if (auditAction == null) {
                auditAction = handlerMethod.getBeanType().getAnnotation(AuditAction.class);
            }
            if (auditAction != null && !auditAction.value().trim().isEmpty()) {
                actionName = auditAction.value();
            } else {
                actionName = handlerMethod.getMethod().getName();
            }
        }

        String username = "ANONYMOUS";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            username = auth.getName();
        }

        int status = response.getStatus();
        log.info("[AUDIT LOG] User: {} | Action: {} | Method: {} | URI: {} | Status: {} | Time: {}ms",
                username, actionName, request.getMethod(), request.getRequestURI(), status, duration);
    }
}
