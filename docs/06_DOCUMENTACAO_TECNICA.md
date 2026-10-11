# Etapa 6 — Manual técnico e funcionalidade crítica

Consolidação em 11/10/2026 do backend Raízes do Nordeste. Este documento registra o estado atual; as situações “planejado/parcial” da etapa 1 são um diagnóstico histórico. O escopo executável é o fluxo A: pedido → pagamento simulado → status.

## Instalação e configuração

Requisitos: Node.js 20 ou superior e npm; alternativamente pnpm com o lockfile existente. Execute na raiz do projeto:

```powershell
npm install
npm run setup
npm run build
npm start
```

Para usar exatamente as versões do lockfile, substitua a instalação por `pnpm install --frozen-lockfile` e use `pnpm run` nos scripts. Não há lockfile npm; `npm install` resolve os intervalos do `package.json`. O primeiro `setup` cria `.env` com segredo aleatório, gera o cliente Prisma, aplica migrações existentes e executa seed idempotente. Não execute `prisma migrate reset` para preparar uma base com dados.

No Windows, `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1` automatiza preparação, build e início. `-SomentePreparar` não inicia o servidor. O fallback para runtime local é específico deste computador; em outro ambiente instale Node e npm. Para desenvolvimento use `npm run dev`.

| Variável | Valor inicial / regra |
|---|---|
| `PORT` | 3000; inteiro entre 1 e 65535 |
| `DATABASE_URL` | `file:./dev.db`; arquivo relativo ao schema em `prisma/` |
| `JWT_SECRET` | Segredo aleatório externo ao código, mínimo de 32 caracteres; placeholder rejeitado |
| `JWT_EXPIRES_IN` | `8h`; inteiro positivo em segundos ou sufixo `s`, `m`, `h`, `d` |
| `NODE_ENV` | O seed é bloqueado quando igual a `production` |

O setup preserva `.env` existente: corrigir um arquivo inválido é uma ação manual. Não publicar `.env`, bancos ou tokens. As contas fictícias e seus perfis estão no [README](../README.md); o seed não é um endpoint de cadastro.

Verificação inicial: abrir `http://localhost:3000/api/health` (esperado `{"status":"ok"}`), depois `http://localhost:3000/api-docs`. O health comprova resposta do processo; não executa consulta de prontidão no banco. O contrato JSON está em `/api-docs.json`. Encerrar com Ctrl+C. Alterar a porta exige atualizar a URL usada no navegador/Postman.

## Arquitetura e decisões

| Camada | Responsabilidade e ponto de entrada |
|---|---|
| Domínio | [Entidades, enums e erros](../src/domain); contratos independentes de Express e Prisma |
| Aplicação | [Casos de uso](../src/application/use-cases); validação, regras, paginação e portas `Store`, `Security`, `PaymentGateway` |
| Infraestrutura | [Adaptadores](../src/infrastructure); Prisma/SQLite, bcrypt, JWT, auditoria e gateway mock |
| API | [Rotas, controllers e middlewares](../src/api); HTTP, autenticação, perfis, serialização e OpenAPI |
| Composição | [composition.ts](../src/composition.ts) injeta implementações nas portas |
| Inicialização | [server.ts](../src/server.ts) valida configuração, conecta banco, inicia listener e trata encerramento |

A chamada percorre rota → middleware → controller → caso de uso → porta → adaptador. A aplicação conhece interfaces; a composição conhece implementações concretas. As entidades são interfaces TypeScript, sem métodos de negócio. Veja os [quatro diagramas](05_DIAGRAMAS.md).

SQLite atende à execução local demonstrativa, com migrações versionadas. Estados são strings persistidas e validados pela aplicação. Preços usam `Float` na persistência; o cálculo do total usa centavos inteiros. O pagamento é local e determinístico: não há cobrança financeira, webhook, estorno ou credenciais de adquirente. O adaptador atual é chamado dentro da transação; uma integração de rede futura exigirá redesenhar duração de transações, idempotência e conciliação.

## Funcionalidade crítica: pedido e pagamento

**Objetivo:** registrar pedido multicanal, reservar saldo da unidade e processar o pagamento mock mantendo consistência entre pedido, estoque e auditoria.

**Atores:** CLIENTE (seus pedidos), ATENDENTE e ADMIN (criação e pagamento); COZINHA e ADMIN (transições operacionais). Participantes técnicos: API, casos de uso, Store/Prisma e gateway mock. O atendente cria com sua própria identidade do JWT; não escolhe outro titular no corpo.

**Pré-condições:** usuário do seed com JWT válido, perfil permitido, unidade/produtos cadastrados e saldo suficiente. Para pagamento, pedido existente em `AGUARDANDO_PAGAMENTO`; cliente deve ser seu proprietário. Consentimento de fidelidade não é pré-condição.

