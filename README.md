# Raízes do Nordeste

API REST acadêmica para pedidos multicanal, estoque por unidade, pagamento simulado, autenticação por perfil, auditoria e registro opcional de consentimento.

## Java API

- Java 21
- Spring Boot 3.5
- SQLite com JDBC
- JWT HS256 e senhas BCrypt
- OpenAPI/Swagger em `/api-docs`

### Requisitos

JDK 21 e Maven 3.9 ou superior.

### Executar

```powershell
$env:JWT_SECRET = 'gere-um-segredo-aleatorio-com-pelo-menos-32-caracteres'
mvn spring-boot:run
```

O banco local padrão é `raizes.db`; use `SPRING_DATASOURCE_URL` para indicar outro banco SQLite. O seed de demonstração é idempotente e não substitui registros ou estoque existentes. Para não inserir dados de demonstração, defina `SEED_ENABLED=false`. Configure `JWT_SECRET` com pelo menos 32 caracteres aleatórios antes de iniciar.

Para somente compilar e empacotar:

```powershell
mvn -DskipTests package
```

Também é possível usar Maven diretamente:

```powershell
$env:JWT_SECRET = 'gere-um-segredo-aleatorio-com-pelo-menos-32-caracteres'
mvn spring-boot:run
```

Swagger: <http://localhost:3000/api-docs>. A raiz `/` redireciona para o Swagger.

## Contas locais de demonstração

Todas usam a senha `123456`. São contas fictícias para desenvolvimento e testes.

| E-mail | Perfil |
|---|---|
| `maria@cliente.com` | CLIENTE |
| `jose@cliente.com` | CLIENTE |
| `atendente@raizes.com` | ATENDENTE |
| `cozinha@raizes.com` | COZINHA |
| `admin@raizes.com` | ADMIN |

Não use essas credenciais em ambientes publicados. Em produção, configure `SEED_ENABLED=false`, use um `JWT_SECRET` secreto e aleatório e forneça um banco gerenciado.

## Endpoints

| Método e rota | Acesso / finalidade |
|---|---|
| `GET /api/health` | Público: saúde da API |
| `POST /api/auth/login` | Público: autenticação e emissão de JWT |
| `GET /api/unidades?page=1&limit=10` | Público: unidades paginadas |
| `GET /api/produtos?unidadeId=1&page=1&limit=10` | Público: catálogo, estoque por unidade e paginação |
| `POST /api/pedidos` | CLIENTE, ATENDENTE ou ADMIN: cria pedido e reserva estoque |
| `GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10` | Autenticado; CLIENTE consulta somente os próprios pedidos |
| `POST /api/pedidos/{id}/pagamento` | Proprietário do pedido, ATENDENTE ou ADMIN: pagamento simulado |
| `PATCH /api/pedidos/{id}/status` | COZINHA ou ADMIN: preparo, entrega ou cancelamento permitido |
| `GET /api/privacidade/aviso` | Público: aviso sobre a demonstração de fidelidade |
| `PUT /api/privacidade/consentimentos/fidelidade` | JWT: registra aceite ou revogação |
| `GET /api/privacidade/consentimentos` | JWT: consulta estado e histórico próprios |
| `GET /api/auditoria?acao=CONSULTA_PEDIDOS` | ADMIN: consulta metadados de auditoria |
| `GET /api-docs.json` | Contrato OpenAPI |

Criação de pedido:

```json
{
  "unidadeId": 1,
  "canalPedido": "TOTEM",
  "itens": [{ "produtoId": 1, "quantidade": 2 }]
}
```

O usuário é obtido do token; o preço é lido do banco. Canais aceitos: `APP`, `TOTEM`, `BALCAO`, `PICKUP` e `WEB`. Listagens usam `{ "dados": [], "paginacao": { ... } }`, com limite máximo de 100 registros por página. Corpos JSON da API têm limite de 100 KB.

## Regras principais

- Criação do pedido, reserva de estoque e auditoria compartilham uma transação SQLite. Falhas revertem todas as alterações.
- Itens repetidos são agrupados; o saldo é reservado na unidade selecionada.
- `APROVADO` leva o pedido a `EM_PREPARO`. `RECUSADO` mantém o pedido em pagamento e permite nova tentativa.
- `INDISPONIVEL` e `TIMEOUT` simulam, respectivamente, respostas 503 e 504; não há cobrança externa nem espera real.
- A cozinha avança `EM_PREPARO → PRONTO → ENTREGUE`. Um pedido não pago pode ser cancelado uma vez e devolve o estoque.
- Consentimento é opcional, demonstrativo e não ativa pontos ou benefícios reais.

## Build e testes

```powershell
mvn test
mvn package
```

Os testes de integração usam SQLite separado em `target/test-api.db` e cobrem login/JWT, paginação, reserva e rollback, pagamento, transições, privacidade, auditoria, limite do corpo HTTP, OpenAPI, concorrência e 20 consultas simultâneas. A última execução de `mvn test package` passou com 18 testes, sem falhas, erros ou testes ignorados.

### Ensaio operacional de carga

Com a API Java em execução, use o gerador incluído:

```powershell
$argsMaven = @(
  '-q',
  '-Dexec.mainClass=br.com.raizesdonordeste.tools.LoadTest',
  '-Dexec.args=http://127.0.0.1:3000 20 60',
  'compile',
  'exec:java'
)
mvn.cmd @argsMaven
```

Ele executa consultas públicas de produtos e unidades com clientes concorrentes e imprime métricas medidas, classificando qualquer falha. A meta local é p95 abaixo de 1.000 ms, zero erros e menos de 1% de respostas 5xx. Consulte [correção e limites do ensaio](docs/06_CORRECAO_DESEMPENHO.md) e [resultado medido](docs/06_RESULTADOS_OPERACIONAIS.json).

### Gerar o PDF acadêmico

Com Maven e JDK 21 instalados, gere o PDF identificado pelo RU:

```powershell
$pdfArgs = @(
  '-q',
  '-Dexec.mainClass=br.com.raizesdonordeste.tools.AcademicPdfGenerator',
  'compile',
  'exec:java'
)
mvn.cmd @pdfArgs
```

O arquivo é criado em `output/pdf/4937552_Projeto_Back_End.pdf`, acompanhado de uma versão textual Markdown. O documento identifica a autora e o RU, registra evidências medidas e declara os limites atuais do ensaio.

## Linguagem e escopo

A API, os testes automatizados e as ferramentas de execução, carga e geração do PDF são implementados em Java 21. O código legado TypeScript/Node.js e os utilitários Python/JavaScript foram removidos após a substituição pelo projeto Java. Arquivos de documentação, configuração, SQL e a coleção Postman continuam em seus formatos próprios; a coleção Postman não é executada por `mvn test` e ainda precisa de validação manual contra esta revisão.

O roteiro do AVA fornecido para a entrega é uma cópia adaptada para estudo; requisitos acadêmicos devem ser conferidos com o roteiro oficial disponibilizado no AVA.
