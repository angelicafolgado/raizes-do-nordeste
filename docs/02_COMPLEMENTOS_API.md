# Etapa 2 — Complementos da API

Esta etapa implementa RF10, RF11 e RF16 definidos no documento de requisitos. Não houve alteração do schema ou necessidade de migração: os resultados e falhas usam as tabelas existentes de pedidos, pagamentos e auditoria.

## Gateway de pagamento desacoplado

- `src/application/ports/PaymentGateway.ts` define solicitação e resposta sem dependências de infraestrutura.
- `src/infrastructure/payments/MockPaymentGateway.ts` implementa a simulação local determinística.
- `src/composition.ts` injeta o adaptador no caso de uso.
- `ProcessarPagamentoMockUseCase` valida autorização e estado, envia o valor em centavos, verifica a correlação e usa o retorno do gateway para decidir o novo status.

Solicitação interna: `pedidoId`, `valorCentavos`, `moeda: BRL`, `correlacaoId` e `cenario`. Valor e identidade do pedido vêm do banco, não do corpo enviado pelo cliente. A resposta mantém pedido e correlação; aprovação/recusa também retornam um identificador de transação.

O endpoint existente aceita os seguintes corpos:

| Corpo JSON | HTTP | Efeito |
|---|---|---|
| `{"statusSimulado":"APROVADO"}` | 200 | Registra aprovação e muda para EM_PREPARO. |
| `{"statusSimulado":"RECUSADO"}` | 200 | Registra recusa e mantém AGUARDANDO_PAGAMENTO. |
| `{"statusSimulado":"INDISPONIVEL"}` | 503 | Registra FALHA_PAGAMENTO_MOCK sem alterar pedido, pagamento anterior ou estoque. |
| `{"statusSimulado":"TIMEOUT"}` | 504 | Mesmo comportamento de preservação, com código GATEWAY_TIMEOUT. |

O ID de correlação aparece nos detalhes do erro técnico e na auditoria. A auditoria da falha é confirmada antes de devolver o erro HTTP. Retorno com pedido/correlação diferente é rejeitado com 502, sem pagamento ou mudança de status.

O mock representa o resultado de timeout imediatamente; não implementa relógio de timeout de rede ou chamada financeira externa. Como é local e imediato, participa da transação existente. Um gateway real exigiria outro desenho para evitar transação aberta durante I/O externo, com idempotência, reconciliação e controle de resultados incertos; isso permanece fora desta entrega.

Pagamento já aprovado continua protegido contra repetição. Recusas e falhas técnicas permitem nova tentativa; o histórico de falhas fica na auditoria e não sobrescreve um pagamento recusado anterior.

## Paginação de produtos e unidades

As rotas `GET /api/produtos` e `GET /api/unidades` aceitam `page` e `limit`. Padrões: 1 e 10. Limite máximo: 100; números fracionários, negativos, parâmetros repetidos ou deslocamento excessivo são rejeitados com 422.

Novo envelope:

```json
{
  "dados": [],
  "paginacao": {
    "pagina": 1,
    "limite": 10,
    "total": 0,
    "totalPaginas": 0
  }
}
```

Cada consulta usa ordenação estável por ID. Total e dados são lidos na mesma transação. Produtos preservam o filtro por unidade, inclusive no cálculo do total e nos estoques retornados. Página além da última retorna lista vazia; unidade inexistente continua retornando 404.

**Mudança de contrato:** produtos e unidades deixaram de retornar um array na raiz. Consumidores devem acessar `dados`. A estrutura é consistente com a listagem de pedidos, que agora também usa a validação compartilhada de paginação.

## Contratos e verificação

Swagger e seu gerador foram atualizados com schemas de páginas, parâmetros e cenários 502/503/504. A coleção Postman passou de 16 para 20 cenários, incluindo falhas técnicas e páginas de produtos/unidades. A organização final em pastas continua prevista para a etapa 4.

A suíte passou de 22 para 26 testes, acrescentando paginação e entradas inválidas, falhas auditadas com recuperação, contrato enviado ao gateway, uso de seu retorno e rejeição de correlação incorreta. Os testes de concorrência existentes permanecem na suíte. A validação usa banco isolado e inclui a execução das 20 requisições Postman.

Verificação concluída em 11/10/2026: compilação TypeScript aprovada, **26 testes aprovados**, incluindo execução das **20 requisições Postman**. Referências locais do OpenAPI, parâmetros de paginação e códigos de falha do gateway também foram verificados. Comandos reproduzíveis: `npm run build` e `npm test` (ou equivalentes via pnpm).

## Situação do planejamento

| Requisito | Resultado desta etapa |
|---|---|
| RF10 | Gateway mock separado, contrato de envio/retorno e correlação implementados. |
| RF11 / RNF11 | Aprovação, recusa, indisponibilidade e timeout simulados, sem alteração indevida do pedido ou duplicação de pagamento. |
| RF16 | Produtos e unidades paginados, com contratos e testes atualizados. |

O documento da etapa 1 permanece como registro do planejamento original. Próxima etapa: privacidade e segurança, incluindo RF14/RF15 e documentação RNF04.
