# Raízes do Nordeste — API REST

Implementação em TypeScript do arquivo `Estrutura Global do Projeto (4 Cama.txt`, diretamente nesta pasta. Usa Express, Prisma 5 e SQLite. Inclui JWT, perfis de acesso, estoque por unidade, pedidos multicanal, pagamento simulado, auditoria, Swagger e coleção Postman.

Repositório público: [github.com/angelicafolgado/raizes-do-nordeste](https://github.com/angelicafolgado/raizes-do-nordeste)

## Iniciar neste Windows

Abra o PowerShell nesta pasta e execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\iniciar.ps1
```

O script procura Node.js instalado ou o runtime local disponível no computador, instala dependências quando necessário, cria `.env` com segredo aleatório, aplica migrações, carrega dados de demonstração, compila e inicia a API. A opção `-SomentePreparar` executa a preparação sem iniciar o servidor. A política de execução é alterada apenas para esse processo.

Abra **http://localhost:3000/api-docs**. A raiz `/` também redireciona ao Swagger. Use **Ctrl+C** no terminal para encerrar. Se alterar `PORT` no `.env`, use a nova porta.

## Instalação convencional

Requisito: Node.js 20 ou superior com npm.

```bash
npm install
npm run setup
npm run dev
```

Para executar o código compilado:

```bash
npm run build
npm start
```

Também é possível usar `pnpm install --frozen-lockfile`, seguido de `pnpm run setup`. O `pnpm-lock.yaml` registra as versões validadas. `setup` preserva um `.env` existente e o seed não apaga pedidos, usuários ou estoques existentes. Um `.env` copiado manualmente do exemplo precisa de um `JWT_SECRET` aleatório de pelo menos 32 caracteres.

O banco de desenvolvimento fica em `prisma/dev.db`. Migrações estão versionadas em `prisma/migrations`; `npm run prisma:deploy` aplica as existentes e `npm run prisma:migrate` cria uma nova durante o desenvolvimento.

## Contas de demonstração

Todas usam a senha **123456**, armazenada com bcrypt. São dados fictícios para uso local; o seed se recusa a rodar com `NODE_ENV=production`.

| E-mail | Perfil |
|---|---|
| maria@cliente.com | CLIENTE |
| jose@cliente.com | CLIENTE |
| atendente@raizes.com | ATENDENTE |
| cozinha@raizes.com | COZINHA |
| admin@raizes.com | ADMIN |

As unidades de Recife e João Pessoa têm estoques independentes para baião de dois, tapioca e suco de cajá.

## Endpoints

| Método e rota | Acesso / finalidade |
|---|---|
| `GET /api/health` | Público: disponibilidade da API |
| `GET /api/privacidade/aviso` | Público: aviso opcional de demonstração |
| `GET /api/privacidade/consentimentos?page=1&limit=10` | JWT: estado e histórico do próprio titular |
| `PUT /api/privacidade/consentimentos/fidelidade` | JWT: aceitar com versão ou revogar |
| `GET /api/auditoria?acao=CONSULTA_PEDIDOS&page=1&limit=10` | ADMIN: metadados de auditoria |
| `POST /api/auth/login` | Público: e-mail e senha, retorna JWT |
| `GET /api/unidades?page=1&limit=10` | Público: unidades disponíveis, paginadas |
| `GET /api/produtos?unidadeId=1&page=1&limit=10` | Público: cardápio e estoque paginados; filtro opcional |
| `POST /api/pedidos` | CLIENTE, ATENDENTE ou ADMIN |
| `GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10` | Autenticado; cliente vê somente seus pedidos |
| `POST /api/pedidos/:id/pagamento` | CLIENTE proprietário, ATENDENTE ou ADMIN |
| `PATCH /api/pedidos/:id/status` | COZINHA ou ADMIN |
| `GET /api-docs` | Swagger interativo |
| `GET /api-docs.json` | Contrato OpenAPI |

No Swagger, execute o login, copie `accessToken` e use **Authorize**. O corpo para criar um pedido é:

```json
{
  "unidadeId": 1,
  "canalPedido": "TOTEM",
  "itens": [{ "produtoId": 1, "quantidade": 2 }]
}
```

O usuário do pedido vem do JWT, e os preços vêm do banco. Canais aceitos: `APP`, `TOTEM`, `BALCAO`, `PICKUP`, `WEB`. IDs e quantidades precisam ser números inteiros positivos. A listagem aceita até 100 itens por página.

Produtos e unidades agora retornam `{ "dados": [...], "paginacao": { "pagina": 1, "limite": 10, "total": 3, "totalPaginas": 1 } }`, em vez de um array na raiz. O padrão é `page=1&limit=10`, com limite máximo de 100. Consumidores antigos devem ler `resposta.dados`. Páginas além do total retornam `dados: []` e parâmetros inválidos retornam 422.

## Regras do fluxo

- Criar pedido reserva estoque na unidade selecionada. Itens repetidos são agrupados; uma falha reverte toda a transação, inclusive auditoria.
- `{"statusSimulado":"APROVADO"}` avança de `AGUARDANDO_PAGAMENTO` para `EM_PREPARO`. Não há cobrança real.
- `RECUSADO` mantém o pedido aguardando pagamento e permite nova tentativa. A tabela de pagamentos guarda a última tentativa; a auditoria registra cada tentativa.
- `INDISPONIVEL` retorna 503 (`GATEWAY_INDISPONIVEL`) e `TIMEOUT` retorna 504 (`GATEWAY_TIMEOUT`), preservando pedido, reserva e eventual pagamento recusado anterior. A falha fica na auditoria com ID de correlação. Ambos são resultados simulados imediatos, sem chamada externa nem espera real.
- A cozinha avança `EM_PREPARO → PRONTO → ENTREGUE`. Não são permitidos saltos ou regressões.
- Um pedido em `AGUARDANDO_PAGAMENTO` pode ir para `CANCELADO`, devolvendo estoque uma única vez. Pedidos pagos não podem ser cancelados neste escopo: não há integração de estorno.
- Reservas não expiram automaticamente. Pedidos abandonados devem ser cancelados por COZINHA ou ADMIN.

Erros têm `error`, `message`, `details`, `timestamp`, `path` e `requestId`, com os status HTTP documentados no Swagger.

## Arquitetura

```text
src/domain/          Entidades, enums e erros; sem dependências externas
src/application/     Casos de uso, DTOs, validações e interfaces de persistência/segurança
src/infrastructure/  Prisma, repositório, bcrypt, JWT e auditoria
src/api/             Controllers, rotas, middlewares e OpenAPI
src/composition.ts   Composição das dependências
prisma/              Schema, migração inicial e seed idempotente
postman/             Coleção executável em sequência
tests/               Testes HTTP com banco SQLite isolado
```

Aplicação e domínio não importam Express ou Prisma. O repositório implementa as interfaces da aplicação. As operações de pedido, estoque, pagamento e auditoria compartilham a mesma transação.

No Prisma 5 com SQLite, os enums ficam no domínio TypeScript e os campos persistidos são strings, validados pela aplicação. Os valores monetários seguem o schema `Float` da referência; os totais são calculados com centavos inteiros antes da persistência.

## Validação

```bash
npm test
npm run build
```

Os 37 testes cobrem autenticação, permissões, privacidade entre clientes, estoque por unidade, rollback, itens duplicados, validação de dados, paginação, recusa e nova tentativa de pagamento, falhas técnicas do gateway, contrato e correlação da integração, transições, cancelamento, concorrência, seed idempotente, consentimento, auditoria, 20 consultas públicas simultâneas e execução dos 32 cenários da coleção Postman. Criam um banco temporário exclusivo em `.test-output` e removem o arquivo ao terminar, sem modificar `prisma/dev.db`.

Importe `postman/raizes_nordeste_clean_collection.json` no Postman e execute a coleção inteira em ordem, com uma iteração. São 32 cenários em 9 pastas, com asserções HTTP e de conteúdo, captura automática de tokens de cliente/cozinha/administrador e do ID do pedido. A coleção usa o banco da API em execução e consome uma unidade de estoque por execução bem-sucedida. Veja [Etapa 4 — Plano de testes](docs/04_PLANO_DE_TESTES.md) para pré-condições, entradas, saídas esperadas e evidências, e [resultado da validação](docs/04_RESULTADO_VALIDACAO.md).

Para manter os artefatos de documentação, `python scripts/update_docs.py` gera Swagger, coleção e matriz de testes. `python scripts/validate_docs.py` valida referências, exemplos e consistência dos cenários (ferramenta opcional de manutenção, requer Python e `jsonschema`; não é dependência da API). O Swagger cobre 12 operações com 81 exemplos explícitos de request/response, incluindo erros. Esses exemplos usam dados fictícios; obtenha um token real pelo login ao testar.

## Escopo

O planejamento da conclusão está em [Etapa 1 — Requisitos e escopo](docs/01_REQUISITOS_E_ESCOPO.md), com RF/RNF, prioridades, critérios de aceite e a distinção entre funcionalidades atuais, complementos planejados e propostas conceituais.

Os complementos implementados estão registrados em [Etapa 2 — Complementos da API](docs/02_COMPLEMENTOS_API.md).

O registro opcional de consentimento, a consulta restrita de auditoria e a política de dados estão em [Etapa 3 — Privacidade e segurança](docs/03_PRIVACIDADE_E_SEGURANCA.md). Aceitar exige `{"aceito":true,"versaoAviso":"1.0"}`; revogar usa `{"aceito":false}`. As decisões não bloqueiam pedidos e não ativam um programa real de pontos. O booleano legado do seed não é prova de consentimento. Após atualizar uma instalação existente, aplique as migrações e gere o cliente Prisma (`npm run setup` também faz isso).

Este projeto implementa o backend solicitado; a interface de uso é o Swagger. Consentimento e logs são mecanismos técnicos de suporte à privacidade, não uma declaração de conformidade jurídica completa. Não inclui cadastro público, integração financeira real, estorno ou implantação em produção. O arquivo de referência original foi preservado.

## Diagramas

A [Etapa 5 — Diagramas](docs/05_DIAGRAMAS.md) reúne DER, casos de uso, classes de domínio e sequência do fluxo de pedidos, com imagens PNG, vetores SVG e fontes editáveis.

## Manual técnico e rastreabilidade

A [Etapa 6 — Manual técnico](docs/06_DOCUMENTACAO_TECNICA.md) consolida configuração, arquitetura, a funcionalidade crítica Pedido + Pagamento, exceções, backup/restauração e diagnóstico. O [mapa de rastreabilidade](docs/06_RASTREABILIDADE.md) atualiza a situação dos requisitos e relaciona código e testes. Os [resultados operacionais](docs/06_RESULTADOS_OPERACIONAIS.json) registram medição de consultas concorrentes e recuperação com banco isolado; podem ser reproduzidos com `python scripts/verify_operations.py` após instalar as dependências do projeto.
