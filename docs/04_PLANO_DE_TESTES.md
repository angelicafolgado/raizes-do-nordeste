# Etapa 4 — Plano de testes da API

> **Evidência da coleção:** a execução anterior registrou 32 requisições/88 asserções em 11/10/2026. Ela não foi repetida na revisão de 12/10/2026; não confundir com a execução Maven atual, que não executa a coleção automaticamente.

## Ambiente e execução

Requisitos para a suíte automatizada: JDK 21 e Maven 3.9 ou superior. `mvn test` inicia a aplicação em ambiente de teste com SQLite isolado. A inspeção manual utiliza a API iniciada conforme o [README](../README.md), a variável `baseUrl` configurada no Postman e a coleção `postman/raizes_nordeste_clean_collection.json` importada. As pastas são executadas em ordem, com seed em uma base de demonstração separada.

`baseUrl` é a única variável que precisa de ajuste para outra porta. `token_cliente`, `token_cozinha`, `token_admin` e `pedidoId` são capturados pelas requisições. Todas as contas são fictícias, com senha de demonstração `123456`. Os valores reais de tokens devem permanecer fora da coleção exportada.

A coleção descreve **32 cenários**, **17 planejados com resposta 2xx** e **15 cenários de erro**, em 9 pastas. A execução registrada em 11/10/2026 aprovou esses cenários e 88 asserções; esse resultado é histórico, não retestado nesta revisão. Recusa de pagamento é resultado de negócio respondido com HTTP 200 e tem asserção declarada na coleção.

A validação automatizada da implementação Java utiliza `mvn test`. Na revisão de 12/10/2026 foram aprovados 21 testes (17 de integração, 2 de migração Flyway e 2 do gateway). Eles cobrem fluxos de API, segurança, transações, concorrência e isolamento por unidade, mas não executam automaticamente as 32 requisições desta coleção nem substituem a execução separada no Postman ou Newman.

A coleção manual usa a base da API em execução e consome uma unidade do produto 1 por execução bem-sucedida. O seed preserva saldo e pedidos; ele não repõe estoque. As avaliações repetidas utilizam uma base separada nova, preservando a base existente. IDs 99999 usados nos casos 404 pressupõem o conjunto do seed, sem recursos com esses IDs.

## Matriz de cenários e evidências

