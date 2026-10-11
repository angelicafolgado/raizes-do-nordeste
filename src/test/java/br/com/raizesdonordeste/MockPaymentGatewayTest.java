package br.com.raizesdonordeste;

import br.com.raizesdonordeste.application.payments.PaymentGateway;
import br.com.raizesdonordeste.infrastructure.payments.MockPaymentGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentGatewayTest {
    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void forwardsCorrelationAndReturnsAnApprovedTransaction() {
        PaymentGateway.Request request = new PaymentGateway.Request(12, 3_850, "BRL", "APROVADO", "corr-1");
        PaymentGateway.Response response = gateway.process(request);

        assertThat(response.status()).isEqualTo("APROVADO");
        assertThat(response.approved()).isTrue();
        assertThat(response.transactionId()).isNotBlank();
        assertThat(response.correlationId()).isEqualTo(request.correlationId());
    }

    @Test
    void rejectedAndTechnicalScenariosHaveDistinctTransactionSemantics() {
        PaymentGateway.Response rejected = gateway.process(
                new PaymentGateway.Request(12, 3_850, "BRL", "RECUSADO", "corr-2"));
        PaymentGateway.Response unavailable = gateway.process(
                new PaymentGateway.Request(12, 3_850, "BRL", "INDISPONIVEL", "corr-3"));

        assertThat(rejected.approved()).isFalse();
        assertThat(rejected.transactionId()).isNotBlank();
        assertThat(unavailable.status()).isEqualTo("INDISPONIVEL");
        assertThat(unavailable.transactionId()).isNull();
    }
}
