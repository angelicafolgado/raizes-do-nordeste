# Etapa 1 — Requisitos e escopo

Projeto: Raízes do Nordeste — API Back-end  
Versão: 1.0 — 11/10/2026  
Situação: baseline de requisitos e decisões de escopo da etapa 1. Para o estado implementado e suas evidências atuais, consulte [rastreabilidade](06_RASTREABILIDADE.md) e [resultado de validação](04_RESULTADO_VALIDACAO.md).

## 1. Objetivo e contexto

A rede Raízes do Nordeste precisa consolidar pedidos de diferentes canais e acompanhar o atendimento em suas unidades. Cada unidade mantém seu próprio estoque; a API deve impedir vendas sem disponibilidade, registrar pagamentos simulados e controlar a preparação e a entrega.

O objetivo da entrega é demonstrar uma API REST reproduzível, com persistência real, autenticação, autorização, regras transacionais e documentação acadêmica coerente com o código. A matriz administra a visão da rede, enquanto as unidades representam os locais de atendimento e armazenamento de estoque. O modelo atual não contém uma entidade Matriz nem vinculação de funcionários a uma unidade: o perfil ADMIN representa a administração da rede e os perfis operacionais têm acesso global. Restrição de funcionários por unidade é uma evolução proposta.

Por orientação da discente, os PDFs fornecidos pela discente foram usados como referência de conteúdo e não são distribuídos neste repositório. Os próprios arquivos se identificam como versões editadas para estudo, com conteúdo omitido e prevalência do original; não se afirma que a procedência oficial no AVA foi autenticada.

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
| Atendente (Balcão) | Criar pedidos em seu próprio usuário operacional, consultar pedidos da rede e processar pagamento mock. Vincular o pedido a um cliente atendido é proposta futura. |
| Cozinha | Consultar pedidos e executar transições operacionais; cancelar pedido ainda não pago. |
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
| RF09 | Consultar pedidos com paginação e isolamento por cliente. | P1 / Implementado | Validar page/limit, limitar a 100 itens por página e retornar metadados; cliente vê apenas seus pedidos; perfis operacionais consultam a rede. |
| RF10 | Solicitar e registrar pagamento mock por contrato de gateway. | P1 / Parcial | Preservar aprovação/recusa atuais; mover simulação para adaptador da infraestrutura, acessado por interface da aplicação; comprovar envio, retorno e correlação com o pedido. |
| RF11 | Tratar recusa, falha técnica e repetição de pagamento. | P1 / Parcial | Recusa mantém pedido aguardando pagamento; nova tentativa é permitida; pedido aprovado não é cobrado novamente. Falha técnica simulada retorna erro padronizado e não aprova pedido nem grava pagamento aprovado. |
| RF12 | Atualizar status respeitando a sequência operacional. | P1 / Implementado | Permitir EM_PREPARO → PRONTO → ENTREGUE apenas a COZINHA/ADMIN; rejeitar saltos/regressões com 409. |
| RF13 | Cancelar pedido ainda não pago e devolver estoque. | P1 / Implementado | COZINHA/ADMIN pode mudar AGUARDANDO_PAGAMENTO para CANCELADO; restituição ocorre uma vez; pedido cancelado não aceita pagamento. |
| RF14 | Auditar ações sensíveis e acessos operacionais a pedidos. | P1 / Parcial | Manter logs de login, criação, pagamento e status; acrescentar registro de consultas operacionais com ator, ação e instante, sem senha, JWT ou cópia integral de dados pessoais. Definir evidência reproduzível de auditoria para o avaliador. |
| RF15 | Registrar e permitir revogar consentimento opcional. | P1 / Planejado | Usuário autenticado registra apenas sua própria decisão para finalidade identificada, com data e versão do aviso; revogação fica rastreável e não bloqueia pedidos comuns. Consentimento de demonstração inserido pelo seed deve ser identificado como fictício. |
| RF16 | Paginar consultas de produtos e unidades. | P2 / Planejado | Aceitar page/limit, com padrão 1/10 e máximo 100; retornar dados e metadados, preservando filtros. Atualizar Swagger, Postman e consumidores de teste para o novo formato. |
| RF17 | Administrar produtos e unidades. | P3 / Proposto | Especificar criação/edição/inativação por ADMIN; impedir remoções que quebrem pedidos históricos e manter identificação estável. |
| RF18 | Registrar entradas e ajustes administrativos de estoque. | P3 / Proposto | Descrever movimentação com unidade, produto, quantidade, tipo, motivo, autor e data; bloquear saída sem saldo e preservar histórico. O MVP já implementa saída por pedido e devolução por cancelamento. |
| RF19 | Acumular e resgatar pontos mediante adesão opcional. | P3 / Proposto | Especificar saldo, lançamentos, acúmulo único por pedido entregue e resgate sem saldo negativo; detalhamento na seção 7. |
| RF20 | Aplicar promoções/campanhas elegíveis. | P3 / Proposto | Especificar vigência, unidade, canal, produtos elegíveis e desconto calculado pelo servidor; detalhamento na seção 7. |

