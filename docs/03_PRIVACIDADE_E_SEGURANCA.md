# Etapa 3 — Privacidade, consentimento e auditoria

## 1. Escopo e estado da implementação

Esta entrega é uma demonstração acadêmica local, com contas fictícias. Implementa RF14/RF15 e documenta RNF04. Os controles descritos abaixo não equivalem a certificação de conformidade jurídica ou a uma política pronta para uma operação comercial real.

Implementado:

- Aviso público, com finalidade identificada e versão.
- Registro e revogação da decisão opcional pelo próprio titular autenticado.
- Histórico persistente de eventos, com data, versão e indicação de demonstração.
- Auditoria de consultas a pedidos, histórico de consentimentos e consulta administrativa de logs.
- Acesso aos logs somente por ADMIN, com paginação, filtro e projeção de metadados permitidos.
- Respostas da API com `Cache-Control: no-store`; erros inesperados não serializam exceções completas no console; o campo `path` dos erros não inclui query string.

Permanecem documentados como procedimentos/propostas: expurgo periódico, anonimização de dados reais, atendimento formal de direitos e política operacional para produção. Não foram criados jobs de exclusão, canais externos de atendimento, programa de pontos ou envio de marketing.

## 2. Inventário de dados e finalidade

| Grupo | Dados persistidos | Finalidade | Acesso e minimização |
|---|---|---|---|
| Usuário | ID, nome, e-mail, hash de senha, perfil, criação e flag legada | Identificação, autenticação e autorização | Login retorna nome/e-mail/perfil do titular, nunca hash. Não são coletados CPF, endereço, telefone ou dados de cartão. |
| Pedido e itens | Usuário, unidade, canal, produtos, quantidades, preço, total, status e datas | Executar e demonstrar atendimento | Cliente consulta seus pedidos; operação consulta a rede. Não são incluídos nome/e-mail do cliente nas listagens de pedidos. |
| Pagamento mock | Pedido, valor, resultado, transação e data | Demonstrar aprovação/recusa e consistência de status | Nenhuma cobrança, cartão ou transmissão a provedor financeiro. |
| Consentimento | Titular por ID, finalidade, versão, decisão, data, demonstracao | Comprovar a preferência opcional dentro da demonstração | Titular só registra/consulta seu histórico. A identidade vem do JWT. Histórico não contém cópia de nome, e-mail ou token. |
| Auditoria | Ator por ID, ação, instante e metadados operacionais selecionados | Rastreabilidade e verificação de acesso/operações | ADMIN consulta; não há endpoint público de logs nem inclusão do objeto Usuario. Não são registrados corpo HTTP completo, IP, senha ou JWT nos logs de negócio. |
| JWT | ID, nome, perfil, emissão e expiração no token assinado | Autenticar chamadas | O token não é gravado na auditoria. Quem usa o cliente deve protegê-lo. Não há refresh/revogação central de tokens nesta versão. |

O SQLite e seus backups não são cifrados por esta aplicação. A proteção local depende das permissões do sistema operacional; o banco e `.env` não devem ser publicados. Auditoria contém IDs vinculáveis, portanto não é tratada como dado anônimo.

## 3. Fundamentos para o trabalho acadêmico

A modelagem distingue tratamento necessário ao pedido e preferência opcional. Como hipóteses para uma futura operação, cadastro/pedido podem ser avaliados sob execução de contrato; a preferência opcional exige manifestação específica. Auditoria, conservação e eventuais obrigações precisam de fundamento e prazos definidos pelo responsável da operação. Não se pressupõe que um consentimento genérico autorize todas as finalidades.

A LGPD prevê demonstração da manifestação de vontade e possibilidade de revogação (art. 8º), além de regras para término/conservação e direitos do titular (arts. 15, 16 e 18). Esses dispositivos orientam a distinção entre revogar uma finalidade e apagar indiscriminadamente registros. Fonte oficial: [Lei 13.709/2018 — texto compilado](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm).

As hipóteses acima são decisões de modelagem para o relatório, sujeitas à avaliação do contexto real; os prazos acadêmicos da seção 6 não são apresentados como prazos legais.

## 4. Aviso e ciclo de consentimento

Finalidade: `FIDELIDADE`. Versão inicial: `1.0`. O texto deixa claro que a aplicação apenas registra a preferência para demonstração: não ativa programa real, não acumula pontos e não envia mensagens. A implantação futura de outro tratamento exige novo aviso e nova decisão. O aviso publicado é servido pela implementação Java em [ApiController](../src/main/java/br/com/raizesdonordeste/api/ApiController.java); versões publicadas devem ser preservadas no histórico do projeto.

| Operação | Contrato |
|---|---|
| Ler aviso | `GET /api/privacidade/aviso`, público |
| Aceitar | `PUT /api/privacidade/consentimentos/fidelidade`, JWT, `{"aceito":true,"versaoAviso":"1.0"}` |
| Revogar | Mesma rota, `{"aceito":false}`; não é preciso aceitar ou informar a versão vigente |
| Consultar estado/histórico | `GET /api/privacidade/consentimentos?page=1&limit=10`, JWT, somente titular |

