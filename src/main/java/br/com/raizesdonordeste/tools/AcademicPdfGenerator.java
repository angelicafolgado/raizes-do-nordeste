package br.com.raizesdonordeste.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

public final class AcademicPdfGenerator {
    private static final float MARGIN = 85; // 3 cm à esquerda
    private static final float RIGHT = 57; // 2 cm à direita
    private static final float TOP = 85; // 3 cm no topo
    private static final float BOTTOM = 57; // 2 cm na base
    private static final float BODY_SIZE = 12;
    private static final float BODY_LEADING = 18;
    private final PDFont BODY;
    private final PDFont BOLD;

    private final PDDocument document = new PDDocument();
    private PDPage page;
    private PDPageContentStream stream;
    private float y;
    private int pageNumber;
    private final StringBuilder markdown = new StringBuilder();

    private final java.util.Map<String, Integer> contentsPages;
    private final java.util.Map<String, Integer> recordedPages = new java.util.LinkedHashMap<>();

    private AcademicPdfGenerator() throws IOException { this(java.util.Map.of()); }
    private AcademicPdfGenerator(java.util.Map<String, Integer> contentsPages) throws IOException {
        BODY = loadFont("LiberationSerif-Regular.ttf");
        BOLD = loadFont("LiberationSerif-Bold.ttf");
        this.contentsPages = contentsPages;
    }

    private PDFont loadFont(String name) throws IOException {
        try (var input = AcademicPdfGenerator.class.getResourceAsStream("/fonts/" + name)) {
            if (input == null) throw new IOException("Fonte não encontrada: " + name);
            return PDType0Font.load(document, input);
        }
    }

