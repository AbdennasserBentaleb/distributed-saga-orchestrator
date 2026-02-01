package com.saga.orchestrator.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ContextCleanupInterceptor());
    }

    public static class ContextCleanupInterceptor implements HandlerInterceptor {

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            // Guaranteed to run after the request completes, even if an exception was thrown.
            // This prevents ThreadLocal leakage (like MDC context) into the thread pool.
            log.debug("Cleaning up MDC context for thread: {}", Thread.currentThread().getName());
            MDC.clear();
        }
    }
}
