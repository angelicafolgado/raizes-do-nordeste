# Etapa 5 — Diagramas do projeto

Artefatos visuais mantidos para documentação acadêmica, revisados em 11/10/2026. As imagens PNG podem ser inseridas no trabalho acadêmico; os SVG preservam qualidade ao ampliar. Os arquivos DOT e Mermaid são fontes editáveis, mas não são gerados por ferramentas mantidas neste projeto.

| Diagrama | Imagem | Vetor | Fonte editável |
|---|---|---|---|
| DER | [PNG](diagramas/01_der.png) | [SVG](diagramas/01_der.svg) | [DOT](diagramas/01_der.dot) |
| Casos de uso | [PNG](diagramas/02_casos_de_uso.png) | [SVG](diagramas/02_casos_de_uso.svg) | [DOT](diagramas/02_casos_de_uso.dot) |
| Classes de domínio | [PNG](diagramas/03_classes.png) | [SVG](diagramas/03_classes.svg) | [DOT](diagramas/03_classes.dot) |
| Sequência | [PNG](diagramas/04_sequencia.png) | [SVG](diagramas/04_sequencia.svg) | [Mermaid](diagramas/04_sequencia.mmd) |

## 1. Modelo de dados — DER

![DER](diagramas/01_der.png)

Fonte de conferência: [schema SQL da API Java](../src/main/resources/schema.sql). O diagrama representa as nove tabelas e relacionamentos por chave estrangeira, incluindo consentimentos e auditoria. PK identifica chave primária, FK chave estrangeira, UQ unicidade e `?` um campo opcional. Os rodapés apresentam os nomes físicos das tabelas.

- Um pedido pertence a um usuário e uma unidade. Cada item referencia um produto; sua exclusão acompanha a exclusão do pedido (`onDelete: Cascade`).
- A combinação unidade/produto é única no estoque. Um pedido tem no máximo um registro de pagamento, atualizado a cada tentativa; a auditoria preserva os eventos das tentativas.
- Um registro de auditoria pode não ter usuário associado. Um consentimento sempre referencia um usuário e conserva a decisão, finalidade, versão e data.
- A relação pedido/itens aparece como `0..*` na persistência: a chave estrangeira não impõe quantidade mínima. A aplicação exige ao menos um item válido ao criar o pedido.
- O campo legado `consentimentoLGPD` não substitui o histórico de consentimentos. Valores monetários usam `Float` no schema atual; os cálculos da aplicação usam centavos inteiros antes de persistir.

## 2. Casos de uso

![Casos de uso](diagramas/02_casos_de_uso.png)

Os atores especializados herdam a participação do ator genérico “Usuário da rede”. A generalização não concede permissões entre perfis. `«include»` indica que criar um pedido sempre inclui validar os itens e reservar estoque.

| Operação | Permissão implementada |
|---|---|
| Autenticar; consultar unidades, produtos e aviso | Pública |
| Consultar pedidos | CLIENTE: próprios; ATENDENTE, COZINHA e ADMIN: pedidos da rede |
| Registrar/revogar consentimento e consultar histórico | Qualquer perfil autenticado, somente suas próprias decisões |
| Criar pedido | CLIENTE, ATENDENTE e ADMIN; titular obtido do JWT |
| Solicitar pagamento | CLIENTE: próprio pedido; ATENDENTE e ADMIN: pedidos da rede |
| Avançar status ou cancelar pedido aguardando pagamento | COZINHA e ADMIN |
| Consultar auditoria | Somente ADMIN |

O gateway é representado como participante de apoio fora da fronteira da API, mas sua implementação é um adaptador local simulado, sem serviço financeiro externo. App, Web e Totem representam canais, não interfaces gráficas já construídas. Não há criação de pedido em nome de outro cliente pelo atendente.

Cadastro público, manutenção administrativa completa, pontuação de fidelidade e promoções permanecem propostas do [escopo](01_REQUISITOS_E_ESCOPO.md), não funcionalidades implementadas nestes diagramas.

## 3. Classes — domínio atual

![Classes](diagramas/03_classes.png)

O desenho de classes preservado é histórico: descreve contratos da implementação TypeScript anterior e não representa as classes Java atuais. A persistência Java usa JDBC e mapas/records pontuais, não uma hierarquia de entidades equivalente. Para as classes técnicas da versão atual, consulte o Anexo C gerado no [PDF acadêmico](../output/pdf/4937552_Projeto_Back_End.pdf).

As associações são uma visão conceitual dos dados de domínio, não um diagrama de classes Java gerado do código atual. A composição `Pedido`–`ItemPedido` representa itens do pedido; os nomes das tabelas e chaves devem ser conferidos no `schema.sql`.

## 4. Sequência — pedido, pagamento e status

![Sequência](diagramas/04_sequencia.png)

O desenho de sequência é um artefato visual anterior à implementação Java. Os fluxos atuais estão em [ApiController](../src/main/java/br/com/raizesdonordeste/api/ApiController.java), [RaizesStore](../src/main/java/br/com/raizesdonordeste/infrastructure/RaizesStore.java) e [MockPaymentGateway](../src/main/java/br/com/raizesdonordeste/infrastructure/payments/MockPaymentGateway.java).

1. Criar pedido valida identidade, unidade e itens, reserva estoque e grava pedido e auditoria na mesma transação. Falhas revertem o conjunto.
2. Pagar valida titularidade e estado antes de acionar o gateway. Aprovação muda para `EM_PREPARO`; recusa mantém `AGUARDANDO_PAGAMENTO`, permitindo outra tentativa.
3. Indisponibilidade e timeout simulados registram auditoria e retornam 503/504 após seu commit, sem modificar pedido, pagamento anterior ou reserva. Correlação inválida causa rollback e 502.
4. Cozinha ou administrador realiza chamadas separadas para `PRONTO` e `ENTREGUE`. O cancelamento somente antes do pagamento devolve o estoque uma vez. Não há estorno implementado.

O desenho agrupa chamadas de persistência para facilitar leitura e não é um rastreamento da implementação Java atual. Na implementação corrente, `RaizesStore` coordena JDBC/SQLite e `MockPaymentGateway` simula o retorno sem serviço financeiro externo.

## Manutenção e verificação

As imagens e fontes editáveis são entregues como artefatos estáticos. Os geradores antigos em Python/Node foram removidos; alterações visuais devem ser feitas nas fontes DOT/Mermaid e renderizadas externamente com ferramenta compatível. Não se declara cobertura automática ou sincronização entre esses desenhos históricos e o código Java.
