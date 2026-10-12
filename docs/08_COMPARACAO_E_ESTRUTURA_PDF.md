# Comparação com o roteiro e organização do relatório

Revisão: 12/10/2026. Referências consultadas: estudo de caso e trilha Back-End em versões adaptadas para estudo; PDF de referência `4937546_Projeto_Back_End (1).pdf`; implementação e evidências do projeto de Angelica.

## Alcance da comparação

A aluna Angelica Dias Folgado desenvolveu o projeto e registrou suas evidências de validação. O repositório de Jeferson retornou HTTP 404 e a clonagem falhou; sua coluna abaixo representa somente o que seu PDF declara. Não foi possível verificar código, coleção, testes ou consistência entre implementação e relatório desse projeto. Não se atribuem notas nem se conclui que ambos atendem integralmente ao roteiro. Os originais completos do AVA prevalecem sobre as versões adaptadas consultadas.

| Critério do roteiro | Angelica: evidência própria | Jeferson: declaração no PDF de referência | Avaliação |
|---|---|---|---|
| Análise e requisitos | Escopo, RF/RNF, prioridades e rastreabilidade; fluxo A completo; funcionalidades adicionais propostas explicitamente. | Relatório apresenta requisitos priorizados, MVP e funcionalidades adicionais descritas como implementadas ou propostas. | Ambos documentam o critério; a implementação de Angelica tem recorte menor, permitido pelo fluxo obrigatório. |
| Modelagem e arquitetura | DER, casos de uso, classes conceituais, arquitetura técnica Java e atividade do fluxo. SQLite/JDBC e Flyway; limites da organização simplificada documentados. | Relatório apresenta modelagem e arquitetura com Java, Flyway e H2, além de diagramas nos apêndices. | Angelica possui os artefatos; os de Jeferson podem ser avaliados documentalmente, sem confirmação no código. |
| API e regras de negócio | Pedido, estoque transacional, gateway simulado e status; validação, paginação, erros e contrato OpenAPI. | Relatório descreve fluxo crítico e contratos, além de cadastro, administração, pontos, promoção e operações adicionais. | Fluxo principal de Angelica validado. Maior número de funcionalidades declarado por outro projeto não comprova melhor atendimento nem obriga ampliar o MVP. |
| Segurança, LGPD e auditoria | BCrypt, JWT, perfis, titularidade, logs e consentimento; anonimização e expurgo automático não implementados. | Relatório declara controles de acesso, consentimento e desidentificação, com limites de operação real. | Angelica documenta e testa seus controles; o PDF de Jeferson não substitui evidência de implementação. |
| Plano e evidências de testes | 21 testes Java aprovados nesta revisão (17 integração, 2 migração, 2 gateway); coleção anterior de Newman: 32 requisições e 88 asserções, não repetida nesta revisão. | Relatório declara 32 testes JUnit e uma coleção de 20 cenários, com evidências citadas. | Contagens representam suítes e datas diferentes. A execução de Jeferson não foi reproduzida nem seus arquivos externos examinados. |
| Entrega técnica e documentação | Repositório público acessível, README, coleção, contrato, diagramas e PDF único. Identificação própria e campos “a informar”. | Relatório organizado, instruções e apêndices; o próprio PDF registra 404 nas URLs públicas, confirmado também nesta revisão. | Angelica possui entrega acessível. Acesso ao código/coleção de Jeferson permanece pendente para uma comparação completa. |

## Estrutura adotada para o PDF de Angelica

A referência foi usada somente para organização: capa, folha de rosto, sumário, capítulos de introdução, requisitos, modelagem, API, segurança, testes, entrega e conclusão; referências; apêndices para contratos, cenários, evidências e diagramas. Foi acrescentado um apêndice para a atividade do fluxo crítico já existente no projeto de Angelica.

O conteúdo dos capítulos foi reorganizado a partir do relatório próprio. Os contratos vêm de `src/main/resources/openapi-contract.json`; os cenários vêm de `docs/04_PLANO_DE_TESTES.md`; diagramas e resultados vêm dos artefatos do projeto de Angelica. Não foram incorporados código, textos narrativos, figuras, regras de fidelidade/promoção, dados de identificação ou resultados de execução do projeto de referência. Títulos acadêmicos e categorias do roteiro servem apenas à organização comum.

O PDF de referência não foi incluído no repositório. Curso, período e trabalho constam na identificação acadêmica da aluna. Docente: Prof. Me. Luciane Yanase Kanashiro. Polo e cidade são apresentados como não informados. Os exemplos de contratos são ilustrativos e a medição anterior de desempenho é identificada como histórica; a execução atual da suíte Maven está registrada em [resultado de validação](04_RESULTADO_VALIDACAO.md).

## Pendências e limites

- Conferir identificação acadêmica e normas institucionais efetivas antes da entrega; os campos incompletos foram mantidos por solicitação.
- Não interpretar consentimento como implementação de pontos ou campanha no projeto de Angelica.
- Não apresentar teste de leitura local como validação de disponibilidade ou desempenho de produção.
- Para comparar o código de ambos, ainda é necessário acesso ao repositório ou ZIP de Jeferson. O PDF de referência permite avaliar estrutura e declarações, sem comprovar sozinho a execução.
- As fontes DOT foram atualizadas para o vínculo operacional de V2, mas PNG/SVG não foram renderizados novamente e o PDF ainda contém as imagens da renderização anterior. Graphviz não está disponível no ambiente; regenerar imagens e PDF permanece necessário para que todos os gráficos visuais mostrem o vínculo novo.

Atualização posterior: o repositório de Jeferson tornou-se público e foi disponibilizado em ZIP. As observações de inacessibilidade acima registram a comparação inicial baseada no PDF. Esta atualização do projeto de Angelica não incorpora arquivos ou resultados de Jeferson.
