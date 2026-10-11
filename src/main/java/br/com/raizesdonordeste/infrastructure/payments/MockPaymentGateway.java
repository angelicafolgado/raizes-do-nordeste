package br.com.raizesdonordeste.infrastructure.payments;

import br.com.raizesdonordeste.application.payments.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {
    @Override
    public Response process(Request request) {
        return switch (request.scenario()) {
            case "APROVADO" -> new Response("APROVADO", true, UUID.randomUUID().toString(),
                    request.correlationId(), "Pagamento simulado aprovado.");
            case "RECUSADO" -> new Response("RECUSADO", false, UUID.randomUUID().toString(),
                    request.correlationId(), "Pagamento simulado recusado.");
            case "INDISPONIVEL", "TIMEOUT" -> new Response(request.scenario(), false, null,
                    request.correlationId(), request.scenario());
            default -> throw new IllegalArgumentException("Cenário de pagamento não suportado.");
        };
    }
}
