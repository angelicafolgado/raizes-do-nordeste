# Análise de aderência ao roteiro UNINTER

> A matriz abaixo é o diagnóstico inicial e foi preservada como histórico; não representa o estado atual. Para a situação atual, consulte o resumo a seguir, a [rastreabilidade da etapa 6](docs/06_RASTREABILIDADE.md) e a [correção de desempenho](docs/06_CORRECAO_DESEMPENHO.md).

Atualização: 11/10/2026. Por orientação da discente, os materiais presentes em `uninter/` foram usados como referência desta análise. A adoção registra a fonte indicada pela discente e não certifica a procedência dos arquivos perante o AVA.

## Situação atual

| Frente | Estado confirmado |
|---|---|
| Etapa 6: desempenho | Meta atingida no ensaio de leitura Java com 20 clientes por 60 segundos: 228.399 consultas, p95 de 11,76 ms, zero erros e zero respostas 5xx. A suíte Java atual passou com 18/18 testes. O teste não mediu escritas, reinício ou restauração. Ver [correção e limites do ensaio](docs/06_CORRECAO_DESEMPENHO.md) e [resultado operacional](docs/06_RESULTADOS_OPERACIONAIS.json). |
| Etapa 7: PDF acadêmico | Gerado e validado em `output/pdf/4937552_Projeto_Back_End.pdf`, com texto-fonte Markdown ao lado. Identifica Angelica Dias Folgado, RU 4937552, inclui os resultados medidos e descreve o código executável Java. |
| Etapa 8: publicação | Ainda pendente. O push para `https://github.com/angelicafolgado/raizes-do-nordeste.git` não foi executado; não afirmar que a publicação ou a substituição do histórico remoto foi concluída. |
| Etapa 8: cópia limpa | Validada em cópia temporária com `pom.xml`, `src` e os diagramas necessários ao PDF: `mvn -o test package` aprovou 18/18 testes e o gerador Java produziu e validou o PDF de 10 páginas. A cópia não recebeu `target` nem banco herdado. |
| Código executável Java | A implementação TypeScript/Node e utilitários em Python/JavaScript foram removidos após a substituição pelos componentes Java. Documentação, SQL, XML, JSON e artefatos de interoperabilidade permanecem em seus formatos próprios; a coleção Postman ainda não foi validada contra esta revisão. |

## Matriz inicial de aderência (registro histórico)

Referência: materiais na pasta `uninter/`, indicada pela discente. A matriz abaixo é um registro histórico da análise inicial, anterior à implementação Java; seus detalhes não descrevem a revisão atual. Para o estado atual, use a tabela anterior e os artefatos Java. A conferência independente do roteiro oficial do AVA permanece pendente.

