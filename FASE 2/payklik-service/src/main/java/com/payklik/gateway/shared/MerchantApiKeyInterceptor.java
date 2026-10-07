package com.payklik.gateway.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

/**
 * La API de comercios ({@code /api/v1/**}) exige la llave del comercio en
 * {@code X-Api-Key}. El portal de operación ({@code /api/portal/**}) es la
 * red interna de PayKlik.
 */
@Configuration
public class MerchantApiKeyInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    static final String HEADER = "X-Api-Key";

    private final PayKlikProperties properties;

    public MerchantApiKeyInterceptor(PayKlikProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/api/v1/**");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),
                properties.apiKey().getBytes(StandardCharsets.UTF_8))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"INVALID_API_KEY\",\"message\":\"Llave de comercio inválida o ausente.\",\"timestamp\":\""
                + Instant.now() + "\",\"path\":\"" + request.getRequestURI() + "\"}");
        return false;
    }
}