Não há decisão positiva presumida: sem evento, `atual.aceito=false`. O booleano `Usuario.consentimentoLGPD` é legado, não é consultado para autorizar a finalidade e não é convertido automaticamente em consentimento. O seed novo usa `false`, não fabrica eventos e preserva os dados existentes. Os registros têm `demonstracao=true`, inclusive quando produzidos via API nas contas fictícias.

Eventos de aceite/revogação são adicionados ao histórico sem sobrescrever os anteriores. O estado atual usa o último evento, independentemente da página consultada. Aceite de versão antiga não ativa a versão atual. Revogação com versão antiga continua possível. Repetir a mesma decisão via PUT não duplica o último evento; nova decisão diferente cria um evento novo.

Evento e auditoria são gravados na mesma transação: falha no log reverte também o consentimento. Recusar/revogar não impede login, pedido ou pagamento. Campos extras como `usuarioId` no corpo são rejeitados; filtros de identidade enviados na URL não ampliam o acesso ao histórico.

## 5. Auditoria de acesso

`CONSULTA_PEDIDOS` registra ator, data, página, limite, filtro de canal, quantidade retornada e escopo PROPRIOS/REDE. O log não copia os pedidos. Os eventos de criação, pagamento e status existentes são mantidos.

`CONSULTA_CONSENTIMENTOS`, `CONSENTIMENTO_REGISTRADO` e `CONSENTIMENTO_REVOGADO` registram a operação de privacidade. Os eventos de decisão referenciam ID do consentimento, finalidade, versão e caráter de demonstração.

`GET /api/auditoria?acao=CONSENTIMENTO_REVOGADO&page=1&limit=10` exige ADMIN. O filtro é por ação exata, em letras maiúsculas/sublinhado, até 64 caracteres. Cada leitura gera `CONSULTA_AUDITORIA` após selecionar a página; esse evento aparece em leituras seguintes. Assim, totais podem aumentar entre requisições, como em qualquer histórico ativo.

A resposta projeta somente chaves operacionais previstas. Campos arbitrários de registros legados e JSON inválido não são devolvidos como texto bruto. Não existe API de edição/exclusão dos logs; isso não torna o banco local imutável contra quem tem acesso direto ao arquivo.

## 6. Retenção e anonimização: política proposta para a demonstração

| Grupo | Regra proposta para o ambiente acadêmico | Execução atual |
|---|---|---|
| Contas, pedidos, pagamentos, decisões e auditoria fictícios | Manter até a avaliação e eventual revisão; revisar necessidade até 30 dias após o encerramento da avaliação | Revisão manual; nenhum job de expurgo implementado |
| Bases de testes isoladas | Excluir o arquivo ao finalizar a suíte | O banco de integração permanece em target/test-api.db até limpeza manual ou mvn clean; bancos temporários de DatabaseMigrationTest são gerenciados pelo JUnit |
| Cópias de segurança da demonstração | Mesma revisão e destino da base de origem; restringir acesso e não publicar | Procedimento manual |
| Evidências acadêmicas | Preferir dados fictícios e exemplos sem tokens, senhas, segredos ou banco real | Revisão na preparação do PDF/repositório |

Procedimento de encerramento: identificar a base correta e suas cópias, confirmar que contém apenas dados fictícios e que a avaliação terminou, separar evidências minimizadas necessárias, documentar a decisão de conservação ou descarte e executar o descarte autorizado. Nenhum dado existente foi apagado por esta etapa.

Se forem usados dados reais futuramente, estabelecer matriz de retenção por finalidade e fundamento antes da coleta. Uma proposta de anonimização deve remover identificadores diretos, revisar campos livres e relações que permitam reidentificação e validar o resultado. Trocar nome/e-mail mantendo vínculos identificáveis é pseudonimização, não garantia de anonimização. Não há mecanismo automático implementado aqui.

## 7. Procedimento proposto para direitos do titular

O titular pode consultar e revogar a preferência pelo próprio token. Para acesso ampliado, correção ou eliminação, o futuro responsável deverá definir canal, verificar identidade sem coleta excessiva, localizar dados em tabelas/logs/backups, avaliar conservação necessária, executar a medida aplicável e registrar a resposta. O projeto não inventa endereço de atendimento nem contato de encarregado.

## 8. Migração e verificação

O esquema Java está na migração [V1__initial_schema.sql](../src/main/resources/db/migration/V1__initial_schema.sql), incluindo consentimentos com FK. Flyway registra versões/checksums; baseline 0 permite incorporar o histórico em bases da versão anterior sem apagar registros. Antes de atualizar uma base existente, faça backup e valide a migração em uma cópia. Migrações futuras devem ser adicionadas, sem alterar V1 aplicada.

As verificações cobrem aviso, flag legada, idempotência, revogação, versões, isolamento por titular, bloqueio de atribuição a outro usuário, permissões ADMIN, auditoria de leituras, filtragem de dados legados, rollback de evento quando auditoria falha e sanitização de erros inesperados. O resultado final está registrado no plano de testes da etapa 4.

## 9. Situação dos requisitos

- RF14: auditoria de leituras e consulta administrativa implementadas.
- RF15: finalidade, versão, registro/revogação e histórico implementados como demonstração opcional.
- RNF04: inventário, hipóteses de tratamento, minimização, retenção/anonimização e procedimento de direitos documentados; limitações operacionais explícitas.
- Cadastro público, fidelização real e promoções permanecem propostas da etapa 1.
