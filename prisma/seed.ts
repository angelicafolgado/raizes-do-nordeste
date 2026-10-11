import 'dotenv/config';
import { PrismaClient } from '@prisma/client';
import { HashService } from '../src/infrastructure/security/HashService.js';
const prisma = new PrismaClient();
async function main() {
  if (process.env.NODE_ENV === 'production') throw new Error('O seed com contas de demonstração não deve ser executado em produção.');
  const senha = await HashService.gerar('123456');
  await prisma.$transaction(async tx => {
    for (const [nome, email, role] of [
      ['Maria da Silva', 'maria@cliente.com', 'CLIENTE'],
      ['José Santos', 'jose@cliente.com', 'CLIENTE'],
      ['Cozinheiro João', 'cozinha@raizes.com', 'COZINHA'],
      ['Atendente Ana', 'atendente@raizes.com', 'ATENDENTE'],
      ['Administrador', 'admin@raizes.com', 'ADMIN']
    ]) await tx.usuario.upsert({ where: { email }, update: {}, create: { nome, email, role, senha, consentimentoLGPD: false } });
    for (const unidade of [
      { id: 1, nome: 'Unidade Recife Olinda', cidade: 'Recife' },
      { id: 2, nome: 'Unidade João Pessoa', cidade: 'João Pessoa' }
    ]) await tx.unidade.upsert({ where: { id: unidade.id }, update: {}, create: unidade });
    const produtos = [
      { id: 1, nome: 'Baião de Dois Completo', descricao: 'Queijo coalho e carne de sol', preco: 38.5 },
      { id: 2, nome: 'Tapioca Tradicional de Manteiga', descricao: 'Tapioca artesanal nordestina', preco: 14 },
      { id: 3, nome: 'Suco Natural de Cajá 500ml', descricao: 'Polpa fresca natural', preco: 9 }
    ];
    for (const produto of produtos) {
      await tx.produto.upsert({ where: { id: produto.id }, update: {}, create: produto });
      for (const unidadeId of [1, 2]) await tx.estoqueUnidade.upsert({
        where: { unidadeId_produtoId: { unidadeId, produtoId: produto.id } }, update: {},
        create: { unidadeId, produtoId: produto.id, quantidade: unidadeId === 1 ? [25, 40, 50][produto.id - 1] : 10 }
      });
    }
  }, { timeout: 10000 });
  console.log('Seed concluído. Contas de demonstração: senha 123456. Dados existentes preservados.');
}
main().catch(err => { console.error(err); process.exitCode = 1; }).finally(() => prisma.$disconnect());
