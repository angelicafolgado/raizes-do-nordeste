import { Request, Response, NextFunction } from 'express';
import { services } from '../../composition.js';
import { AppError } from '../../domain/errors/AppError.js';
import { queryNumber } from '../validation.js';
export class PedidoController {
  static async criar(req: Request, res: Response, next: NextFunction) {
    try {
      const { unidadeId, canalPedido, itens } = req.body ?? {};
      res.status(201).json(await services.criarPedido.executar({ usuarioId: req.usuario!.id, unidadeId, canalPedido, itens }));
    } catch (err) { next(err); }
  }
  static async listar(req: Request, res: Response, next: NextFunction) {
    try {
      const { canalPedido } = req.query;
      if (canalPedido !== undefined && typeof canalPedido !== 'string') throw new AppError('CANAL_INVALIDO', 'Canal inválido.', 400);
      res.json(await services.pedidos.executar(req.usuario!, queryNumber(req.query.page ?? '1'), queryNumber(req.query.limit ?? '10'), canalPedido));
    } catch (err) { next(err); }
  }
  static async simularPagamento(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.pagar.executar(queryNumber(req.params.id), req.body?.statusSimulado, req.usuario!)); }
    catch (err) { next(err); }
  }
  static async atualizarStatus(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.atualizarStatus.executar(queryNumber(req.params.id), req.body?.status, req.usuario!.id)); }
    catch (err) { next(err); }
  }
}
