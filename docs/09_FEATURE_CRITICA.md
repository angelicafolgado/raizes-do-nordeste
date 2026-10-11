# Feature crítica — Pedido, pagamento mock e atualização de status

## Atores e responsabilidades

CLIENTE cria e paga o próprio pedido; ATENDENTE/ADMIN podem criar e solicitar pagamento conforme as permissões da API; COZINHA/ADMIN atualizam status. O gateway é local simulado. O canal APP/WEB/TOTEM/BALCAO/PICKUP não concede permissões adicionais. Funcionários não são vinculados a unidades neste MVP.

## Pré-condições

API iniciada, Flyway aplicado e banco disponível. Usuário autenticado com perfil permitido; unidade e produtos existentes; canal válido; itens com IDs e quantidades inteiros positivos; saldo suficiente. Preços são lidos do banco e o titular é obtido do JWT. Para pagar, o pedido deve estar AGUARDANDO_PAGAMENTO e pertencer ao cliente quando esse perfil solicita a operação.

## Fluxo principal

1. Login em `POST /api/auth/login` e consulta de unidades/produtos.
2. Criação em `POST /api/pedidos`: validar corpo, agrupar produtos repetidos e conferir referências.
3. Na transação, reservar estoque, calcular o valor em centavos, gravar pedido/itens e auditoria. Retornar 201 com AGUARDANDO_PAGAMENTO.
4. Solicitar `POST /api/pedidos/{id}/pagamento` com statusSimulado=APROVADO. Validar estado e autorização e chamar PaymentGateway com valor, moeda e correlação.
5. Validar resposta correlacionada, persistir pagamento/auditoria e mudar para EM_PREPARO.
6. COZINHA/ADMIN usa `PATCH /api/pedidos/{id}/status` para PRONTO e depois ENTREGUE. Registrar cada transição.

## Pós-condições

Sucesso: pedido, itens, estoque, pagamento e auditoria persistem; status termina ENTREGUE. Falha na criação ou em uma transação de domínio: rollback preserva a consistência. Não há cobrança externa. Recusa mantém AGUARDANDO_PAGAMENTO e a reserva, permitindo nova tentativa; falha técnica registra auditoria sem pagamento aprovado.

## Fluxos alternativos e exceções

- Sem token: 401; perfil/titular indevido: 403.
- Canal ausente/inválido: 400; itens/IDs/quantidades inválidos: 422; unidade/produto ausente: 404.
- Estoque insuficiente: 409, sem criação parcial ou baixa persistida.
- RECUSADO: response 200 com sucesso=false; pedido continua aguardando pagamento.
- INDISPONIVEL/TIMEOUT: 503/504, sem avanço do pedido. Resposta inválida ou correlação incorreta do gateway: 502.
- Transição fora de AGUARDANDO_PAGAMENTO→CANCELADO, EM_PREPARO→PRONTO ou PRONTO→ENTREGUE: 409.
- Cancelar pedido não pago devolve estoque uma única vez; repetir cancelamento ou pagar pedido já aprovado retorna conflito.

## Idempotência e limites

Não há Idempotency-Key: repetir uma criação válida pode criar outro pedido. Bloqueio de pagamento repetido por estado não equivale a reproduzir a resposta original de uma operação idempotente. Não há expiração automática de reservas ou estorno real. Essas capacidades são evoluções propostas.

## Arquitetura, modelagem e evidências

ApiController traduz HTTP e resolve Identity; RaizesService coordena autorização, casos de uso, gateway e transações; OrderRules valida transições sem HTTP/SQL; RaizesStore implementa JDBC. DER/classes conceituais representam usuário, unidade, estoque, pedido/itens e pagamento; o diagrama técnico mostra as dependências Java reais. Casos de uso representam os atores, e atividade/sequência representa as etapas acima.

Evidências: T01–T18, T29–T32 e testes Java de rollback, cancelamento, concorrência, pagamento e isolamento. Consultas/auditoria/consentimento constam em T21–T28. Coleção e plano: `postman/raizes_nordeste_clean_collection.json` e `docs/04_PLANO_DE_TESTES.md`.
