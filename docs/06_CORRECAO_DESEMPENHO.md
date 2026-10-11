# Desempenho da API Java — ensaio com 20 clientes

Atualizado em 11/10/2026. Esta medição é da aplicação empacotada Java 21/Spring Boot, não da implementação TypeScript anterior. O resultado não deve ser comparado diretamente aos ensaios antigos: o cliente, a aplicação e a base utilizados foram substituídos.

## Critério e cenário

- 20 workers concorrentes durante 60 segundos, em ciclo fechado.
- Cada worker alterna consultas paginadas públicas a produtos de uma unidade e à lista de unidades.
- HTTP em loopback, SQLite isolado e populado pelo seed, com clientes HTTP persistentes.
- Timeout por chamada: 10 segundos. Falhas são contadas; não há retries silenciosos.
- Meta local: p95 ≤ 1.000 ms, zero falhas e menos de 1% de respostas HTTP 5xx.
- O ensaio mede leituras; não representa carga de escrita, rede externa, volume de produção ou disponibilidade garantida.

## Resultado medido

O [relatório JSON](06_RESULTADOS_OPERACIONAIS.json) foi gerado pelo comando Java em 11/10/2026:

| Métrica | Resultado |
|---|---:|
| Clientes simultâneos | 20 |
| Duração | 60,01 s |
| Requisições | 228.399 |
| p95 | 11,76 ms |
| Falhas totais | 0 |
| Respostas 5xx | 0 |
| Critério do ensaio | Atingido |

Ambiente medido: Windows 11 amd64, JDK 21.0.10, 12 CPUs lógicas e servidor Spring Boot empacotado. O relatório anterior, produzido para o backend TypeScript, foi substituído; esta execução não validou reinício ou restauração de banco e esses campos estão explicitamente sem medição no JSON.

## Reproduzir

Em um terminal, prepare uma base isolada e inicie a aplicação:

```powershell
$env:JWT_SECRET = 'um-segredo-aleatorio-com-mais-de-32-caracteres'
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlite:target/java-load-test.db'
mvn spring-boot:run
```

Em outro terminal:

```powershell
$argsMaven = @(
  '-q',
  '-Dexec.mainClass=br.com.raizesdonordeste.tools.LoadTest',
  '-Dexec.args=http://127.0.0.1:3000 20 60',
  'compile',
  'exec:java'
)
mvn.cmd @argsMaven
```

O processo imprime JSON com os valores observados e termina com código diferente de zero se a meta não for atingida. O relatório versionado registra a execução concluída; ensaios diagnósticos não devem substituir essa evidência sem revisar os resultados e a configuração.

## Correções de concorrência na implementação Java

As reservas de estoque continuam sendo atualizações condicionais dentro de transações. Uma trava justa na camada de persistência serializa transações de escrita dentro do processo Java para evitar conflitos de leitura-modificação-escrita do SQLite, que permite apenas um escritor por vez. As consultas de catálogo concorrentes permanecem fora dessa trava. Testes de integração verificam rollback da reserva, disputa de pagamento e 20 consultas concorrentes.

O ensaio de leitura não demonstra throughput de escrita ou comportamento com múltiplas instâncias da aplicação apontando para o mesmo arquivo SQLite. Um ambiente distribuído deve usar banco com concorrência multiwriter e uma estratégia de transação apropriada.
