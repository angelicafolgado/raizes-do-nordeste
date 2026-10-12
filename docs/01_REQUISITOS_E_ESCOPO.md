# Etapa 1 — Requisitos e escopo

Projeto: Raízes do Nordeste — API Back-end  
Versão: 1.0 — 11/10/2026  
Situação: matriz revisada de requisitos e decisões de escopo. O estado implementado e suas evidências atuais constam em [rastreabilidade](06_RASTREABILIDADE.md) e [resultado de validação](04_RESULTADO_VALIDACAO.md).

## 1. Objetivo e contexto

A rede Raízes do Nordeste precisa consolidar pedidos de diferentes canais e acompanhar o atendimento em suas unidades. Cada unidade mantém seu próprio estoque; a API deve impedir vendas sem disponibilidade, registrar pagamentos simulados e controlar a preparação e a entrega.

O objetivo da entrega é demonstrar uma API REST reproduzível, com persistência real, autenticação, autorização, regras transacionais e documentação acadêmica coerente com o código. Não há uma entidade Matriz: ADMIN representa a administração da rede. Desde a migração V2, cada ATENDENTE e COZINHA deve ter vínculo com uma unidade; esses perfis só consultam e operam pedidos daquela unidade. ADMIN mantém visão de rede. A atribuição operacional ainda é feita por provisionamento/seed, não por endpoint administrativo.

A aluna Angelica Dias Folgado desenvolveu o projeto com base no estudo de caso Raízes do Nordeste e no roteiro da trilha Back-end. Os documentos de referência não são distribuídos neste repositório. As versões consultadas são adaptadas para estudo, com conteúdo omitido; os documentos institucionais do AVA prevalecem para a conferência dos requisitos.

## 2. Decisão de escopo

O fluxo principal escolhido é o **Fluxo A: Pedido → Pagamento mock → Atualização de status**. Ele já está implementado e permite demonstrar integração entre autenticação, estoque, persistência, pagamento e operação da cozinha.

Para concluir a entrega, serão mantidas as funcionalidades existentes e completados o isolamento do gateway mock, os contratos, a paginação, os controles de privacidade definidos nesta etapa e os artefatos acadêmicos. Cadastro público, gestão administrativa completa, fidelização e promoções serão apresentados como propostas detalhadas, sem serem anunciados como funcionalidades executáveis.

Essa decisão usa a possibilidade do roteiro de contemplar requisitos fora do MVP ao menos conceitualmente. Ela concentra a implementação no fluxo obrigatório e permite explicar a evolução da solução. Se o roteiro oficial trouxer exigência adicional de implementação, esta matriz deverá ser atualizada antes de iniciar essa ampliação.

### Legenda

- **Implementado:** existe no código Java atual; a evidência funcional e os limites estão em [rastreabilidade](06_RASTREABILIDADE.md) e [resultado de validação](04_RESULTADO_VALIDACAO.md).
- **Parcial:** existe uma parte, mas falta complemento para o escopo final.
- **Planejado:** será executado nas próximas etapas desta entrega.
- **Proposto:** será especificado e justificado na documentação; não integra a implementação desta entrega.
- **P1:** essencial ao fluxo, à segurança ou à entrega acadêmica.
- **P2:** melhoria de consistência e qualidade incluída na conclusão.
- **P3:** evolução conceitual fora da implementação final definida aqui.

As prioridades são decisões do projeto. Não representam pesos oficiais de avaliação. Esta matriz registra o baseline de planejamento; os critérios de aceite não substituem os resultados executados registrados na documentação de validação.

## 3. Atores e responsabilidades

