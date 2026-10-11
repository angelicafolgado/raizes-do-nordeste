package br.com.raizesdonordeste.application.payments;

public interface PaymentGateway {
    Response process(Request request);

    record Request(long orderId, long amountCents, String currency, String scenario, String correlationId) {}

    record Response(String status, boolean approved, String transactionId, String correlationId, String message) {}
}
