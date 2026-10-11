package br.com.raizesdonordeste.domain;

import java.util.List;

public class ApiException extends RuntimeException {
    private final String code;
    private final int status;
    private final List<Object> details;

    public ApiException(String code, String message, int status) {
        this(code, message, status, List.of());
    }

    public ApiException(String code, String message, int status, List<?> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = List.copyOf(details);
    }

    public String code() { return code; }
    public int status() { return status; }
    public List<Object> details() { return details; }
}