| Ator | Responsabilidade no escopo |
|---|---|
| Cliente (App/Web/Totem) | Autenticar, consultar unidades/cardápio, criar e consultar seus pedidos e solicitar pagamento mock. |
| Atendente (Balcão) | Criar pedidos associados à sua unidade, consultar pedidos da unidade e processar pagamento mock dos pedidos nela criados. Vincular o pedido a um cliente atendido é proposta futura. |
| Cozinha | Consultar pedidos da unidade vinculada, executar transições operacionais e cancelar pedido ainda não pago daquela unidade. |
| Gerente/Administrador | Representado pelo perfil ADMIN, com as permissões operacionais implementadas. Cadastros administrativos são propostas. |
| Gateway externo de pagamento (simulado) | Receber solicitação e produzir resultado de aprovação, recusa ou falha técnica, sem cobrança real. |

## 4. Requisitos funcionais

| ID | Requisito | Prioridade / estado | Critério de aceite ou especificação da proposta |
|---|---|---|---|
| RF01 | Autenticar por e-mail e senha e emitir JWT com validade definida. | P1 / Implementado | Credenciais válidas retornam token; inválidas retornam 401; dados ausentes retornam erro de validação. Senha e hash não aparecem no retorno. |
| RF02 | Autorizar operações por perfil e proprietário do pedido. | P1 / Implementado | Ausência de token retorna 401; perfil indevido retorna 403; cliente não consulta nem paga pedido de outro cliente. |
| RF03 | Cadastrar cliente com e-mail único e senha protegida. | P3 / Proposto | Descrever validação, prevenção de duplicidade e atribuição exclusiva de CLIENTE no cadastro público. O seed continua sendo a origem das contas de demonstração. |
| RF04 | Consultar unidades da rede. | P1 / Implementado | Retornar identificação, nome e cidade das unidades cadastradas. Paginação será completada pelo RF16. |
| RF05 | Consultar cardápio e disponibilidade por unidade. | P1 / Implementado | Filtro por unidade retorna apenas seu cardápio/estoque; unidade inexistente retorna 404; saldo de outra unidade não é agregado. |
| RF06 | Criar pedido com itens e valores calculados no servidor. | P1 / Implementado | Pedido válido retorna 201; itens repetidos são agrupados; IDs/quantidades inválidos são rejeitados; preço enviado pelo consumidor não controla o total. |
| RF07 | Registrar e filtrar o canal de origem do pedido. | P1 / Implementado | Exigir APP, TOTEM, BALCAO, PICKUP ou WEB; ausência/valor inválido retorna 400/422; filtro retorna somente pedidos do canal solicitado. |
| RF08 | Reservar estoque da unidade transacionalmente. | P1 / Implementado | Falta de saldo retorna 409; falha em qualquer item reverte toda a reserva; pedidos concorrentes não tornam o saldo negativo. |
| RF09 | Consultar pedidos com paginação e isolamento por cliente/unidade. | P1 / Implementado | Validar page/limit, limitar a 100 itens por página e retornar metadados; cliente vê apenas seus pedidos; ATENDENTE/COZINHA veem apenas a unidade vinculada; ADMIN consulta a rede. Perfil operacional sem unidade recebe 403. |
| RF10 | Solicitar e registrar pagamento mock por contrato de gateway. | P1 / Implementado | Preservar aprovação/recusa atuais; mover simulação para adaptador da infraestrutura, acessado por interface da aplicação; comprovar envio, retorno e correlação com o pedido. |
| RF11 | Tratar recusa, falha técnica e repetição de pagamento. | P1 / Implementado | Recusa mantém pedido aguardando pagamento; nova tentativa é permitida; pedido aprovado não é cobrado novamente. Falha técnica simulada retorna erro padronizado e não aprova pedido nem grava pagamento aprovado. |
| RF12 | Atualizar status respeitando a sequência operacional. | P1 / Implementado | Permitir EM_PREPARO → PRONTO → ENTREGUE apenas a COZINHA/ADMIN; rejeitar saltos/regressões com 409. |
| RF13 | Cancelar pedido ainda não pago e devolver estoque. | P1 / Implementado | COZINHA/ADMIN pode mudar AGUARDANDO_PAGAMENTO para CANCELADO; restituição ocorre uma vez; pedido cancelado não aceita pagamento. |
| RF14 | Auditar ações sensíveis e acessos operacionais a pedidos. | P1 / Implementado | Manter logs de login, criação, pagamento e status; acrescentar registro de consultas operacionais com ator, ação e instante, sem senha, JWT ou cópia integral de dados pessoais. Definir evidência reproduzível de auditoria para o avaliador. |
| RF15 | Registrar e permitir revogar consentimento opcional. | P1 / Implementado | Usuário autenticado registra apenas sua própria decisão para finalidade identificada, com data e versão do aviso; revogação fica rastreável e não bloqueia pedidos comuns. Consentimento de demonstração inserido pelo seed deve ser identificado como fictício. |
| RF16 | Paginar consultas de produtos e unidades. | P2 / Implementado | Aceitar page/limit, com padrão 1/10 e máximo 100; retornar dados e metadados, preservando filtros. Atualizar Swagger, Postman e consumidores de teste para o novo formato. |
| RF17 | Administrar produtos e unidades. | P3 / Proposto | Especificar criação/edição/inativação por ADMIN; impedir remoções que quebrem pedidos históricos e manter identificação estável. |
| RF18 | Registrar entradas e ajustes administrativos de estoque. | P3 / Proposto | Descrever movimentação com unidade, produto, quantidade, tipo, motivo, autor e data; bloquear saída sem saldo e preservar histórico. O MVP já implementa saída por pedido e devolução por cancelamento. |
| RF19 | Acumular e resgatar pontos mediante adesão opcional. | P3 / Proposto | Especificar saldo, lançamentos, acúmulo único por pedido entregue e resgate sem saldo negativo; detalhamento na seção 7. |
| RF20 | Aplicar promoções/campanhas elegíveis. | P3 / Proposto | Especificar vigência, unidade, canal, produtos elegíveis e desconto calculado pelo servidor; detalhamento na seção 7. |

