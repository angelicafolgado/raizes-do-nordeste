import { Store } from '../ports/Store.js';
import { AppError } from '../../domain/errors/AppError.js';
import { avisoFidelidade } from '../privacy/aviso.js';
import { pagina, validarPaginacao } from '../pagination.js';

export class GerenciarConsentimentoUseCase {
  constructor(private store: Store) {}

  async registrar(usuarioId: number, body: unknown) {
    if (!body || typeof body !== 'object' || Array.isArray(body))
      throw new AppError('DADOS_INVALIDOS', 'Informe aceito como booleano.', 422);
    const input = body as Record<string, unknown>;
    if (Object.keys(input).some(key => !['aceito', 'versaoAviso'].includes(key)) || typeof input.aceito !== 'boolean')
      throw new AppError('DADOS_INVALIDOS', 'Use apenas aceito (booleano) e versaoAviso. A identidade vem do token.', 422);
    if (input.versaoAviso !== undefined && typeof input.versaoAviso !== 'string')
      throw new AppError('DADOS_INVALIDOS', 'versaoAviso deve ser texto.', 422);
    if (input.aceito && input.versaoAviso !== avisoFidelidade.versaoAviso)
      throw new AppError('VERSAO_AVISO_INVALIDA', 'Consulte o aviso atual antes de aceitar.', 422);
    const aceito = input.aceito;
    return this.store.transaction(async tx => {
      const ultimo = await tx.ultimoConsentimento(usuarioId, avisoFidelidade.finalidade);
      // PUT repetido não duplica o evento; revogar nunca depende de aceitar aviso novo.
      if (ultimo?.aceito === aceito && (!aceito || ultimo.versaoAviso === avisoFidelidade.versaoAviso)) return ultimo;
      const evento = await tx.registrarConsentimento({ usuarioId, finalidade: avisoFidelidade.finalidade,
        versaoAviso: avisoFidelidade.versaoAviso, aceito });
      await tx.auditar(aceito ? 'CONSENTIMENTO_REGISTRADO' : 'CONSENTIMENTO_REVOGADO', {
        consentimentoId: evento.id, finalidade: evento.finalidade, versaoAviso: evento.versaoAviso, demonstracao: true
      }, usuarioId);
      return evento;
    });
  }

  async consultar(usuarioId: number, page: number, limit: number) {
    validarPaginacao({ page, limit });
    return this.store.transaction(async tx => {
      const ultimo = await tx.ultimoConsentimento(usuarioId, avisoFidelidade.finalidade);
      const result = await tx.listarConsentimentos(usuarioId, { page, limit });
      await tx.auditar('CONSULTA_CONSENTIMENTOS', { pagina: page, limite: limit }, usuarioId);
      return { ...pagina(result.dados, result.total, { page, limit }),
        atual: { finalidade: avisoFidelidade.finalidade, versaoAviso: avisoFidelidade.versaoAviso,
          aceito: ultimo?.aceito === true && ultimo.versaoAviso === avisoFidelidade.versaoAviso,
          demonstracao: true, ultimoRegistroId: ultimo?.id ?? null } };
    });
  }
}