| ID | Cenário | Endpoint e método | Pré-condição | Entrada | Saída esperada | Evidência na coleção |
|---|---|---|---|---|---|---|
| T01 | Login cliente | `POST /api/auth/login` | API iniciada; migrations e seed aplicados | `{"email":"maria@cliente.com","senha":"123456"}` | 200; accessToken presente; CLIENTE | 01 Auth/T01 - Login cliente |
| T02 | Login cozinha | `POST /api/auth/login` | API iniciada; migrations e seed aplicados | `{"email":"cozinha@raizes.com","senha":"123456"}` | 200; accessToken presente; COZINHA | 01 Auth/T02 - Login cozinha |
| T03 | Senha incorreta | `POST /api/auth/login` | API iniciada; migrations e seed aplicados | `{"email":"maria@cliente.com","senha":"errada"}` | 401; CREDENCIAIS_INVALIDAS | 01 Auth/T03 - Senha incorreta |
| T04 | Cardápio | `GET /api/produtos` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 200; dados e paginacao; estoque por unidade | 02 Cardápio/T04 - Cardápio |
| T05 | Criar pedido TOTEM | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":1}]}` | 201; id capturado; AGUARDANDO_PAGAMENTO; total 38,50 | 03 Pedidos/T05 - Criar pedido TOTEM |
| T06 | Filtrar pedidos | `GET /api/pedidos?canalPedido=TOTEM&page=1&limit=5` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `Sem body; parâmetros na rota` | 200; canal TOTEM nos resultados | 03 Pedidos/T06 - Filtrar pedidos |
| T07 | Canal inválido | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"INVALIDO","itens":[{"produtoId":1,"quantidade":1}]}` | 400; CANAL_INVALIDO | 03 Pedidos/T07 - Canal inválido |
| T08 | Estoque insuficiente | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":99999}]}` | 409; ESTOQUE_INSUFICIENTE | 03 Pedidos/T08 - Estoque insuficiente |
| T09 | Quantidade inválida | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":0}]}` | 422; PARAMETRO_INVALIDO | 03 Pedidos/T09 - Quantidade inválida |
| T10 | Pagamento recusado | `POST /api/pedidos/{{pedidoId}}/pagamento` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pedido aguardando pagamento | `{"statusSimulado":"RECUSADO"}` | 200; sucesso=false; AGUARDANDO_PAGAMENTO | 04 Pagamento mock/T10 - Pagamento recusado |
| T11 | Gateway indisponível | `POST /api/pedidos/{{pedidoId}}/pagamento` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pedido aguardando pagamento | `{"statusSimulado":"INDISPONIVEL"}` | 503; GATEWAY_INDISPONIVEL | 04 Pagamento mock/T11 - Gateway indisponível |
| T12 | Timeout gateway | `POST /api/pedidos/{{pedidoId}}/pagamento` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pedido aguardando pagamento | `{"statusSimulado":"TIMEOUT"}` | 504; GATEWAY_TIMEOUT | 04 Pagamento mock/T12 - Timeout gateway |
| T13 | Nova tentativa aprovada | `POST /api/pedidos/{{pedidoId}}/pagamento` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pedido aguardando pagamento | `{"statusSimulado":"APROVADO"}` | 200; sucesso=true; EM_PREPARO | 04 Pagamento mock/T13 - Nova tentativa aprovada |
| T14 | Cliente sem permissão | `PATCH /api/pedidos/{{pedidoId}}/status` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pagamento aprovado em T13 | `{"status":"PRONTO"}` | 403; ACESSO_NEGADO | 05 Status e duplicidade/T14 - Cliente sem permissão |
| T15 | Cozinha marca pronto | `PATCH /api/pedidos/{{pedidoId}}/status` | API iniciada; migrations e seed aplicados; {{token_cozinha}} obtido no login anterior; pedidoId capturado em T05; pagamento aprovado em T13 | `{"status":"PRONTO"}` | 200; status=PRONTO | 05 Status e duplicidade/T15 - Cozinha marca pronto |
| T16 | Entrega | `PATCH /api/pedidos/{{pedidoId}}/status` | API iniciada; migrations e seed aplicados; {{token_cozinha}} obtido no login anterior; pedidoId capturado em T05; pedido PRONTO após T15 | `{"status":"ENTREGUE"}` | 200; status=ENTREGUE | 05 Status e duplicidade/T16 - Entrega |
| T17 | Pagamento repetido | `POST /api/pedidos/{{pedidoId}}/pagamento` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; pedidoId capturado em T05; pedido já ENTREGUE | `{"statusSimulado":"APROVADO"}` | 409; ESTADO_INVALIDO | 05 Status e duplicidade/T17 - Pagamento repetido |
| T18 | Sem autenticação | `GET /api/pedidos` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 401; NAO_AUTENTICADO | 06 Acesso e paginação/T18 - Sem autenticação |
| T19 | Produtos paginados | `GET /api/produtos?unidadeId=1&page=2&limit=1` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 200; pagina=2; um produto | 06 Acesso e paginação/T19 - Produtos paginados |
| T20 | Unidades paginadas | `GET /api/unidades?page=1&limit=1` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 200; uma unidade | 06 Acesso e paginação/T20 - Unidades paginadas |
| T21 | Aviso opcional | `GET /api/privacidade/aviso` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 200; opcional=true; versão 1.0 | 07 Privacidade/T21 - Aviso opcional |
| T22 | Aceitar demonstração | `PUT /api/privacidade/consentimentos/fidelidade` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"aceito":true,"versaoAviso":"1.0"}` | 200; aceito=true | 07 Privacidade/T22 - Aceitar demonstração |
| T23 | Histórico próprio | `GET /api/privacidade/consentimentos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior; aceite registrado em T22 | `Sem body; parâmetros na rota` | 200; atual.aceito=true | 07 Privacidade/T23 - Histórico próprio |
| T24 | Revogar decisão | `PUT /api/privacidade/consentimentos/fidelidade` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"aceito":false}` | 200; aceito=false | 07 Privacidade/T24 - Revogar decisão |
| T25 | Histórico sem token | `GET /api/privacidade/consentimentos` | API iniciada; migrations e seed aplicados | `Sem body; parâmetros na rota` | 401; NAO_AUTENTICADO | 07 Privacidade/T25 - Histórico sem token |
| T26 | Cliente não acessa auditoria | `GET /api/auditoria` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `Sem body; parâmetros na rota` | 403; ACESSO_NEGADO | 08 Auditoria/T26 - Cliente não acessa auditoria |
| T27 | Login administrador | `POST /api/auth/login` | API iniciada; migrations e seed aplicados | `{"email":"admin@raizes.com","senha":"123456"}` | 200; accessToken presente; ADMIN | 08 Auditoria/T27 - Login administrador |
| T28 | Evidência de revogação auditada | `GET /api/auditoria?acao=CONSENTIMENTO_REVOGADO&limit=1` | API iniciada; migrations e seed aplicados; {{token_admin}} obtido no login anterior; revogação registrada em T24 | `Sem body; parâmetros na rota` | 200; evento CONSENTIMENTO_REVOGADO | 08 Auditoria/T28 - Evidência de revogação auditada |
| T29 | Canal obrigatório ausente | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"itens":[{"produtoId":1,"quantidade":1}]}` | 400; CANAL_INVALIDO | 09 Validações adicionais/T29 - Canal obrigatório ausente |
| T30 | Tipo de quantidade inválido | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":"1"}]}` | 422; PARAMETRO_INVALIDO | 09 Validações adicionais/T30 - Tipo de quantidade inválido |
| T31 | Unidade inexistente | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":99999,"canalPedido":"TOTEM","itens":[{"produtoId":1,"quantidade":1}]}` | 404; UNIDADE_NAO_ENCONTRADA | 09 Validações adicionais/T31 - Unidade inexistente |
| T32 | Produto inexistente | `POST /api/pedidos` | API iniciada; migrations e seed aplicados; {{token_cliente}} obtido no login anterior | `{"unidadeId":1,"canalPedido":"TOTEM","itens":[{"produtoId":99999,"quantidade":1}]}` | 404; PRODUTO_NAO_ENCONTRADO | 09 Validações adicionais/T32 - Produto inexistente |

