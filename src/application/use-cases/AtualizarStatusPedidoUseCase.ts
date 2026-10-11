import { Store } from '../ports/Store.js';
import { AppError } from '../../domain/errors/AppError.js';
import { StatusPedido } from '../../domain/enums/StatusPedido.js';
import { inteiroPositivo } from '../validation.js';
const transicoes: Record<string, string[]> = {
  AGUARDANDO_PAGAMENTO: ['CANCELADO'], EM_PREPARO: ['PRONTO'], PRONTO: ['ENTREGUE'], ENTREGUE: [], CANCELADO: []
};
export class AtualizarStatusPedidoUseCase {
  constructor(private store: Store) {}
  async executar(pedidoId: number, status: StatusPedido, usuarioId: number) {
    inteiroPositivo(pedidoId, 'pedidoId');
    if (!Object.values(StatusPedido).includes(status)) throw new AppError('STATUS_INVALIDO', 'Status inválido.', 400);
    return this.store.transaction(async tx => {
      const pedido = await tx.pedido(pedidoId);
      if (!pedido) throw new AppError('PEDIDO_NAO_ENCONTRADO', 'Pedido não encontrado.', 404);
      if (!transicoes[pedido.status]?.includes(status))
        throw new AppError('TRANSICAO_INVALIDA', 'Transição de status não permitida.', 409);
      if (!await tx.mudarStatus(pedidoId, pedido.status, status))
        throw new AppError('CONFLITO_PEDIDO', 'Pedido alterado por outra operação.', 409);
      if (status === 'CANCELADO') await tx.devolver(pedido.unidadeId, pedido.itens);
      await tx.auditar('ATUALIZACAO_STATUS', { pedidoId, anterior: pedido.status, novoStatus: status }, usuarioId);
      return (await tx.pedido(pedidoId))!;
    });
  }
}
