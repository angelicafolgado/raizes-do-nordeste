# Raízes do Nordeste

**Angelica Dias Folgado**  
**RU: 4937552**

Projeto acadêmico: API REST para pedidos multicanal, estoque por unidade e pagamento simulado. Relatório técnico da implementação Java 21 e Spring Boot 3.5.

Disciplina: Projeto Multidisciplinar - Trilha Back-end  
Curso: a informar  
Polo: a informar  
Semestre: a informar  
Docente: a informar

## Apresentação

Angelica Dias Folgado - RU 4937552

Projeto acadêmico: Raízes do Nordeste

API REST para pedidos multicanal, estoque por unidade e pagamento simulado

Relatório técnico da implementação Java 21 e Spring Boot 3.5.

Disciplina: Projeto Multidisciplinar - Trilha Back-end. Curso, polo, semestre e docente: a informar.

A discente indicou os documentos fornecidos para estudo como referência. Os próprios arquivos contêm aviso de edição para estudo, conteúdo omitido e prevalência do original; a adoção registra sua orientação, sem autenticação independente no AVA.

O sistema é uma demonstração acadêmica local. Pagamentos são simulados e os resultados de desempenho não constituem garantia para produção.

## Sumário

1. Introdução ... 4

2. Estudo de caso e escopo ... 4

3. Análise e requisitos ... 4

4. Modelagem e arquitetura ... 5

Anexo A - Diagrama entidade-relacionamento ... 6

Anexo B - Casos de uso ... 7

Anexo C - Classes conceituais de domínio ... 8

Anexo C.1 - Arquitetura Java ... 9

Anexo D - Atividade do fluxo crítico ... 10

5. API e endpoints ... 11

6. Segurança, privacidade e limites ... 12

7. Entrega técnica e aderência à trilha ... 12

8. Plano de testes e evidências ... 12

9. Ensaio de desempenho ... 13

10. Conclusão ... 13

Referências ... 14

## 1. Introdução

Uma rede de alimentação precisa receber pedidos por diferentes canais, consultar a disponibilidade de cada unidade e acompanhar o preparo. O projeto implementa uma API para demonstrar esse fluxo com persistência e controle de acesso.

O objetivo é demonstrar o fluxo Pedido -> Pagamento simulado -> Status com banco real, autenticação e regras de autorização. Os canais APP, WEB, TOTEM, BALCAO e PICKUP são atributos do pedido.

## 2. Estudo de caso e escopo

A matriz acompanha a rede; cada unidade mantém saldo próprio por produto. ADMIN representa a administração da rede. O sistema não implementa interface gráfica nem integração financeira externa.

Foi escolhido o Fluxo A do material de referência: criar pedido, validar itens e saldo, registrar resultado de pagamento mock e atualizar status. O preço vem do banco, itens repetidos são agregados e o titular do pedido vem do JWT.

Cadastro público, administração completa de produtos/unidades, entradas administrativas de estoque, pontos e campanhas são propostas fora do MVP.

## 3. Análise e requisitos

A API autentica usuários por e-mail e senha e emite JWT. Senhas são armazenadas com BCrypt.

As permissões distinguem CLIENTE, ATENDENTE, COZINHA e ADMIN. Clientes consultam apenas seus pedidos; operações de cozinha e administração seguem regras de perfil.

O catálogo informa produtos e estoque da unidade selecionada. Listagens são paginadas, com limite máximo de 100 registros por página.

Criar pedido, reservar estoque e registrar auditoria ocorre em transação. Itens repetidos são agrupados; falta de saldo ou falha reverte a operação.

O pagamento mock aceita aprovação, recusa e falhas técnicas simuladas. Aprovação move o pedido para EM_PREPARO; a cozinha pode avançar para PRONTO e ENTREGUE.

Pedidos não pagos podem ser cancelados por perfis autorizados, com devolução transacional do estoque. Consentimento de fidelidade é demonstrativo, opcional e revogável.

Requisitos não funcionais atendidos incluem separação por responsabilidades, persistência SQLite, JWT, hashing de senha, padrão JSON de erro, limite de corpo HTTP de 100 KB, paginação e proteção de reserva sob concorrência.

Migrações Flyway versionadas inicializam bancos novos e registram a atualização de bases existentes sem apagar dados. Checksums são validados ao iniciar.

Requisitos propostos: cadastro, CRUD administrativo de catálogo, movimentação de estoque, fidelidade por pontos, promoções e persistência distribuída multiwriter.

## 4. Modelagem e arquitetura

A aplicação foi implementada em Java 21 com Spring Boot 3.5, API REST, JDBC e SQLite. A documentação OpenAPI está disponível em /api-docs.