## Testes de integração adicionais desta revisão

O escopo por unidade é coberto por `ApiIntegrationTest.operationalUsersAreRestrictedToTheirAssignedUnit`, fora da coleção Postman T01–T32: ATENDENTE/COZINHA consultam somente Recife; criação, pagamento e atualização de status de pedido de João Pessoa são negados; ADMIN consulta ambas as unidades; remover o vínculo do perfil operacional resulta em 403. A suíte completa executada em 12/10/2026 aprovou 17 testes de integração, 2 de migração e 2 do gateway.

## Cobertura e rastreabilidade

| Requisito | Cenários principais |
|---|---|
| RF01–RF02: autenticação e autorização | T01–T03, T14, T18, T25–T27 |
| RF04–RF05/RF16: unidades, cardápio e paginação | T04, T19–T20 |
| RF06–RF08: pedido, canal, estoque e validação | T05–T09, T29–T32 |
| RF09: consulta por canal | T06; isolamento por cliente nos testes automatizados |
| RF10–RF11: gateway mock e falhas | T10–T13, T17 |
| RF12: transições operacionais | T14–T16 |
| RF13: cancelamento e devolução | Teste automatizado de cancelamento, devolução única e bloqueio de pagamento |
| RF14: auditoria | T28 comprova evento persistido; testes automatizados verificam ator, filtros e consultas |
| RF15: consentimento | T21–T25; testes automatizados verificam histórico, isolamento, revogação e rollback |
| RNF05/RNF08: erros e evidência executável | Todos os cenários de erro verificam status, envelope e código de erro |

## Testes automatizados complementares

Os testes Java estão em [ApiIntegrationTest](../src/test/java/br/com/raizesdonordeste/ApiIntegrationTest.java), [MockPaymentGatewayTest](../src/test/java/br/com/raizesdonordeste/MockPaymentGatewayTest.java) e [DatabaseMigrationTest](../src/test/java/br/com/raizesdonordeste/DatabaseMigrationTest.java): 16 de integração, 2 do gateway e 2 de migrações. Cobrem autorização, validação, rollback, concorrência, consentimento, sanitização de auditoria e compatibilidade da documentação. As migrações são verificadas em bancos temporários novos e existentes, com preservação de registros e validação de checksums.

Execução da coleção por runner externo:

```bash
npx --yes newman@6.2.1 run postman/raizes_nordeste_clean_collection.json --env-var baseUrl=http://127.0.0.1:3000
```

Node/npm servem apenas ao Newman; não são dependências da aplicação Java. Também é possível executar a coleção no Postman sem instalar Newman.

## Critério de conclusão

Build e suíte devem terminar com código zero, sem testes falhando; coleção precisa manter os cenários acima e executar suas asserções; exemplos OpenAPI devem corresponder aos schemas. Não considerar prints ou contagens isoladas como substitutos da evidência executável. A execução final e suas contagens estão registradas em `docs/04_RESULTADO_VALIDACAO.md`.
