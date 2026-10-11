# Resultado de validação da implementação Java

Atualizado em 11/10/2026. Este registro descreve a revisão Java 21/Spring Boot, não reexecuta nem atribui resultados da implementação Node/TypeScript removida.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| Compilação e pacote Java | `mvn test package`: BUILD SUCCESS. |
| Suíte automatizada Java | 18 testes aprovados; 0 falhas, 0 erros e 0 ignorados (16 de integração e 2 do gateway mock). |
| Cópia limpa | Cópia temporária contendo `pom.xml`, `src` e `docs/diagramas`, sem `target` ou banco herdado: `mvn -o test package` e geração do PDF concluídos com sucesso. |
| PDF acadêmico | 10 páginas; geração Java validada por extração de texto, com nome, RU, resultados de desempenho e plano de testes. |
| Ensaio de carga | 20 clientes por 60,01 s: 228.399 requisições, p95 de 11,76 ms, zero erros e zero respostas HTTP 5xx. Ver [método e limites](06_CORRECAO_DESEMPENHO.md). |
| Suíte legada TypeScript | Não executada nesta revisão; Node.js/npm não estão disponíveis no ambiente. O teste legado foi removido após a substituição da implementação e da suíte pela versão Java. |
| Coleção Postman | Os 32 cenários permanecem como artefato histórico; a coleção não foi reexecutada nem validada contra o contrato Java atual. |

Os testes Java usam SQLite isolado e cobrem autenticação/JWT, permissões, pedidos, reserva e rollback de estoque, pagamento simulado, transições, privacidade, auditoria, seed idempotente, erros HTTP e concorrência. Os resultados demonstram apenas o que foi executado localmente; não certificam produção ou integração financeira real.

## Comandos reproduzíveis

```powershell
mvn test
mvn package
```

Para gerar o PDF e o texto Markdown sincronizado:

```powershell
$pdfArgs = @(
  '-q',
  '-Dexec.mainClass=br.com.raizesdonordeste.tools.AcademicPdfGenerator',
  'compile',
  'exec:java'
)
mvn.cmd @pdfArgs
```

## Limites e pendências

- A coleção Postman requer revisão e execução manual contra a API Java antes de ser apresentada como evidência.
- Os diagramas de DER, casos de uso e sequência são artefatos documentais cuja origem precede a revisão Java; as ressalvas estão em [diagramas](05_DIAGRAMAS.md) e no PDF.
- A fonte indicada pela discente para o roteiro contém avisos de edição para estudo e prevalência do original. A conferência independente no AVA continua necessária.
- A publicação remota e qualquer atualização do histórico Git ainda não foram realizadas.
