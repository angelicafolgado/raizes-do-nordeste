# Análise de aderência ao roteiro UNINTER

> A matriz abaixo é o diagnóstico inicial e foi preservada como histórico; não representa o estado atual. Para a situação atual, consulte o resumo a seguir, a [rastreabilidade da etapa 6](docs/06_RASTREABILIDADE.md) e a [correção de desempenho](docs/06_CORRECAO_DESEMPENHO.md).

Atualização: 11/10/2026. O roteiro local, apesar do nome do arquivo indicar cópia adaptada para estudo, foi aceito como referência oficial desta entrega por orientação do aluno. Esta decisão identifica a referência usada no trabalho; não altera o conteúdo do arquivo.

## Situação atual

| Frente | Estado confirmado |
|---|---|
| Etapa 6: desempenho | Resolvido no cenário medido. Ensaio com 20 clientes por 60 segundos: 48.486 consultas, p95 de 39,37 ms, zero erros e zero respostas 5xx. A suíte atual passa com 37/37 testes. Ver [correção e limites do ensaio](docs/06_CORRECAO_DESEMPENHO.md) e [resultado operacional](docs/06_RESULTADOS_OPERACIONAIS.json). |
| Etapa 7: PDF acadêmico | Elaborado com nome e RU informados nos dados existentes do projeto. O gerador e o PDF incorporam o roteiro adotado como oficial por orientação do aluno; a etapa de elaboração do documento está concluída. |
| Etapa 8: publicação | O conteúdo final será publicado em `https://github.com/angelicafolgado/raizes-do-nordeste.git` em um único commit de Angelica Dias Folgado (`Angelicafolgado1@gmail.com`); o push com substituição da história remota ainda está pendente. |
| Etapa 8: cópia limpa | Validada em cópia isolada temporária: instalação pelo lockfile congelado, geração do Prisma, migrations e seed em SQLite novo, compilação TypeScript e 37/37 testes aprovados. A cópia começou sem `.env`, dependências instaladas ou banco local; o setup criou os próprios dados de ambiente. |

## Matriz inicial de aderência (registro histórico)

Referência: `UNINTER_Roteiro_Back_End_Copia_Adaptada_Estudo.pdf`, fornecido nesta pasta e adotado como oficial para esta entrega por orientação do aluno. A numeração abaixo é a página física do PDF (incluindo capa). A tabela consolidada da página 18 foi considerada prioritária, conforme instrução do roteiro. As situações desta matriz são o retrato registrado na análise inicial; para o estado atualizado, use a tabela anterior e os artefatos da etapa 6.

| Requisito | Referência | Situação e evidência |
|---|---|---|
| MVP: pedido → pagamento mock → status | p. 10, seção 7.2 | Atendido. Casos de uso de criação, pagamento e atualização; persistência real e testes de ponta a ponta. |
| JWT, perfis e senha com hash | pp. 9–10 | Atendido no MVP. `src/infrastructure/security/`, `src/api/middlewares/authMiddleware.ts` e `src/api/routes/pedidoRoutes.ts`. |
| Multicanalidade | p. 5 | Atendido na API e domínio. Enum TypeScript, canal obrigatório e filtro na listagem. O Prisma 5/SQLite armazena string; a validação ocorre na aplicação, conforme explicado no README. |
| Estoque por unidade | pp. 5, 10 | Atendido para reserva, consulta, restrição de venda e devolução no cancelamento. Não há endpoint administrativo de entrada/ajuste nem modelo de movimentações. Isso não impede escolher o Fluxo A, mas deve constar na análise do escopo proposto. |
| Arquitetura por camadas | pp. 6–7 | Atendida. Domain, Application, Infrastructure e API; interfaces de persistência e segurança com composição de dependências. |
| Banco, migrations e seed | pp. 9–10 | Atendidos. `prisma/schema.prisma`, `prisma/migrations/` e `prisma/seed.ts`. O schema não substitui o DER exigido. |
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
5. A publicação no repositório público, com README e links acessíveis, foi concluída. O roteiro aceita instruções de Swagger local e torna deploy opcional.
6. O PDF acadêmico com identificação do aluno/RU e referências foi gerado em `output/pdf/4937552_Projeto_Back_End.pdf`. A referência do roteiro foi adotada conforme orientação do aluno.

## Fontes locais inspecionadas

- Roteiro PDF completo e inspeção visual da tabela consolidada.
- `README.md` e inventário dos arquivos da pasta.
- `src/api/routes/`, `src/api/docs/swagger.json` e middlewares.
- `src/application/use-cases/ProcessarPagamentoMockUseCase.ts` e demais casos de uso indicados pela busca de rotas/auditoria.
- `src/infrastructure/repositories/PrismaStore.ts`, entidades e seed.
- `postman/raizes_nordeste_clean_collection.json` e `tests/api.test.ts`.

Nenhuma alteração no código da API foi feita nesta revisão. Não foi atribuída nota: a avaliação final cabe ao professor e depende dos artefatos e links efetivamente entregues.
