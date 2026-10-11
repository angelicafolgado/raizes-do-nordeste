import { before, after, test } from 'node:test';
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync, unlinkSync, existsSync, readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import { resolve } from 'node:path';
import { randomUUID } from 'node:crypto';
import type { Server } from 'node:http';
import type { PrismaClient } from '@prisma/client';

const dbFile = resolve('.test-output', 'api-' + randomUUID() + '.db');
process.env.DATABASE_URL = 'file:' + dbFile.replaceAll('\\', '/');
process.env.JWT_SECRET = randomUUID() + randomUUID();
process.env.JWT_EXPIRES_IN = '1h';
process.env.NODE_ENV = 'test';
let server: Server;
let db: PrismaClient;
let base: string;
let cliente: string;
let outro: string;
let cozinha: string;
let atendente: string;
let admin: string;
let pedidoId: number;

function cli(args: string[]) {
  const result = spawnSync(process.execPath, args, { env: process.env, encoding: 'utf8', timeout: 60000 });
  assert.equal(result.status, 0, result.stdout + result.stderr);
}
async function request(path: string, method = 'GET', body?: unknown, token?: string) {
  const response = await fetch(base + path, { method,
    headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  if (path.startsWith('/api/')) assert.equal(response.headers.get('cache-control'), 'no-store');
  return { status: response.status, body: await response.json() };
}
async function login(email: string) {
  const result = await request('/api/auth/login', 'POST', { email, senha: '123456' });
  assert.equal(result.status, 200);
  assert.equal(result.body.expiresIn, 3600);
  assert.equal(result.body.usuario.senha, undefined);
  return result.body.accessToken as string;
}
function criar(body: Record<string, unknown> = {}, token?: string) {
  return request('/api/pedidos', 'POST', { unidadeId: 1, canalPedido: 'TOTEM', itens: [{ produtoId: 1, quantidade: 1 }], ...body }, token ?? cliente);
}
async function estoque(unidadeId = 1, produtoId = 1) {
  return (await db.estoqueUnidade.findUniqueOrThrow({ where: { unidadeId_produtoId: { unidadeId, produtoId } } })).quantidade;
}
before(async () => {
  mkdirSync(resolve('.test-output'), { recursive: true });
  writeFileSync(dbFile, '');
  cli([require.resolve('prisma/build/index.js'), 'migrate', 'deploy']);
  cli(['--import', 'tsx', 'prisma/seed.ts']);
  const { app } = await import('../src/app.js');
  db = (await import('../src/infrastructure/database/prismaClient.js')).prisma;
  server = await new Promise<Server>(resolve => { const s = app.listen(0, '127.0.0.1', () => resolve(s)); });
  const address = server.address();
  assert.ok(address && typeof address !== 'string');
  base = 'http://127.0.0.1:' + address.port;
  cliente = await login('maria@cliente.com'); outro = await login('jose@cliente.com');
  cozinha = await login('cozinha@raizes.com'); atendente = await login('atendente@raizes.com');
  admin = await login('admin@raizes.com');
});
after(async () => {
  if (server) await new Promise<void>((resolve, reject) => server.close(err => err ? reject(err) : resolve()));
  if (db) await db.$disconnect();
  for (const suffix of ['', '-journal', '-wal', '-shm']) if (existsSync(dbFile + suffix)) unlinkSync(dbFile + suffix);
});

test('login rejeita senha incorreta e dados inválidos', async () => {
  assert.equal((await request('/api/auth/login', 'POST', { email: 'maria@cliente.com', senha: 'errada' })).status, 401);
  for (const body of [{}, { email: [], senha: {} }]) assert.equal((await request('/api/auth/login', 'POST', body)).status, 422);
  assert.equal((await request('/api/auth/login', 'POST', null)).status, 400);
});
test('rotas privadas exigem JWT válido e perfil permitido', async () => {
  assert.equal((await request('/api/pedidos')).status, 401);
  assert.equal((await request('/api/pedidos', 'GET', undefined, 'falso')).status, 401);
  assert.equal((await criar({}, cozinha)).status, 403);
});
test('cardápio público retorna estoque separado por unidade', async () => {
  const result = await request('/api/produtos');
  assert.equal(result.status, 200); assert.equal(result.body.dados.length, 3);
  assert.equal(result.body.dados[0].estoques.length, 2);
  const filtrado = await request('/api/produtos?unidadeId=2');
  assert.equal(filtrado.body.dados[0].estoques.length, 1); assert.equal(filtrado.body.dados[0].estoques[0].unidadeId, 2);
  assert.equal((await request('/api/unidades')).body.dados.length, 2);
  assert.equal((await request('/api/produtos?unidadeId=999')).status, 404);
});
test('cria pedido, calcula total e reserva somente na unidade escolhida', async () => {
  const result = await criar({ itens: [{ produtoId: 1, quantidade: 2 }, { produtoId: 2, quantidade: 1 }] });
  assert.equal(result.status, 201); pedidoId = result.body.id;
  assert.equal(result.body.total, 91); assert.equal(result.body.status, 'AGUARDANDO_PAGAMENTO');
  assert.equal(await estoque(), 23); assert.equal(await estoque(2), 10);
  assert.equal(await db.auditLog.count({ where: { acao: 'CRIACAO_PEDIDO' } }), 1);
});
test('canal inválido e unidade inexistente retornam erros de domínio', async () => {
  assert.equal((await criar({ canalPedido: 'INVALIDO' })).status, 400);
  assert.equal((await criar({ unidadeId: 999 })).status, 404);
  assert.equal((await criar({ unidadeId: '1' })).status, 422);
});
test('itens ausentes, nulos e quantidades inválidas retornam 422', async () => {
  for (const itens of [[], null, [null], [{ produtoId: 1, quantidade: -1 }], [{ produtoId: 1, quantidade: 1.5 }], [{ produtoId: 1, quantidade: '2' }], [{ produtoId: 1 }]])
    assert.equal((await criar({ itens })).status, 422, JSON.stringify(itens));
});
test('falha no segundo item reverte reserva do primeiro e não cria pedido', async () => {
  const before = await estoque(); const pedidos = await db.pedido.count();
  const result = await criar({ itens: [{ produtoId: 1, quantidade: 1 }, { produtoId: 2, quantidade: 99999 }] });
  assert.equal(result.status, 409); assert.equal(await estoque(), before); assert.equal(await db.pedido.count(), pedidos);
  assert.equal((await criar({ itens: [{ produtoId: 999, quantidade: 1 }] })).status, 404);
});
test('produtos repetidos são agrupados e não excedem estoque', async () => {
  const before = await estoque();
  assert.equal((await criar({ itens: [{ produtoId: 1, quantidade: before }, { produtoId: 1, quantidade: 1 }] })).status, 409);
  assert.equal(await estoque(), before);
  const result = await criar({ itens: [{ produtoId: 1, quantidade: 1 }, { produtoId: 1, quantidade: 2 }] });
  assert.equal(result.status, 201); assert.equal(result.body.itens.length, 1); assert.equal(result.body.itens[0].quantidade, 3);
});
test('cliente vê somente seus pedidos e não paga pedido alheio', async () => {
  const result = await request('/api/pedidos', 'GET', undefined, outro);
  assert.equal(result.body.dados.length, 0);
  assert.equal((await request('/api/pedidos/' + pedidoId + '/pagamento', 'POST', { statusSimulado: 'APROVADO' }, outro)).status, 403);
});
test('filtro por canal e paginação são validados', async () => {
  const result = await request('/api/pedidos?canalPedido=TOTEM&page=1&limit=1', 'GET', undefined, cliente);
  assert.equal(result.status, 200); assert.equal(result.body.dados.length, 1); assert.equal(result.body.paginacao.total, 2);
  for (const query of ['page=abc', 'page=1x', 'limit=101', 'page=0', 'page=1&page=2'])
    assert.equal((await request('/api/pedidos?' + query, 'GET', undefined, cliente)).status, 422);
  assert.equal((await request('/api/pedidos?canalPedido=X', 'GET', undefined, cliente)).status, 400);
});
test('cliente não altera status e cozinha não pula pagamento', async () => {
  assert.equal((await request('/api/pedidos/' + pedidoId + '/status', 'PATCH', { status: 'PRONTO' }, cliente)).status, 403);
  assert.equal((await request('/api/pedidos/' + pedidoId + '/status', 'PATCH', { status: 'PRONTO' }, cozinha)).status, 409);
});
test('pagamento recusado permite nova tentativa aprovada', async () => {
  const path = '/api/pedidos/' + pedidoId + '/pagamento';
  assert.equal((await request(path, 'POST', { statusSimulado: 'INVALIDO' }, cliente)).status, 400);
  const recusado = await request(path, 'POST', { statusSimulado: 'RECUSADO' }, cliente);
  assert.equal(recusado.status, 200); assert.equal(recusado.body.sucesso, false);
  assert.equal(recusado.body.statusPedido, 'AGUARDANDO_PAGAMENTO');
  const aprovado = await request(path, 'POST', { statusSimulado: 'APROVADO' }, cliente);
  assert.equal(aprovado.status, 200); assert.equal(aprovado.body.statusPedido, 'EM_PREPARO');
  assert.notEqual(aprovado.body.transacaoId, recusado.body.transacaoId);
  assert.equal(await db.pagamento.count({ where: { pedidoId } }), 1);
  assert.equal(await db.auditLog.count({ where: { acao: 'PAGAMENTO_MOCK' } }), 2);
  assert.equal((await request(path, 'POST', { statusSimulado: 'APROVADO' }, cliente)).status, 409);
});
test('cozinha avança preparo para pronto e entregue sem regressão', async () => {
  const path = '/api/pedidos/' + pedidoId + '/status';
  assert.equal((await request(path, 'PATCH', { status: 'ENTREGUE' }, cozinha)).status, 409);
  assert.equal((await request(path, 'PATCH', { status: 'PRONTO' }, cozinha)).status, 200);
  assert.equal((await request(path, 'PATCH', { status: 'ENTREGUE' }, cozinha)).status, 200);
  assert.equal((await request(path, 'PATCH', { status: 'EM_PREPARO' }, cozinha)).status, 409);
});
test('cancelamento devolve estoque exatamente uma vez', async () => {
  const before = await estoque(); const pedido = await criar();
  assert.equal(await estoque(), before - 1);
  const path = '/api/pedidos/' + pedido.body.id + '/status';
  assert.equal((await request(path, 'PATCH', { status: 'CANCELADO' }, cozinha)).status, 200);
  assert.equal(await estoque(), before);
  assert.equal((await request(path, 'PATCH', { status: 'CANCELADO' }, cozinha)).status, 409);
  assert.equal(await estoque(), before);
  assert.equal((await request('/api/pedidos/' + pedido.body.id + '/pagamento', 'POST', { statusSimulado: 'APROVADO' }, cliente)).status, 409);
});
test('atendente pode criar e listar pedidos', async () => {
  assert.equal((await criar({ canalPedido: 'BALCAO' }, atendente)).status, 201);
  const result = await request('/api/pedidos', 'GET', undefined, atendente);
  assert.ok(result.body.paginacao.total >= 4);
});
test('IDs inválidos não passam por parseInt parcial e ausentes retornam 404', async () => {
  for (const id of ['1abc', '-1', '0']) assert.equal((await request('/api/pedidos/' + id + '/status', 'PATCH', { status: 'PRONTO' }, cozinha)).status, 422);
  assert.equal((await request('/api/pedidos/99999/status', 'PATCH', { status: 'PRONTO' }, cozinha)).status, 404);
  assert.equal((await request('/api/pedidos/99999/pagamento', 'POST', { statusSimulado: 'APROVADO' }, cliente)).status, 404);
});
test('JSON malformado e rota desconhecida usam envelope padronizado', async () => {
  const malformed = await fetch(base + '/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{' });
  assert.equal(malformed.status, 400);
  const result = await request('/inexistente'); assert.equal(result.status, 404);
  for (const key of ['error', 'message', 'details', 'timestamp', 'path', 'requestId']) assert.ok(key in result.body);
});
test('Swagger e health estão disponíveis', async () => {
  assert.equal((await request('/api/health')).status, 200);
  assert.equal((await request('/api-docs.json')).body.openapi, '3.0.0');
  assert.equal((await fetch(base + '/api-docs/')).status, 200);
});
test('seed repetido preserva pedidos e estoque', async () => {
  const before = await estoque(); const pedidos = await db.pedido.count();
  cli(['--import', 'tsx', 'prisma/seed.ts']);
  assert.equal(await estoque(), before); assert.equal(await db.pedido.count(), pedidos);
});
test('pedidos concorrentes não vendem a última unidade duas vezes', async () => {
  await db.estoqueUnidade.update({ where: { unidadeId_produtoId: { unidadeId: 2, produtoId: 3 } }, data: { quantidade: 1 } });
  const payload = { unidadeId: 2, itens: [{ produtoId: 3, quantidade: 1 }] };
  const results = await Promise.all([criar(payload), criar(payload)]);
  assert.deepEqual(results.map(r => r.status).sort(), [201, 409]);
  assert.equal(await estoque(2, 3), 0);
});
test('pagamentos concorrentes aprovam um pedido uma única vez', async () => {
  const pedido = await criar();
  const path = '/api/pedidos/' + pedido.body.id + '/pagamento';
  const results = await Promise.all([request(path, 'POST', { statusSimulado: 'APROVADO' }, cliente), request(path, 'POST', { statusSimulado: 'APROVADO' }, cliente)]);
  assert.deepEqual(results.map(r => r.status).sort(), [200, 409]);
  assert.equal(await db.pagamento.count({ where: { pedidoId: pedido.body.id } }), 1);
});
test('coleção Postman executa os cenários em sequência', async () => {
  const collection = JSON.parse(readFileSync(resolve('postman/raizes_nordeste_clean_collection.json'), 'utf8'));
  const variables = new Map<string, unknown>([['baseUrl', base]]);
  const interpolate = (value: string) => value.replace(/\{\{(\w+)\}\}/g, (_match, key) => String(variables.get(key) ?? ''));
  const flatten = (items: Array<Record<string, any>>): Array<Record<string, any>> => items.flatMap(item => item.item ? flatten(item.item) : [item]);
  const requests = flatten(collection.item);
  for (const item of requests) {
    const req = item.request;
    const response = await fetch(interpolate(req.url), {
      method: req.method,
      headers: Object.fromEntries(req.header.map((h: { key: string; value: string }) => [h.key, interpolate(h.value)])),
      body: req.body ? interpolate(req.body.raw) : undefined
    });
    const body = await response.json();
    const pm = {
      response: { json: () => body, to: { have: { status: (code: number) => assert.equal(response.status, code, item.name) } } },
      collectionVariables: { set: (key: string, value: unknown) => variables.set(key, value) },
      test: (_name: string, callback: () => void) => callback(),
      expect: (actual: unknown) => ({ to: { eql: (expected: unknown) => assert.deepEqual(actual, expected, item.name) } })
    };
    for (const event of item.event) runInNewContext(event.script.exec.join('\n'), { pm }, { timeout: 1000 });
  }
  assert.equal(requests.length, 32);
  assert.equal(collection.item.length, 9);
});
test('20 consultas publicas concorrentes retornam paginas sem falhas', async () => {
  const results = await Promise.all(Array.from({ length: 20 }, (_, i) =>
    request(i % 2 ? '/api/produtos?unidadeId=1&page=1&limit=1' : '/api/unidades?page=1&limit=1')));
  for (const result of results) {
    assert.equal(result.status, 200);
    assert.equal(result.body.dados.length, 1);
    assert.ok(result.body.paginacao.total >= 1);
    assert.equal(result.body.paginacao.limite, 1);
  }
});

test('produtos e unidades paginam com totais, limites e páginas vazias', async () => {
  for (const [resource, total] of [['produtos', 3], ['unidades', 2]] as const) {
    const first = await request('/api/' + resource + '?page=1&limit=1');
    const second = await request('/api/' + resource + '?page=2&limit=1');
    assert.equal(first.status, 200);
    assert.equal(first.body.dados.length, 1);
    assert.notEqual(first.body.dados[0].id, second.body.dados[0].id);
    assert.deepEqual(second.body.paginacao, { pagina: 2, limite: 1, total, totalPaginas: total });
    assert.deepEqual((await request('/api/' + resource + '?page=99')).body.dados, []);
    for (const query of ['page=0', 'limit=101', 'page=1x', 'limit=-1', 'page=1&page=2', 'limit=1.5', 'page=2147483647&limit=100'])
      assert.equal((await request('/api/' + resource + '?' + query)).status, 422);
  }
  const filtered = await request('/api/produtos?unidadeId=2&page=2&limit=1');
  assert.equal(filtered.body.paginacao.total, 3);
  assert.equal(filtered.body.dados[0].estoques.length, 1);
  assert.equal(filtered.body.dados[0].estoques[0].unidadeId, 2);
});
test('falhas técnicas preservam pedido e reserva, auditam e permitem nova tentativa', async () => {
  const pedido = await criar(); const id = pedido.body.id;
  const saldo = await estoque();
  const original = await db.pedido.findUniqueOrThrow({ where: { id } });
  for (const [cenario, http, error] of [['INDISPONIVEL', 503, 'GATEWAY_INDISPONIVEL'], ['TIMEOUT', 504, 'GATEWAY_TIMEOUT']] as const) {
    const result = await request('/api/pedidos/' + id + '/pagamento', 'POST', { statusSimulado: cenario }, cliente);
    assert.equal(result.status, http); assert.equal(result.body.error, error);
    assert.ok(result.body.requestId); assert.ok(result.body.details[0].issue);
    assert.deepEqual(await db.pedido.findUniqueOrThrow({ where: { id } }), original);
    assert.equal(await estoque(), saldo);
    assert.equal(await db.pagamento.count({ where: { pedidoId: id } }), 0);
    const logs = await db.auditLog.findMany({ where: { acao: 'FALHA_PAGAMENTO_MOCK' } });
    assert.ok(logs.some(log => { const value = JSON.parse(log.detalhes); return value.pedidoId === id && value.motivo === cenario && value.correlacaoId === result.body.details[0].issue; }));
  }
  assert.equal((await request('/api/pedidos/' + id + '/pagamento', 'POST', { statusSimulado: 'RECUSADO' }, cliente)).status, 200);
  const pagamentoAnterior = await db.pagamento.findUniqueOrThrow({ where: { pedidoId: id } });
  assert.equal((await request('/api/pedidos/' + id + '/pagamento', 'POST', { statusSimulado: 'TIMEOUT' }, cliente)).status, 504);
  assert.deepEqual(await db.pagamento.findUniqueOrThrow({ where: { pedidoId: id } }), pagamentoAnterior);
  assert.equal((await request('/api/pedidos/' + id + '/pagamento', 'POST', { statusSimulado: 'APROVADO' }, cliente)).status, 200);
});
test('caso de uso envia contrato ao gateway e usa seu retorno para aprovar', async () => {
  const { ProcessarPagamentoMockUseCase } = await import('../src/application/use-cases/ProcessarPagamentoMockUseCase.js');
  const { PrismaStore } = await import('../src/infrastructure/repositories/PrismaStore.js');
  const pedido = await criar(); const id = pedido.body.id;
  const usuario = await db.usuario.findUniqueOrThrow({ where: { email: 'maria@cliente.com' } });
  const useCase = new ProcessarPagamentoMockUseCase(new PrismaStore(db), {
    async solicitar(input) {
      assert.deepEqual(input, { pedidoId: id, valorCentavos: 3850, moeda: 'BRL', correlacaoId: 'contrato-teste', cenario: 'RECUSADO' });
      return { status: 'APROVADO', pedidoId: id, correlacaoId: input.correlacaoId, transacaoId: 'GATEWAY-TESTE' };
    }
  }, () => 'contrato-teste');
  const result = await useCase.executar(id, 'RECUSADO', usuario);
  assert.equal(result.statusPedido, 'EM_PREPARO');
  assert.equal(result.transacaoId, 'GATEWAY-TESTE');
});
test('resposta de gateway com correlação inválida não altera o pedido', async () => {
  const { ProcessarPagamentoMockUseCase } = await import('../src/application/use-cases/ProcessarPagamentoMockUseCase.js');
  const { PrismaStore } = await import('../src/infrastructure/repositories/PrismaStore.js');
  const pedido = await criar(); const id = pedido.body.id;
  const usuario = await db.usuario.findUniqueOrThrow({ where: { email: 'maria@cliente.com' } });
  const useCase = new ProcessarPagamentoMockUseCase(new PrismaStore(db), {
    async solicitar() { return { status: 'APROVADO', pedidoId: id, correlacaoId: 'incorreto', transacaoId: 'TESTE' }; }
  }, () => 'correto');
  await assert.rejects(useCase.executar(id, 'APROVADO', usuario), { errorCode: 'RESPOSTA_GATEWAY_INVALIDA', statusCode: 502 });
  assert.equal((await db.pedido.findUniqueOrThrow({ where: { id } })).status, 'AGUARDANDO_PAGAMENTO');
  assert.equal(await db.pagamento.count({ where: { pedidoId: id } }), 0);
});
test('aviso público identifica finalidade, versão e caráter opcional de demonstração', async () => {
  const aviso = await request('/api/privacidade/aviso');
  assert.equal(aviso.status, 200);
  assert.equal(aviso.body.finalidade, 'FIDELIDADE');
  assert.equal(aviso.body.versaoAviso, '1.0');
  assert.equal(aviso.body.opcional, true);
  assert.equal(aviso.body.demonstracao, true);
});
test('flag legada não concede consentimento e seed não fabrica eventos', async () => {
  const jose = await db.usuario.findUniqueOrThrow({ where: { email: 'jose@cliente.com' } });
  await db.usuario.update({ where: { id: jose.id }, data: { consentimentoLGPD: true } });
  const before = await db.consentimento.count();
  cli(['--import', 'tsx', 'prisma/seed.ts']);
  assert.equal(await db.consentimento.count(), before);
  const result = await request('/api/privacidade/consentimentos', 'GET', undefined, outro);
  assert.equal(result.status, 200); assert.equal(result.body.atual.aceito, false);
  assert.equal(result.body.atual.ultimoRegistroId, null); assert.deepEqual(result.body.dados, []);
});
test('consentimento exige booleano e aviso atual, sem aceitar identidade do corpo', async () => {
  const before = await db.consentimento.count();
  for (const body of [{}, { aceito: 'true', versaoAviso: '1.0' }, { aceito: true },
    { aceito: true, versaoAviso: 'antigo' }, { aceito: false, versaoAviso: 1 },
    { aceito: true, versaoAviso: '1.0', usuarioId: 1 }, { aceito: true, versaoAviso: '1.0', finalidade: 'OUTRA' }]) {
    assert.equal((await request('/api/privacidade/consentimentos/fidelidade', 'PUT', body, outro)).status, 422);
  }
  assert.equal(await db.consentimento.count(), before);
  assert.equal((await request('/api/privacidade/consentimentos/fidelidade', 'PUT', { aceito: true, versaoAviso: '1.0' })).status, 401);
  assert.equal((await request('/api/privacidade/consentimentos')).status, 401);
});
test('aceitar e revogar preserva histórico próprio e PUT repetido não duplica', async () => {
  const path = '/api/privacidade/consentimentos/fidelidade';
  const maria = await request('/api/privacidade/consentimentos', 'GET', undefined, cliente);
  const aceite = await request(path, 'PUT', { aceito: true, versaoAviso: '1.0' }, outro);
  assert.equal(aceite.status, 200); assert.equal(aceite.body.aceito, true);
  assert.equal(aceite.body.demonstracao, true); assert.ok(aceite.body.registradoEm);
  assert.equal((await request(path, 'PUT', { aceito: true, versaoAviso: '1.0' }, outro)).body.id, aceite.body.id);
  assert.equal((await request('/api/privacidade/consentimentos', 'GET', undefined, outro)).body.atual.aceito, true);
  const revogado = await request(path, 'PUT', { aceito: false, versaoAviso: 'antigo' }, outro);
  assert.equal(revogado.status, 200); assert.equal(revogado.body.aceito, false);
  assert.notEqual(revogado.body.id, aceite.body.id);
  assert.equal((await request(path, 'PUT', { aceito: false }, outro)).body.id, revogado.body.id);
  const historico = await request('/api/privacidade/consentimentos?page=2&limit=1', 'GET', undefined, outro);
  assert.equal(historico.body.atual.aceito, false);
  assert.equal(historico.body.paginacao.total, 2);
  assert.equal(historico.body.dados[0].id, aceite.body.id);
  assert.deepEqual((await request('/api/privacidade/consentimentos', 'GET', undefined, cliente)).body.dados, maria.body.dados);
  assert.deepEqual((await request('/api/privacidade/consentimentos?usuarioId=' + aceite.body.usuarioId, 'GET', undefined, cliente)).body.dados, maria.body.dados);
  assert.equal((await request('/api/privacidade/consentimentos?limit=101', 'GET', undefined, outro)).status, 422);
  const eventos = await db.auditLog.findMany({ where: { usuarioId: aceite.body.usuarioId, acao: { in: ['CONSENTIMENTO_REGISTRADO', 'CONSENTIMENTO_REVOGADO'] } } });
  assert.equal(eventos.length, 2);
});
test('revogar consentimento não bloqueia login, pedido e pagamento', async () => {
  await login('jose@cliente.com');
  const pedido = await criar({}, outro);
  assert.equal(pedido.status, 201);
  const pago = await request('/api/pedidos/' + pedido.body.id + '/pagamento', 'POST', { statusSimulado: 'APROVADO' }, outro);
  assert.equal(pago.status, 200);
});
test('consultas de pedidos registram ator, escopo e filtros sem dados pessoais', async () => {
  await request('/api/pedidos?canalPedido=TOTEM&page=1&limit=2', 'GET', undefined, cozinha);
  const usuario = await db.usuario.findUniqueOrThrow({ where: { email: 'cozinha@raizes.com' } });
  const log = await db.auditLog.findFirstOrThrow({ where: { acao: 'CONSULTA_PEDIDOS', usuarioId: usuario.id }, orderBy: { id: 'desc' } });
  assert.deepEqual(JSON.parse(log.detalhes), { pagina: 1, limite: 2, canalPedido: 'TOTEM', quantidadeRetornada: 2, escopo: 'REDE' });
  assert.ok(log.timestamp);
  await request('/api/pedidos', 'GET', undefined, outro);
  const proprio = await db.auditLog.findFirstOrThrow({ where: { acao: 'CONSULTA_PEDIDOS' }, orderBy: { id: 'desc' } });
  assert.equal(JSON.parse(proprio.detalhes).escopo, 'PROPRIOS');
});
test('auditoria exige ADMIN, valida filtros e audita sua própria consulta', async () => {
  assert.equal((await request('/api/auditoria')).status, 401);
  for (const token of [cliente, outro, cozinha, atendente])
    assert.equal((await request('/api/auditoria', 'GET', undefined, token)).status, 403);
  for (const query of ['page=0', 'limit=101', 'acao=login', 'acao=A&acao=B', 'acao='])
    assert.equal((await request('/api/auditoria?' + query, 'GET', undefined, admin)).status, 422);
  const result = await request('/api/auditoria?acao=CONSENTIMENTO_REVOGADO&limit=1', 'GET', undefined, admin);
  assert.equal(result.status, 200); assert.equal(result.body.dados.length, 1);
  assert.equal(result.body.dados[0].acao, 'CONSENTIMENTO_REVOGADO');
  assert.equal(result.body.dados[0].usuario, undefined);
  const log = await db.auditLog.findFirstOrThrow({ where: { acao: 'CONSULTA_AUDITORIA' }, orderBy: { id: 'desc' } });
  assert.equal(JSON.parse(log.detalhes).acaoFiltro, 'CONSENTIMENTO_REVOGADO');
});
test('projeção de auditoria não expõe campos arbitrários ou JSON malformado legado', async () => {
  await db.auditLog.create({ data: { acao: 'TESTE_PRIVACIDADE', detalhes: JSON.stringify({ pedidoId: 1, senha: 'SEGREDO_TESTE', email: 'PESSOA_TESTE', token: 'TOKEN_TESTE', outro: { senha: 'SEGREDO_TESTE' } }) } });
  await db.auditLog.create({ data: { acao: 'TESTE_PRIVACIDADE', detalhes: '{texto inválido' } });
  const result = await request('/api/auditoria?acao=TESTE_PRIVACIDADE', 'GET', undefined, admin);
  assert.equal(result.status, 200);
  assert.deepEqual(result.body.dados.map((log: { detalhes: unknown }) => log.detalhes), [{}, { pedidoId: 1 }]);
  assert.equal(JSON.stringify(result.body).includes('SEGREDO_TESTE'), false);
});
test('falha na auditoria reverte também o evento de consentimento', async () => {
  const { GerenciarConsentimentoUseCase } = await import('../src/application/use-cases/GerenciarConsentimentoUseCase.js');
  const { PrismaStore } = await import('../src/infrastructure/repositories/PrismaStore.js');
  const store = new PrismaStore(db);
  const falho = new Proxy(store, {
    get(target, property, receiver) {
      if (property !== 'transaction') return Reflect.get(target, property, receiver);
      return (work: (tx: unknown) => Promise<unknown>) => target.transaction(tx => work(new Proxy(tx, {
        get(inner, key, context) {
          if (key === 'auditar') return async () => { throw new Error('AUDITORIA_TESTE'); };
          return Reflect.get(inner, key, context);
        }
      })));
    }
  });
  const usuario = await db.usuario.findUniqueOrThrow({ where: { email: 'jose@cliente.com' } });
  const before = await db.consentimento.count();
  await assert.rejects(new GerenciarConsentimentoUseCase(falho).registrar(usuario.id, { aceito: true, versaoAviso: '1.0' }), /AUDITORIA_TESTE/);
  assert.equal(await db.consentimento.count(), before);
  assert.equal((await request('/api/privacidade/consentimentos', 'GET', undefined, outro)).body.atual.aceito, false);
});
test('erro inesperado não serializa exceção nem query sensível', async () => {
  const { errorHandlerMiddleware } = await import('../src/api/middlewares/errorHandlerMiddleware.js');
  const original = console.error; const logs: unknown[] = [];
  let status = 0; let body: Record<string, unknown> = {};
  console.error = (...args: unknown[]) => { logs.push(args); };
  try {
    const req = { path: '/api/teste', originalUrl: '/api/teste?segredo=SEGREDO_TESTE' };
    const res = { status(code: number) { status = code; return this; }, json(value: Record<string, unknown>) { body = value; } };
    errorHandlerMiddleware(new Error('SEGREDO_TESTE'), req as never, res as never, () => {});
  } finally { console.error = original; }
  assert.equal(status, 500); assert.equal(body.path, '/api/teste');
  assert.equal(JSON.stringify({ logs, body }).includes('SEGREDO_TESTE'), false);
  assert.ok(body.requestId);
});
