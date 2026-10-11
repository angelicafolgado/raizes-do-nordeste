export interface ItemPedido { produtoId: number; quantidade: number; precoUnitario: number }
export interface Pedido {
  id: number; usuarioId: number; unidadeId: number; canalPedido: string; status: string; total: number;
  criadoEm: Date; atualizadoEm: Date; itens: ItemPedido[];
}
