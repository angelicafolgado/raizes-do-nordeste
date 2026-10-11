# Análise de aderência ao roteiro UNINTER — revisão de correções

Atualização: 11/10/2026. Referências: PDFs da pasta uninter e anexos de estudo de caso/trilha Back-end fornecidos. As cópias indicam edição para estudo, omissões e prevalência do original; esta comparação não autentica requisitos adicionais do AVA.

## Parecer

O projeto fecha o MVP do **Fluxo A: Pedido → Pagamento mock → Atualização de status**, com persistência SQLite, reserva/rollback, JWT/perfis e multicanalidade. As lacunas técnicas e documentais apontadas na revisão foram corrigidas: contrato OpenAPI, configuração de exemplo, migrações versionadas, diagramas atuais, validação da coleção e atualização do PDF.

| Critério do material | Evidência atual | Limite |
|---|---|---|
| Análise e requisitos (20 pontos) | RF/RNF, prioridades, atores e propostas no documento de escopo; rastreabilidade atual. | Cadastro, pontos, campanhas e gestão administrativa são propostas, conforme possibilidade de recorte conceitual fora do MVP. |
| Modelagem e arquitetura (20 pontos) | DER, casos de uso, classes conceituais, classes técnicas reais e atividade/ sequência com componentes Java atuais. | Implementação JDBC usa mapas/records, não entidades ORM; ApiController coordena regras e RaizesStore persiste. Arquitetura simplificada é explicitada. |
| API e regras (25 pontos) | Fluxo completo validado, paginação, estoque transacional, pagamento aprovado/recusado/falha, status e OpenAPI detalhado. Flyway versiona o esquema e valida checksums. | Gateway é local simulado. Bases divergentes da versão anterior exigem avaliação antes de migrar. |
| Segurança, LGPD e auditoria (15 pontos) | JWT, BCrypt, perfis/titularidade, logs de operações/leitura, consentimento opcional e inventário/políticas documentados. | Sem anonimização/expurgo automático ou operação comercial de direitos; não é certificação jurídica. |
| Plano e evidências de testes (10 pontos) | 20 testes Java/Flyway e coleção com 32 requisições/88 asserções sem falhas; 17 positivos e 15 negativos em 9 pastas, matriz T01–T32. | JUnit e coleção são executados separadamente. |
| Entrega/documentação (10 pontos) | Repositório público, README reproduzível, .env.example Java, Swagger local, coleção vinculada, PDF atualizado e referências. | Campos acadêmicos permanecem “a informar” por orientação da autora; conferir normas ABNT do AVA antes da entrega. |

Os pesos são os do material fornecido, não notas atribuídas ao trabalho. A avaliação final cabe ao docente.

## Correções aplicadas

1. OpenAPI com schemas explícitos, campos obrigatórios, exemplos de request/response, enumerações, permissões e erros. O customizador verifica que as operações documentadas existem no Spring.
2. .env.example com SPRING_DATASOURCE_URL, Maven e seed; README explica exportação de variáveis e que .env não é carregado automaticamente.
3. Flyway V1 com baseline 0 para bases anteriores, checksums, instruções de backup/evolução e testes em banco novo/existente.
4. DER sem Prisma; classes conceituais e arquitetura real Java separadas; atividade/ sequência sem casos de uso removidos; imagens regeneradas.
5. Evidências atualizadas: Postman validado na API Java, novas migrações testadas, persistência após reinício confirmada e documentação sincronizada.
6. PDF A4 com fonte incorporada, sumário paginado, diagramas e exemplos atualizados. Identificação Angelica Dias Folgado, RU 4937552; demais dados mantidos como solicitado.

## Escopo e recomendações

- Os requisitos fora do MVP estão detalhados conceitualmente em [escopo](docs/01_REQUISITOS_E_ESCOPO.md). Consentimento não equivale a pontos/resgate implementados.
- A API, testes e geradores do projeto são Java; SQL, JSON, XML, documentação e fontes de diagramas permanecem nos formatos adequados. Runner externo Newman não é dependência da API.
- Deploy é opcional: a seção 7.3 admite instruções para Swagger local. O repositório e a coleção devem continuar acessíveis ao avaliador.
- A revisão registra um commit real de correções; não se fabrica histórico para alcançar os cinco commits recomendados.
- O ensaio de carga anterior não foi repetido; restauração de backup e alta disponibilidade em produção não foram ensaiadas.

Resultados e comandos: [validação](docs/04_RESULTADO_VALIDACAO.md), [evidência estruturada](docs/07_EVIDENCIAS_CORRECOES.json), [plano de testes](docs/04_PLANO_DE_TESTES.md), [rastreabilidade](docs/06_RASTREABILIDADE.md) e [diagramas](docs/05_DIAGRAMAS.md).
