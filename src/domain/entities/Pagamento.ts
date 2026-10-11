export interface Pagamento {
  id: number; pedidoId: number; valor: number; status: string; metodo: string; transacaoId: string; criadoEm: Date;
}
