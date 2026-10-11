import { randomUUID } from 'node:crypto';
import { prisma } from './infrastructure/database/prismaClient.js';
import { PrismaStore } from './infrastructure/repositories/PrismaStore.js';
import { HashService } from './infrastructure/security/HashService.js';
import { TokenService } from './infrastructure/security/TokenService.js';
import { AutenticarUsuarioUseCase } from './application/use-cases/AutenticarUsuarioUseCase.js';
import { CriarPedidoUseCase } from './application/use-cases/CriarPedidoUseCase.js';
import { ProcessarPagamentoMockUseCase } from './application/use-cases/ProcessarPagamentoMockUseCase.js';
import { AtualizarStatusPedidoUseCase } from './application/use-cases/AtualizarStatusPedidoUseCase.js';
import { ListarProdutosCardapioUseCase } from './application/use-cases/ListarProdutosCardapioUseCase.js';
import { ListarPedidosUseCase } from './application/use-cases/ListarPedidosUseCase.js';
import { ListarUnidadesUseCase } from './application/use-cases/ListarUnidadesUseCase.js';
import { MockPaymentGateway } from './infrastructure/payments/MockPaymentGateway.js';
import { GerenciarConsentimentoUseCase } from './application/use-cases/GerenciarConsentimentoUseCase.js';
import { ListarAuditoriaUseCase } from './application/use-cases/ListarAuditoriaUseCase.js';
const store = new PrismaStore(prisma);
export const services = {
  autenticar: new AutenticarUsuarioUseCase(store, HashService, TokenService),
  criarPedido: new CriarPedidoUseCase(store),
  pagar: new ProcessarPagamentoMockUseCase(store, new MockPaymentGateway(), randomUUID),
  atualizarStatus: new AtualizarStatusPedidoUseCase(store),
  produtos: new ListarProdutosCardapioUseCase(store),
  pedidos: new ListarPedidosUseCase(store),
  consentimentos: new GerenciarConsentimentoUseCase(store),
  auditoria: new ListarAuditoriaUseCase(store),
  unidades: new ListarUnidadesUseCase(store)
};
