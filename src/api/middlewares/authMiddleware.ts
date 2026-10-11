import { Request, Response, NextFunction } from 'express';
import { TokenService } from '../../infrastructure/security/TokenService.js';
import { AppError } from '../../domain/errors/AppError.js';

export interface AuthenticatedUser {
  id: number;
  nome: string;
  role: string;
}

declare global {
  namespace Express {
    interface Request {
      usuario?: AuthenticatedUser;
    }
  }
}

export function authMiddleware(perfisPermitidos: string[] = []) {
  return (req: Request, _res: Response, next: NextFunction) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
      throw new AppError('NAO_AUTENTICADO', 'Cabeçalho de autorização não fornecido.', 401);
    }

    const [scheme, token, extra] = authHeader.split(' ');
    if (scheme !== 'Bearer' || !token || extra !== undefined) {
      throw new AppError('TOKEN_MALFORMATADO', 'Formato de token Bearer inválido.', 401);
    }

    try {
      const decoded = TokenService.verificarToken(token);
      req.usuario = decoded;

      if (perfisPermitidos.length > 0 && !perfisPermitidos.includes(decoded.role)) {
        throw new AppError('ACESSO_NEGADO', 'O seu perfil não tem permissão para aceder a esta funcionalidade.', 403);
      }

      next();
    } catch (error) {
      if (error instanceof AppError) throw error;
      throw new AppError('TOKEN_EXPIRADO_OU_INVALIDO', 'Sessão expirada ou assinatura de token inválida.', 401);
    }
  };
}
