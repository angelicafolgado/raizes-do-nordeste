import { Request, Response, NextFunction } from 'express';
import { randomUUID } from 'node:crypto';
import { Prisma } from '@prisma/client';
import { AppError } from '../../domain/errors/AppError.js';
export function errorHandlerMiddleware(err: unknown, req: Request, res: Response, _next: NextFunction) {
  const requestId = randomUUID();
  let failure = err instanceof AppError ? err : undefined;
  if (err instanceof SyntaxError && 'body' in err) failure = new AppError('JSON_INVALIDO', 'Corpo JSON inválido.', 400);
  if (err && typeof err === 'object' && 'type' in err && err.type === 'entity.too.large')
    failure = new AppError('CORPO_MUITO_GRANDE', 'O corpo excede o limite de 100 KB.', 413);
  if (err instanceof Prisma.PrismaClientKnownRequestError && ['P2002', 'P2034', 'P2028'].includes(err.code))
    failure = new AppError('CONFLITO_OPERACAO', 'Conflito ao processar a operação. Consulte o pedido antes de tentar novamente.', 409);
  if (!failure) {
    // Não serializar exceções/queries: podem carregar dados de entrada ou credenciais.
    console.error('Erro interno:', { requestId, codigo: 'ERRO_INTERNO_SERVIDOR' });
    failure = new AppError('ERRO_INTERNO_SERVIDOR', 'Falha inesperada no processamento.', 500);
  }
  res.status(failure.statusCode).json({
    error: failure.errorCode, message: failure.message, details: failure.details,
    timestamp: new Date().toISOString(), path: req.path, requestId
  });
}