**Fluxo principal:**

1. Autenticar em `POST /api/auth/login`; copiar `accessToken` para Authorize no Swagger.
2. Consultar unidades e produtos para escolher IDs e verificar saldo.
3. Enviar `POST /api/pedidos` com unidade, canal e 1 a 100 itens. Identidade vem do token, preço vem do banco; produtos repetidos são agrupados.
4. Na mesma transação, validar referências, reservar saldo condicionalmente, calcular total, persistir pedido/itens e auditar. Retornar 201 e `AGUARDANDO_PAGAMENTO`.
5. Enviar `POST /api/pedidos/{id}/pagamento` com `{"statusSimulado":"APROVADO"}`. Validar identidade/estado e enviar pedido, centavos, moeda BRL e correlação ao gateway.
6. Validar correlação, atualizar estado condicionalmente, gravar pagamento e auditoria. Retornar 200 com `sucesso: true` e `statusPedido: "EM_PREPARO"`.
7. Com token COZINHA/ADMIN, enviar `PATCH /api/pedidos/{id}/status` primeiro com `{"status":"PRONTO"}`, depois `{"status":"ENTREGUE"}`.

Exemplo do corpo de criação:

```json
{"unidadeId":1,"canalPedido":"WEB","itens":[{"produtoId":1,"quantidade":1}]}
```

**Pós-condições de sucesso:** pedido e itens persistidos, estoque reservado somente na unidade escolhida, pagamento aprovado associado ao pedido e eventos auditados. O pagamento aprovado muda para preparo; a entrega depende de chamadas operacionais posteriores.

| Alternativa/exceção | Resultado e preservação de dados |
|---|---|
| Entrada inválida / token ausente / perfil indevido | 400 ou 422 / 401 / 403; operação rejeitada |
| Referência inexistente | 404 |
| Saldo insuficiente ou conflito de estado | 409; reserva parcial da criação é revertida |
| `RECUSADO` | 200, `sucesso: false`; pedido aguarda pagamento, reserva permanece; nova tentativa permitida |
| `INDISPONIVEL` / `TIMEOUT` | 503 / 504; auditoria da falha confirmada, pedido/estoque/pagamento anterior preservados |
| Correlação incompatível | 502 e rollback da transação |
| Tentativa após aprovação/cancelamento | 409; não registra segunda aprovação |
| Cancelar antes do pagamento | COZINHA/ADMIN envia `CANCELADO`; devolução de estoque e auditoria transacionais, uma única vez |
| Falha inesperada | Envelope padronizado sem stack trace; não prometer sucesso ao consumidor |

O pagamento guarda a última tentativa no registro único do pedido; a auditoria conserva eventos. Uma recusa não libera saldo. Não há expiração automática de reservas nem cancelamento de pedido pago. Para erros detalhados, exemplos e permissões de cada operação, use o contrato [OpenAPI](../src/api/docs/swagger.json) e a [matriz T01–T32](04_PLANO_DE_TESTES.md).

## Segurança, privacidade e limites operacionais

JWT usa HS256, emissor e audiência validados, e validade configurável. Senhas são armazenadas com bcrypt. A API restringe perfis e propriedade de pedidos, limita JSON a 100 KB, padroniza erros e retorna `Cache-Control: no-store` nas rotas `/api`. CORS está habilitado genericamente; não há rate limiting ou infraestrutura de produção configurada.

O [registro da etapa 3](03_PRIVACIDADE_E_SEGURANCA.md) detalha inventário de dados, consentimento opcional, minimização, retenção proposta e consulta de auditoria exclusiva do ADMIN. Esses mecanismos não implementam pontuação de fidelidade nem expurgo automático.

## Backup, restauração e recuperação local

Procedimento para a configuração padrão, sem apagar a base original:

1. Encerrar a API com Ctrl+C e garantir que nenhum outro processo esteja escrevendo na base.
2. Criar uma pasta de backup e copiar `prisma/dev.db` para um arquivo com data. Caso existam arquivos WAL/journal ativos, não copiar apenas o arquivo principal; usar a API de backup do SQLite, como no verificador operacional.
3. Para verificar uma restauração, copiar o backup para **outro arquivo**, por exemplo `prisma/restauracao-teste.db`. Configurar temporariamente `DATABASE_URL="file:./restauracao-teste.db"` e outra porta, mantendo o mesmo schema/migrações da cópia.
4. Iniciar a API, fazer login e confirmar os pedidos esperados, estados e totais. Executar `PRAGMA integrity_check` com uma ferramenta SQLite; resultado esperado `ok`.
5. Encerrar a instância de teste e restaurar a configuração original. Não sobrescrever a base ativa durante a validação.