RF15 não implica implementar o programa de pontos: nesta entrega, o consentimento é uma capacidade técnica demonstrável para uma finalidade opcional claramente descrita. A política documental deverá manter coerência com as funcionalidades realmente executadas.

## 5. Requisitos não funcionais

| ID | Requisito | Prioridade / estado | Critério de aceite |
|---|---|---|---|
| RNF01 | Separar responsabilidades de API, segurança, persistência e pagamento. | P1 / Implementado | Implementação Java separa `ApiController`, `RaizesService`, `OrderRules`, filtros/configuração, `RaizesStore` e o contrato `PaymentGateway`; limites e responsabilidades estão descritos na documentação técnica. |
| RNF02 | Persistir dados com integridade e migrações reproduzíveis. | P1 / Implementado | Banco recriado por migrações; PK/FK e unicidade coerentes; seed repetido não apaga pedidos nem repõe estoque consumido. |
| RNF03 | Proteger credenciais e segredos. | P1 / Implementado | Senhas com bcrypt, JWT assinado e validado, segredo externo ao código e ignorado pelo versionamento; responses sem hashes/tokens de terceiros. |
| RNF04 | Explicitar finalidade, minimização e ciclo de vida dos dados. | P1 / Documentado; procedimentos propostos | Inventário por campo e finalidade, base de tratamento a fundamentar, controle de acesso, estratégia de retenção/anonimização e procedimento de revogação; distinguir política proposta de mecanismo implementado. |
| RNF05 | Padronizar erros e validar entradas. | P1 / Implementado | Envelope com error, message, details, timestamp, path e requestId; sem stack trace no cliente; status coerentes para validação, acesso e conflito. |
| RNF06 | Manter contratos OpenAPI completos e compatíveis. | P1 / Implementado | Cada operação tem parâmetros, permissões, exemplos de request quando aplicável, response de sucesso e erro, além dos códigos possíveis. |
| RNF07 | Permitir instalação e execução local reproduzíveis. | P1 / Implementado | Em diretório limpo, instruções do README instalam dependências, configuram ambiente, migram, populam e iniciam a API. Revalidar ao fechar a entrega. |
| RNF08 | Garantir evidências de teste reproduzíveis. | P1 / Implementado | Build e testes passam; coleção organizada em pastas com pelo menos 10 cenários, 6 positivos e 4 negativos, cobrindo os casos obrigatórios e evidência de auditoria; cada cenário possui pré-condição e resultado esperado. |
| RNF09 | Limitar custo das consultas e avaliar concorrência. | P2 / Parcial | Aplicar RF16; preservar testes de última unidade e pagamento concorrente. Proposta de medição local: 20 clientes concorrentes durante 60 s em consultas, registrar ambiente, p95 e erros; meta inicial p95 ≤ 1 s e menos de 1% de 5xx. Medição de leituras anterior registrada em docs/06_RESULTADOS_OPERACIONAIS.json; não repetida nesta revisão e não é garantia de produção. |
| RNF10 | Recuperar a operação após falhas locais. | P2 / Parcial | Após reiniciar a API, pedidos persistidos permanecem consultáveis; manter health de processo e encerramento controlado; documentar backup/restauração do SQLite e verificar restauração em cópia de teste. Sem compromisso de disponibilidade 24×7. |
| RNF11 | Tolerar falhas do gateway simulado. | P1 / Implementado | Simular indisponibilidade e timeout determinísticos, sem rede financeira real; estado do pedido e reserva permanecem coerentes; registrar resultado técnico sem transformar falha em aprovação. |
| RNF12 | Preservar rastreabilidade entre requisito, código e evidência. | P1 / Parcial | Cada requisito implementado aponta para caso de uso/rota e teste; cada proposta é identificada como tal; diagramas e texto não apresentam funcionalidade inexistente como executável. |

