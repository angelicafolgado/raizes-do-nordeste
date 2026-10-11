import { Store } from '../ports/Store.js';
import { pagina, validarPaginacao } from '../pagination.js';
export class ListarUnidadesUseCase {
  constructor(private store: Store) {}
  async executar(page = 1, limit = 10) {
    validarPaginacao({ page, limit });
    const result = await this.store.unidades({ page, limit });
    return pagina(result.dados, result.total, { page, limit });
  }
}
