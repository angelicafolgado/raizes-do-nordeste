# Análise de aderência ao roteiro UNINTER

> A matriz abaixo é o diagnóstico inicial e foi preservada como histórico; não representa o estado atual. Para a situação atual, consulte o resumo a seguir, a [rastreabilidade da etapa 6](docs/06_RASTREABILIDADE.md) e a [correção de desempenho](docs/06_CORRECAO_DESEMPENHO.md).

Atualização: 11/10/2026. Por orientação da discente, os materiais presentes em `uninter/` foram usados como referência desta análise. Os próprios PDFs se identificam como versões editadas para estudo, com conteúdo omitido e prevalência do original. A análise abaixo compara a entrega ao conteúdo desses arquivos indicados; não autentica a versão oficial no AVA.

## Situação atual

| Frente | Estado confirmado |
|---|---|
| Etapa 6: desempenho | Meta atingida no ensaio de leitura Java com 20 clientes por 60 segundos: 228.399 consultas, p95 de 11,76 ms, zero erros e zero respostas 5xx. A suíte Java atual passou com 18/18 testes. O teste não mediu escritas, reinício ou restauração. Ver [correção e limites do ensaio](docs/06_CORRECAO_DESEMPENHO.md) e [resultado operacional](docs/06_RESULTADOS_OPERACIONAIS.json). |
| Etapa 7: PDF acadêmico | Gerado em `output/pdf/4937552_Projeto_Back_End.pdf`, com texto-fonte Markdown ao lado. Identifica Angelica Dias Folgado, RU 4937552, inclui resultados medidos, referências, seções acadêmicas e o link público. Curso, polo, semestre e docente não foram informados; o PDF não os presume. Formatação ABNT não foi certificada contra as normas do AVA. |
| Etapa 8: publicação | Concluída em <https://github.com/angelicafolgado/raizes-do-nordeste>. `main` foi atualizada com `--force-with-lease`; o histórico anterior de Lako foi substituído. Os commits publicados estão no nome de Angelica Dias Folgado, sem coautoria Copilot. O histórico verdadeiro permanece abaixo dos cinco commits recomendados no material; não foram criados commits artificiais. |
| Etapa 8: cópia limpa | Validada em cópia temporária com `pom.xml`, `src` e os diagramas necessários ao PDF: `mvn -o test package` aprovou 18/18 testes e o gerador Java produziu e validou o PDF de 10 páginas. A cópia não recebeu `target` nem banco herdado. |
| Código executável Java | A API, os testes automatizados e as ferramentas mantidas são Java; a API de linguagens do GitHub reporta somente Java. Documentação, SQL, XML, JSON e artefatos de interoperabilidade permanecem em seus formatos próprios. A coleção Postman tem 32 cenários históricos e ainda não foi validada contra esta revisão Java. |

## Aderência atual aos principais critérios do material indicado

| Critério do material | Estado atual e evidência |
|---|---|
| 7.1 — Repositório público e README | Atendido quanto a código, estrutura, execução, Swagger, testes e link público no README. O histórico publicado está no nome de Angelica e permanece abaixo dos cinco commits recomendados, sem tornar isso requisito obrigatório. |
| 7.1 — OpenAPI/Swagger | A aplicação Java serve Swagger em `/api-docs` e o documento OpenAPI em `/api-docs.json`; o contrato é gerado pela aplicação. |
| 7.1 — Coleção Postman | Parcial. A coleção JSON está no repositório, mas seus 32 cenários foram preparados para a implementação anterior e não foram reexecutados nem conferidos contra o contrato Java. Não é evidência de execução atual. |
| 7.1 — Banco e DER | SQLite e `schema.sql` estão implementados. DER, casos de uso e parte dos diagramas são artefatos anteriores à implementação Java; a documentação os identifica como históricos e requer revisão de coerência antes de tratá-los como modelagem final. |
| 7.2 — MVP | Fluxo pedido → pagamento mock → status, persistência SQLite, JWT/perfis e erros JSON implementados; cobertos por testes Java. Limites da demonstração constam na documentação. |
| 8 — Plano de testes | 18 testes Java passaram, incluindo cenários positivos, negativos, autorização, estoque, pagamento, auditoria e concorrência. A coleção Postman exigida pelo material ainda não foi validada contra a API atual; a aderência a esse item é parcial. |
| Documento único PDF | PDF gerado com capa, sumário, seções, diagramas, conclusão e referências. O material pede curso, polo, semestre e docente na capa; esses dados não foram fornecidos. A verificação das normas ABNT do AVA e a revisão visual final ainda não foram realizadas. |
| Desempenho | Ensaio local de 20 clientes por 60,01 s atingiu p95 ≤ 1 s, sem erros nem 5xx. Não mede escrita, múltiplas instâncias, restauração ou disponibilidade de produção. |

## Matriz inicial de aderência (registro histórico)

A matriz abaixo é o diagnóstico inicial, anterior à implementação Java; seus estados não descrevem a revisão atual. Use a tabela de aderência atual acima e os artefatos Java para o estado final.

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
