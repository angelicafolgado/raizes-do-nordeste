import { Store } from '../ports/Store.js';
import { Identidade } from '../../domain/entities/Usuario.js';
import { CanalPedido } from '../../domain/enums/CanalPedido.js';
import { AppError } from '../../domain/errors/AppError.js';
import { pagina, validarPaginacao } from '../pagination.js';
export class ListarPedidosUseCase {
  constructor(private store: Store) {}
  async executar(usuario: Identidade, page: number, limit: number, canalPedido?: string) {
    validarPaginacao({ page, limit });
    if (canalPedido !== undefined && !Object.values(CanalPedido).includes(canalPedido as CanalPedido))
      throw new AppError('CANAL_INVALIDO', 'Filtro de canal inválido.', 400);
    const { dados, total } = await this.store.transaction(async tx => {
      const result = await tx.listarPedidos({
        usuarioId: usuario.role === 'CLIENTE' ? usuario.id : undefined, canalPedido, page, limit
      });
      await tx.auditar('CONSULTA_PEDIDOS', { pagina: page, limite: limit, canalPedido,
        quantidadeRetornada: result.dados.length, escopo: usuario.role === 'CLIENTE' ? 'PROPRIOS' : 'REDE' }, usuario.id);
      return result;
    });
    return pagina(dados, total, { page, limit });
  }
}
