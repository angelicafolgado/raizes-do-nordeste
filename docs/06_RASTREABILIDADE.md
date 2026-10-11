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
| RNF02 integridade | SQLite e `schema.sql`; testes de rollback, seed idempotente e concorrência. O inicializador SQL não migra tabelas existentes. |
| RNF03 credenciais | JWT, BCrypt, segredo externo e seed controlável; não publicar tokens, senhas ou banco local. |
| RNF04 privacidade | [Inventário e limites](03_PRIVACIDADE_E_SEGURANCA.md); não é certificação jurídica e não há expurgo automático. |
| RNF05 erros | Envelope JSON uniforme, validação e limites de corpo; coberto pelos testes de API. |
| RNF06 OpenAPI | `/api-docs.json` e Swagger em `/api-docs`; smoke test está na integração da aplicação. |
| RNF07 instalação | JDK 21/Maven; build/test em cópia temporária sem `target` ou base anterior, usando dependências já disponíveis no Maven local. |
| RNF08 evidências | 18 testes Java aprovados; ver [resultado de validação](04_RESULTADO_VALIDACAO.md). A coleção Postman não foi revalidada. |
| RNF09 desempenho | Meta do ensaio local atingida: 20 clientes, 60,01 s, 228.399 consultas, p95 11,76 ms, 0 erros e 0 respostas 5xx. Ver [método e escopo](06_CORRECAO_DESEMPENHO.md). |
| RNF10 recuperação | Reinício e restauração não foram medidos nesta revisão; campos correspondentes no JSON estão `null`. |
| RNF11 falha do gateway | Aprovação, recusa, indisponibilidade/timeout simulados e proteção contra repetição cobertas por testes Java. |
| RNF12 rastreabilidade | Este mapa, requisitos, plano de testes e diagramas com limites explicitados. |

## Entregáveis

| Etapa | Situação |
|---|---|
| 1–6 | Requisitos, implementação, privacidade, plano, documentação e desempenho registrados nos arquivos de `docs/`. |
| 7 — PDF acadêmico | Gerado em Java, com Angelica Dias Folgado, RU 4937552, 10 páginas e validação textual. |
| 8 — cópia limpa | Build/test e geração do PDF passaram em cópia temporária sem artefatos locais herdados. |
| 8 — publicação remota | Publicada em <https://github.com/angelicafolgado/raizes-do-nordeste>. Os commits de `main` estão no nome de Angelica Dias Folgado; o histórico remoto anterior de Lako foi substituído conforme solicitado. O histórico verdadeiro permanece abaixo dos cinco commits recomendados pelo material. |
