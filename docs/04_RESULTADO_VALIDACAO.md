# Resultado de validação — revisão de 12/10/2026

## Execuções desta revisão

| Verificação | Resultado observado |
|---|---|
| Java e suíte Maven | JDK 21.0.10; `mvn test` terminou com BUILD SUCCESS: 21 testes, 0 falhas, 0 erros e 0 ignorados. |
| Composição da suíte | 17 testes em `ApiIntegrationTest`, 2 em `DatabaseMigrationTest` e 2 em `MockPaymentGatewayTest`. |
| Isolamento por unidade | Coberto pela integração: empregado Recife não consulta/cria/paga/atualiza pedido de João Pessoa; ADMIN mantém visão da rede; vínculo operacional ausente falha com 403. |
| Migrações | Base temporária nova e base legada aplicaram V1/V2, repetição não reaplicou alterações e checksums foram validados; registro de usuário legado preservado. |
| PDF | Gerador `AcademicPdfGenerator` executado novamente por Maven; validação textual interna passou. PDFBox confirmou 46 páginas. Capa conferida em captura do navegador; identificação, sumário e conteúdo conferidos por extração textual. Não foi feita inspeção visual página a página. |
| GitHub | Nenhuma alteração desta revisão foi enviada ao GitHub. `output/` continua ignorado pelo Git. |

## Reproduzir

Com JDK 21 e Maven:

```powershell
mvn test
mvn "-Dexec.mainClass=br.com.raizesdonordeste.tools.AcademicPdfGenerator" compile exec:java
```

A primeira linha executa build de teste e toda a suíte Java/Flyway. A segunda gera o PDF local em `output/pdf/4937552_Projeto_Back_End.pdf` e o Markdown textual auxiliar. A pasta inteira permanece fora do Git.

## Evidências históricas que não foram repetidas

- A coleção Postman/Newman registra 32 requisições, 88 asserções, 17 respostas 2xx e 15 cenários negativos em nove pastas na execução anterior de 11/10/2026. Newman não foi executado nesta revisão; esses números não são resultado atual.
- O ensaio de carga anterior registrou 20 clientes, 60,01 s, 228.399 requisições e p95 de 11,76 ms. Não foi repetido após o escopo por unidade; não representa a carga desta versão.
- A validação em lote dos exemplos OpenAPI e a reinicialização HTTP local não foram repetidas.

Nesta revisão não foram executados teste de restauração de backup, carga, Newman, validação completa de cada norma ABNT nem certificação jurídica. Os PNG/SVG dos diagramas não foram regenerados após V2; o PDF inclui a renderização anterior, identificada como tal, e as fontes DOT atualizadas. As referências consultadas são versões adaptadas para estudo; prevalece a orientação institucional oficial do AVA. O PDF registra polo e cidade como não informados; não foram inventados dados.

## Limites funcionais documentados

Cadastro público, administração de unidades/produtos, movimentação de estoque, pontos e campanhas permanecem requisitos conceituais propostos, sem endpoint implementado. O consentimento executável é demonstrativo e não ativa fidelidade. A proposta concreta `NORDESTE10`, a regra de pontos e os limites do tratamento estão descritos no relatório e em [requisitos](01_REQUISITOS_E_ESCOPO.md) e [privacidade](03_PRIVACIDADE_E_SEGURANCA.md).
