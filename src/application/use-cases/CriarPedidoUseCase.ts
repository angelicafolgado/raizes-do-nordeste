import { Store } from '../ports/Store.js';
import { CanalPedido } from '../../domain/enums/CanalPedido.js';
import { AppError } from '../../domain/errors/AppError.js';
import { CriarPedidoInputDTO } from '../dtos/PedidoDTO.js';
import { inteiroPositivo } from '../validation.js';
import { ItemPedido } from '../../domain/entities/Pedido.js';
export class CriarPedidoUseCase {
  constructor(private store: Store) {}
  async executar(dto: CriarPedidoInputDTO) {
    if (!Object.values(CanalPedido).includes(dto.canalPedido))
      throw new AppError('CANAL_INVALIDO', 'Canal inválido. Use APP, TOTEM, BALCAO, PICKUP ou WEB.', 400);
    inteiroPositivo(dto.unidadeId, 'unidadeId');
    if (!Array.isArray(dto.itens) || !dto.itens.length || dto.itens.length > 100)
      throw new AppError('ITENS_INVALIDOS', 'Informe entre 1 e 100 itens.', 422);
    const agrupados = new Map<number, number>();
    for (const item of dto.itens) {
      if (!item || typeof item !== 'object') throw new AppError('ITENS_INVALIDOS', 'Item inválido.', 422);
      inteiroPositivo(item.produtoId, 'produtoId'); inteiroPositivo(item.quantidade, 'quantidade');
      const quantidade = (agrupados.get(item.produtoId) ?? 0) + item.quantidade;
      inteiroPositivo(quantidade, 'quantidade');
      agrupados.set(item.produtoId, quantidade);
    }
    return this.store.transaction(async tx => {
      if (!await tx.unidade(dto.unidadeId)) throw new AppError('UNIDADE_NAO_ENCONTRADA', 'Unidade não encontrada.', 404);
      let centavos = 0;
      const itens: ItemPedido[] = [];
      for (const [produtoId, quantidade] of agrupados) {
        const produto = await tx.produto(produtoId);
        if (!produto) throw new AppError('PRODUTO_NAO_ENCONTRADO', 'Produto não encontrado.', 404);
        if (!await tx.reservar(dto.unidadeId, produtoId, quantidade))
          throw new AppError('ESTOQUE_INSUFICIENTE', 'Estoque insuficiente para ' + produto.nome + '.', 409);
        centavos += Math.round(produto.preco * 100) * quantidade;
        if (!Number.isSafeInteger(centavos)) throw new AppError('TOTAL_INVALIDO', 'Total excede o limite permitido.', 422);
        itens.push({ produtoId, quantidade, precoUnitario: produto.preco });
      }
      const pedido = await tx.criarPedido({ usuarioId: dto.usuarioId, unidadeId: dto.unidadeId,
        canalPedido: dto.canalPedido, total: centavos / 100, itens });
      await tx.auditar('CRIACAO_PEDIDO', { pedidoId: pedido.id, total: pedido.total, canal: dto.canalPedido }, dto.usuarioId);
      return pedido;
    });
  }
}