| Requisito | Referência | Situação e evidência |
|---|---|---|
| MVP: pedido → pagamento mock → status | p. 10, seção 7.2 | Atendido. Casos de uso de criação, pagamento e atualização; persistência real e testes de ponta a ponta. |
| JWT, perfis e senha com hash | pp. 9–10 | Estado inicial: atendido no MVP. A implementação atual correspondente está em Java, incluindo `JwtService` e `AuthenticationFilter`; consulte os arquivos Java e os testes de integração. |
| Multicanalidade | p. 5 | Estado inicial: atendido na API e domínio. A implementação atual Java valida o canal e filtra pedidos; SQLite persiste o valor como texto. |
| Estoque por unidade | pp. 5, 10 | Atendido para reserva, consulta, restrição de venda e devolução no cancelamento. Não há endpoint administrativo de entrada/ajuste nem modelo de movimentações. Isso não impede escolher o Fluxo A, mas deve constar na análise do escopo proposto. |
| Arquitetura por camadas | pp. 6–7 | Atendida. Domain, Application, Infrastructure e API; interfaces de persistência e segurança com composição de dependências. |
| Banco, migrations e seed | pp. 9–10 | Registro histórico da solução inicial com Prisma. Os arquivos legados foram removidos; o código Java atual inicializa o esquema por `src/main/resources/schema.sql` e o seed é `SeedRunner`. O schema não substitui o DER exigido. |
| Requisitos RF/RNF e prioridades | pp. 5–6, 18 | Parcial. README descreve implementação e limites, mas falta catálogo formal, contexto da rede, prioridades, justificativas e requisitos de desempenho, disponibilidade e tolerância a falhas. |
| Cadastro, fidelização, promoções e gestão | p. 5 | Incompleto na análise. Usuários, produtos e unidades são cadastrados pelo seed, sem gestão via API. Fidelização (pontos/resgate/consentimento) e promoções não têm implementação nem proposta documentada. O roteiro permite contemplar requisitos fora do MVP ao menos conceitualmente. |
| Casos de uso, DER e classes | pp. 6–7, 10 | Ausentes como artefatos de entrega. Faltam atores, relacionamentos, PK/FK, cardinalidades e restrições. A seção detalhada exige classes; a tabela consolidada aceita classes e/ou sequência/atividade. Entregar classes e sequência evita essa ambiguidade. |
| Descrição da feature crítica | p. 6 | Parcial. Há regras no README, mas faltam pré-condições, pós-condições, fluxo principal, exceções e vínculo com diagramas. |
| Pagamento externo simulado | pp. 5–6, 10 | Fluxo mock funcional, com aprovação/recusa, registro e retorno ao cliente. A decisão é interna ao caso de uso a partir de `statusSimulado`; não existe adaptador de gateway. Para representar melhor a integração descrita, documentar contrato de envio/retorno e falhas; considerar interface de gateway com implementação mock na infraestrutura. Não é necessário pagamento real. |
| Swagger/OpenAPI | pp. 7–10 | Presente, com rotas, schemas, permissões, códigos e exemplos de request. Faltam exemplos explícitos e realistas de responses e erros (o Swagger pode gerar amostras a partir dos schemas). Pedidos têm paginação; produtos e unidades não. |
| Segurança, privacidade e auditoria | pp. 9, 16 | Parcial. JWT, bcrypt, isolamento dos pedidos por cliente e logs de login/pedido/pagamento/status existem. Falta documentação de dados coletados, finalidade, base legal, minimização e retenção/anonimização segundo o roteiro. `consentimentoLGPD` é um booleano preenchido pelo seed, sem fluxo de captura ou revogação. Consultas de pedidos não geram logs de acesso. Esta é uma avaliação do requisito acadêmico, não certificação jurídica. |
| Quantidade e cobertura dos testes | pp. 11–12 | Boa cobertura. Na validação anterior, os 22 testes automatizados passaram; a coleção executada tem 16 cenários (9 respostas 2xx e 7 cenários de erro), superando 6 positivos e 4 negativos. Os testes não foram reexecutados nesta revisão documental. |
| Plano formal e organização Postman | pp. 11–12 | Parcial. A coleção está em lista única, sem pastas. Faltam pré-condições e documentação padronizada por cenário. Campo obrigatório ausente, recurso inexistente (404) e evidência de auditoria estão nos testes automatizados, mas não na coleção, embora a seção 8.1 peça que os cenários estejam refletidos nela. |
| README e execução local | pp. 9–10 | Atendidos para o MVP. Contém ambiente, instalação, banco, seed, execução, Swagger, testes e credenciais fictícias. Falta link do repositório público. |
| Repositório público e histórico | pp. 9–10 | Não comprovados. Não há `.git` nesta pasta nem link real do projeto no README. O roteiro exige repositório público; cinco commits são um mínimo recomendado, não uma quantidade obrigatória absoluta. Não se deve fabricar um histórico de evolução. |
| PDF acadêmico final | p. 13 | Ausente. O PDF nesta pasta é o roteiro, não o trabalho. Faltam capa, sumário, análise, diagramas, contratos, privacidade, plano de testes, conclusão, referências e nome no formato `RU_Projeto_Back_End.pdf`. As normas devem ser consultadas no AVA. |

## Prioridades para adequação

1. Elaborar RF/RNF, matriz implementado/proposto e justificativa da escolha do Fluxo A. Descrever cadastro, gestão, fidelização, promoções e movimentação de estoque ao menos como propostas consistentes.
2. Produzir casos de uso, DER e classes compatíveis com o código; detalhar Pedido + Pagamento. Acrescentar sequência do gateway mock para esclarecer a integração.
3. Documentar o tratamento de dados e consentimento, distinguindo controles implementados de propostas. Explicar retenção e anonimização.
4. Organizar a coleção em pastas e completar cenários e matriz T01… com pré-condições, entradas, status, trechos da resposta e evidências. Acrescentar exemplos reais ao Swagger e paginação de produtos quando aplicável.
5. A publicação no repositório público, com README e links acessíveis, permanece pendente. O roteiro aceita instruções de Swagger local e torna deploy opcional.
6. O PDF acadêmico com identificação do aluno/RU e referências foi gerado em `output/pdf/4937552_Projeto_Back_End.pdf`. A referência do roteiro foi adotada conforme orientação do aluno.

## Fontes locais inspecionadas

- Roteiro PDF completo e inspeção visual da tabela consolidada.
- `README.md` e inventário dos arquivos da pasta.
- A implementação Java atual em `src/main/java/` e os testes em `src/test/java/`.
- O esquema SQL atual em `src/main/resources/schema.sql`.
- `postman/raizes_nordeste_clean_collection.json`, mantida como artefato ainda não validado contra a API Java.

Nenhuma alteração no código da API foi feita nesta revisão. Não foi atribuída nota: a avaliação final cabe ao professor e depende dos artefatos e links efetivamente entregues.
