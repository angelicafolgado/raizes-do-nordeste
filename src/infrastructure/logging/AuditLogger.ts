import { Prisma } from '@prisma/client';
export class AuditLogger {
  static async registar(db: Prisma.TransactionClient, acao: string, detalhes: Record<string, unknown>, usuarioId: number) {
    await db.auditLog.create({ data: { acao, detalhes: JSON.stringify(detalhes), usuarioId } });
  }
}
