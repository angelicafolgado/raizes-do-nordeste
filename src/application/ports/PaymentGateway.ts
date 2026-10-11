export const cenariosPagamento = ['APROVADO', 'RECUSADO', 'INDISPONIVEL', 'TIMEOUT'] as const;
export type CenarioPagamento = typeof cenariosPagamento[number];
export interface SolicitacaoPagamento {
  pedidoId: number;
  valorCentavos: number;
  moeda: 'BRL';
  correlacaoId: string;
  cenario: CenarioPagamento;
}
export type ResultadoGateway =
  | { status: 'APROVADO' | 'RECUSADO'; pedidoId: number; correlacaoId: string; transacaoId: string }
  | { status: 'INDISPONIVEL' | 'TIMEOUT'; pedidoId: number; correlacaoId: string };
export interface PaymentGateway {
  solicitar(solicitacao: SolicitacaoPagamento): Promise<ResultadoGateway>;
}
