package br.com.raizesdonordeste.api;

import br.com.raizesdonordeste.domain.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> api(ApiException exception, HttpServletRequest request) {
        return response(exception.code(), exception.getMessage(), exception.status(), exception.details(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> json(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return response("JSON_INVALIDO", "Corpo JSON inválido.", 400, List.of(), request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public org.springframework.http.ResponseEntity<Map<String, Object>> parameter(Exception exception, HttpServletRequest request) {
        return response("PARAMETRO_INVALIDO", "Parâmetro inválido.", 422, List.of(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> status(ResponseStatusException exception, HttpServletRequest request) {
        int status = exception.getStatusCode().value();
        return response(status == 404 ? "ROTA_NAO_ENCONTRADA" : "ERRO_HTTP",
                status == 404 ? "Rota não encontrada." : "Requisição inválida.",
                status, List.of(), request);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public org.springframework.http.ResponseEntity<Map<String, Object>> notFound(Exception exception, HttpServletRequest request) {
        return response("ROTA_NAO_ENCONTRADA", "Rota não encontrada.", 404, List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<Map<String, Object>> unexpected(Exception exception, HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        LOGGER.error("Erro interno {} ({})", requestId, exception.getClass().getName());
        return response("ERRO_INTERNO_SERVIDOR", "Falha inesperada no processamento.", 500, List.of(), request, requestId);
    }

    private static org.springframework.http.ResponseEntity<Map<String, Object>> response(
            String code, String message, int status, List<?> details, HttpServletRequest request) {
        return response(code, message, status, details, request, UUID.randomUUID().toString());
    }

    private static org.springframework.http.ResponseEntity<Map<String, Object>> response(
            String code, String message, int status, List<?> details, HttpServletRequest request, String requestId) {
        Map<String, Object> body = Map.of("error", code, "message", message, "details", details,
                "timestamp", java.time.Instant.now().toString(), "path", request.getRequestURI(), "requestId", requestId);
        return org.springframework.http.ResponseEntity.status(status).header("Cache-Control", "no-store").body(body);
    }
}
