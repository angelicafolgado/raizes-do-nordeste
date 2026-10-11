# Correção de desempenho das consultas públicas

Data: 11/10/2026. Escopo: RNF09, consultas de produtos e unidades com 20 clientes concorrentes durante 60 segundos.

## Alteração da API

As listagens abriam transações interativas por meio de `Store.transaction`, executando página e contagem em chamadas separadas entre JavaScript e o engine. O ensaio curto anterior à alteração reproduziu 15 respostas 500 e 7 timeouts em 33 tentativas.

`ListarUnidadesUseCase` e `ListarProdutosCardapioUseCase` agora chamam diretamente os métodos de consulta da porta. `PrismaStore` executa página e contagem por `$transaction([...])`, em lote. Mantém a transação para consistência da paginação, sem manter uma transação interativa aberta entre callbacks da aplicação. Quando já existe um cliente transacional, as consultas usam a transação externa.

Não foram removidas as transações de criação de pedido, reserva/devolução de estoque, pagamento, consentimento ou auditoria. Não há cache de saldo nem alteração do contrato HTTP.

## Correção do gerador de carga

Após a alteração da API, a execução com o cliente antigo apresentou p95 de 93,34 ms, mas registrou 8.763 `URLError` e uma resposta 500 em 31.874 tentativas. Esses resultados estão preservados em [ensaio sem reuso](06_RESULTADOS_OPERACIONAIS_CLIENTE_SEM_REUSO.json). `URLError` sozinho não identifica a causa de sistema operacional; esgotamento de recursos por abertura de conexões é uma hipótese, não uma conclusão comprovada.

O gerador antigo abria uma conexão para cada requisição. O verificador agora mantém uma conexão HTTP por cliente, lê integralmente cada resposta, verifica conteúdo de sucesso e classifica erros HTTP e de transporte. Continua usando 20 clientes em ciclo fechado, mesmos endpoints, base isolada, janela de 60 segundos e timeout de 10 segundos. Em caso de falha, fecha a conexão antes da tentativa seguinte. Não faz repetição oculta nem exclui falhas da latência.

A metodologia de conexão mudou: a comparação de antes/depois envolve tanto o caminho de consulta quanto o cliente de carga, portanto não é uma estimativa isolada do ganho da API. O resultado atual serve para verificar a meta no cenário explicitado.

## Evidências

**Meta atingida:** 48.486 consultas em janela de 60 segundos, p95 de 39,37 ms e zero erros (incluindo zero 5xx). Tempo total com conclusão das requisições: 60,26 s. O indicador `rnf09_target_met` é `true`.

- [Resultado original](06_RESULTADOS_OPERACIONAIS_ANTES.json): 151 tentativas, 138 erros e p95 de 10.015,89 ms.
- [Resultado final](06_RESULTADOS_OPERACIONAIS.json): medição após as correções, com reutilização de conexões.
- Compilação TypeScript aprovada.
- Suíte com **37 testes aprovados**, sem falhas; inclui o novo cenário “20 consultas publicas concorrentes retornam paginas sem falhas”, estoque concorrente, pagamento concorrente e os 32 cenários Postman.
- OpenAPI: 12 operações e 81 exemplos validados; coleção: 32 cenários em 9 pastas.
- Reinício, restauração e integridade SQLite conferidos pelo verificador operacional.

Reproduzir com `python scripts/verify_operations.py`, após instalar as dependências. O relatório é substituído a cada execução completa; o processo sinaliza falha quando não atinge a meta. `OPERATIONS_SECONDS` permite diagnósticos curtos, salvos somente na pasta isolada de teste. Logs ficam em `.test-output/operations-*/server.log`.

O ensaio não representa carga de escrita, rede externa, base volumosa ou garantia de produção.
