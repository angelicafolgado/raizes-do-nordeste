# Etapa 6 — Manual técnico da implementação Java

Atualizado em 11/10/2026. Este manual descreve a implementação executável atual em Java 21. Requisitos, dados e contratos continuam em formatos próprios, como Markdown, JSON e SQL.

## Ambiente e execução

Requisitos: JDK 21 e Maven 3.9 ou superior.

```powershell
$env:JWT_SECRET = 'gere-um-segredo-aleatorio-com-pelo-menos-32-caracteres'
mvn spring-boot:run
```

Configuração padrão: API na porta 3000; banco SQLite `raizes.db`; seed de demonstração ligado. Variáveis:

| Variável | Uso |
|---|---|
| `PORT` | Porta HTTP; padrão `3000`. |
| `SPRING_DATASOURCE_URL` | JDBC URL do SQLite; padrão `jdbc:sqlite:raizes.db`. |
| `JWT_SECRET` | Segredo externo de no mínimo 32 caracteres; não o publique. |
| `JWT_EXPIRES_IN` | Validade do token; padrão `8h`. |
| `SEED_ENABLED` | Controla o seed; padrão `true`. Desative com `false` quando necessário. |

Swagger: `http://localhost:3000/api-docs`; contrato JSON: `/api-docs.json`; health: `/api/health`. O seed cria contas fictícias e não sobrescreve usuários, produtos, unidades, pedidos ou estoque existentes. Use `SEED_ENABLED=false` em ambiente que não deve receber dados de demonstração.

Flyway aplica [V1__initial_schema.sql](../src/main/resources/db/migration/V1__initial_schema.sql) e mantém versões/checksums em flyway_schema_history. Bases anteriores sem histórico usam baseline 0 e V1 idempotente; bases novas começam em V1. Faça backup antes de atualizar bases existentes e não edite migrações já aplicadas. O teste automatizado usa banco separado em target/test-api.db.

## Arquitetura

| Componente | Responsabilidade |
|---|---|
| [ApiController](../src/main/java/br/com/raizesdonordeste/api/ApiController.java) | Rotas REST, autorização por perfil e coordenação dos fluxos de API. |
| [AuthenticationFilter](../src/main/java/br/com/raizesdonordeste/config/AuthenticationFilter.java) e [JwtService](../src/main/java/br/com/raizesdonordeste/config/JwtService.java) | Validação de tokens e identidade da requisição. |
| [RaizesStore](../src/main/java/br/com/raizesdonordeste/infrastructure/RaizesStore.java) | Persistência JDBC, consultas e operações transacionais sobre SQLite. |
| [PaymentGateway](../src/main/java/br/com/raizesdonordeste/application/payments/PaymentGateway.java) | Contrato Java de solicitação e resposta de pagamento. |
| [MockPaymentGateway](../src/main/java/br/com/raizesdonordeste/infrastructure/payments/MockPaymentGateway.java) | Simulação local de aprovação, recusa, indisponibilidade e timeout. |
| [SeedRunner](../src/main/java/br/com/raizesdonordeste/config/SeedRunner.java) | Inserção idempotente dos dados fictícios, quando habilitada. |
| [AcademicPdfGenerator](../src/main/java/br/com/raizesdonordeste/tools/AcademicPdfGenerator.java) e [LoadTest](../src/main/java/br/com/raizesdonordeste/tools/LoadTest.java) | Geração do relatório acadêmico e ensaio concorrente de leituras. |

O código utiliza JDBC e estruturas de resposta do Spring; não há hierarquia de entidades Java persistidas por ORM. SQLite permite um escritor por vez. A aplicação serializa transações de escrita no processo atual, mas isso não coordena múltiplas instâncias que apontem ao mesmo arquivo.

## Fluxo crítico: pedido, pagamento e status

