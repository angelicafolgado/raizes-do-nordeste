# Diagramas da implementação Java

Atualização: 12/10/2026. Os arquivos DOT são as fontes canônicas e refletem a API Java, suas regras e as migrações Flyway V1/V2. Os PNG/SVG ainda são prévias da renderização anterior a V2 e não exibem o novo vínculo Usuario–Unidade; não devem ser tratados como imagens atualizadas. Graphviz não está disponível neste ambiente. Não se representam funcionalidades propostas como executáveis.

| Artefato | PNG | SVG | Fonte |
|---|---|---|---|
| DER | [imagem](diagramas/01_der.png) | [vetor](diagramas/01_der.svg) | [DOT](diagramas/01_der.dot) |
| Casos de uso | [imagem](diagramas/02_casos_de_uso.png) | [vetor](diagramas/02_casos_de_uso.svg) | [DOT](diagramas/02_casos_de_uso.dot) |
| Classes conceituais de domínio | [imagem](diagramas/03_classes.png) | [vetor](diagramas/03_classes.svg) | [DOT](diagramas/03_classes.dot) |
| Classes técnicas Java | [imagem](diagramas/03_arquitetura_java.png) | [vetor](diagramas/03_arquitetura_java.svg) | [DOT](diagramas/03_arquitetura_java.dot) |
| Atividade do fluxo crítico | [imagem](diagramas/04_sequencia.png) | [vetor](diagramas/04_sequencia.svg) | [DOT](diagramas/04_sequencia.dot) |
| Sequência complementar | — | — | [Mermaid](diagramas/04_sequencia.mmd) |

## DER e domínio

A fonte física são [V1__initial_schema.sql](../src/main/resources/db/migration/V1__initial_schema.sql) e [V2__vinculo_unidade_operacional.sql](../src/main/resources/db/migration/V2__vinculo_unidade_operacional.sql). São nove tabelas de domínio, além do histórico técnico Flyway. V2 acrescenta `usuarios.unidadeId` opcional para empregados operacionais; CLIENTE e ADMIN podem manter esse campo nulo. Um pedido pode ter no máximo um registro de pagamento; a auditoria mantém os eventos das tentativas. A aplicação exige itens, embora a FK não imponha quantidade mínima. Moeda é calculada em centavos e persistida em REAL; datas SQLite são texto UTC.

O desenho de classes é **conceitual**: Usuario, Unidade, Produto, Pedido, ItemPedido, EstoqueUnidade, Pagamento, Consentimento e RegistroAuditoria representam o modelo de dados. Não se afirma que existam entidades Java/ORM com esses nomes: RaizesStore projeta JDBC em mapas e records pontuais. As regras são coordenadas por RaizesService; ApiController apenas traduz HTTP; RaizesStore implementa as transações. O desenho técnico separado registra as classes reais e o contrato PaymentGateway.process.

## Atores e permissões

Cliente consulta os próprios pedidos. Atendente opera somente na unidade vinculada e ADMIN mantém escopo da rede; o titular do pedido criado pelo empregado vem do JWT. Cozinha opera somente na unidade vinculada; ADMIN atualiza status/cancela pedidos de toda a rede. CLIENTE consulta os próprios pedidos. Apenas ADMIN consulta auditoria. Consentimento é próprio e acessível a qualquer perfil autenticado. Usuário genérico no desenho não transfere permissões entre perfis.

O gateway é um adaptador local simulado. App/Web/Totem são canais, não telas implementadas. Cadastro público, gestão administrativa, pontos e promoções continuam propostas documentadas no escopo.

## Fluxo crítico

Criação valida unidade/itens, reserva saldo, grava pedido e auditoria na mesma transação; falha reverte o conjunto. Pagamento valida titular/estado e resultado correlacionado do gateway. Aprovação move para EM_PREPARO; recusa mantém AGUARDANDO_PAGAMENTO. Falhas técnicas gravam auditoria antes de retornar 503/504, sem alterar saldo/pagamento anterior. Resposta inválida causa rollback e 502.

COZINHA/ADMIN faz chamadas separadas para PRONTO e ENTREGUE. Cancelamento só antes de aprovar; restitui estoque uma vez. Não há estorno financeiro real. A sequência complementar usa os mesmos componentes Java que o diagrama de atividade.

## Atualizar imagens

Com Graphviz instalado, a renderização é executada na raiz do repositório:

```bash
dot -Tpng docs/diagramas/01_der.dot -o docs/diagramas/01_der.png
dot -Tsvg docs/diagramas/01_der.dot -o docs/diagramas/01_der.svg
dot -Tpng docs/diagramas/02_casos_de_uso.dot -o docs/diagramas/02_casos_de_uso.png
dot -Tsvg docs/diagramas/02_casos_de_uso.dot -o docs/diagramas/02_casos_de_uso.svg
```

O procedimento é repetido para as cinco fontes DOT, seguido da execução do gerador Java do PDF. O PDF desta revisão inclui as imagens existentes com legendas que descrevem V2; para uma entrega gráfica final, é necessário regenerar as prévias e o PDF. A renderização externa de documentação não é dependência da API.

Casos de uso em `application/RaizesService`; regras de transição em `domain/OrderRules`. As informações constam na [feature crítica](09_FEATURE_CRITICA.md).
