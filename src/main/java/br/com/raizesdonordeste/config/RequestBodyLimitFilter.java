package br.com.raizesdonordeste.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestBodyLimitFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES = 100 * 1024;
    private final ObjectMapper mapper;

    public RequestBodyLimitFilter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") ||
                !isJson(request) || request.getMethod().equals("GET") ||
                request.getMethod().equals("HEAD");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, jakarta.servlet.FilterChain chain)
            throws jakarta.servlet.ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            tooLarge(request, response);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            tooLarge(request, response);
            return;
        }
        chain.doFilter(new BodyRequest(request, body), response);
    }

    private void tooLarge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), Map.of(
                "error", "CORPO_MUITO_GRANDE",
                "message", "O corpo excede o limite de 100 KB.",
                "details", List.of(),
                "timestamp", Instant.now().toString(),
                "path", request.getRequestURI(),
                "requestId", UUID.randomUUID().toString()));
    }

    private static boolean isJson(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && (contentType.equalsIgnoreCase("application/json") ||
                contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json;"));
    }

    private final class BodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        private BodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override public int getContentLength() { return body.length; }
        @Override public long getContentLengthLong() { return body.length; }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    try {
                        if (isFinished()) listener.onAllDataRead();
                        else listener.onDataAvailable();
                    } catch (IOException exception) {
                        listener.onError(exception);
                    }
                }
            };
        }

        @Override public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
