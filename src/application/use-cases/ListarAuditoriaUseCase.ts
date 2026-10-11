import { Store } from '../ports/Store.js';
import { Identidade } from '../../domain/entities/Usuario.js';
import { AppError } from '../../domain/errors/AppError.js';
import { pagina, validarPaginacao } from '../pagination.js';

// Projeção explícita: dados arbitrários eventualmente presentes em logs legados não são expostos.
const camposPermitidos = ['pedidoId', 'total', 'canal', 'statusPagamento', 'transacaoId', 'correlacaoId',
  'motivo', 'anterior', 'novoStatus', 'pagina', 'limite', 'canalPedido', 'quantidadeRetornada',
  'escopo', 'acaoFiltro', 'consentimentoId', 'finalidade', 'versaoAviso', 'demonstracao'];
function detalhesSeguros(serializado: string) {
  let valor: unknown;
  try { valor = JSON.parse(serializado); } catch { return {}; }
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) return {};
  return Object.fromEntries(Object.entries(valor).filter(([key, value]) =>
    camposPermitidos.includes(key) && ['string', 'number', 'boolean'].includes(typeof value)));
}
export class ListarAuditoriaUseCase {
  constructor(private store: Store) {}
  async executar(usuario: Identidade, page: number, limit: number, acao?: unknown) {
    if (usuario.role !== 'ADMIN') throw new AppError('ACESSO_NEGADO', 'Auditoria restrita ao administrador.', 403);
    validarPaginacao({ page, limit });
    if (acao !== undefined && (typeof acao !== 'string' || !/^[A-Z_]{1,64}$/.test(acao)))
      throw new AppError('PARAMETRO_INVALIDO', 'acao deve conter apenas letras maiúsculas e sublinhado (até 64 caracteres).', 422);
    const filtro = acao as string | undefined;
    return this.store.transaction(async tx => {
      const result = await tx.listarAuditoria({ page, limit, acao: filtro });
      await tx.auditar('CONSULTA_AUDITORIA', { pagina: page, limite: limit, acaoFiltro: filtro }, usuario.id);
      return pagina(result.dados.map(log => ({ ...log, detalhes: detalhesSeguros(log.detalhes) })), result.total, { page, limit });
    });
  }
}
