# Rastreabilidade da implementação Java

Atualizado em 12/10/2026. Este mapa descreve o estado atual em `src/main/java` e `src/test/java`; os requisitos conceituais da etapa 1 permanecem em [01_REQUISITOS_E_ESCOPO.md](01_REQUISITOS_E_ESCOPO.md). “Proposto” significa que não há funcionalidade executável correspondente.

## Requisitos funcionais

| RF | Estado | Implementação Java | Evidência automatizada |
|---|---|---|---|
| RF01 autenticar | Implementado | `ApiController`, `RaizesService`, `JwtService`, `AuthenticationFilter` | `loginAndProtectedEndpointsReturnConsistentErrors` |
| RF02 autorizar | Implementado | `AuthenticationFilter`, `ApiController` | `loginAndProtectedEndpointsReturnConsistentErrors`; `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF03 cadastrar cliente | Proposto | Não há endpoint de cadastro público | Sem teste de cadastro |
| RF04 unidades | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `catalogPaginationAndStockAreReturned`; `twentyConcurrentCatalogRequestsSucceed` |
| RF05 cardápio e estoque por unidade | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `catalogPaginationAndStockAreReturned` |
| RF06 criar pedido | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `orderValidationAndRepeatedProductsUseAggregatedQuantity` |
| RF07 canal do pedido | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `orderValidationAndRepeatedProductsUseAggregatedQuantity` |
| RF08 reserva transacional de estoque | Implementado | `RaizesStore` | `failedOrderRollsBackReservationsAndCancellationReturnsStockOnce`; `concurrentOrderRequestsCannotReserveTheLastUnitTwice` |
| RF09 listar pedidos | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `orderPaymentAndStatusTransitionsPreserveOwnershipRules`; `attendantCanCreateAndListWhileKitchenCannotCreate`; `operationalUsersAreRestrictedToTheirAssignedUnit` |
| RF10 pagamento mock | Implementado | `PaymentGateway`, `MockPaymentGateway`, `ApiController`, `RaizesService`, `RaizesStore` | `MockPaymentGatewayTest`; `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF11 recusa, falha e repetição | Implementado | `MockPaymentGateway`, `RaizesStore` | `technicalPaymentFailuresPreserveOrderStockAndPreviousPayment`; `concurrentPaymentOnlyTransitionsTheOrderOnce` |
| RF12 atualizar status | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `orderPaymentAndStatusTransitionsPreserveOwnershipRules` |
| RF13 cancelar pedido não pago | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `failedOrderRollsBackReservationsAndCancellationReturnsStockOnce` |
| RF14 auditoria | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `consentIsIdempotentAndAuditIsRestrictedToAdministrators`; `auditProjectionHidesUnapprovedFieldsAndMalformedPayloads` |
| RF15 consentimento opcional | Implementado | `ApiController`, `RaizesService`, `RaizesStore`, tabela `consentimentos` | `consentIsIdempotentAndAuditIsRestrictedToAdministrators`; `consentAuditFailureRollsBackTheConsentEvent` |
| RF16 paginar produtos/unidades | Implementado | `ApiController`, `RaizesService`, `RaizesStore` | `catalogPaginationAndStockAreReturned`; `twentyConcurrentCatalogRequestsSucceed` |
| RF17 gestão de catálogo | Proposto | Sem endpoints de CRUD | Sem teste |
| RF18 ajuste administrativo de estoque | Proposto | Sem endpoint de movimentação administrativa | Sem teste |
| RF19 pontos de fidelidade | Proposto | Consentimento demonstrativo; sem pontos/resgate | Sem teste |
| RF20 promoções | Proposto | Sem campanhas/descontos | Sem teste |

## Requisitos não funcionais

| RNF | Evidência e limite |
|---|---|
| RNF01 responsabilidades | API, autenticação, armazenamento e gateway mock em componentes Java distintos; ver [manual técnico](06_DOCUMENTACAO_TECNICA.md). |
| RNF02 integridade | SQLite e migrações Flyway V1/V2; testes de rollback, seed idempotente e concorrência. DatabaseMigrationTest verifica base nova, migração repetível e preservação de usuário existente em banco temporário. |
| RNF03 credenciais | JWT, BCrypt, segredo externo e seed controlável; não publicar tokens, senhas ou banco local. |
| RNF04 privacidade | [Inventário e limites](03_PRIVACIDADE_E_SEGURANCA.md); não é certificação jurídica e não há expurgo automático. |
| RNF05 erros | Envelope JSON uniforme, validação e limites de corpo; coberto pelos testes de API. |
| RNF06 OpenAPI | `/api-docs.json` e Swagger em `/api-docs`; smoke test está na integração da aplicação. |
| RNF07 instalação | JDK 21/Maven. Nesta revisão, `mvn test` concluiu com sucesso; não foi feita cópia limpa nem teste de inicialização HTTP independente. |
| RNF08 evidências | 21 testes Java/Flyway aprovados nesta revisão; ver [resultado de validação](04_RESULTADO_VALIDACAO.md). Newman não foi repetido; execução anterior 32/88 é histórica. |
| RNF09 desempenho | O ensaio histórico registrou 20 clientes, 60,01 s, 228.399 consultas e p95 11,76 ms; não foi repetido após o isolamento por unidade. |
| RNF10 recuperação | Restauração de backup e reinício após esta alteração não foram ensaiados nesta revisão. |
| RNF11 falha do gateway | Aprovação, recusa, indisponibilidade/timeout simulados e proteção contra repetição cobertas por testes Java. |
| RNF12 rastreabilidade | Este mapa, requisitos, plano de testes e diagramas com limites explicitados. |

## Entregáveis

| Etapa | Situação |
|---|---|
| 1–6 | Requisitos, implementação, privacidade, plano, documentação e desempenho registrados nos arquivos de `docs/`. |
| 7 — PDF acadêmico | Gerado novamente em `output/pdf/4937552_Projeto_Back_End.pdf` (46 páginas, nome/RU preservados, polo/cidade não informados); PDF e Markdown auxiliar permanecem em `output/`, ignorado pelo Git. As legendas identificam que as imagens PNG/SVG precisam ser renderizadas novamente a partir das fontes DOT atualizadas. |
| 8 — publicação remota | Nenhuma alteração desta revisão foi enviada ao GitHub, conforme o registro de versionamento da revisão. |
