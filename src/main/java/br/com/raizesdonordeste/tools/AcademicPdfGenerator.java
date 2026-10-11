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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;

public final class AcademicPdfGenerator {
    private static final float MARGIN = 54;
    private static final float BOTTOM = 54;
    private static final float BODY_SIZE = 10;
    private static final float BODY_LEADING = 15;
    private static final PDType1Font BODY = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final PDDocument document = new PDDocument();
    private PDPage page;
    private PDPageContentStream stream;
    private float y;
    private int pageNumber;
    private final StringBuilder markdown = new StringBuilder();

    private AcademicPdfGenerator() {}

    public static void main(String[] args) throws IOException {
        Path output = args.length == 0
                ? Path.of("output", "pdf", "4937552_Projeto_Back_End.pdf")
                : Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        AcademicPdfGenerator generator = new AcademicPdfGenerator();
        generator.generate(output);
        Path textOutput = output.resolveSibling(output.getFileName().toString()
                .replaceFirst("(?i)\\.pdf$", "_texto.md"));
        Files.writeString(textOutput, generator.markdown, StandardCharsets.UTF_8);
        validate(output);
    }

    private void generate(Path output) throws IOException {
        try {
            cover();
            section("Apresentação", List.of(
                    "Angelica Dias Folgado - RU 4937552",
                    "Projeto acadêmico: Raízes do Nordeste",
                    "API REST para pedidos multicanal, estoque por unidade e pagamento simulado",
                    "Relatório técnico da implementação Java 21 e Spring Boot 3.5.",
                    "Disciplina: Projeto Multidisciplinar - Trilha Back-end. Curso, polo, semestre e docente: "
                            + "a informar.",
                    "A discente indicou os documentos locais da pasta uninter como referência. Os próprios "
                            + "arquivos contêm aviso de edição para estudo, conteúdo omitido e prevalência do "
                            + "original; a adoção registra sua orientação, sem autenticação independente no AVA.",
                    "O sistema é uma demonstração acadêmica local. Pagamentos são simulados e os resultados "
                            + "de desempenho não constituem garantia para produção."
            ));
            section("Sumário", List.of(
                    "1. Introdução",
                    "2. Estudo de caso e escopo",
                    "3. Análise e requisitos",
                    "4. Modelagem e arquitetura",
                    "5. API e endpoints",
                    "6. Segurança, privacidade e limites",
                    "7. Entrega técnica e aderência à trilha",
                    "8. Plano de testes e evidências",
                    "9. Ensaio de desempenho",
                    "10. Conclusão e referências",
                    "Anexos: diagramas do banco, casos de uso, arquitetura Java e sequência."
            ));
            section("1. Introdução", List.of(
                    "Uma rede de alimentação precisa receber pedidos por diferentes canais, consultar "
                            + "a disponibilidade de cada unidade e acompanhar o preparo. O projeto implementa "
                            + "uma API para demonstrar esse fluxo com persistência e controle de acesso.",
                    "O objetivo é demonstrar o fluxo Pedido -> Pagamento simulado -> Status com banco real, "
                            + "autenticação e regras de autorização. Os canais APP, WEB, TOTEM, BALCAO e PICKUP "
                            + "são atributos do pedido."
            ));
            section("2. Estudo de caso e escopo", List.of(
                    "A matriz acompanha a rede; cada unidade mantém saldo próprio por produto. ADMIN representa "
                            + "a administração da rede. O sistema não implementa interface gráfica nem integração "
                            + "financeira externa.",
                    "Foi escolhido o Fluxo A do material de referência: criar pedido, validar itens e saldo, "
                            + "registrar resultado de pagamento mock e atualizar status. O preço vem do banco, "
                            + "itens repetidos são agregados e o titular do pedido vem do JWT.",
                    "Cadastro público, administração completa de produtos/unidades, entradas administrativas "
                            + "de estoque, pontos e campanhas são propostas fora do MVP."
            ));
            section("3. Análise e requisitos", List.of(
                    "A API autentica usuários por e-mail e senha e emite JWT. Senhas são armazenadas com BCrypt.",
                    "As permissões distinguem CLIENTE, ATENDENTE, COZINHA e ADMIN. Clientes consultam apenas "
                            + "seus pedidos; operações de cozinha e administração seguem regras de perfil.",
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
                    "Requisitos propostos: cadastro, CRUD administrativo de catálogo, movimentação de estoque, "
                            + "fidelidade por pontos, promoções e persistência distribuída multiwriter."
            ));
            section("4. Modelagem e arquitetura", List.of(
                    "A aplicação foi implementada em Java 21 com Spring Boot 3.5, API REST, JDBC e SQLite. "
                            + "A documentação OpenAPI está disponível em /api-docs.",
                    "ApiController atende HTTP; AuthenticationFilter e JwtService validam identidade; "
                            + "RaizesStore centraliza JDBC e transações; PaymentGateway é a porta de pagamento "
                            + "e MockPaymentGateway seu adaptador local; ApiExceptionHandler projeta erros JSON.",
                    "O banco tem nove tabelas: usuarios, unidades, produtos, estoques_unidades, pedidos, "
                            + "itens_pedido, pagamentos, audit_logs e consentimentos. O par unidade/produto do "
                            + "estoque é único; pedidos têm usuário e unidade; itens referenciam pedido e produto; "
                            + "há no máximo um pagamento por pedido.",
                    "O código Java usa mapas JDBC e records pontuais; não possui classes Java de entidade para "
                            + "todo o domínio. O diagrama conceitual de classes preservado é histórico; o anexo "
                            + "de arquitetura Java registra as classes técnicas da implementação atual.",
                    "SQLite permite um escritor por vez. Uma trava local serializa transações de escrita "
                            + "dentro do processo; isso não coordena múltiplas instâncias distribuídas.",
                    "A implementação da API, dos testes automatizados e das ferramentas mantidas no projeto é "
                            + "Java 21. A afirmação 100% Java refere-se a esse código-fonte executável; "
                            + "documentação, configuração, SQL e artefatos de integração permanecem em seus "
                            + "formatos próprios."
            ));
            addImagePage("Anexo A - Diagrama entidade-relacionamento",
                    Path.of("docs", "diagramas", "01_der.png"),
                    "Nove tabelas e relacionamentos. schema.sql é a fonte do modelo persistido pela API Java.");
            addImagePage("Anexo B - Casos de uso",
                    Path.of("docs", "diagramas", "02_casos_de_uso.png"),
                    "Visão dos atores e operações; gateway é um adaptador simulado, não uma integração financeira ativa.");
            addJavaClassDiagram();
            addImagePage("Anexo D - Sequência do fluxo crítico",
                    Path.of("docs", "diagramas", "04_sequencia.png"),
                    "Visão de pedido, pagamento mock e status. A fonte visual original antecede a versão Java e requer revisão final.");
            section("5. API e endpoints", List.of(
                    "GET /api/health - público; responde à verificação de saúde.",
                    "POST /api/auth/login - público; e-mail e senha; retorna JWT ou 401.",
                    "GET /api/unidades?page=1&limit=10 e GET /api/produtos?unidadeId=1&page=1&limit=10 "
                            + "- públicos; listagens paginadas com limite máximo de 100.",
                    "POST /api/pedidos - CLIENTE, ATENDENTE ou ADMIN; cria pedido e reserva estoque; 201 em "
                            + "sucesso; 400/401/403/404/409/422 em erros conforme o caso.",
                    "GET /api/pedidos?canalPedido=TOTEM&page=1&limit=10 - autenticado; cliente vê apenas "
                            + "pedidos próprios; perfis operacionais consultam a rede.",
                    "POST /api/pedidos/{id}/pagamento - titular, ATENDENTE ou ADMIN; resultado aprovado ou "
                            + "recusado; falhas simuladas retornam 503 (INDISPONIVEL) ou 504 (TIMEOUT).",
                    "PATCH /api/pedidos/{id}/status - COZINHA ou ADMIN; transições permitidas de preparo/entrega.",
                    "GET /api/privacidade/aviso; PUT /api/privacidade/consentimentos/fidelidade; "
                            + "GET /api/privacidade/consentimentos - aviso e histórico do próprio titular.",
                    "GET /api/auditoria?acao=CONSULTA_PEDIDOS - ADMIN. GET /api-docs.json serve o contrato "
                            + "OpenAPI; / redireciona para Swagger.",
                    "Exemplo: {\"unidadeId\":1,\"canalPedido\":\"APP\","
                            + "\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}. O servidor calcula o preço."
            ));
            section("6. Segurança, privacidade e limites", List.of(
                    "O seed fornece contas fictícias. Senhas são armazenadas com BCrypt; chamadas protegidas "
                            + "usam JWT; perfis e titularidade limitam operações.",
                    "O pagamento não captura dados de cartão nem contata adquirente. O consentimento de "
                            + "fidelidade apenas registra uma preferência de demonstração; não há pontos ou "
                            + "envio de marketing.",
                    "A aplicação não implementa expurgo periódico, anonimização automatizada ou operação "
                            + "comercial de direitos do titular. O SQLite local e seus backups não são cifrados "
                            + "pela aplicação; segredos e bancos não devem ser publicados.",
                    "A discussão de privacidade é acadêmica e não representa certificação jurídica ou parecer."
            ));
            section("7. Entrega técnica e aderência à trilha", List.of(
                    "O README documenta JDK, Maven, variáveis, banco/seed, início da API, Swagger, testes e "
                            + "geração do PDF. O OpenAPI é servido pela aplicação. A cópia limpa Java foi "
                            + "validada sem target ou banco herdado, usando dependências já disponíveis no Maven local.",
                    "Repositório público: https://github.com/angelicafolgado/raizes-do-nordeste. A branch main "
                            + "tem histórico publicado em nome de Angelica Dias Folgado. O material indicado "
                            + "recomenda cinco commits; o histórico verdadeiro permanece abaixo dessa recomendação.",
                    "A coleção Postman contém 32 cenários históricos e não foi validada contra a API Java atual; "
                            + "não apresentá-la como execução comprovada desta revisão.",
                    "A pasta uninter foi adotada como referência por instrução da discente, embora os próprios "
                            + "PDFs indiquem edição para estudo, omissões e prevalência do original. A adoção "
                            + "não autentica a procedência do material no AVA."
            ));
            section("8. Plano de testes e evidências", List.of(
                    "A validação Maven executada em 11/10/2026 concluiu com BUILD SUCCESS: 18 testes "
                            + "aprovados, sem falhas, erros ou testes ignorados.",
                    "Casos positivos cobrem token de autenticação usado nos testes, listagens, pedido criado "
                            + "com itens agrupados, aprovação de pagamento, avanço para PRONTO, consentimento "
                            + "idempotente, seed idempotente e consultas concorrentes.",
                    "Casos negativos cobrem credencial/token inválido (401), perfil ou titular indevido (403), "
                            + "dados inválidos (400/422), unidade/produto inexistente (404), estoque insuficiente "
                            + "e conflito de estado (409), gateway (503/504), corpo excessivo (413) e rollback "
                            + "quando falha auditoria.",
                    "A evidência é JUnit: 16 métodos de integração em ApiIntegrationTest e 2 testes do gateway "
                            + "em MockPaymentGatewayTest. Os métodos podem conter várias asserções e códigos HTTP. "
                            + "A coleção Postman histórica não foi reexecutada contra Java."
            ));
            section("9. Ensaio de desempenho", List.of(
                    "A medição foi executada contra o JAR Spring Boot empacotado, em loopback, com SQLite "
                            + "isolado e seed local. Vinte workers concorrentes alternaram consultas públicas "
                            + "de produtos e unidades por 60 segundos, usando clientes HTTP persistentes.",
                    "Resultado observado: 228.399 requisições em 60,01 segundos; p95 de 11,76 ms; zero "
                            + "falhas e zero respostas HTTP 5xx. Meta local: p95 até 1.000 ms, zero falhas e "
                            + "menos de 1% de respostas 5xx. O cenário atingiu esses limites.",
                    "É um ensaio de leituras locais, não de escritas, rede externa, múltiplas instâncias, "
                            + "reinício, restauração ou disponibilidade em produção."
            ));
            section("10. Conclusão", List.of(
                    "A implementação Java entrega o fluxo acadêmico de pedidos, reserva de estoque, pagamento "
                            + "simulado e acompanhamento operacional. Os testes e a medição documentam somente "
                            + "o comportamento observado no ambiente local.",
                    "O código executável da API, testes e ferramentas mantidas está em Java 21. O repositório "
                            + "público foi atualizado; a cópia limpa Java foi validada. A coleção Postman ainda "
                            + "não foi validada contra esta revisão. Os campos acadêmicos não fornecidos foram "
                            + "marcados como a informar. A referência da pasta uninter foi usada conforme orientação "
                            + "da discente, preservada a ressalva indicada pelos próprios arquivos."
            ));
            section("Referências", List.of(
                    "Materiais de referência designados pela discente: PDFs presentes na pasta uninter; os "
                            + "arquivos locais indicam versão editada para estudo.",
                    "Projeto Raízes do Nordeste. Documentação técnica, requisitos, privacidade e desempenho. docs/.",
                    "Oracle. Java Documentation. docs.oracle.com/en/java/.",
                    "Spring. Spring Boot Reference Documentation. docs.spring.io/spring-boot/.",
                    "Brasil. Lei nº 13.709, de 14 de agosto de 2018 (LGPD). Texto compilado no Portal da Legislação."
            ));
            closePage();
            document.save(output.toFile());
        } finally {
            document.close();
        }
    }

