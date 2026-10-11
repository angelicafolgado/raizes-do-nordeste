import { Store } from '../ports/Store.js';
import { Identidade } from '../../domain/entities/Usuario.js';
import { AppError } from '../../domain/errors/AppError.js';
import { inteiroPositivo } from '../validation.js';
import { PaymentGateway, CenarioPagamento, cenariosPagamento } from '../ports/PaymentGateway.js';
export class ProcessarPagamentoMockUseCase {
  constructor(private store: Store, private gateway: PaymentGateway, private gerarId: () => string) {}
  async executar(pedidoId: number, status: unknown, usuario: Identidade) {
    inteiroPositivo(pedidoId, 'pedidoId');
    if (!cenariosPagamento.includes(status as CenarioPagamento))
      throw new AppError('PARAMETRO_INVALIDO', 'statusSimulado deve ser APROVADO, RECUSADO, INDISPONIVEL ou TIMEOUT.', 400);
    const resultado = await this.store.transaction(async tx => {
      const pedido = await tx.pedido(pedidoId);
      if (!pedido) throw new AppError('PEDIDO_NAO_ENCONTRADO', 'Pedido não encontrado.', 404);
      if (usuario.role === 'CLIENTE' && pedido.usuarioId !== usuario.id)
        throw new AppError('ACESSO_NEGADO', 'Este pedido pertence a outro cliente.', 403);
      if (pedido.status !== 'AGUARDANDO_PAGAMENTO')
        throw new AppError('ESTADO_INVALIDO', 'Pedido não está aguardando pagamento.', 409);
      const correlacaoId = this.gerarId();
      const resposta = await this.gateway.solicitar({ pedidoId, valorCentavos: Math.round(pedido.total * 100), moeda: 'BRL', correlacaoId, cenario: status as CenarioPagamento });
      if (resposta.pedidoId !== pedidoId || resposta.correlacaoId !== correlacaoId)
        throw new AppError('RESPOSTA_GATEWAY_INVALIDA', 'Resposta do gateway não corresponde à solicitação.', 502);
      if (!('transacaoId' in resposta)) {
        await tx.auditar('FALHA_PAGAMENTO_MOCK', { pedidoId, correlacaoId, motivo: resposta.status }, usuario.id);
        return { falha: resposta.status, correlacaoId };
      }
      const aprovado = resposta.status === 'APROVADO';
      const novoStatus = aprovado ? 'EM_PREPARO' : 'AGUARDANDO_PAGAMENTO';
      if (!await tx.mudarStatus(pedidoId, pedido.status, novoStatus))
        throw new AppError('CONFLITO_PEDIDO', 'Pedido alterado por outra operação.', 409);
      const transacaoId = resposta.transacaoId;
      const detalhes = await tx.pagamento({ pedidoId, valor: pedido.total, status: resposta.status, transacaoId });
      await tx.auditar('PAGAMENTO_MOCK', { pedidoId, statusPagamento: resposta.status, transacaoId, correlacaoId }, usuario.id);
      return { sucesso: aprovado, mensagem: aprovado ? 'Pagamento simulado aprovado.' : 'Pagamento simulado recusado.',
        transacaoId, statusPedido: novoStatus, detalhes };
    });
    // A falha técnica é lançada após o commit para preservar sua auditoria.
    if (resultado.correlacaoId !== undefined) {
      const timeout = resultado.falha === 'TIMEOUT';
      throw new AppError(timeout ? 'GATEWAY_TIMEOUT' : 'GATEWAY_INDISPONIVEL',
        timeout ? 'Tempo de resposta do gateway simulado esgotado.' : 'Gateway simulado indisponível.', timeout ? 504 : 503,
        [{ field: 'correlacaoId', issue: resultado.correlacaoId }]);
    }
    return resultado;
  }
}
