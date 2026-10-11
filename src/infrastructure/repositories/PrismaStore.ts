import { Prisma, PrismaClient } from '@prisma/client';
import { Store, PedidoFiltro } from '../../application/ports/Store.js';
import { ItemPedido } from '../../domain/entities/Pedido.js';
import { AuditLogger } from '../logging/AuditLogger.js';
import { Paginacao } from '../../application/pagination.js';
export class PrismaStore implements Store {
  constructor(private readonly db: PrismaClient | Prisma.TransactionClient) {}
  async transaction<T>(work: (store: Store) => Promise<T>): Promise<T> {
    if ('$transaction' in this.db) return this.db.$transaction(tx => work(new PrismaStore(tx)), { timeout: 10000 });
    return work(this);
  }
  usuarioPorEmail(email: string) { return this.db.usuario.findUnique({ where: { email } }); }
  unidade(id: number) { return this.db.unidade.findUnique({ where: { id } }); }
  async unidades({ page, limit }: Paginacao) {
    const queries = [this.db.unidade.findMany({ skip: (page - 1) * limit, take: limit, orderBy: { id: 'asc' } }), this.db.unidade.count()] as const;
    // Batch keeps page/count consistent without an interactive transaction holding
    // a SQLite connection while waiting for a JavaScript callback.
    const [dados, total] = '$transaction' in this.db
      ? await this.db.$transaction([...queries]) : await Promise.all(queries);
    return { dados, total };
  }
  produto(id: number) { return this.db.produto.findUnique({ where: { id } }); }
  async produtos({ unidadeId, page, limit }: Paginacao & { unidadeId?: number }) {
    const where = unidadeId ? { estoques: { some: { unidadeId } } } : {};
    const consulta = this.db.produto.findMany({
      where, skip: (page - 1) * limit, take: limit,
      include: { estoques: { where: unidadeId ? { unidadeId } : {}, include: { unidade: true } } },
      orderBy: { id: 'asc' }
    });
    const queries = [consulta, this.db.produto.count({ where })] as const;
    const [dados, total] = '$transaction' in this.db
      ? await this.db.$transaction([...queries]) : await Promise.all(queries);
    return { dados, total };
  }
  async reservar(unidadeId: number, produtoId: number, quantidade: number) {
    const result = await this.db.estoqueUnidade.updateMany({
      where: { unidadeId, produtoId, quantidade: { gte: quantidade } }, data: { quantidade: { decrement: quantidade } }
    });
    return result.count === 1;
  }
  async devolver(unidadeId: number, itens: ItemPedido[]) {
    for (const item of itens) await this.db.estoqueUnidade.update({
      where: { unidadeId_produtoId: { unidadeId, produtoId: item.produtoId } },
      data: { quantidade: { increment: item.quantidade } }
    });
  }
  criarPedido(data: { usuarioId: number; unidadeId: number; canalPedido: string; total: number; itens: ItemPedido[] }) {
    return this.db.pedido.create({ data: { ...data, itens: { create: data.itens } }, include: { itens: true } });
  }
  pedido(id: number) { return this.db.pedido.findUnique({ where: { id }, include: { itens: true } }); }
  async listarPedidos({ usuarioId, canalPedido, page, limit }: PedidoFiltro) {
    const where = { usuarioId, canalPedido };
    const dados = await this.db.pedido.findMany({ where, skip: (page - 1) * limit, take: limit,
      include: { itens: { include: { produto: true } }, pagamento: true }, orderBy: [{ criadoEm: 'desc' }, { id: 'desc' }] });
    const total = await this.db.pedido.count({ where });
    return { dados, total };
  }
  async mudarStatus(id: number, anterior: string, novo: string) {
    const result = await this.db.pedido.updateMany({ where: { id, status: anterior }, data: { status: novo } });
    return result.count === 1;
  }
  pagamento(data: { pedidoId: number; valor: number; status: string; transacaoId: string }) {
    const values = { ...data, metodo: 'SIMULACAO_GATEWAY' };
    return this.db.pagamento.upsert({ where: { pedidoId: data.pedidoId }, create: values, update: values });
  }
  async auditar(acao: string, detalhes: Record<string, unknown>, usuarioId: number) {
    await AuditLogger.registar(this.db, acao, detalhes, usuarioId);
  }
  ultimoConsentimento(usuarioId: number, finalidade: string) {
    return this.db.consentimento.findFirst({ where: { usuarioId, finalidade }, orderBy: { id: 'desc' } });
  }
  registrarConsentimento(data: { usuarioId: number; finalidade: string; versaoAviso: string; aceito: boolean }) {
    return this.db.consentimento.create({ data: { ...data, demonstracao: true } });
  }
  async listarConsentimentos(usuarioId: number, { page, limit }: Paginacao) {
    const where = { usuarioId };
    const dados = await this.db.consentimento.findMany({ where, skip: (page - 1) * limit, take: limit, orderBy: { id: 'desc' } });
    return { dados, total: await this.db.consentimento.count({ where }) };
  }
  async listarAuditoria({ page, limit, acao }: Paginacao & { acao?: string }) {
    const where = { acao };
    const dados = await this.db.auditLog.findMany({ where, skip: (page - 1) * limit, take: limit, orderBy: { id: 'desc' } });
    return { dados, total: await this.db.auditLog.count({ where }) };
  }
}