O [verificador operacional](../scripts/verify_operations.py) automatiza criação por migrações e seed em `.test-output`, criação de pedido, término do processo, reinício, backup via `sqlite3.Connection.backup`, restauração em outro arquivo e consulta HTTP. Ele não lê nem modifica `prisma/dev.db`. Mantém as cópias isoladas para inspeção; `.test-output/` está ignorado pelo Git. Backup local no mesmo disco é uma demonstração técnica, não proteção contra perda do dispositivo.

## Validação e diagnóstico

```powershell
npm run build
npm test
python scripts/validate_docs.py
python scripts/verify_operations.py
```

O último comando requer Python e dependências Node já instaladas; leva pelo menos 60 segundos. Compila, migra e semeia sua própria base. Mede 20 clientes concorrentes sem pausa, por 60 segundos, alternando consultas públicas de produtos/unidades, em loopback. Registra latência p95 de todas as tentativas, erros de transporte/HTTP e contagem de 5xx. Não mede pagamento, consultas autenticadas, acesso remoto ou comportamento de produção. O encerramento forçado verifica recuperação de processo; não simula queda elétrica durante gravação nem comprova o caminho de desligamento gracioso.

Resultados atuais: [relatório operacional JSON](06_RESULTADOS_OPERACIONAIS.json). A suíte funcional anterior tem [36 testes aprovados](04_RESULTADO_VALIDACAO.md); a etapa 6 não altera regras da API. A verificação de instalação a partir de cópia limpa continua prevista para a etapa 8.

### Resultado inicial da medição em 11/10/2026 (histórico)

Ambiente: Windows 11, Node v24.19.0, 12 processadores lógicos; identificação de CPU no JSON. A janela de disparo foi de 60 segundos, com conclusão das requisições em andamento em 64,61 segundos. O cliente encerra cada requisição após 10 segundos.

| Indicador | Resultado |
|---|---|
| Tentativas HTTP | 151 |
| p95, incluindo tentativas que falharam | 10.015,89 ms |
| Erros totais | 138 (91,39%) |
| Respostas HTTP 5xx | 59 (39,07% das tentativas) |
| Outros erros de transporte/HTTP | 79; esta execução não classificou individualmente os demais erros |
| Meta RNF09 | **Não atingida** (p95 ≤ 1 s e menos de 1% de 5xx) |
| Pedido preservado após reinício | Aprovado |
| Pedido preservado após restauração | Aprovado |
| Integridade do banco restaurado | `ok` |

O resultado inicial foi preservado em [JSON histórico](06_RESULTADOS_OPERACIONAIS_ANTES.json). A pendência motivou a correção descrita abaixo.

### Resultado após correção — RNF09 atendido no ensaio local

As consultas públicas passaram a usar transações em lote para página e contagem; o gerador de carga passou a reutilizar uma conexão HTTP por cliente. Com 20 clientes durante 60 segundos (60,26 s incluindo conclusão das requisições), foram realizadas **48.486 consultas**, com **zero erros**, **zero 5xx** e **p95 de 39,37 ms**. A meta de p95 ≤ 1 s e menos de 1% de 5xx foi atingida. Reinício, restauração e integridade SQLite também passaram.

A suíte atual contém **37 testes aprovados**, incluindo regressão de 20 consultas simultâneas. Consulte [correção e metodologia](06_CORRECAO_DESEMPENHO.md). A mudança também no cliente de carga impede atribuir todo o ganho exclusivamente à API. O resultado se limita às consultas públicas, em loopback e base de demonstração.

| Sintoma | Verificação / ação |
|---|---|
| Node ou npm não encontrado | Instalar Node 20+ com npm e reabrir o terminal; no computador atual usar `iniciar.ps1` |
| Segredo/validade JWT inválido | Revisar `.env`; setup não substitui configuração existente |
| Porta ocupada (`EADDRINUSE`) | Encerrar a instância anterior ou escolher outra `PORT` |
| Tabela ausente / cliente Prisma desatualizado | Executar `npm run prisma:generate` e `npm run prisma:deploy` |
| Login retorna 401 | Conferir seed e credenciais; gerar novo token se expirado |
| HTTP 403 | Conferir perfil e proprietário; não alterar titular no body |
| Estoque retorna 409 | Consultar saldo; seed não repõe estoque consumido; cancelar pedido não pago autorizado |
| Banco bloqueado | Evitar múltiplas instâncias gravando na mesma base; encerrar processos de teste e tentar novamente |

## Rastreabilidade e andamento

O [mapa atualizado](06_RASTREABILIDADE.md) relaciona todos os RF/RNF a código e evidências. Etapas 1–6 concluídas no escopo local descrito. Restam etapa 7 (documento acadêmico com identificação real e revisão das orientações do AVA) e etapa 8 (repositório público, histórico verdadeiro e validação em cópia limpa).