    private String contents(String title) {
        return title + " ... " + Math.max(0, contentsPages.getOrDefault(title, 1) - 1);
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0
                ? Path.of("output", "pdf", "4937552_Projeto_Back_End.pdf")
                : Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(output.toAbsolutePath().getParent(), "relatorio-", ".pdf");
        try {
            Files.delete(temporary);
            AcademicPdfGenerator generator = new AcademicPdfGenerator();
            generator.generate(temporary);
            Files.delete(temporary);
            generator = new AcademicPdfGenerator(generator.recordedPages);
            generator.generate(temporary);
            validate(temporary);
            Files.move(temporary, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Path textOutput = output.resolveSibling(output.getFileName().toString()
                    .replaceFirst("(?i)\\.pdf$", "_texto.md"));
            Files.writeString(textOutput, generator.markdown, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void generate(Path output) throws IOException {
        try {
            cover();
            titlePage();
            section("Sumário", List.of(
                    contents("1. Introdução e objetivos"),
                    contents("2. Análise e requisitos"),
                    contents("2.2 Requisitos conceituais propostos (não implementados)"),
                    contents("3. Modelagem e arquitetura"),
                    contents("4. API e contratos"),
                    contents("5. LGPD, privacidade, segurança e auditoria"),
                    contents("6. Plano de testes e evidências"),
                    contents("7. Entrega técnica"),
                    contents("8. Conclusão"),
                    contents("Referências"),
                    contents("Apêndice A - Contratos e exemplos da API"),
                    contents("Apêndice B - Cenários de teste da API"),
                    contents("Apêndice C - Evidências de execução"),
                    contents("Apêndice D - Casos de uso"),
                    contents("Apêndice E - Classes conceituais de domínio"),
                    contents("Apêndice F - Arquitetura Java"),
                    contents("Apêndice G - Diagrama entidade-relacionamento"),
                    contents("Apêndice H - Atividade do fluxo crítico")
            ));
            closePage();
            section("1. Introdução e objetivos", List.of(
                    "Uma rede de alimentação precisa receber pedidos por diferentes canais, consultar "
                            + "a disponibilidade de cada unidade e acompanhar o preparo. O projeto implementa "
                            + "uma API para demonstrar esse fluxo com persistência e controle de acesso.",
                    "O objetivo é demonstrar o fluxo Pedido -> Pagamento simulado -> Status com banco real, "
                            + "autenticação e regras de autorização. Os canais APP, WEB, TOTEM, BALCAO e PICKUP "
                            + "são atributos do pedido.",
                    "ADMIN representa a administração da rede. ATENDENTE e COZINHA têm vínculo obrigatório "
                            + "com uma unidade para operar pedidos; clientes veem os próprios pedidos. O sistema "
                            + "não implementa interface gráfica nem integração financeira externa.",
                    "Foi escolhido o Fluxo A do material de referência: criar pedido, validar itens e saldo, "
                            + "registrar resultado de pagamento mock e atualizar status. O preço vem do banco, "
                            + "itens repetidos são agregados e o titular do pedido vem do JWT.",
                    "Cadastro público, administração completa de produtos/unidades, entradas administrativas "
                            + "de estoque, pontos e campanhas são propostas fora do MVP."
            ));
            closePage();
            section("2. Análise e requisitos", List.of(
                    "A API autentica usuários por e-mail e senha e emite JWT. Senhas são armazenadas com BCrypt.",
                    "As permissões distinguem CLIENTE, ATENDENTE, COZINHA e ADMIN. CLIENTE consulta os próprios "
                            + "pedidos; ATENDENTE e COZINHA consultam/operam somente a unidade vinculada; ADMIN "
                            + "mantém visão da rede. Perfil operacional sem unidade recebe 403.",
                    "O catálogo informa produtos e estoque da unidade selecionada. Listagens são paginadas, "
                            + "com limite máximo de 100 registros por página.",
                    "Criar pedido, reservar estoque e registrar auditoria ocorre em transação. Itens repetidos "
                            + "são agrupados; falta de saldo ou falha reverte a operação.",
                    "O pagamento mock aceita aprovação, recusa e falhas técnicas simuladas. Aprovação move "
                            + "o pedido para EM_PREPARO; a cozinha pode avançar para PRONTO e ENTREGUE.",
                    "Pedidos não pagos podem ser cancelados por perfis autorizados, com devolução transacional "
                            + "do estoque. Consentimento de fidelidade é demonstrativo, opcional e revogável.",
                    "Requisitos não funcionais atendidos incluem separação por responsabilidades, persistência "
                            + "SQLite, JWT, hashing de senha, padrão JSON de erro, limite de corpo HTTP de 100 KB, "
                            + "paginação e proteção de reserva sob concorrência.",
                    "Migrações Flyway versionadas inicializam bancos novos e registram a atualização de bases existentes sem apagar dados. Checksums são validados ao iniciar.",
                    "Requisitos propostos: cadastro, CRUD administrativo de catálogo, movimentação de estoque, "
                            + "fidelidade por pontos, promoções e persistência distribuída multiwriter."
            ));
            section("2.2 Requisitos conceituais propostos (não implementados)", List.of(
                    "Cadastro: visitante informa nome, e-mail, senha e confirmação; servidor normaliza e valida "
                            + "e-mail, rejeita duplicidade, aplica BCrypt e atribui exclusivamente CLIENTE. Papel, "
                            + "unidade e consentimento não são aceitos como autoridade do cliente. Erro não cria conta; "
                            + "sucesso não retorna senha/hash. Consentimento de fidelidade permanece separado.",
                    "Catálogo: ADMIN cria, edita e inativa unidades/produtos; empregados podem consultar mas não "
                            + "administrar. Validar campos obrigatórios, tamanhos, chave única, preço não negativo "
                            + "em centavos e referência a unidade/produto existente. Inativação preserva histórico; "
                            + "preços atuais nunca reescrevem itens de pedidos antigos.",
                    "Estoque: ADMIN informa unidade, produto, ENTRADA/SAIDA/AJUSTE, quantidade positiva e motivo; "
                            + "identidade/data vêm do servidor. Validar ativos e saldo para saída; ajuste exige "
                            + "justificativa e saldo final não negativo. Saldo e lançamento imutável com saldo "
                            + "anterior/posterior são gravados na mesma transação; falha reverte ambos.",
                    "Fidelidade: proposta de 1 ponto por real efetivamente pago após descontos quando o pedido "
                            + "chega a ENTREGUE, arredondando para baixo; sem crédito por recusa/cancelamento ou "
                            + "sem adesão ativa. Resgate: 100 pontos = R$ 5,00, até 20% do subtotal elegível. "
                            + "Reserva no pedido, débito no pagamento e devolução única ao cancelar antes de pagar.",
                    "Promoção: exemplo proposto NORDESTE10, 10% em Tapioca nos canais APP/WEB e unidades habilitadas, "
                            + "entre 01/11/2026 e 31/12/2026 UTC. Servidor valida vigência, unidade, canal e produto; "
                            + "registra desconto e itens elegíveis no pedido. Na proposta inicial, uma campanha no "
                            + "máximo e sem combinação com resgate de pontos. Não há campanha/endpoints implementados."
            ));
            closePage();
            section("2.1 Feature crítica: pedido, pagamento e status", List.of(
                    "Pré-condições: API/banco disponíveis, migrations aplicadas, JWT e perfil permitidos, unidade/produtos existentes, canal válido e saldo suficiente. Para pagar, pedido do titular cliente em AGUARDANDO_PAGAMENTO.",
                    "Fluxo: validar e agrupar itens; reservar estoque, persistir pedido/itens e auditoria na transação; solicitar gateway com valor e correlação; validar retorno; persistir pagamento e EM_PREPARO; COZINHA/ADMIN avança para PRONTO e ENTREGUE.",
                    "Pós-condições: dados persistentes e estoque consistente. Criação falha reverte a reserva; recusa mantém pedido aguardando; falha técnica não aprova pagamento. Cancelamento autorizado de pedido não pago devolve estoque uma vez.",
                    "Exceções: 401 token, 403 perfil/titular, 400/422 entrada, 404 referências, 409 estoque/estado, 502 retorno inválido, 503/504 gateway. Não há Idempotency-Key: repetir criação pode criar outro pedido; pagamento repetido aprovado é bloqueado por estado. Descrição completa: docs/09_FEATURE_CRITICA.md."
            ));
            closePage();
            section("3. Modelagem e arquitetura", List.of(
                    "A aplicação foi implementada em Java 21 com Spring Boot 3.5, API REST, JDBC e SQLite. "
                            + "A documentação OpenAPI está disponível em /api-docs.",
                    "ApiController traduz HTTP e resolve a identidade; RaizesService coordena os casos de uso; OrderRules valida transições; AuthenticationFilter e JwtService validam identidade; "
                            + "RaizesStore centraliza JDBC e transações; PaymentGateway é a porta de pagamento "
                            + "e MockPaymentGateway seu adaptador local; ApiExceptionHandler projeta erros JSON.",
                    "O banco tem nove tabelas: usuarios, unidades, produtos, estoques_unidades, pedidos, "
                            + "itens_pedido, pagamentos, audit_logs e consentimentos. O par unidade/produto do "
                            + "estoque é único; pedidos têm usuário e unidade; itens referenciam pedido e produto; "
                            + "há no máximo um pagamento por pedido. A migração V2 adiciona `usuarios.unidadeId` "
                            + "opcional para associar perfis operacionais; perfis sem unidade não operam pedidos.",
                    "O código Java usa mapas JDBC e records pontuais; não possui classes Java de entidade para "
                            + "todo o domínio. O diagrama conceitual representa os dados projetados por JDBC; o apêndice "
                            + "de arquitetura Java registra as classes técnicas da implementação atual.",
                    "SQLite permite um escritor por vez. Uma trava local serializa transações de escrita "
                            + "dentro do processo; isso não coordena múltiplas instâncias distribuídas.",
                    "A implementação da API, dos testes automatizados e das ferramentas mantidas no projeto é "
                            + "Java 21. A afirmação 100% Java refere-se a esse código-fonte executável; "
                            + "documentação, configuração, SQL e artefatos de integração permanecem em seus "
                            + "formatos próprios."
            ));
            closePage();
            section("4. API e contratos", List.of(
                    "GET /api/health - público; responde à verificação de saúde.",
                    "POST /api/auth/login - público; e-mail e senha; retorna JWT ou 401.",
                    "GET /api/unidades?page=1&limit=10 e GET /api/produtos?unidadeId=1&page=1&limit=10 "
                            + "- públicos; listagens paginadas com limite máximo de 100.",
                    "POST /api/pedidos - CLIENTE, ATENDENTE ou ADMIN; cria pedido e reserva estoque; 201 em "
                            + "sucesso; 400/401/403/404/409/422 em erros conforme o caso.",
                    "GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10 - autenticado; CLIENTE vê os próprios "
                            + "pedidos; ATENDENTE/COZINHA veem somente a unidade vinculada; ADMIN vê a rede.",
                    "POST /api/pedidos/{id}/pagamento - CLIENTE titular, ATENDENTE da unidade do pedido ou ADMIN; "
                            + "resultado aprovado ou recusado; falhas simuladas retornam 503/504.",
                    "PATCH /api/pedidos/{id}/status - COZINHA da unidade do pedido ou ADMIN; transições permitidas.",
                    "GET /api/privacidade/aviso; PUT /api/privacidade/consentimentos/fidelidade; "
                            + "GET /api/privacidade/consentimentos - aviso e histórico do próprio titular.",
                    "GET /api/auditoria?acao=CONSULTA_PEDIDOS - ADMIN. GET /api-docs.json serve o contrato "
                            + "OpenAPI; / redireciona para Swagger.",
                    "O contrato OpenAPI contém schemas, campos obrigatórios, enumerações, parâmetros, perfis, exemplos e respostas de erro. Ele é aplicado somente a rotas descobertas pelo Spring.",
                    "Request de pedido: {\"unidadeId\": 1, \"canalPedido\": \"APP\", "
                            + "\"itens\": [{\"produtoId\": 1, \"quantidade\": 1}]}. O servidor calcula o preço.",
                    "Trecho de response 201: {\"id\": 1, \"canalPedido\": \"APP\", "
                            + "\"status\": \"AGUARDANDO_PAGAMENTO\", \"total\": 38.5}. O contrato completo inclui itens, titular, unidade e datas.",
                    "Pagamento recusado: a requisição contém {\"statusSimulado\": \"RECUSADO\"}. Response 200 inclui "
                            + "{\"sucesso\": false, \"statusPedido\": \"AGUARDANDO_PAGAMENTO\"}; a reserva permanece e nova tentativa é permitida.",
                    "Erro de estoque, HTTP 409: {\"error\": \"ESTOQUE_INSUFICIENTE\", "
                            + "\"message\": \"Estoque insuficiente para Baião de dois.\", \"details\": [], "
                            + "\"timestamp\": \"2026-10-11T13:00:00Z\", \"path\": \"/api/pedidos\", "
                            + "\"requestId\": \"00000000-0000-4000-8000-000000000001\"}. Exemplo ilustrativo; não é uma captura de credenciais."
            ));
            closePage();
            section("5. LGPD, privacidade, segurança e auditoria", List.of(
                    "O seed fornece contas fictícias. Senhas são armazenadas com BCrypt; chamadas protegidas "
                            + "usam JWT; perfis e titularidade limitam operações.",
                    "O pagamento não captura dados de cartão nem contata adquirente. O consentimento de "
                            + "fidelidade apenas registra uma preferência de demonstração; não há pontos ou "
                            + "envio de marketing.",
                    "Inventário e hipóteses propostas: conta (ID, nome, e-mail, hash; autenticação; execução "
                            + "de contrato a avaliar; titular/admin conforme função; retenção enquanto ativa e "
                            + "revisão manual); pedido/itens (titular, unidade, canal, itens, preço, status; "
                            + "execução do pedido/obrigação aplicável a confirmar; cliente próprio, empregados "
                            + "da unidade e ADMIN; prazos de produção ainda não definidos).",
                    "Consentimento guarda titular por ID, finalidade, versão, decisão e data; uma finalidade "
                            + "real de fidelidade exigiria análise específica da hipótese de consentimento. "
                            + "Auditoria guarda ator/ação/metadados; interesse legítimo/segurança ou obrigação "
                            + "são apenas hipóteses a avaliar. Titular consulta e revoga sua decisão; revogar "
                            + "não apaga pedidos automaticamente. Dados fictícios são revistos manualmente até "
                            + "30 dias após avaliação; não há expurgo automático, anonimização ou canal de direitos.",
                    "A aplicação não implementa expurgo periódico, anonimização automatizada ou operação "
                            + "comercial de direitos do titular. O SQLite local e seus backups não são cifrados "
                            + "pela aplicação; segredos e bancos não devem ser publicados.",
                    "A discussão de privacidade é acadêmica e não representa certificação jurídica ou parecer. "
                            + "O SQLite e seus backups não são cifrados pela aplicação; IDs de auditoria são "
                            + "pseudônimos/vinculáveis, não dados anônimos."
            ));
            closePage();
            section("6. Plano de testes e evidências", List.of(
                    "Na execução desta revisão, `mvn test` em JDK 21.0.10 terminou com BUILD SUCCESS: 21 "
                            + "testes, zero falhas/erros/ignorados (17 integração, 2 migração, 2 gateway).",
                    "Casos positivos cobrem token de autenticação usado nos testes, listagens, pedido criado "
                            + "com itens agrupados, aprovação de pagamento, avanço para PRONTO, consentimento "
                            + "idempotente, seed idempotente e consultas concorrentes.",
                    "Casos negativos cobrem credencial/token inválido (401), perfil ou titular indevido (403), "
                            + "dados inválidos (400/422), unidade/produto inexistente (404), estoque insuficiente "
                            + "e conflito de estado (409), gateway (503/504), corpo excessivo (413) e rollback "
                            + "quando falha auditoria.",
                    "O reinício local foi validado: pedido e pagamento permaneceram consultáveis em SQLite. Migrações e seed são repetíveis. Restauração de backup não foi ensaiada nesta revisão.",
                    "O teste de isolamento por unidade verifica consulta, criação, pagamento e status entre "
                            + "Recife e João Pessoa, ADMIN em escopo de rede e negação quando o vínculo é removido.",
                    "A coleção Postman/Newman e a medição de carga são evidências históricas do projeto e não "
                            + "foram repetidas nesta revisão; não são apresentadas como validação atual."
            ));
            closePage();
            section("7. Entrega técnica", List.of(
                    "O README documenta JDK, Maven, variáveis, banco/seed, início da API, Swagger, testes e "
                            + "geração do PDF. O OpenAPI é servido pela aplicação. A pasta output permanece local e ignorada pelo Git.",
                    "Nenhuma alteração desta revisão foi enviada ao GitHub. A coleção Postman/Newman documentada "
                            + "em execução anterior não foi repetida nesta revisão.",
                    "A aluna desenvolveu o projeto com base no estudo de caso e no roteiro da trilha Back-end. Os "
                            + "PDFs consultados indicam edição para estudo, omissões e prevalência do original. A adoção "
                            + "não autentica a procedência do material no AVA."
            ));
            closePage();
            section("8. Conclusão", List.of(
                    "A implementação Java entrega o fluxo acadêmico de pedidos, reserva de estoque, pagamento "
                            + "simulado e acompanhamento operacional, com escopo de empregados por unidade. "
                            + "As propostas administrativas, de pontos e de campanhas não são funcionalidades executáveis.",
                    "A aluna implementou a API, os testes e as ferramentas em Java 21. Curso, período e "
                            + "identificação do trabalho constam nos elementos acadêmicos do relatório. Polo e cidade permanecem "
                            + "não informados. As referências foram utilizadas na elaboração do projeto, "
                            + "preservadas as ressalvas indicadas pelos próprios documentos."
            ));
            closePage();
            section("Referências", List.of(
                    "UNINTER. Projeto Multidisciplinar: estudo de caso - Raízes do Nordeste. 2026. Versão adaptada para estudo consultada na elaboração do projeto; original institucional prevalece.",
                    "UNINTER. Projeto Multidisciplinar: trilha Back-End. 2026. Versão adaptada para estudo consultada na elaboração do projeto; original institucional prevalece.",
                    "FOLGADO, Angelica Dias. Raízes do Nordeste: documentação técnica, requisitos e evidências. 2026. Disponível em: https://github.com/angelicafolgado/raizes-do-nordeste. Acesso em: 11 out. 2026.",
                    "ORACLE. Java Platform, Standard Edition 21: documentação técnica. [s. d.]. Disponível em: https://docs.oracle.com/en/java/javase/21/. Referência técnica da versão utilizada.",
                    "SPRING. Spring Boot: reference documentation. [s. d.]. Disponível em: https://docs.spring.io/spring-boot/reference/. Referência técnica do framework utilizado.",
                    "BRASIL. Lei nº 13.709, de 14 de agosto de 2018. Lei Geral de Proteção de Dados Pessoais. Brasília, DF: Presidência da República, 2018. Disponível em: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm. Referência normativa; não representa parecer jurídico."
                    ,"ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. ABNT NBR 14724:2024: informação e documentação — trabalhos acadêmicos — apresentação. Rio de Janeiro: ABNT, 2024. Referência para conferência da apresentação acadêmica; a edição institucional aplicável orienta a entrega."
                    ,"ASSOCIAÇÃO BRASILEIRA DE NORMAS TÉCNICAS. ABNT NBR 6027:2012: informação e documentação — sumário — apresentação. Rio de Janeiro: ABNT, 2012. Referência para conferência da apresentação acadêmica; a edição institucional aplicável orienta a entrega."
            ));
            contractAppendix();
            testAppendix();
            closePage();
            section("Apêndice C - Evidências de execução", List.of(
                    "Evidências próprias: docs/04_RESULTADO_VALIDACAO.md e docs/07_EVIDENCIAS_CORRECOES.json. Os resultados referem-se apenas aos comandos registrados nessa atualização; Newman não foi executado novamente.",
                    "A medição de desempenho abaixo é anterior às correções recentes e não foi repetida; os valores são históricos, relativos ao projeto de Angelica."
            ));
            section("C.1 Ensaio de desempenho anterior", List.of(
                    "A medição foi executada contra o JAR Spring Boot empacotado, em loopback, com SQLite "
                            + "isolado e seed local. Vinte workers concorrentes alternaram consultas públicas "
                            + "de produtos e unidades por 60 segundos, usando clientes HTTP persistentes.",
                    "Resultado observado: 228.399 requisições em 60,01 segundos; p95 de 11,76 ms; zero "
                            + "falhas e zero respostas HTTP 5xx. Meta local: p95 até 1.000 ms, zero falhas e "
                            + "menos de 1% de respostas 5xx. O cenário atingiu esses limites.",
                    "É um ensaio de leituras locais, não de escritas, rede externa, múltiplas instâncias, "
                            + "reinício, restauração ou disponibilidade em produção."
            ));
            addImagePage("Apêndice D - Casos de uso",
                    Path.of("docs", "diagramas", "02_casos_de_uso.png"),
                    "Visão dos atores e operações; gateway é um adaptador simulado, não uma integração financeira ativa.");
            addImagePage("Apêndice E - Classes conceituais de domínio",
                    Path.of("docs", "diagramas", "03_classes.png"),
                    "Modelo conceitual de dados; a implementação usa projeções JDBC em mapas, não entidades ORM.");
            addImagePage("Apêndice F - Arquitetura Java",
                    Path.of("docs", "diagramas", "03_arquitetura_java.png"),
                    "Classes reais e suas dependências. PaymentGateway.process é implementado por MockPaymentGateway.");
            addImagePage("Apêndice G - Diagrama entidade-relacionamento",
                    Path.of("docs", "diagramas", "01_der.png"),
                    "Nove tabelas de domínio. V1__initial_schema.sql define a estrutura-base; V2__vinculo_unidade_operacional.sql acrescenta usuarios.unidadeId (FK opcional): uma unidade pode vincular vários empregados e um empregado pode ter zero ou uma unidade. A imagem é a renderização anterior a V2; a relação está na fonte docs/diagramas/01_der.dot e deve ser renderizada para a entrega gráfica final. flyway_schema_history mantém versões e checksums.");
            addImagePage("Apêndice H - Atividade do fluxo crítico",
                    Path.of("docs", "diagramas", "04_sequencia.png"),
                    "Fluxo crítico Java: transações JDBC, aprovação/recusa, falhas técnicas, cancelamento e status; a autorização por unidade está detalhada neste relatório e no requisito RN12.");
            closePage();
            document.save(output.toFile());
        } finally {
            document.close();
        }
    }

    private void contractAppendix() throws IOException {
        closePage();
        section("Apêndice A - Contratos e exemplos da API", List.of(
                "Contratos extraídos do OpenAPI do projeto de Angelica. Os exemplos são ilustrativos; não representam novas capturas de execução. O schema completo está em src/main/resources/openapi-contract.json e em /api-docs.json."));
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var contract = mapper.readTree(Files.readString(Path.of("src/main/resources/openapi-contract.json")));
        var paths = contract.path("paths").fields();
        while (paths.hasNext()) {
            var route = paths.next();
            var operations = route.getValue().fields();
            while (operations.hasNext()) {
                var operation = operations.next();
                var data = operation.getValue();
                var paragraphs = new java.util.ArrayList<String>();
                paragraphs.add(data.path("summary").asText());
                paragraphs.add(data.path("description").asText());
                if (data.has("parameters")) paragraphs.add("Parâmetros: " + data.path("parameters"));
                var request = data.path("requestBody").path("content").path("application/json");
                if (!request.isMissingNode()) {
                    paragraphs.add("Request - schema: " + request.path("schema"));
                    if (request.has("example")) paragraphs.add("Exemplo de request: " + request.path("example"));
                }
                var responses = data.path("responses").fields();
                while (responses.hasNext()) {
                    var response = responses.next();
                    var json = response.getValue().path("content").path("application/json");
                    paragraphs.add("HTTP " + response.getKey() + ": " + response.getValue().path("description").asText()
                            + (json.has("schema") ? "; schema: " + json.path("schema") : ""));
                    if (json.has("example")) paragraphs.add("Exemplo HTTP " + response.getKey() + ": " + json.path("example"));
                }
                section(operation.getKey().toUpperCase(java.util.Locale.ROOT) + " " + route.getKey(), paragraphs);
            }
        }
    }

    private void testAppendix() throws IOException {
        closePage();
        section("Apêndice B - Cenários de teste da API", List.of(
                "Cenários do plano de testes próprio. Pré-condição geral: API iniciada, migrations e seed aplicados em banco de demonstração separado. Tokens são obtidos pelos logins da coleção. As entradas completas constam em docs/04_PLANO_DE_TESTES.md e postman/raizes_nordeste_clean_collection.json."));
        for (String row : Files.readAllLines(Path.of("docs/04_PLANO_DE_TESTES.md"))) {
            if (!row.matches("\\| T[0-9]{2} .*")) continue;
            String[] cells = row.split("\\|", -1);
            section(cells[1].trim() + " - " + cells[2].trim(), List.of(
                    "Operação: " + cells[3].trim().replace("`", ""),
                    "Pré-condições: " + cells[4].trim(),
                    "Entrada: " + cells[5].trim().replace("`", ""),
                    "Resultado esperado: " + cells[6].trim(),
                    "Localização na coleção: " + cells[7].trim()));
        }
    }

    private void cover() throws IOException {
        newPage();
        markdown.append("# Raízes do Nordeste\n\n")
                .append("**Angelica Dias Folgado**  \n**RU: 4937552**\n\n")
                .append("Projeto acadêmico: API REST para pedidos multicanal, estoque por unidade e pagamento simulado. ")
                .append("Relatório técnico da implementação Java 21 e Spring Boot 3.5.\n\n")
                .append("Trabalho: Projeto: Desenvolvimento Back-end (898968)  \n")
                .append("Curso: CST Análise e desenvolvimento de sistemas  \nPolo: não informado  \nCidade: não informada  \n")
                .append("Semestre: B fase II 2026 - regular  \nDocente: Prof. Me. Luciane Yanase Kanashiro\n\n");
        centered("CENTRO UNIVERSITÁRIO INTERNACIONAL UNINTER", BOLD, 12, 750);
        centered("CURSO: CST Análise e desenvolvimento de sistemas", BODY, 12, 723);
        centered("ANGELICA DIAS FOLGADO", BOLD, 12, 650);
        centered("RU: 4937552", BODY, 12, 628);
        centered("RAÍZES DO NORDESTE", BOLD, 12, 465);
        centered("PROJETO: DESENVOLVIMENTO BACK-END (898968)", BOLD, 12, 435);
        centered("Polo: não informado", BODY, 12, 355);
        centered("Cidade: não informada", BODY, 12, 333);
        centered("Semestre/período: B fase II 2026 - regular", BODY, 12, 311);
        centered("Docente: Prof. Me. Luciane Yanase Kanashiro", BODY, 12, 289);
        centered("CIDADE NÃO INFORMADA", BODY, 12, 105);
        centered("2026", BODY, 12, 83);
        closePage();
    }

    private void centered(String value, PDFont font, float size, float baseline) throws IOException {
        float width = PDRectangle.A4.getWidth() - MARGIN - RIGHT;
        textAt(value, font, size, MARGIN + (width - font.getStringWidth(value) * size / 1000) / 2, baseline);
    }

    private void titlePage() throws IOException {
        newPage();
        centered("ANGELICA DIAS FOLGADO", BOLD, 12, 750);
        centered("RU: 4937552", BODY, 12, 728);
        centered("RAÍZES DO NORDESTE", BOLD, 12, 575);
        centered("PROJETO: DESENVOLVIMENTO BACK-END (898968)", BOLD, 12, 547);
        String note = "Trabalho apresentado à disciplina Projeto: Desenvolvimento Back-end (898968), do curso CST Análise e desenvolvimento de sistemas, "
                + "no Centro Universitário Internacional UNINTER. Relatório da implementação própria "
                + "de uma API REST em Java para a rede Raízes do Nordeste. Docente: Prof. Me. Luciane Yanase Kanashiro.";
        float noteX = MARGIN + (PDRectangle.A4.getWidth() - MARGIN - RIGHT) / 2;
        float baseline = 420;
        for (String line : wrap(note, BODY, 12, PDRectangle.A4.getWidth() - RIGHT - noteX)) {
            textAt(line, BODY, 12, noteX, baseline);
            baseline -= 12;
        }
        centered("Polo: não informado · Cidade: não informada", BODY, 12, 105);
        centered("2026", BODY, 12, 83);
        markdown.append("## Folha de rosto\n\nAngelica Dias Folgado — RU 4937552\n\n")
                .append(note).append("\n\n");
        closePage();
    }

    private void justified(String line, float start, float baseline, float width, boolean expand) throws IOException {
        String[] words = line.split(" ");
        if (!expand || words.length < 2) {
            textAt(line, BODY, BODY_SIZE, start, baseline);
            return;
        }
        float natural = BODY.getStringWidth(line) * BODY_SIZE / 1000;
        float extra = Math.max(0, (width - natural) / (words.length - 1));
        var positions = new java.util.ArrayList<Object>();
        for (int i = 0; i < words.length; i++) {
            positions.add(words[i] + (i < words.length - 1 ? " " : ""));
            if (i < words.length - 1) positions.add(-extra * 1000 / BODY_SIZE);
        }
        stream.beginText();
        stream.setFont(BODY, BODY_SIZE);
        stream.newLineAtOffset(start, baseline);
        stream.showTextWithPositioning(positions.toArray());
        stream.endText();
    }

    private void section(String title, List<String> paragraphs) throws IOException {
        if (page == null) newPage();
        ensureSpace(60);
        recordedPages.put(title, pageNumber);
        float width = PDRectangle.A4.getWidth() - MARGIN - RIGHT;
        boolean contents = title.equals("Sumário");
        boolean references = title.equals("Referências");
        boolean narrative = title.matches("[1-8](?:\\.[0-9]+)?\\.? .*");
        if (contents || references) centered(title.toUpperCase(java.util.Locale.ROOT), BOLD, 12, y);
        else {
            for (String line : wrap(title, BOLD, 12, width)) {
                text(line, BOLD, 12, y);
                y -= 18;
            }
        }
        y -= contents || references ? 36 : 18;
        markdown.append("## ").append(title).append("\n\n");
        for (String paragraph : paragraphs) {
            markdown.append(paragraph).append("\n\n");
            if (contents) {
                int separator = paragraph.lastIndexOf(" ... ");
                String label = paragraph.substring(0, separator);
                String number = paragraph.substring(separator + 5);
                float numberWidth = BODY.getStringWidth(number) * BODY_SIZE / 1000;
                float numberX = PDRectangle.A4.getWidth() - RIGHT - numberWidth;
                text(label, BODY, 12, y);
                float labelEnd = MARGIN + BODY.getStringWidth(label) * BODY_SIZE / 1000 + 8;
                float dotWidth = BODY.getStringWidth(".") * BODY_SIZE / 1000;
                int dots = Math.max(0, (int) ((numberX - labelEnd - 8) / dotWidth));
                textAt(".".repeat(dots), BODY, 12, labelEnd, y);
                textAt(number, BODY, 12, numberX, y);
                y -= 28;
                continue;
            }
            float indent = narrative ? 35.43f : 0;
            List<String> lines = wrap(paragraph, BODY, BODY_SIZE, width - indent);
            ensureSpace(Math.min(lines.size() * BODY_LEADING, BODY_LEADING * 2));
            for (int i = 0; i < lines.size(); i++) {
                ensureSpace(references ? 12 : BODY_LEADING);
                float currentIndent = i == 0 ? indent : 0;
                justified(lines.get(i), MARGIN + currentIndent, y, width - currentIndent,
                        narrative && i < lines.size() - 1);
                y -= references ? 12 : BODY_LEADING;
            }
            y -= references ? 12 : narrative ? 0 : 10;
        }
    }

    private List<String> wrap(String content, PDFont font, float size, float width) throws IOException {
        var lines = new java.util.ArrayList<String>();
        var current = new StringBuilder();
        for (String word : content.split("\\s+")) {
            if (font.getStringWidth(word) * size / 1000 > width) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                for (char character : word.toCharArray()) {
                    if (font.getStringWidth(current.toString() + character) * size / 1000 > width) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    current.append(character);
                }
                continue;
            }
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.getStringWidth(candidate) * size / 1000 > width) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < BOTTOM) {
            closePage();
            newPage();
        }
    }

    private void newPage() throws IOException {
        page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        pageNumber++;
        stream = new PDPageContentStream(document, page);
        y = page.getMediaBox().getHeight() - TOP;

    }

    private void closePage() throws IOException {
        if (stream != null) {
            if (pageNumber >= 4) {
                String number = Integer.toString(pageNumber - 1);
                float x = PDRectangle.A4.getWidth() - RIGHT - BODY.getStringWidth(number) * 10 / 1000;
                textAt(number, BODY, 10, x, PDRectangle.A4.getHeight() - 57);
            }
            stream.close();
            stream = null;
            page = null;
        }
    }

    private void text(String value, PDFont font, float size, float baseline) throws IOException {
        textAt(value, font, size, MARGIN, baseline);
    }

    private void textAt(String value, PDFont font, float size, float x, float baseline) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.newLineAtOffset(x, baseline);
        stream.showText(value);
        stream.endText();
    }

    private void addImagePage(String title, Path imagePath, String caption) throws IOException {
        if (!Files.isRegularFile(imagePath)) {
            return;
        }
        closePage();
        newPage();
        recordedPages.put(title, pageNumber);
        text(title, BOLD, 12, y);
        markdown.append("## ").append(title).append("\n\n")
                .append(caption).append("\n\n")
                .append("![ ").append(title).append(" ](../../")
                .append(imagePath.toString().replace('\\', '/')).append(")\n\n");
        y -= 26;
        for (String line : wrap(caption, BODY, 9, PDRectangle.A4.getWidth() - MARGIN - RIGHT)) {
            text(line, BODY, 9, y);
            y -= 13;
        }
        PDImageXObject image = PDImageXObject.createFromFile(imagePath.toAbsolutePath().toString(), document);
        float maxWidth = PDRectangle.A4.getWidth() - MARGIN - RIGHT;
        float maxHeight = y - BOTTOM - 35;
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        float x = MARGIN + (maxWidth - width) / 2;
        stream.drawImage(image, x, Math.max(BOTTOM, y - height - 10), width, height);
        text("Fonte: elaboração própria a partir da implementação de Angelica (2026).", BODY, 10, BOTTOM + 12);
        closePage();
    }

    private static void validate(Path output) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(pdf);
            if (pdf.getNumberOfPages() < 8
                    || !extracted.toUpperCase(java.util.Locale.ROOT).contains("ANGELICA DIAS FOLGADO")
                    || !extracted.contains("4937552")
                    || !extracted.contains("228.399")
                    || !extracted.contains("11,76 ms")
                    || !extracted.contains("100% Java")
                    || !extracted.contains("https://github.com/angelicafolgado/raizes-do-nordeste")
                    || !extracted.contains("CURSO: CST Análise e desenvolvimento de sistemas")
                    || !extracted.contains("Polo: não informado")
                    || !extracted.contains("Cidade: não informada")
                    || !extracted.contains("Semestre/período: B fase II 2026 - regular")
                    || !extracted.replaceAll("\\s+", " ").contains("Prof. Me. Luciane Yanase Kanashiro")
                    || !extracted.contains("Plano de testes e evidências")
                    || !extracted.contains("NORDESTE10")
                    || !extracted.contains("não foram repetidas nesta revisão")
                    || !extracted.contains("Flyway")) {
                throw new IOException("O PDF gerado não contém as seções ou evidências acadêmicas esperadas.");
            }
        }
    }
}