RF15 não implica implementar o programa de pontos: nesta entrega, o consentimento será uma capacidade técnica demonstrável para uma finalidade opcional claramente descrita. A política documental deverá manter coerência com as funcionalidades realmente executadas.

## 5. Requisitos não funcionais

| ID | Requisito | Prioridade / estado | Critério de aceite |
|---|---|---|---|
| RNF01 | Separar responsabilidades de API, segurança, persistência e pagamento. | P1 / Implementado | Implementação Java separa `ApiController`, filtros/configuração, `RaizesStore` e o contrato `PaymentGateway`; limites e responsabilidades estão descritos na documentação técnica. |
| RNF02 | Persistir dados com integridade e migrações reproduzíveis. | P1 / Implementado | Banco recriado por migrações; PK/FK e unicidade coerentes; seed repetido não apaga pedidos nem repõe estoque consumido. |
| RNF03 | Proteger credenciais e segredos. | P1 / Implementado | Senhas com bcrypt, JWT assinado e validado, segredo externo ao código e ignorado pelo versionamento; responses sem hashes/tokens de terceiros. |
| RNF04 | Explicitar finalidade, minimização e ciclo de vida dos dados. | P1 / Planejado | Inventário por campo e finalidade, base de tratamento a fundamentar, controle de acesso, estratégia de retenção/anonimização e procedimento de revogação; distinguir política proposta de mecanismo implementado. |
| RNF05 | Padronizar erros e validar entradas. | P1 / Implementado | Envelope com error, message, details, timestamp, path e requestId; sem stack trace no cliente; status coerentes para validação, acesso e conflito. |
| RNF06 | Manter contratos OpenAPI completos e compatíveis. | P1 / Parcial | Cada operação tem parâmetros, permissões, exemplos de request quando aplicável, response de sucesso e erro, além dos códigos possíveis. |
| RNF07 | Permitir instalação e execução local reproduzíveis. | P1 / Implementado | Em diretório limpo, instruções do README instalam dependências, configuram ambiente, migram, populam e iniciam a API. Revalidar ao fechar a entrega. |
| RNF08 | Garantir evidências de teste reproduzíveis. | P1 / Parcial | Build e testes passam; coleção organizada em pastas com pelo menos 10 cenários, 6 positivos e 4 negativos, cobrindo os casos obrigatórios e evidência de auditoria; cada cenário possui pré-condição e resultado esperado. |
| RNF09 | Limitar custo das consultas e avaliar concorrência. | P2 / Parcial | Aplicar RF16; preservar testes de última unidade e pagamento concorrente. Proposta de medição local: 20 clientes concorrentes durante 60 s em consultas, registrar ambiente, p95 e erros; meta inicial p95 ≤ 1 s e menos de 1% de 5xx. Meta ainda não medida; não é garantia de produção. |
| RNF10 | Recuperar a operação após falhas locais. | P2 / Parcial | Após reiniciar a API, pedidos persistidos permanecem consultáveis; manter health de processo e encerramento controlado; documentar backup/restauração do SQLite e verificar restauração em cópia de teste. Sem compromisso de disponibilidade 24×7. |
| RNF11 | Tolerar falhas do gateway simulado. | P1 / Planejado | Simular indisponibilidade e timeout determinísticos, sem rede financeira real; estado do pedido e reserva permanecem coerentes; registrar resultado técnico sem transformar falha em aprovação. |
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

## 7. Funcionalidades propostas: especificação conceitual

### 7.1 Cadastro e gestão administrativa

Cadastro de cliente receberia nome, e-mail e senha, normalizaria o e-mail, impediria duplicidade e nunca aceitaria ADMIN como papel escolhido pelo consumidor. A adesão opcional teria campo separado. Perfis operacionais seriam criados por administrador autorizado. Produtos/unidades seriam inativados em vez de apagados quando tivessem vínculo histórico.

Rotas candidatas, **não implementadas**: `POST /api/usuarios`, `POST /api/unidades`, `PATCH /api/unidades/{id}`, `POST /api/produtos` e `PATCH /api/produtos/{id}`. Funcionários por unidade exigiriam vínculo de autorização adicional no modelo.

