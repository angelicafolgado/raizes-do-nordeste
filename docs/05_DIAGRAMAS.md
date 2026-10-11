# Etapa 5 — Diagramas do projeto

Diagramas do backend implementado, revisados em 11/10/2026. As imagens PNG podem ser inseridas no trabalho acadêmico; os SVG preservam qualidade ao ampliar. Os arquivos DOT e Mermaid são fontes editáveis.

| Diagrama | Imagem | Vetor | Fonte editável |
|---|---|---|---|
| DER | [PNG](diagramas/01_der.png) | [SVG](diagramas/01_der.svg) | [DOT](diagramas/01_der.dot) |
| Casos de uso | [PNG](diagramas/02_casos_de_uso.png) | [SVG](diagramas/02_casos_de_uso.svg) | [DOT](diagramas/02_casos_de_uso.dot) |
| Classes de domínio | [PNG](diagramas/03_classes.png) | [SVG](diagramas/03_classes.svg) | [DOT](diagramas/03_classes.dot) |
| Sequência | [PNG](diagramas/04_sequencia.png) | [SVG](diagramas/04_sequencia.svg) | [Mermaid](diagramas/04_sequencia.mmd) |

## 1. Modelo de dados — DER

![DER](diagramas/01_der.png)

Fonte: [schema Prisma](../prisma/schema.prisma). Contém as nove tabelas e nove relacionamentos por chave estrangeira, incluindo consentimentos e auditoria. PK identifica chave primária, FK chave estrangeira, UQ unicidade e `?` um campo opcional. Os rodapés apresentam os nomes físicos das tabelas.

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

Fonte: [interfaces de domínio](../src/domain/entities). As nove estruturas são interfaces TypeScript, identificadas por `«interface»`; não possuem métodos de negócio. As regras ficam nos [casos de uso](../src/application/use-cases). Este é um diagrama de domínio, não um inventário de todas as classes técnicas.

As associações descrevem vínculos por IDs. A composição `Pedido`–`ItemPedido` representa a coleção `itens` e a regra de pelo menos um item. O contrato `ItemPedido` expõe produto, quantidade e preço, enquanto a tabela correspondente também possui `id` e `pedidoId`. `RegistroAuditoria` é o nome no domínio; `AuditLog`, o nome no Prisma. Campos de status e perfil são strings nessas interfaces; os enums e as validações restringem seus valores na aplicação.

## 4. Sequência — pedido, pagamento e status

![Sequência](diagramas/04_sequencia.png)

Fontes: [criação](../src/application/use-cases/CriarPedidoUseCase.ts), [pagamento](../src/application/use-cases/ProcessarPagamentoMockUseCase.ts) e [atualização de status](../src/application/use-cases/AtualizarStatusPedidoUseCase.ts).

1. Criar pedido valida identidade, unidade e itens, reserva estoque e grava pedido e auditoria na mesma transação. Falhas revertem o conjunto.
2. Pagar valida titularidade e estado antes de acionar o gateway. Aprovação muda para `EM_PREPARO`; recusa mantém `AGUARDANDO_PAGAMENTO`, permitindo outra tentativa.
3. Indisponibilidade e timeout simulados registram auditoria e retornam 503/504 após seu commit, sem modificar pedido, pagamento anterior ou reserva. Correlação inválida causa rollback e 502.
4. Cozinha ou administrador realiza chamadas separadas para `PRONTO` e `ENTREGUE`. O cancelamento somente antes do pagamento devolve o estoque uma vez. Não há estorno implementado.

O desenho agrupa chamadas de persistência por responsabilidade para facilitar leitura; `Store / Prisma / SQLite` representa a porta e sua implementação. Não é um rastreamento de cada método ou instrução SQL. As alternativas de pagamento são mutuamente exclusivas, e o timeout simulado não envolve espera de rede.

## Manutenção e verificação

Os quatro SVG foram renderizados em PNG e revisados visualmente. O gerador verifica a cobertura de nove modelos, nove relacionamentos e nove interfaces; os casos de uso e a sequência são mantidos conforme as regras do código.

Para regenerar a documentação, na raiz do projeto:

```powershell
python scripts/generate_diagrams.py
node scripts/render_diagrams.cjs
python scripts/rasterize_diagrams.py
```

Ferramentas de documentação: Python com `PyMuPDF` e `Pillow`, Node com `@viz-js/viz` (ou o runtime local usado nesta geração). Não são dependências de execução da API. DOT pode ser editado com ferramentas Graphviz; Mermaid, em editor compatível. Regenerar substitui as fontes e imagens: alterações permanentes devem ser feitas no gerador. O contato visual fica em `tmp/diagramas/contato.png`.

A etapa 5 entrega a modelagem do estado atual. As próximas etapas são consolidar a documentação técnica, montar o trabalho acadêmico e preparar o repositório e a entrega final.
