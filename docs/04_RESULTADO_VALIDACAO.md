# Resultado de validação — correções da API Java

Atualização: 11/10/2026. Evidências desta revisão, sem reutilizar contagens da implementação removida. [Resumo estruturado](07_EVIDENCIAS_CORRECOES.json).

| Verificação | Resultado |
|---|---|
| Build e testes | `mvn test package`: BUILD SUCCESS; 20 testes, zero falhas, erros ou ignorados. |
| Composição da suíte | 16 de integração, 2 do gateway e 2 de migrações Flyway. |
| Coleção Postman | Newman 6.2.1: 32 requisições e 88 asserções aprovadas; 9 pastas, 17 cenários 2xx e 15 negativos. |
| Contrato OpenAPI | Schemas, campos obrigatórios, enumerações, permissões, exemplos e erros servidos pela API; 76 exemplos compatíveis com schemas e 4 respostas públicas reais validadas. |
| Banco novo | Flyway V1 cria nove tabelas de domínio; migração repetida não reaplica V1 e valida checksums. |
| Banco existente | Baseline 0 + V1 incorporados sem apagar dados; 2 pedidos anteriores preservados na base local. Teste automatizado também preserva registro de usuário em banco temporário. |
| Reinício local | Pedido e pagamento preservados; startup, health e consulta HTTP confirmados. |
| PDF | 14 páginas A4; fontes incorporadas, margens 3/2 cm aproximadas, corpo 12 com entrelinha 1,5, sumário paginado, diagramas e exemplos atuais; validação textual e revisão visual. |
| GitHub | Repositório público acessível por HTTP; atualizações desta revisão enviadas em commit de evolução real, sem reescrever histórico. |

## Reproduzir

Prepare variáveis e seed conforme o [README](../README.md), usando banco separado para execução repetida da coleção.

```bash
mvn test package
npx --yes newman@6.2.1 run postman/raizes_nordeste_clean_collection.json --env-var baseUrl=http://127.0.0.1:3000
mvn -Dexec.mainClass=br.com.raizesdonordeste.tools.AcademicPdfGenerator compile exec:java
```

Newman é runner externo opcional; Postman permite executar a mesma coleção sem Node/npm. Não exportar tokens preenchidos. Maven não executa a coleção automaticamente.

O gerador PDF foi executado e validado separadamente; a recompilação/empacotamento após seus últimos ajustes usou `-DskipTests`, pois os ajustes finais foram somente no gerador e nas fontes incorporadas. A execução efetiva dos 20 testes é a de `mvn test package` acima, não a etapa que pulou os testes.

## Evidência anterior e limites

O ensaio de carga em [06_RESULTADOS_OPERACIONAIS.json](06_RESULTADOS_OPERACIONAIS.json) pertence à medição anterior: 20 clientes/60,01 s, 228.399 requisições e p95 11,76 ms. Não foi repetido após estas correções; não mede escrita, disponibilidade distribuída ou restauração de backup.

Os materiais de referência fornecidos são cópias editadas para estudo, com omissões e prevalência do original. A conformidade ABNT deve ser conferida com as normas efetivas do AVA. Curso, polo, semestre e docente permanecem “a informar” por orientação expressa da autora.

Cadastro público, gestão administrativa completa, entradas/ajustes de estoque, pontos e campanhas são propostas documentadas; não se apresentam como endpoints implementados. O sistema é uma demonstração local, sem integração financeira real.