## 6. Regras de negócio e limites do MVP

| ID | Regra |
|---|---|
| RN01 | Um pedido pertence a uma única unidade e ao usuário autenticado que o criou. O canal não concede permissões extras. |
| RN02 | O canal é obrigatório e restrito aos valores aceitos pela aplicação Java. No SQLite ele é persistido como texto validado pela API. |
| RN03 | IDs e quantidades são inteiros positivos; pedido contém de 1 a 100 entradas de itens; entradas duplicadas são agrupadas. |
| RN04 | Preço vem do catálogo; total é calculado com centavos inteiros e persistido segundo o esquema atual. O preço do item é preservado no pedido. |
| RN05 | Reserva de estoque, criação do pedido e auditoria da criação formam uma transação; falha reverte o conjunto. |
| RN06 | O pagamento aprovado só ocorre para pedido AGUARDANDO_PAGAMENTO e o move para EM_PREPARO. O mock não solicita dados de cartão nem realiza cobrança. |
| RN07 | Recusa mantém a reserva e permite nova tentativa. Hoje a tabela Pagamento guarda a última tentativa e a auditoria guarda o histórico das tentativas. |
| RN08 | Repetição de pagamento após aprovação retorna conflito. Isso impede duplicidade, mas não equivale a implementar chave de idempotência com reprodução da resposta original. Essa distinção deve constar na documentação. |
| RN09 | A cozinha não pula pagamento ou etapas operacionais. Cancelamento só é permitido antes da aprovação; não há estorno de pedido pago neste escopo. |
| RN10 | Cancelamento restitui os itens à mesma unidade, exatamente uma vez. Reservas não expiram automaticamente; expiração é evolução futura. |
| RN11 | Aceitar uma finalidade opcional não é condição para fazer pedidos. Seu registro não comprova, por si só, conformidade jurídica de todo o sistema. |
| RN12 | ATENDENTE e COZINHA precisam de `unidadeId` ativo; consultas, pagamento operacional, criação e mudanças de status são limitadas à mesma unidade. ADMIN mantém escopo de rede; CLIENTE mantém escopo dos próprios pedidos. Vínculos legados nulos falham fechados com 403. |

## 7. Funcionalidades propostas: especificação conceitual

### 7.1 Cadastro e gestão administrativa

