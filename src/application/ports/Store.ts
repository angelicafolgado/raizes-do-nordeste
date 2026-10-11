import { Usuario } from '../../domain/entities/Usuario.js';
import { Produto } from '../../domain/entities/Produto.js';
import { Pedido, ItemPedido } from '../../domain/entities/Pedido.js';
import { Pagamento } from '../../domain/entities/Pagamento.js';
import { Unidade } from '../../domain/entities/Unidade.js';
import { Paginacao } from '../pagination.js';
import { Consentimento } from '../../domain/entities/Consentimento.js';
import { RegistroAuditoria } from '../../domain/entities/RegistroAuditoria.js';
export interface PedidoFiltro { usuarioId?: number; canalPedido?: string; page: number; limit: number }
export interface Store {
  transaction<T>(work: (store: Store) => Promise<T>): Promise<T>;
  usuarioPorEmail(email: string): Promise<Usuario | null>;
  unidade(id: number): Promise<Unidade | null>;
  unidades(filtro: Paginacao): Promise<{ dados: Unidade[]; total: number }>;
  produto(id: number): Promise<Produto | null>;
  produtos(filtro: Paginacao & { unidadeId?: number }): Promise<{ dados: Produto[]; total: number }>;
  reservar(unidadeId: number, produtoId: number, quantidade: number): Promise<boolean>;
  devolver(unidadeId: number, itens: ItemPedido[]): Promise<void>;
  criarPedido(data: { usuarioId: number; unidadeId: number; canalPedido: string; total: number; itens: ItemPedido[] }): Promise<Pedido>;
  pedido(id: number): Promise<Pedido | null>;
  listarPedidos(filtro: PedidoFiltro): Promise<{ dados: Pedido[]; total: number }>;
  mudarStatus(id: number, anterior: string, novo: string): Promise<boolean>;
  pagamento(data: { pedidoId: number; valor: number; status: string; transacaoId: string }): Promise<Pagamento>;
  auditar(acao: string, detalhes: Record<string, unknown>, usuarioId: number): Promise<void>;
  ultimoConsentimento(usuarioId: number, finalidade: string): Promise<Consentimento | null>;
  registrarConsentimento(data: { usuarioId: number; finalidade: string; versaoAviso: string; aceito: boolean }): Promise<Consentimento>;
  listarConsentimentos(usuarioId: number, filtro: Paginacao): Promise<{ dados: Consentimento[]; total: number }>;
  listarAuditoria(filtro: Paginacao & { acao?: string }): Promise<{ dados: RegistroAuditoria[]; total: number }>;
}