ApiController atende HTTP; AuthenticationFilter e JwtService validam identidade; RaizesStore centraliza JDBC e transações; PaymentGateway é a porta de pagamento e MockPaymentGateway seu adaptador local; ApiExceptionHandler projeta erros JSON.

O banco tem nove tabelas: usuarios, unidades, produtos, estoques_unidades, pedidos, itens_pedido, pagamentos, audit_logs e consentimentos. O par unidade/produto do estoque é único; pedidos têm usuário e unidade; itens referenciam pedido e produto; há no máximo um pagamento por pedido.

O código Java usa mapas JDBC e records pontuais; não possui classes Java de entidade para todo o domínio. O diagrama conceitual representa os dados projetados por JDBC; o anexo de arquitetura Java registra as classes técnicas da implementação atual.

SQLite permite um escritor por vez. Uma trava local serializa transações de escrita dentro do processo; isso não coordena múltiplas instâncias distribuídas.

A implementação da API, dos testes automatizados e das ferramentas mantidas no projeto é Java 21. A afirmação 100% Java refere-se a esse código-fonte executável; documentação, configuração, SQL e artefatos de integração permanecem em seus formatos próprios.

## Anexo A - Diagrama entidade-relacionamento

Nove tabelas de domínio e relacionamentos. V1__initial_schema.sql é a migração Flyway; flyway_schema_history mantém versões e checksums.

![ Anexo A - Diagrama entidade-relacionamento ](../../docs/diagramas/01_der.png)

## Anexo B - Casos de uso

Visão dos atores e operações; gateway é um adaptador simulado, não uma integração financeira ativa.

![ Anexo B - Casos de uso ](../../docs/diagramas/02_casos_de_uso.png)

## Anexo C - Classes conceituais de domínio

Modelo conceitual de dados; a implementação usa projeções JDBC em mapas, não entidades ORM.

![ Anexo C - Classes conceituais de domínio ](../../docs/diagramas/03_classes.png)

## Anexo C.1 - Arquitetura Java

Classes reais e suas dependências. PaymentGateway.process é implementado por MockPaymentGateway.

![ Anexo C.1 - Arquitetura Java ](../../docs/diagramas/03_arquitetura_java.png)

## Anexo D - Atividade do fluxo crítico

Atividade da implementação Java atual: transações JDBC, aprovação/recusa, falhas técnicas, cancelamento e status.

![ Anexo D - Atividade do fluxo crítico ](../../docs/diagramas/04_sequencia.png)

## 5. API e endpoints

GET /api/health - público; responde à verificação de saúde.

POST /api/auth/login - público; e-mail e senha; retorna JWT ou 401.

GET /api/unidades?page=1&limit=10 e GET /api/produtos?unidadeId=1&page=1&limit=10 - públicos; listagens paginadas com limite máximo de 100.

POST /api/pedidos - CLIENTE, ATENDENTE ou ADMIN; cria pedido e reserva estoque; 201 em sucesso; 400/401/403/404/409/422 em erros conforme o caso.

GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10 - autenticado; cliente vê apenas pedidos próprios; perfis operacionais consultam a rede.

POST /api/pedidos/{id}/pagamento - titular, ATENDENTE ou ADMIN; resultado aprovado ou recusado; falhas simuladas retornam 503 (INDISPONIVEL) ou 504 (TIMEOUT).

PATCH /api/pedidos/{id}/status - COZINHA ou ADMIN; transições permitidas de preparo/entrega.

GET /api/privacidade/aviso; PUT /api/privacidade/consentimentos/fidelidade; GET /api/privacidade/consentimentos - aviso e histórico do próprio titular.

GET /api/auditoria?acao=CONSULTA_PEDIDOS - ADMIN. GET /api-docs.json serve o contrato OpenAPI; / redireciona para Swagger.

O contrato OpenAPI contém schemas, campos obrigatórios, enumerações, parâmetros, perfis, exemplos e respostas de erro. Ele é aplicado somente a rotas descobertas pelo Spring.

Request de pedido: {"unidadeId": 1, "canalPedido": "APP", "itens": [{"produtoId": 1, "quantidade": 1}]}. O servidor calcula o preço.

Trecho de response 201: {"id": 1, "canalPedido": "APP", "status": "AGUARDANDO_PAGAMENTO", "total": 38.5}. O contrato completo inclui itens, titular, unidade e datas.

Pagamento recusado: envie {"statusSimulado": "RECUSADO"}. Response 200 inclui {"sucesso": false, "statusPedido": "AGUARDANDO_PAGAMENTO"}; a reserva permanece e nova tentativa é permitida.

Erro de estoque, HTTP 409: {"error": "ESTOQUE_INSUFICIENTE", "message": "Estoque insuficiente para Baião de dois.", "details": [], "timestamp": "2026-10-11T13:00:00Z", "path": "/api/pedidos", "requestId": "00000000-0000-4000-8000-000000000001"}. Exemplo ilustrativo; não é uma captura de credenciais.