**Cadastro de cliente (proposto, não implementado).** Ator: visitante não autenticado. Entradas: nome, e-mail, senha e confirmação de senha; o papel seria sempre atribuído pelo servidor como `CLIENTE`, sem aceitar role, unidade ou consentimento no payload. Validar campos obrigatórios, limites de tamanho, formato/normalização de e-mail e política de senha; duplicidade após normalização retorna conflito sem revelar credenciais. Hash BCrypt, resposta sem senha/hash e evento de auditoria minimizado. Resultado esperado: nova conta CLIENTE pronta para autenticação; falha de validação/duplicidade não cria conta. Consentimento para fidelidade deve ser uma decisão separada e opcional.

**Gestão de unidades e produtos (proposta, não implementada).** Atores: ADMIN cria/edita/inativa; perfis operacionais apenas consultam dados autorizados e não alteram cadastros. Unidade receberia nome, cidade, endereço operacional estritamente necessário e situação; validar campos obrigatórios, tamanhos e unicidade da chave operacional. Produto receberia nome, descrição, preço em centavos não negativo, situação e, quando aplicável, categorias; validar nome, preço, moeda e IDs de unidade/produto. Desativação não apaga referências históricas; produto inativo não entra em novo pedido. Resultado esperado: catálogo atualizado e visível nas consultas permitidas, com alterações administrativas auditadas. Mudança de preço não reescreve o preço histórico dos itens já pedidos.

Rotas candidatas, **não implementadas**: `POST /api/usuarios`, `POST /api/unidades`, `PATCH /api/unidades/{id}`, `POST /api/produtos` e `PATCH /api/produtos/{id}`. Funcionários por unidade exigiriam vínculo de autorização adicional no modelo.

### 7.2 Movimentação administrativa de estoque

**Movimentação administrativa de estoque (proposta, não implementada).** Ator: ADMIN; empregado só poderia movimentar a própria unidade se uma permissão adicional fosse formalmente definida. Entradas: `unidadeId`, `produtoId`, tipo (`ENTRADA`, `SAIDA` ou `AJUSTE`), quantidade inteira positiva e motivo obrigatório; `usuarioId`/data viriam da identidade e relógio do servidor. Validar unidade/produto ativos, IDs positivos, quantidade, motivo não vazio e saldo suficiente para saída; ajuste exigiria valor final não negativo e justificativa. Regra: gravar evento imutável e alterar o saldo na mesma transação; falha em qualquer dos dois reverte ambos. Resultado esperado: novo saldo consultável e lançamento auditável com saldo anterior/posterior; sem movimento silencioso nem saldo negativo.

Rotas candidatas, **não implementadas**: `POST /api/estoques/movimentacoes` e `GET /api/estoques/movimentacoes?unidadeId=...`. O extrato seria paginado e restrito a administradores autorizados.

### 7.3 Fidelização

Política de demonstração proposta, não exigência literal do roteiro: conceder 1 ponto por R$ 1,00 efetivamente pago em pedido entregue, descartando a fração. Para R$ 38,50, conceder 38 pontos. Só acumular após adesão opcional ativa; não conceder pontos por pedidos recusados ou cancelados.

Resgate proposto: blocos de 100 pontos concedem R$ 5,00 de desconto, limitados ao saldo disponível e a 20% do subtotal elegível antes de descontos. Se o subtotal elegível for R$ 20,00, o teto é R$ 4,00; o bloco de R$ 5,00 não cabe e não será aplicado. O cliente escolhe o resgate; o servidor recalcula elegibilidade e valor, reserva pontos na criação do pedido e confirma o débito apenas no pagamento aprovado, tudo transacionalmente. Falha técnica/recusa libera ou mantém a reserva conforme a decisão transacional; cancelamento antes do pagamento devolve pontos reservados uma única vez. Não acumular pontos sobre parcela paga com resgate.

