import { inteiroPositivo } from './validation.js';
import { AppError } from '../domain/errors/AppError.js';
export interface Paginacao { page: number; limit: number }
export function validarPaginacao({ page, limit }: Paginacao) {
  inteiroPositivo(page, 'page'); inteiroPositivo(limit, 'limit');
  if (limit > 100 || (page - 1) * limit > 2147483647)
    throw new AppError('PAGINACAO_INVALIDA', 'Paginação fora do limite (máximo 100 itens).', 422);
}
export function pagina<T>(dados: T[], total: number, { page, limit }: Paginacao) {
  return { dados, paginacao: { pagina: page, limite: limit, total, totalPaginas: Math.ceil(total / limit) } };
}
