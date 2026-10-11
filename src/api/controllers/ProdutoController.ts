import { Request, Response, NextFunction } from 'express';
import { services } from '../../composition.js';
import { queryNumber } from '../validation.js';
export class ProdutoController {
  static async listar(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.produtos.executar(req.query.unidadeId === undefined ? undefined : queryNumber(req.query.unidadeId), queryNumber(req.query.page ?? '1'), queryNumber(req.query.limit ?? '10'))); }
    catch (err) { next(err); }
  }
  static async unidades(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.unidades.executar(queryNumber(req.query.page ?? '1'), queryNumber(req.query.limit ?? '10'))); } catch (err) { next(err); }
  }
}