## 6. Segurança, privacidade e limites

O seed fornece contas fictícias. Senhas são armazenadas com BCrypt; chamadas protegidas usam JWT; perfis e titularidade limitam operações.

O pagamento não captura dados de cartão nem contata adquirente. O consentimento de fidelidade apenas registra uma preferência de demonstração; não há pontos ou envio de marketing.

A aplicação não implementa expurgo periódico, anonimização automatizada ou operação comercial de direitos do titular. O SQLite local e seus backups não são cifrados pela aplicação; segredos e bancos não devem ser publicados.

A discussão de privacidade é acadêmica e não representa certificação jurídica ou parecer.

## 7. Entrega técnica e aderência à trilha

O README documenta JDK, Maven, variáveis, banco/seed, início da API, Swagger, testes e geração do PDF. O OpenAPI é servido pela aplicação. A cópia limpa de uma revisão anterior foi validada sem target ou banco herdado, usando dependências já disponíveis no Maven local.

Repositório público: https://github.com/angelicafolgado/raizes-do-nordeste. A branch main tem histórico publicado em nome de Angelica Dias Folgado. O material indicado recomenda cinco commits de evolução real; esta revisão registra correções efetivas, sem fabricar histórico.

A coleção Postman foi validada com Newman 6.2.1: 32 requisições e 88 asserções aprovadas; 17 cenários 2xx e 15 cenários de erro, organizados em nove pastas.

Os materiais fornecidos foram adotados como referência por instrução da discente, embora os próprios PDFs indiquem edição para estudo, omissões e prevalência do original. A adoção não autentica a procedência do material no AVA.

## 8. Plano de testes e evidências

A validação Maven executada em 11/10/2026 concluiu com BUILD SUCCESS: 20 testes aprovados, sem falhas, erros ou testes ignorados.

Casos positivos cobrem token de autenticação usado nos testes, listagens, pedido criado com itens agrupados, aprovação de pagamento, avanço para PRONTO, consentimento idempotente, seed idempotente e consultas concorrentes.

Casos negativos cobrem credencial/token inválido (401), perfil ou titular indevido (403), dados inválidos (400/422), unidade/produto inexistente (404), estoque insuficiente e conflito de estado (409), gateway (503/504), corpo excessivo (413) e rollback quando falha auditoria.

O reinício local foi validado: pedido e pagamento permaneceram consultáveis em SQLite. Migrações e seed são repetíveis. Restauração de backup não foi ensaiada nesta revisão.

A evidência é JUnit: 16 métodos de integração em ApiIntegrationTest e 2 testes do gateway em MockPaymentGatewayTest, mais 2 testes de migrações Flyway em DatabaseMigrationTest. A coleção Postman executou 32 requisições e 88 asserções, sem falhas.

## 9. Ensaio de desempenho

A medição foi executada contra o JAR Spring Boot empacotado, em loopback, com SQLite isolado e seed local. Vinte workers concorrentes alternaram consultas públicas de produtos e unidades por 60 segundos, usando clientes HTTP persistentes.

Resultado observado: 228.399 requisições em 60,01 segundos; p95 de 11,76 ms; zero falhas e zero respostas HTTP 5xx. Meta local: p95 até 1.000 ms, zero falhas e menos de 1% de respostas 5xx. O cenário atingiu esses limites.

É um ensaio de leituras locais, não de escritas, rede externa, múltiplas instâncias, reinício, restauração ou disponibilidade em produção.

## 10. Conclusão

A implementação Java entrega o fluxo acadêmico de pedidos, reserva de estoque, pagamento simulado e acompanhamento operacional. Os testes e a medição documentam somente o comportamento observado no ambiente local.

O código executável da API, testes e ferramentas mantidas está em Java 21. O repositório público contém a entrega Java. A coleção Postman foi validada: 32 requisições e 88 asserções aprovadas. Os campos acadêmicos foram mantidos por orientação da discente e marcados como a informar. A referência fornecida foi usada conforme orientação da discente, preservada a ressalva indicada pelos próprios arquivos.

## Referências

Materiais de referência designados pela discente: PDFs fornecidos para estudo, não distribuídos no repositório; os materiais indicam versão editada para estudo.

Projeto Raízes do Nordeste. Documentação técnica, requisitos, privacidade e desempenho. docs/.

Oracle. Java Documentation. docs.oracle.com/en/java/.

Spring. Spring Boot Reference Documentation. docs.spring.io/spring-boot/.

Brasil. Lei nº 13.709, de 14 de agosto de 2018 (LGPD). Texto compilado no Portal da Legislação.

