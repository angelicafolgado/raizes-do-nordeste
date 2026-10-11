import { Request, Response, NextFunction } from 'express';
import { services } from '../../composition.js';
import { avisoFidelidade } from '../../application/privacy/aviso.js';
import { queryNumber } from '../validation.js';
export class PrivacidadeController {
  static aviso(_req: Request, res: Response) { res.json(avisoFidelidade); }
  static async consultar(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.consentimentos.consultar(req.usuario!.id, queryNumber(req.query.page ?? '1'), queryNumber(req.query.limit ?? '10'))); }
    catch (err) { next(err); }
  }
  static async registrar(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.consentimentos.registrar(req.usuario!.id, req.body)); }
    catch (err) { next(err); }
  }
  static async auditoria(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.auditoria.executar(req.usuario!, queryNumber(req.query.page ?? '1'), queryNumber(req.query.limit ?? '10'), req.query.acao)); }
    catch (err) { next(err); }
  }
}