1. Autenticar em `POST /api/auth/login`; a API retorna JWT após validar senha BCrypt.
2. Consultar unidades/produtos e estoque antes de criar o pedido.
3. Enviar `POST /api/pedidos` com unidade, canal e itens; titular vem do token e os preços vêm do banco.
4. A transação valida referências e saldo, reserva estoque, persiste pedido/itens e registra auditoria. Falha reverte as alterações.
5. Enviar `POST /api/pedidos/{id}/pagamento`. A implementação valida titularidade/estado e chama o gateway mock.
6. Aprovação grava o pagamento e muda o pedido para `EM_PREPARO`; recusa preserva `AGUARDANDO_PAGAMENTO` e permite nova tentativa.
7. COZINHA ou ADMIN pode avançar `EM_PREPARO → PRONTO → ENTREGUE`. Pedido ainda não pago pode ser cancelado, com devolução transacional do estoque.

O exemplo de criação é `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":2}]}`. Canais aceitos: `APP`, `TOTEM`, `BALCAO`, `PICKUP` e `WEB`. Itens repetidos são agregados. `INDISPONIVEL` e `TIMEOUT` são resultados simulados imediatos, sem cobrança externa ou espera de rede.

## Rotas e permissões

| Método e rota | Acesso |
|---|---|
| `GET /api/health` | Público |
| `POST /api/auth/login` | Público |
| `GET /api/unidades?page=1&limit=10` | Público |
| `GET /api/produtos?unidadeId=1&page=1&limit=10` | Público |
| `POST /api/pedidos` | CLIENTE, ATENDENTE ou ADMIN |
| `GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10` | JWT; CLIENTE vê apenas os próprios pedidos |
| `POST /api/pedidos/{id}/pagamento` | Proprietário, ATENDENTE ou ADMIN |
| `PATCH /api/pedidos/{id}/status` | COZINHA ou ADMIN |
| `GET /api/privacidade/aviso` | Público |
| `PUT /api/privacidade/consentimentos/fidelidade` | JWT; altera decisão do titular |
| `GET /api/privacidade/consentimentos` | JWT; consulta histórico do titular |
| `GET /api/auditoria?acao=CONSULTA_PEDIDOS` | ADMIN |

Listagens retornam `{ "dados": [], "paginacao": { ... } }`. Paginação usa padrão 1/10 e limite máximo 100. Entradas inválidas recebem envelope JSON de erro; respostas `/api` usam `Cache-Control: no-store`.

## Privacidade e segurança

As contas de demonstração são fictícias e usam senha `123456`; não use essas credenciais em produção. Configure segredo JWT aleatório, restrinja o acesso a banco e segredos e desative o seed em ambiente publicado. O consentimento de fidelidade é opcional e demonstrativo, não ativa pontos ou marketing. Retenção/anonimização automatizadas, atendimento operacional de direitos, cifragem do arquivo SQLite e integração financeira real não fazem parte desta implementação.

## Build, testes e ferramentas

```powershell
mvn test
mvn package
```

A suíte atual contém 16 testes de integração, 2 do gateway e 2 de migrações, executados com SQLite isolado. Ela verifica autenticação, pedidos, estoque, pagamentos, transições, privacidade, auditoria, rollback, seed e concorrência. A coleção Postman é executada separadamente e passou com 32 requisições/88 asserções na API Java.

Para gerar o PDF e o Markdown:

```powershell
$pdfArgs = @(
  '-q',
  '-Dexec.mainClass=br.com.raizesdonordeste.tools.AcademicPdfGenerator',
  'compile',
  'exec:java'
)
mvn.cmd @pdfArgs
```

Para repetir o ensaio com 20 clientes por 60 segundos, inicie a API em uma base isolada; em outro terminal:

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

Consulte o [resultado do ensaio](06_RESULTADOS_OPERACIONAIS.json) para as métricas e os limites medidos. O teste publicado é de leituras locais; não mede escrita, recuperação, múltiplas instâncias ou disponibilidade de produção. Reinício local com preservação de pedido/pagamento foi confirmado nesta revisão; restauração de backup permanece sem ensaio.
