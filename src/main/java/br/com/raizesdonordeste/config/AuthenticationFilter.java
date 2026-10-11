package br.com.raizesdonordeste.config;

import br.com.raizesdonordeste.domain.ApiException;
import br.com.raizesdonordeste.domain.Identity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final ObjectMapper mapper;

    public AuthenticationFilter(JwtService jwt, ObjectMapper mapper) {
        this.jwt = jwt;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        String auth = request.getHeader("Authorization");
        if (auth != null) {
            if (!auth.startsWith("Bearer ") || auth.length() <= 7) {
                writeError(response, request, new ApiException("NAO_AUTENTICADO", "Token inválido ou expirado.", 401));
                return;
            }
            try {
                Identity identity = jwt.verify(auth.substring(7));
                request.setAttribute("identity", identity);
            } catch (ApiException exception) {
                writeError(response, request, exception);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, ApiException failure)
            throws IOException {
        response.setStatus(failure.status());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), Map.of(
                "error", failure.code(),
                "message", failure.getMessage(),
                "details", List.of(),
                "timestamp", Instant.now().toString(),
                "path", request.getRequestURI(),
                "requestId", UUID.randomUUID().toString()));
    }
}
