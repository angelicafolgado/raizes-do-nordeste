# Rastreabilidade atual — etapa 6

Atualização de 11/10/2026. Substitui o diagnóstico inicial da etapa 1 para fins de situação atual. “Proposto” significa especificação conceitual sem endpoint implementado. Os nomes de testes abaixo identificam cenários reais em [tests/api.test.ts](../tests/api.test.ts); os resultados funcionais estão no [registro da etapa 4](04_RESULTADO_VALIDACAO.md).

## Requisitos funcionais

Os casos de uso citados estão em [src/application/use-cases](../src/application/use-cases); seus nomes correspondem aos arquivos `.ts`.

| RF | Situação | Código / artefato | Teste ou evidência |
|---|---|---|---|
| RF01 autenticar | Implementado | AutenticarUsuarioUseCase; TokenService | login rejeita senha incorreta e dados inválidos; login válido usado na preparação da suíte |
| RF02 autorizar | Implementado | authMiddleware; ProcessarPagamentoMockUseCase | rotas privadas exigem JWT válido e perfil permitido; cliente vê somente seus pedidos e não paga pedido alheio |
| RF03 cadastrar cliente | Proposto | Etapa 1, seção 7 | Sem teste executável; contas originadas no seed |
| RF04 unidades | Implementado | ListarUnidadesUseCase | produtos e unidades paginam com totais, limites e páginas vazias |
| RF05 cardápio por unidade | Implementado | ListarProdutosCardapioUseCase | cardápio público retorna estoque separado por unidade |
| RF06 criar pedido | Implementado | CriarPedidoUseCase | cria pedido, calcula total e reserva somente na unidade escolhida; itens ausentes/nulos/inválidos |
| RF07 canal | Implementado | CriarPedidoUseCase; ListarPedidosUseCase | canal inválido e unidade inexistente; filtro por canal e paginação |
| RF08 estoque transacional | Implementado | CriarPedidoUseCase; PrismaStore | falha no segundo item reverte reserva; pedidos concorrentes não vendem última unidade duas vezes |
| RF09 consultar pedidos | Implementado | ListarPedidosUseCase | isolamento por cliente; filtro por canal e paginação |
| RF10 contrato de pagamento | Implementado | ProcessarPagamentoMockUseCase; PaymentGateway; MockPaymentGateway | caso de uso envia contrato ao gateway e usa seu retorno para aprovar |
| RF11 falhas e repetição | Implementado | ProcessarPagamentoMockUseCase | recusa permite nova tentativa; falhas técnicas preservam pedido; correlação inválida; pagamentos concorrentes |
| RF12 status | Implementado | AtualizarStatusPedidoUseCase | cozinha avança preparo para pronto e entregue sem regressão |
| RF13 cancelamento | Implementado | AtualizarStatusPedidoUseCase | cancelamento devolve estoque exatamente uma vez |
| RF14 auditoria | Implementado | ListarPedidosUseCase; ListarAuditoriaUseCase; AuditLogger | consultas registram ator/escopo/filtros; auditoria exige ADMIN e audita própria consulta |
| RF15 consentimento | Implementado | GerenciarConsentimentoUseCase; tabela Consentimento | aceitar/revogar preserva histórico; versão atual; rollback de auditoria; revogação não bloqueia pedidos |
| RF16 paginação | Implementado | ListarUnidadesUseCase; ListarProdutosCardapioUseCase | totais, limites e páginas vazias |
| RF17 gestão de produtos/unidades | Proposto | Etapa 1, seção 7 | Sem endpoints/testes de administração |
| RF18 ajustes administrativos de estoque | Proposto | Etapa 1, seção 7 | Saída por pedido/devolução existem; ajuste administrativo não implementado |
| RF19 fidelidade | Proposto | Etapa 1, seção 7 | Consentimento existe; pontos e resgate não implementados |
| RF20 promoções | Proposto | Etapa 1, seção 7 | Sem campanhas/descontos executáveis |

## Requisitos não funcionais

| RNF | Situação e evidência |
|---|---|
| RNF01 separação de camadas | Conferência de domínio/aplicação, portas e composição; [arquitetura](06_DOCUMENTACAO_TECNICA.md). |
| RNF02 integridade/migrações | Schema/migrações; testes de rollback, concorrência e seed idempotente; base operacional criada por migrações. |
| RNF03 credenciais/segredos | HashService, TokenService, authMiddleware e `.gitignore`; testes de acesso. Revisão da publicação efetiva permanece na etapa 8. |
| RNF04 ciclo de vida de dados | [Etapa 3](03_PRIVACIDADE_E_SEGURANCA.md): inventário, minimização e política; retenção/anonimização sem expurgo automático. |
| RNF05 validação/erros | errorHandlerMiddleware e validações; testes de JSON malformado, rota desconhecida, IDs inválidos e exceção sem exposição de dados. |
| RNF06 OpenAPI | [swagger.json](../src/api/docs/swagger.json); `scripts/validate_docs.py`: referências e 81 exemplos de 12 operações. |
| RNF07 instalação | README, setup, migrações e inicializador; build e preparação isolada verificados nesta etapa. Instalação de dependências em cópia limpa prevista na etapa 8. |
| RNF08 evidências reproduzíveis | [Plano](04_PLANO_DE_TESTES.md), 36 testes e coleção com 32 cenários; resultado registrado na etapa 4. |
| RNF09 consultas/concorrência | **Atendido no ensaio local após correção.** [20 clientes por 60 s](06_RESULTADOS_OPERACIONAIS.json): 48.486 consultas, p95 39,37 ms, zero erros/5xx. Paginação e testes de disputa preservados; 37 testes aprovados. [Método e limites](06_CORRECAO_DESEMPENHO.md). |
| RNF10 recuperação | Reinício preserva pedido e restauração em outra base mantém ID/total/status consultáveis; integridade SQLite `ok`. [Procedimento](06_DOCUMENTACAO_TECNICA.md) e relatório operacional. Sem promessa de disponibilidade contínua. |
| RNF11 gateway tolerante a falha simulada | MockPaymentGateway e ProcessarPagamentoMockUseCase; testes de indisponibilidade, timeout, correlação e nova tentativa. |
| RNF12 rastreabilidade | Este mapa, plano de testes e diagramas do estado implementado; propostas identificadas separadamente. |

## Entregáveis

| Etapa | Entrega | Situação |
|---|---|---|
| 1 | Requisitos e propostas | Concluída; diagnóstico inicial preservado |
| 2 | Complementos da API | Concluída |
| 3 | Privacidade e segurança | Concluída no escopo documentado |
| 4 | Contratos e plano de testes | Concluída |
| 5 | Quatro diagramas | Concluída |
| 6 | Manual, funcionalidade crítica, rastreabilidade e verificações operacionais | Consolidada nestes documentos e no relatório operacional |
| 7 | PDF acadêmico | Pendente: identificação real e orientações de entrega |
| 8 | Repositório público e revisão final | Pendente: publicação e execução em cópia limpa |
