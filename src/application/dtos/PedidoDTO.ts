import { CanalPedido } from '../../domain/enums/CanalPedido.js';

export interface ItemPedidoInputDTO {
  produtoId: number;
  quantidade: number;
}

export interface CriarPedidoInputDTO {
  usuarioId: number;
  unidadeId: number;
  canalPedido: CanalPedido;
  itens: ItemPedidoInputDTO[];
}
