import { PaymentGateway, SolicitacaoPagamento, ResultadoGateway } from '../../application/ports/PaymentGateway.js';

/** Simulador local determinístico: TIMEOUT representa o resultado, sem espera ou rede. */
export class MockPaymentGateway implements PaymentGateway {
  async solicitar(input: SolicitacaoPagamento): Promise<ResultadoGateway> {
    const identidade = { pedidoId: input.pedidoId, correlacaoId: input.correlacaoId };
    if (input.cenario === 'TIMEOUT' || input.cenario === 'INDISPONIVEL')
      return { ...identidade, status: input.cenario };
    return { ...identidade, status: input.cenario, transacaoId: 'MOCK-TX-' + input.correlacaoId };
  }
}
