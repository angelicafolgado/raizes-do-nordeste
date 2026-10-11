# Rastreabilidade da implementação Java

Atualizado em 11/10/2026. Este mapa descreve o estado atual em `src/main/java` e `src/test/java`; os requisitos conceituais da etapa 1 permanecem em [01_REQUISITOS_E_ESCOPO.md](01_REQUISITOS_E_ESCOPO.md). “Proposto” significa que não há funcionalidade executável correspondente.

## Requisitos funcionais

| RF | Estado | Implementação Java | Evidência automatizada |
|---|---|---|---|
| RF01 autenticar | Implementado | `ApiController`, `JwtService`, `AuthenticationFilter` | `loginAndProtectedEndpointsReturnConsistentErrors` |
| RF02 autorizar | Implementado | `AuthenticationFilter`, `ApiController` | `loginAndProtectedEndpointsReturnConsistentErrors`; `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF03 cadastrar cliente | Proposto | Não há endpoint de cadastro público | Sem teste de cadastro |
| RF04 unidades | Implementado | `ApiController`, `RaizesStore` | `catalogPaginationAndStockAreReturned`; `twentyConcurrentCatalogRequestsSucceed` |
| RF05 cardápio e estoque por unidade | Implementado | `ApiController`, `RaizesStore` | `catalogPaginationAndStockAreReturned` |
| RF06 criar pedido | Implementado | `ApiController`, `RaizesStore` | `orderValidationAndRepeatedProductsUseAggregatedQuantity` |
| RF07 canal do pedido | Implementado | `ApiController`, `RaizesStore` | `orderValidationAndRepeatedProductsUseAggregatedQuantity` |
| RF08 reserva transacional de estoque | Implementado | `RaizesStore` | `failedOrderRollsBackReservationsAndCancellationReturnsStockOnce`; `concurrentOrderRequestsCannotReserveTheLastUnitTwice` |
| RF09 listar pedidos | Implementado | `ApiController`, `RaizesStore` | `orderPaymentAndStatusTransitionsPreserveOwnershipRules`; `attendantCanCreateAndListWhileKitchenCannotCreate` |
| RF10 pagamento mock | Implementado | `PaymentGateway`, `MockPaymentGateway`, `ApiController`, `RaizesStore` | `MockPaymentGatewayTest`; `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF11 recusa, falha e repetição | Implementado | `MockPaymentGateway`, `RaizesStore` | `technicalPaymentFailuresPreserveOrderStockAndPreviousPayment`; `concurrentPaymentOnlyTransitionsTheOrderOnce` |
| RF12 atualizar status | Implementado | `ApiController`, `RaizesStore` | `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF13 cancelar pedido não pago | Implementado | `ApiController`, `RaizesStore` | `failedOrderRollsBackReservationsAndCancellationReturnsStockOnce` |
| RF14 auditoria | Implementado | `ApiController`, `RaizesStore` | `consentIsIdempotentAndAuditIsRestrictedToAdministrators`; `auditProjectionHidesUnapprovedFieldsAndMalformedPayloads` |
| RF15 consentimento opcional | Implementado | `ApiController`, `RaizesStore`, tabela `consentimentos` | `consentIsIdempotentAndAuditIsRestrictedToAdministrators`; `consentAuditFailureRollsBackTheConsentEvent` |
| RF16 paginar produtos/unidades | Implementado | `ApiController`, `RaizesStore` | `catalogPaginationAndStockAreReturned`; `twentyConcurrentCatalogRequestsSucceed` |
| RF17 gestão de catálogo | Proposto | Sem endpoints de CRUD | Sem teste |
| RF18 ajuste administrativo de estoque | Proposto | Sem endpoint de movimentação administrativa | Sem teste |
| RF19 pontos de fidelidade | Proposto | Consentimento demonstrativo; sem pontos/resgate | Sem teste |
| RF20 promoções | Proposto | Sem campanhas/descontos | Sem teste |

## Requisitos não funcionais

| RNF | Evidência e limite |
|---|---|
| RNF01 responsabilidades | API, autenticação, armazenamento e gateway mock em componentes Java distintos; ver [manual técnico](06_DOCUMENTACAO_TECNICA.md). |
| RNF02 integridade | SQLite e `V1__initial_schema.sql`; testes de rollback, seed idempotente e concorrência. Flyway controla versões/checksums, com baseline 0 e V1 idempotente para bases anteriores; DatabaseMigrationTest verifica preservação de registros. |
| RNF03 credenciais | JWT, BCrypt, segredo externo e seed controlável; não publicar tokens, senhas ou banco local. |
| RNF04 privacidade | [Inventário e limites](03_PRIVACIDADE_E_SEGURANCA.md); não é certificação jurídica e não há expurgo automático. |
| RNF05 erros | Envelope JSON uniforme, validação e limites de corpo; coberto pelos testes de API. |
| RNF06 OpenAPI | `/api-docs.json` e Swagger em `/api-docs`; smoke test está na integração da aplicação. |
| RNF07 instalação | JDK 21/Maven; a revisão anterior validou cópia temporária sem `target` ou base anterior. Esta revisão executou instalação, build, testes, migrações, startup e coleção. |
| RNF08 evidências | 20 testes Java/Flyway aprovados; ver [resultado de validação](04_RESULTADO_VALIDACAO.md). Coleção validada com 32 requisições/88 asserções sem falhas. |
| RNF09 desempenho | Meta do ensaio local atingida: 20 clientes, 60,01 s, 228.399 consultas, p95 11,76 ms, 0 erros e 0 respostas 5xx. Ver [método e escopo](06_CORRECAO_DESEMPENHO.md). |
| RNF10 recuperação | Reinício local preservou pedido e pagamento nesta revisão; restauração de backup não foi ensaiada. O JSON antigo de desempenho não representa esse teste de reinício. |
| RNF11 falha do gateway | Aprovação, recusa, indisponibilidade/timeout simulados e proteção contra repetição cobertas por testes Java. |
| RNF12 rastreabilidade | Este mapa, requisitos, plano de testes e diagramas com limites explicitados. |

## Entregáveis

| Etapa | Situação |
|---|---|
| 1–6 | Requisitos, implementação, privacidade, plano, documentação e desempenho registrados nos arquivos de `docs/`. |
| 7 — PDF acadêmico | Gerado em Java, com Angelica Dias Folgado, RU 4937552, A4, sumário paginado e diagramas atuais; contagem final registrada na validação. |
| 8 — cópia limpa anterior | A revisão anterior registrou build/test e PDF em cópia temporária. Nesta revisão, os comandos foram executados no checkout atual com banco de testes isolado. |
| 8 — publicação remota | Publicada em <https://github.com/angelicafolgado/raizes-do-nordeste>. Os commits de `main` estão no nome de Angelica Dias Folgado; o histórico remoto anterior de Lako foi substituído conforme solicitado. A revisão de correções registra evolução real; não foram fabricados commits. |