Modelo proposto: ContaFidelidade e LancamentoPontos (crédito, reserva, débito e devolução), com pedido de origem e proteção contra crédito duplicado. O acúmulo ocorre somente quando o pedido chega a ENTREGUE, desde que pago e com adesão ativa; incide sobre o valor efetivamente pago após desconto, excluindo a parcela coberta por pontos, e arredonda para baixo em reais inteiros. Não creditar em pedido recusado, cancelado ou apenas pago ainda não entregue. Revogar adesão impede créditos/resgates novos e congela o saldo existente; não apaga histórico de pedidos nem converte pontos em dinheiro. Antes de pagar, cancelamento devolve a reserva de pontos uma única vez; para cancelamento após pagamento, que o MVP não oferece, a futura política deverá reverter pontos/benefícios junto do estorno antes de concluir o cancelamento.

Rotas candidatas, **não implementadas**: `GET /api/fidelidade/saldo`, `GET /api/fidelidade/extrato` e solicitação de resgate durante criação de pedido. O próprio cliente acessaria seu saldo/extrato; o resgate seria recalculado pelo servidor.

### 7.4 Promoções e campanhas

Regra concreta de exemplo (**proposta; nenhuma campanha está implementada**): campanha `NORDESTE10`, válida de 01/11/2026 00:00 a 31/12/2026 23:59 UTC, aplica 10% aos itens ativos do produto elegível “Tapioca” em pedidos dos canais APP/WEB nas unidades explicitamente habilitadas. A API valida intervalo semiaberto de vigência, situação ativa, unidade, canal, produto e subtotal elegível; não há desconto sobre taxa/itens inelegíveis. O cálculo usa centavos inteiros e arredondamento para baixo por item; o pedido persiste campanha, valor e itens abrangidos para preservar histórico. Campanha expirada, desativada ou inelegível não concede desconto e produz erro de negócio.

Permitir no máximo uma campanha por pedido. **Não combinar campanha e resgate de pontos** na proposta inicial: se o pedido aplicar `NORDESTE10`, não aceita resgate; a resposta deve indicar essa incompatibilidade antes da confirmação. Essa escolha evita descontos encadeados e simplifica conferência; poderá ser revista como decisão de negócio futura. Datas seriam armazenadas em UTC e apresentadas no fuso da operação.

Rotas candidatas, **não implementadas**: gestão de `/api/campanhas` por ADMIN e campo de campanha na criação de pedido. Campanha inexistente, expirada ou inelegível produziria erro de negócio documentado.

## 8. Entregáveis obrigatórios da conclusão

| ID | Entregável | Etapa de execução | Critério de conclusão |
|---|---|---|---|
| ENT01 | Requisitos, escopo e propostas | 1 — este documento | RF/RNF identificados, prioridades, estados e critérios de aceite definidos. |
| ENT02 | Complementos da API | 2 | RF10, RF11 e RF16 implementados; migrações quando necessárias; regressões verificadas. |
| ENT03 | Privacidade e segurança | 3 | RF14/RF15 e documentação RNF04 concluídos; controles técnicos demonstrados com dados fictícios. |
| ENT04 | Swagger, Postman e plano de testes | 4 | Contratos completos; cenários organizados com evidências; build e testes aprovados após as alterações. |
| ENT05 | Diagramas | 5 | Casos de uso, DER, classes e sequência em formato visual legível, coerentes com a versão final; propostas claramente separadas do modelo implementado. |
| ENT06 | Documentação técnica e feature crítica | 6 | README reproduzível; Pedido + Pagamento com atores, pré/pós-condições, fluxo, exceções; mapa requisito → código → teste e resultados de verificações RNF09/RNF10. |
| ENT07 | PDF acadêmico | 7 | Capa, sumário, introdução, requisitos, diagramas, contratos, privacidade, testes, conclusão e referências; identificação e RU reais; layout revisado segundo orientações do AVA. |
| ENT08 | Repositório e validação da entrega | 8 | Repositório público e links verificados, histórico verdadeiro, sem segredos versionados; execução a partir de cópia limpa. |

## 9. Rastreabilidade inicial