    private void cover() throws IOException {
        newPage();
        markdown.append("# Raízes do Nordeste\n\n")
                .append("**Angelica Dias Folgado**  \n**RU: 4937552**\n\n")
                .append("Projeto acadêmico: API REST para pedidos multicanal, estoque por unidade e pagamento simulado. ")
                .append("Relatório técnico da implementação Java 21 e Spring Boot 3.5.\n\n")
                .append("Disciplina: Projeto Multidisciplinar - Trilha Back-end  \n")
                .append("Curso: a informar  \nPolo: a informar  \nSemestre: a informar  \nDocente: a informar\n\n");
        text("Angelica Dias Folgado", BOLD, 20, 455);
        text("RU: 4937552", BODY, 12, 430);
        text("RAÍZES DO NORDESTE", BOLD, 22, 330);
        text("Projeto de API REST - Back-end", BODY, 14, 302);
        text("Implementação Java 21 / Spring Boot", BODY, 12, 276);
        text("Disciplina: Projeto Multidisciplinar - Trilha Back-end", BODY, 10, 248);
        text("Curso: a informar", BODY, 10, 232);
        text("Polo: a informar", BODY, 10, 216);
        text("Semestre: a informar", BODY, 10, 200);
        text("Docente: a informar", BODY, 10, 184);
        text("Relatório acadêmico e evidências técnicas", BODY, 11, 154);
        text("2026", BODY, 11, 100);
        closePage();
    }

