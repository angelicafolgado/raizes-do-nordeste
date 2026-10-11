import { AppError } from '../domain/errors/AppError.js';
export function inteiroPositivo(value: unknown, field: string): asserts value is number {
  if (typeof value !== 'number' || !Number.isSafeInteger(value) || value <= 0 || value > 2147483647)
    throw new AppError('PARAMETRO_INVALIDO', field + ' deve ser um inteiro positivo.', 422, [{ field, issue: 'Inteiro positivo obrigatório' }]);
}
