# Raízes do Nordeste

API REST acadêmica para pedidos multicanal, estoque por unidade, pagamento simulado, autenticação por perfil, auditoria e registro opcional de consentimento.

Repositório público: <https://github.com/angelicafolgado/raizes-do-nordeste>

## Java API

- Java 21
- Spring Boot 3.5
- SQLite com JDBC
- JWT HS256 e senhas BCrypt
- OpenAPI/Swagger em `/api-docs`

### Requisitos

JDK 21 e Maven 3.9 ou superior.

### Configurar e executar

Java 21 (JDK completo, incluindo `javac`) e Maven 3.9 ou superior são necessários. Maven baixa as dependências declaradas no `pom.xml`; a API não depende de Node, npm ou Python.

No Linux/macOS:

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
export SPRING_DATASOURCE_URL="jdbc:sqlite:raizes.db"
export SEED_ENABLED=true
mvn spring-boot:run
```

No PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($bytes)
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlite:raizes.db'
$env:SEED_ENABLED = 'true'
mvn spring-boot:run
```

Use o mesmo segredo local em reinícios quando quiser preservar a validade dos tokens. Nunca o publique. O [.env.example](.env.example) lista as variáveis; Spring **não carrega esse arquivo automaticamente**, portanto exporte os valores no processo que inicia Java. `JWT_SECRET` deve ter pelo menos 32 caracteres aleatórios; o placeholder do exemplo é rejeitado.

| Variável | Padrão / finalidade |
|---|---|
| `PORT` | `3000`, porta da API |
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:raizes.db`, caminho do banco |
| `JWT_SECRET` | Obrigatório; segredo externo |
| `JWT_EXPIRES_IN` | `8h`, validade do JWT |
| `SEED_ENABLED` | `true`, dados fictícios idempotentes; use `false` para não inserir |

### Banco e migrações

Flyway executa as migrações em [src/main/resources/db/migration](src/main/resources/db/migration) antes de usar o banco. Não é preciso criar tabelas manualmente. `V1__initial_schema.sql` cria as nove tabelas de domínio e seus índices; `flyway_schema_history` registra versões/checksums.

Para bases da versão anterior sem histórico Flyway, o baseline `0` permite executar V1 sem apagar registros, pois as criações são idempotentes. Faça backup consistente antes de atualizar uma base existente, confirme que o esquema corresponde à versão anterior e teste a atualização em uma cópia. Bases com divergências de estrutura precisam de avaliação específica; não são corrigidas automaticamente por V1.

Para evoluir o banco, acrescente uma nova migração `V2__descricao.sql`, mantendo V1 intacta depois de aplicada. Checksums são validados no início; não use `clean` ou `repair` para contornar divergências sem diagnóstico. O seed não substitui contas, catálogo ou saldo existentes e não repõe estoque consumido.

Swagger: <http://localhost:3000/api-docs>; contrato: `/api-docs.json`; saúde: `/api/health`. A raiz `/` redireciona ao Swagger. O contrato servido contém schemas, campos obrigatórios, exemplos, permissões e respostas de erro. Sua fonte é [openapi-contract.json](src/main/resources/openapi-contract.json), aplicada às rotas descobertas pelo Spring.

Para compilar e empacotar:

```bash
mvn package
```

Depois execute `java -jar target/raizes-do-nordeste-api-1.0.0.jar` com as variáveis exportadas. Compilação e testes usam um banco separado e não exigem o segredo da API local.

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

Os testes de integração usam SQLite separado em `target/test-api.db` e cobrem login/JWT, paginação, reserva e rollback, pagamento, transições, privacidade, auditoria, limite do corpo HTTP, OpenAPI, concorrência e 20 consultas simultâneas. A suíte contém 20 testes: 16 de integração, 2 do gateway e 2 de migração de banco. A validação de 11/10/2026 passou sem falhas, erros ou testes ignorados. Os testes de migração verificam base nova, repetibilidade e preservação de registros de uma base existente.

### Coleção Postman/Insomnia

A [coleção com 32 cenários](postman/raizes_nordeste_clean_collection.json) está organizada em 9 pastas. Consulte o [plano T01–T32](docs/04_PLANO_DE_TESTES.md) para pré-condições, entradas, saídas e evidências.

1. Inicie a API com seed em uma base de demonstração separada. Use um caminho SQLite novo para cada avaliação repetida, preservando bases existentes.
2. Importe a coleção no Postman e configure `baseUrl` (padrão `http://localhost:3000`).
3. Execute todas as pastas na ordem apresentada. Os logins capturam `token_cliente`, `token_cozinha` e `token_admin`; T05 captura `pedidoId`. Não é preciso preencher tokens manualmente.
4. O fluxo cria, paga e entrega um pedido, consumindo uma unidade do produto 1. O seed não repõe estoque. Não exporte tokens preenchidos.

Opcionalmente, com Node/npm disponíveis **somente para o runner externo**:

```bash
npx --yes newman@6.2.1 run postman/raizes_nordeste_clean_collection.json --env-var baseUrl=http://127.0.0.1:3000
```

Resultado em 11/10/2026: **32 requisições, 88 asserções, zero falhas**, com 17 cenários 2xx e 15 de erro. A coleção é executada separadamente de `mvn test`. [Evidências e limites](docs/04_RESULTADO_VALIDACAO.md).

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

O arquivo é criado em `output/pdf/4937552_Projeto_Back_End.pdf`, acompanhado de uma versão textual Markdown. O documento identifica a autora e o RU, contém diagramas atuais, sumário paginado e evidências. Usa A4, margens esquerda/superior de 3 cm, direita/inferior de 2 cm e fonte Liberation Serif incorporada, tamanho 12 no corpo. Curso, polo, semestre e docente permanecem “a informar” por orientação da autora; conferir as normas efetivas do AVA antes da entrega.

## Linguagem e escopo

A API, os testes automatizados e as ferramentas de execução, carga e geração do PDF são implementados em Java 21. O código legado TypeScript/Node.js e os utilitários Python/JavaScript foram removidos após a substituição pelo projeto Java. Arquivos de documentação, configuração, SQL e a coleção Postman continuam em seus formatos próprios; a coleção Postman é executada separadamente de `mvn test` e foi validada com Newman nesta revisão.

O roteiro do AVA fornecido para a entrega é uma cópia adaptada para estudo; requisitos acadêmicos devem ser conferidos com o roteiro oficial disponibilizado no AVA.