| Requisitos | Implementação/evidência atual ou destino |
|---|---|
| RF01–RF02, RNF03 | Implementação atual: `JwtService`, `AuthenticationFilter`, `ApiController` e testes em `src/test/java`. |
| RF04–RF05, RF16 | `ApiController` e `RaizesStore`; unidades e produtos são paginados e o estoque permanece separado por unidade. |
| RF06–RF08, RN01–RN05 | `ApiController`, `RaizesStore` e testes Java de reserva/rollback/concorrência. |
| RF09 | `ApiController`, `RaizesStore`, listagem paginada e testes de isolamento por cliente. |
| RF10–RF11, RN06–RN08, RNF11 | `PaymentGateway`, `MockPaymentGateway`, `ApiController` e testes Java de recusa/nova tentativa/concorrência. |
| RF12–RF13, RN09–RN10 | `ApiController`, `RaizesStore` e testes Java de transições/cancelamento. |
| RF14–RF15, RNF04 | `RaizesStore`, rotas de auditoria/consentimento e testes Java; conforme [privacidade](03_PRIVACIDADE_E_SEGURANCA.md). |
| RF03, RF17–RF20 | Seção 7 deste documento; sem endpoints executáveis ou testes de funcionalidade nesta versão. |
| RNF06–RNF08, ENT04/ENT06 | `/api-docs.json`, `postman/raizes_nordeste_clean_collection.json`, testes em `src/test/java` e `README.md`. `mvn test` passou com 21 testes em 12/10/2026. Newman tem evidência histórica de 32 requisições/88 asserções em 11/10/2026; não foi repetido nesta revisão. |

## 10. Dependências e decisões para as próximas etapas

- RF16 muda o formato de retorno de produtos e unidades; ajustar documentação e testes na mesma entrega técnica, sem apresentar o contrato antigo como atual.
- O gateway mock deve produzir falhas determinísticas para testes. Não introduzir dependência de provedor real nem credenciais financeiras.
- Consentimento é persistido com finalidade, versão, decisão e histórico; o booleano legado não substitui esses registros. A migração V1 mantém o modelo atual.
- A auditoria deverá ter uma forma segura de verificação pelo avaliador: consulta controlada por ADMIN ou evidência automatizada/local documentada, sem endpoint público de logs.
- A migração V1 SQLite é adequado à demonstração local escolhida. Escala de produção e disponibilidade elevada exigem avaliação adicional e ficam fora das promessas desta entrega.
- Cinco commits de evolução são recomendados no roteiro; registrar as alterações reais a partir do estado disponível, sem inventar histórico retroativo.
- Dados acadêmicos (nome, RU, curso, polo e cidade), orientações do AVA e destino do repositório serão necessários nas etapas de entrega. Não bloqueiam a etapa técnica seguinte.

## 11. Critério de encerramento da etapa 1

- [x] Contexto, objetivo e atores definidos.
- [x] Fluxo A escolhido e justificado.
- [x] Requisitos funcionais e não funcionais identificados e priorizados.
- [x] Situação atual separada de implementação planejada e proposta conceitual.
- [x] Regras de negócio e critérios de aceite registrados.
- [x] Fidelização, promoções, cadastro e gestão especificados conceitualmente.
- [x] Entregáveis e dependências das etapas 2 a 8 definidos.

Esta matriz foi atualizada após os complementos de pedido/pagamento, privacidade, paginação e escopo por unidade. Permanecem propostas as capacidades administrativas, de pontos, campanhas e produção explicitamente identificadas.

## Referências locais

- `UNINTER_Roteiro_Back_End_Copia_Adaptada_Estudo.pdf`: páginas físicas 5–6 (RF/RNF), 6–9 (diagramas, contratos e privacidade), 9–13 (entregáveis e testes) e 18 (tabela consolidada), conforme leitura registrada na análise de aderência.
- [README e funcionalidades atuais](../README.md).
- [Schema SQL implementado](../src/main/resources/db/migration/V1__initial_schema.sql).
- [Testes Java](../src/test/java/br/com/raizesdonordeste/ApiIntegrationTest.java).

Feature crítica consolidada: [pedido, pagamento e status](09_FEATURE_CRITICA.md).