### 7.2 Movimentação administrativa de estoque

Modelo proposto: MovimentacaoEstoque com produtoId, unidadeId, tipo (ENTRADA/SAIDA/AJUSTE), quantidade, motivo, usuarioId e criadoEm. Saldo e lançamento seriam atualizados na mesma transação, sem saldo negativo. Ajuste usaria variação assinada e justificativa obrigatória, mantendo o saldo anterior rastreável.

Rotas candidatas, **não implementadas**: `POST /api/estoques/movimentacoes` e `GET /api/estoques/movimentacoes?unidadeId=...`. O extrato seria paginado e restrito a administradores autorizados.

### 7.3 Fidelização

Política de demonstração proposta, não exigência literal do roteiro: conceder 1 ponto por R$ 1,00 efetivamente pago em pedido entregue, descartando a fração. Para R$ 38,50, conceder 38 pontos. Só acumular após adesão opcional ativa; não conceder pontos por pedidos recusados ou cancelados.

Resgate proposto: blocos de 100 pontos concedem R$ 5,00 de desconto, limitados ao saldo disponível e a 20% do subtotal elegível. Se o subtotal for R$ 20,00, um bloco de R$ 5,00 não cabe no limite e não será aplicado. Débito e reserva dos pontos devem ocorrer transacionalmente; cancelamento anterior ao pagamento devolve os pontos reservados uma única vez.

Modelo proposto: ContaFidelidade e LancamentoPontos (crédito, débito, devolução), com pedido de origem e proteção contra crédito duplicado. O acúmulo incide sobre o valor efetivamente pago após desconto. Revogar adesão impede novos créditos e resgates; tratamento do saldo e retenção deve ser explicitado na política proposta, sem exclusão indiscriminada do histórico de pedidos.

Rotas candidatas, **não implementadas**: `GET /api/fidelidade/saldo`, `GET /api/fidelidade/extrato` e solicitação de resgate durante criação de pedido. O próprio cliente acessaria seu saldo/extrato; o resgate seria recalculado pelo servidor.

### 7.4 Promoções e campanhas

Modelo proposto: Campanha com nome, início/fim, situação, unidades/canais elegíveis, produtos aplicáveis e desconto fixo ou percentual. O servidor validaria a vigência e calcularia o desconto em centavos, sem total negativo, gravando a campanha e o desconto efetivamente aplicados no pedido.

Para reduzir ambiguidades na primeira versão proposta, permitir apenas uma campanha por pedido e não acumulá-la com resgate de pontos. Registrar o desconto no pedido evita que edição futura da campanha altere pedidos anteriores. Datas seriam armazenadas em UTC e apresentadas no fuso da operação.

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
| RF14–RF15, RNF04 | `RaizesStore`, rotas de auditoria/consentimento e testes Java; consulte [privacidade](03_PRIVACIDADE_E_SEGURANCA.md). |
| RF03, RF17–RF20 | Seção 7 deste documento; sem endpoints executáveis ou testes de funcionalidade nesta versão. |
| RNF06–RNF08, ENT04/ENT06 | `/api-docs.json`, `postman/raizes_nordeste_clean_collection.json`, testes em `src/test/java` e `README.md`. A coleção foi validada contra Java: 32 requisições, 88 asserções sem falhas. |

## 10. Dependências e decisões para as próximas etapas

- RF16 muda o formato de retorno de produtos e unidades; ajustar documentação e testes na mesma entrega técnica, sem apresentar o contrato antigo como atual.
- O gateway mock deve produzir falhas determinísticas para testes. Não introduzir dependência de provedor real nem credenciais financeiras.
- Captura de consentimento não deve reutilizar um booleano genérico sem finalidade identificada; o modelo detalhado e a migração serão definidos na etapa 3.
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

A conclusão desta etapa não significa que os complementos planejados já foram implementados. O próximo trabalho é a **etapa 2: complementos da API**, guiada por RF10, RF11 e RF16.

## Referências locais

- `UNINTER_Roteiro_Back_End_Copia_Adaptada_Estudo.pdf`: páginas físicas 5–6 (RF/RNF), 6–9 (diagramas, contratos e privacidade), 9–13 (entregáveis e testes) e 18 (tabela consolidada), conforme leitura registrada na análise de aderência.
- [Análise de aderência](../ANALISE_ADERENCIA_UNINTER.md).
- [README e funcionalidades atuais](../README.md).
- [Schema SQL implementado](../src/main/resources/db/migration/V1__initial_schema.sql).
- [Testes Java](../src/test/java/br/com/raizesdonordeste/ApiIntegrationTest.java).
