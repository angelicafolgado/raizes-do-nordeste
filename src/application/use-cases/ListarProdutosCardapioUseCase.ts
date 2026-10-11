import { Store } from '../ports/Store.js';
import { inteiroPositivo } from '../validation.js';
import { AppError } from '../../domain/errors/AppError.js';
import { pagina, validarPaginacao } from '../pagination.js';
export class ListarProdutosCardapioUseCase {
  constructor(private store: Store) {}
  async executar(unidadeId?: number, page = 1, limit = 10) {
    validarPaginacao({ page, limit });
    if (unidadeId !== undefined) {
      inteiroPositivo(unidadeId, 'unidadeId');
      if (!await this.store.unidade(unidadeId)) throw new AppError('UNIDADE_NAO_ENCONTRADA', 'Unidade não encontrada.', 404);
    }
    const result = await this.store.produtos({ unidadeId, page, limit });
    return pagina(result.dados, result.total, { page, limit });
  }
}
