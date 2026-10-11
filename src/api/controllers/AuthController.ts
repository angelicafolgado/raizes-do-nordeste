import { Request, Response, NextFunction } from 'express';
import { services } from '../../composition.js';
export class AuthController {
  static async login(req: Request, res: Response, next: NextFunction) {
    try { res.json(await services.autenticar.executar(req.body?.email, req.body?.senha)); } catch (err) { next(err); }
  }
}