    private void section(String title, List<String> paragraphs) throws IOException {
        if (page == null) {
            newPage();
        }
        ensureSpace(45);
        text(title, BOLD, 15, y);
        y -= 28;
        markdown.append("## ").append(title).append("\n\n");
        for (String paragraph : paragraphs) {
            markdown.append(paragraph).append("\n\n");
            List<String> lines = wrap(paragraph, BODY, BODY_SIZE, PDRectangle.LETTER.getWidth() - 2 * MARGIN);
            ensureSpace(lines.size() * BODY_LEADING + 12);
            for (String line : lines) {
                text(line, BODY, BODY_SIZE, y);
                y -= BODY_LEADING;
            }
            y -= 10;
        }
    }

    private List<String> wrap(String content, PDType1Font font, float size, float width) throws IOException {
        var lines = new java.util.ArrayList<String>();
        var current = new StringBuilder();
        for (String word : content.split("\\s+")) {
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
        page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        pageNumber++;
        stream = new PDPageContentStream(document, page);
        y = page.getMediaBox().getHeight() - MARGIN;
        if (pageNumber > 1) {
            text("RAIZES DO NORDESTE | ANGELICA DIAS FOLGADO | RU 4937552", BODY, 8,
                    page.getMediaBox().getHeight() - 30);
        }
    }

    private void closePage() throws IOException {
        if (stream != null) {
            stream.beginText();
            stream.setFont(BODY, 8);
            stream.newLineAtOffset(MARGIN, 30);
            stream.showText("RU 4937552 | " + pageNumber);
            stream.endText();
            stream.close();
            stream = null;
            page = null;
        }
    }

    private void text(String value, PDType1Font font, float size, float baseline) throws IOException {
        textAt(value, font, size, MARGIN, baseline);
    }

    private void textAt(String value, PDType1Font font, float size, float x, float baseline) throws IOException {
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
        text(title, BOLD, 15, y);
        markdown.append("## ").append(title).append("\n\n")
                .append(caption).append("\n\n")
                .append("![ ").append(title).append(" ](../../")
                .append(imagePath.toString().replace('\\', '/')).append(")\n\n");
        y -= 26;
        for (String line : wrap(caption, BODY, 9, PDRectangle.LETTER.getWidth() - 2 * MARGIN)) {
            text(line, BODY, 9, y);
            y -= 13;
        }
        PDImageXObject image = PDImageXObject.createFromFile(imagePath.toAbsolutePath().toString(), document);
        float maxWidth = PDRectangle.LETTER.getWidth() - 2 * MARGIN;
        float maxHeight = y - BOTTOM - 10;
        float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        float x = MARGIN + (maxWidth - width) / 2;
        stream.drawImage(image, x, Math.max(BOTTOM, y - height - 10), width, height);
    }

    private void addJavaClassDiagram() throws IOException {
        closePage();
        newPage();
        text("Anexo C - Diagrama de classes Java (visão técnica)", BOLD, 14, y);
        markdown.append("## Anexo C - Diagrama de classes Java (visão técnica)\n\n")
                .append("O diagrama representa as classes reais da implementação. Ele não substitui um modelo ")
                .append("de entidades Java: as entidades de domínio são projetadas por JDBC em mapas e records pontuais.\n\n")
                .append("ApiController usa RaizesStore e PaymentGateway; MockPaymentGateway implementa o contrato; ")
                .append("AuthenticationFilter usa JwtService; ApiExceptionHandler converte ApiException em HTTP.\n\n");
        y -= 34;
        float left = MARGIN;
        float right = 320;
        float width = 220;
        float height = 82;
        float[] rows = {620, 475, 330, 185};
        drawClassBox("ApiController", List.of("REST e regras de autorização", "pedidos, pagamentos, privacidade"), left, rows[0], width, height);
        drawClassBox("PaymentGateway <<interface>>", List.of("submit(request)", "contrato de pagamento"), right, rows[0], width, height);
        drawClassBox("RaizesStore", List.of("JDBC, consultas e transações", "serialização de escrita SQLite"), left, rows[1], width, height);
        drawClassBox("MockPaymentGateway", List.of("APROVADO / RECUSADO", "INDISPONIVEL / TIMEOUT"), right, rows[1], width, height);
        drawClassBox("AuthenticationFilter", List.of("valida JWT por requisição", "identidade e perfil"), left, rows[2], width, height);
        drawClassBox("JwtService", List.of("emissão e validação HS256", "expiração e claims"), right, rows[2], width, height);
        drawClassBox("ApiExceptionHandler", List.of("envelope HTTP de erro", "requestId e código"), left, rows[3], width, height);
        drawClassBox("ApiException", List.of("erro de aplicação", "status e detalhes"), right, rows[3], width, height);
        drawLine(left + width, rows[0] + height / 2, right, rows[0] + height / 2);
        drawLine(left + width / 2, rows[0], left + width / 2, rows[1] + height);
        drawLine(right + width / 2, rows[1] + height, right + width / 2, rows[0]);
        drawLine(left + width, rows[2] + height / 2, right, rows[2] + height / 2);
        drawLine(left + width, rows[3] + height / 2, right, rows[3] + height / 2);
    }

    private void drawClassBox(String title, List<String> details, float x, float bottom, float width, float height)
            throws IOException {
        stream.addRect(x, bottom, width, height);
        stream.stroke();
        stream.moveTo(x, bottom + height - 22);
        stream.lineTo(x + width, bottom + height - 22);
        stream.stroke();
        textAt(title, BOLD, 9, x + 6, bottom + height - 15);
        float baseline = bottom + height - 36;
        for (String detail : details) {
            textAt(detail, BODY, 8, x + 6, baseline);
            baseline -= 13;
        }
    }

    private void drawLine(float x1, float y1, float x2, float y2) throws IOException {
        stream.moveTo(x1, y1);
        stream.lineTo(x2, y2);
        stream.stroke();
    }

    private static void validate(Path output) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            String extracted = new PDFTextStripper().getText(pdf);
            if (pdf.getNumberOfPages() < 8
                    || !extracted.contains("Angelica Dias Folgado")
                    || !extracted.contains("4937552")
                    || !extracted.contains("228.399")
                    || !extracted.contains("11,76 ms")
                    || !extracted.contains("100% Java")
                    || !extracted.contains("https://github.com/angelicafolgado/raizes-do-nordeste")
                    || !extracted.contains("Curso: a informar")
                    || !extracted.contains("Polo: a informar")
                    || !extracted.contains("Semestre: a informar")
                    || !extracted.contains("Docente: a informar")
                    || !extracted.contains("Plano de testes e evidências")) {
                throw new IOException("O PDF gerado não contém as seções ou evidências acadêmicas esperadas.");
            }
        }
    }
}
