package br.com.raizesdonordeste.application;

import br.com.raizesdonordeste.config.JwtService;
import br.com.raizesdonordeste.domain.ApiException;
import br.com.raizesdonordeste.domain.Identity;
import br.com.raizesdonordeste.application.payments.PaymentGateway;
import br.com.raizesdonordeste.infrastructure.RaizesStore;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import org.springframework.util.MultiValueMap;


import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class RaizesService {
    private static final com.fasterxml.jackson.databind.ObjectMapper AUDIT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();
    private static final String PURPOSE = "FIDELIDADE";
    private static final String NOTICE_VERSION = "1.0";
    private static final Set<String> CHANNELS = Set.of("APP", "TOTEM", "BALCAO", "PICKUP", "WEB");
    private static final Set<String> STATUSES = Set.of("AGUARDANDO_PAGAMENTO", "EM_PREPARO", "PRONTO", "ENTREGUE", "CANCELADO");
    private static final Pattern AUDIT_ACTION = Pattern.compile("^[A-Z_]{1,64}$");
    private static final List<String> AUDIT_FIELDS = List.of("pedidoId", "total", "canal", "statusPagamento", "transacaoId",
            "correlacaoId", "motivo", "anterior", "novoStatus", "pagina", "limite", "canalPedido",
            "quantidadeRetornada", "escopo", "acaoFiltro", "consentimentoId", "finalidade", "versaoAviso", "demonstracao");

    private final RaizesStore store;
    private final JwtService jwt;
    private final PaymentGateway paymentGateway;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public RaizesService(RaizesStore store, JwtService jwt, PaymentGateway paymentGateway) {
        this.store = store;
        this.jwt = jwt;
        this.paymentGateway = paymentGateway;
    }

    public Map<String, Object> login(Map<String, Object> body) {
        if (body == null) throw new ApiException("JSON_INVALIDO", "Corpo JSON inválido.", 400);
        if (!body.keySet().stream().allMatch(Set.of("email", "senha")::contains)
                || !(body.get("email") instanceof String email) || !(body.get("senha") instanceof String password)
                || email.isBlank() || password.isBlank())
            throw new ApiException("DADOS_INVALIDOS", "E-mail e senha são obrigatórios.", 422);
        Map<String, Object> user = store.userByEmail(email).orElseThrow(
                () -> new ApiException("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.", 401));
        if (!passwords.matches(password, String.valueOf(user.get("senha"))))
            throw new ApiException("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos.", 401);
        Identity principal = new Identity(((Number) user.get("id")).longValue(),
                String.valueOf(user.get("nome")), String.valueOf(user.get("role")));
        return Map.of("accessToken", jwt.issue(principal), "tokenType", "Bearer", "expiresIn", jwt.expiresIn(),
                "usuario", Map.of("id", principal.id(), "nome", principal.nome(), "email", email, "role", principal.role()));
    }

    public Map<String, String> health() { return Map.of("status", "ok"); }

    public Map<String, Object> units(MultiValueMap<String, String> query) {
        int page = pageNumber(query, "page", 1);
        int limit = pageNumber(query, "limit", 10);
        validatePagination(page, limit);
        return store.units(page, limit).asMap();
    }

    public Map<String, Object> products(MultiValueMap<String, String> query) {
        int page = pageNumber(query, "page", 1);
        int limit = pageNumber(query, "limit", 10);
        validatePagination(page, limit);
        Long unitId = optionalPositiveQuery(query, "unidadeId");
        if (unitId != null && store.unit(unitId).isEmpty())
            throw new ApiException("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.", 404);
        return store.products(unitId, page, limit).asMap();
    }

    public Map<String, Object> createOrder(Map<String, Object> body, Identity identity) {
        requireRole(identity, "CLIENTE", "ATENDENTE", "ADMIN");
        if (body == null) throw new ApiException("DADOS_INVALIDOS", "Corpo do pedido inválido.", 422);
        if (!Set.of("unidadeId", "canalPedido", "itens").containsAll(body.keySet()))
            throw new ApiException("DADOS_INVALIDOS", "Corpo do pedido contém campos não permitidos.", 422);
        long unitId = positive(body.get("unidadeId"), "unidadeId");
        Object rawChannel = body.get("canalPedido");
        if (!(rawChannel instanceof String channel) || !CHANNELS.contains(channel))
            throw new ApiException("CANAL_INVALIDO", "Canal inválido. Os valores aceitos são APP, TOTEM, BALCAO, PICKUP e WEB.", 400);
        if (identity.role().equals("ATENDENTE") && assignedUnit(identity) != unitId)
            throw new ApiException("ACESSO_NEGADO", "Atendente não pode criar pedidos para outra unidade.", 403);
        if (!(body.get("itens") instanceof List<?> rawItems) || rawItems.isEmpty() || rawItems.size() > 100)
            throw new ApiException("ITENS_INVALIDOS", "Informe entre 1 e 100 itens.", 422);
        Map<Long, Integer> grouped = new LinkedHashMap<>();
        for (Object raw : rawItems) {
            if (!(raw instanceof Map<?, ?> item) || !Set.of("produtoId", "quantidade").containsAll(item.keySet()))
                throw new ApiException("ITENS_INVALIDOS", "Item inválido.", 422);
            long productId = positive(item.get("produtoId"), "produtoId");
            long quantity = positive(item.get("quantidade"), "quantidade");
            try {
                grouped.merge(productId, Math.toIntExact(quantity), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new ApiException("PARAMETRO_INVALIDO", "quantidade excede o limite permitido.", 422);
            }
        }

        long orderId = store.transaction(status -> {
            if (store.unit(unitId).isEmpty())
                throw new ApiException("UNIDADE_NAO_ENCONTRADA", "Unidade não encontrada.", 404);
            long cents = 0;
            List<Map<String, Object>> items = new ArrayList<>();
            for (var entry : grouped.entrySet()) {
                Map<String, Object> product = store.product(entry.getKey()).orElseThrow(
                        () -> new ApiException("PRODUTO_NAO_ENCONTRADO", "Produto não encontrado.", 404));
                if (!store.reserve(unitId, entry.getKey(), entry.getValue()))
                    throw new ApiException("ESTOQUE_INSUFICIENTE", "Estoque insuficiente para " + product.get("nome") + ".", 409);
                long priceCents = Math.round(((Number) product.get("preco")).doubleValue() * 100);
                try { cents = Math.addExact(cents, Math.multiplyExact(priceCents, entry.getValue())); }
                catch (ArithmeticException exception) { throw new ApiException("TOTAL_INVALIDO", "Total excede o limite permitido.", 422); }
                items.add(Map.of("produtoId", entry.getKey(), "quantidade", entry.getValue(),
                        "precoUnitario", product.get("preco")));
            }
            long createdId = store.createOrder(identity.id(), unitId, channel, cents / 100.0, items);
            store.audit("CRIACAO_PEDIDO", Map.of("pedidoId", createdId, "total", cents / 100.0, "canal", channel), identity.id());
            return createdId;
        });
        return store.order(orderId).orElseThrow();
    }

    public Map<String, Object> orders(MultiValueMap<String, String> query, Identity identity) {
        int page = pageNumber(query, "page", 1);
        int limit = pageNumber(query, "limit", 10);
        validatePagination(page, limit);
        String channel = optionalStringQuery(query, "canalPedido");
        if (channel != null && !CHANNELS.contains(channel))
            throw new ApiException("CANAL_INVALIDO", "Filtro de canal inválido.", 400);
        return store.transaction(status -> {
            Long assignedUnitId = unitScopedRole(identity) ? assignedUnit(identity) : null;
            RaizesStore.Page result = store.orders(identity, assignedUnitId, channel, page, limit);
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("pagina", page); audit.put("limite", limit); audit.put("canalPedido", channel);
            audit.put("quantidadeRetornada", result.dados().size());
            audit.put("escopo", identity.role().equals("CLIENTE") ? "PROPRIOS"
                    : identity.role().equals("ADMIN") ? "REDE" : "UNIDADE");
            store.audit("CONSULTA_PEDIDOS", audit, identity.id());
            return result.asMap();
        });
    }

    public Map<String, Object> pay(String id, Map<String, Object> body,
                                   Identity identity) {
        requireRole(identity, "CLIENTE", "ATENDENTE", "ADMIN");
        long orderId = pathId(id);
        if (body == null || !Set.of("statusSimulado").containsAll(body.keySet()) ||
                !(body.get("statusSimulado") instanceof String scenario) ||
                !Set.of("APROVADO", "RECUSADO", "INDISPONIVEL", "TIMEOUT").contains(scenario))
            throw new ApiException("PARAMETRO_INVALIDO",
                    "statusSimulado deve ser APROVADO, RECUSADO, INDISPONIVEL ou TIMEOUT.", 400);
        String correlation = UUID.randomUUID().toString();
        PaymentResult result = store.transaction(status -> {
            Map<String, Object> order = store.order(orderId).orElseThrow(
                    () -> new ApiException("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado.", 404));
            if (identity.role().equals("CLIENTE") && ((Number) order.get("usuarioId")).longValue() != identity.id())
                throw new ApiException("ACESSO_NEGADO", "Este pedido pertence a outro cliente.", 403);
            requireOrderUnitAccess(order, identity);
            if (!"AGUARDANDO_PAGAMENTO".equals(order.get("status")))
                throw new ApiException("ESTADO_INVALIDO", "Pedido não está aguardando pagamento.", 409);
            PaymentGateway.Response gatewayResult = paymentGateway.process(new PaymentGateway.Request(
                    orderId, Math.round(((Number) order.get("total")).doubleValue() * 100), "BRL", scenario, correlation));
            if (gatewayResult == null || !correlation.equals(gatewayResult.correlationId()))
                throw new ApiException("RESPOSTA_GATEWAY_INVALIDA",
                        "Resposta do gateway não corresponde à solicitação.", 502);
            boolean technicalFailure = Set.of("INDISPONIVEL", "TIMEOUT").contains(gatewayResult.status());
            boolean knownStatus = Set.of("APROVADO", "RECUSADO", "INDISPONIVEL", "TIMEOUT").contains(gatewayResult.status());
            if (!knownStatus ||
                    gatewayResult.approved() != "APROVADO".equals(gatewayResult.status()) ||
                    (technicalFailure && gatewayResult.transactionId() != null) ||
                    (!technicalFailure && (gatewayResult.transactionId() == null || gatewayResult.transactionId().isBlank())))
                throw new ApiException("RESPOSTA_GATEWAY_INVALIDA",
                        "Resposta do gateway não corresponde à solicitação.", 502);
            if (technicalFailure) {
                store.audit("FALHA_PAGAMENTO_MOCK",
                        Map.of("pedidoId", orderId, "correlacaoId", correlation, "motivo", gatewayResult.status()), identity.id());
                return new PaymentResult(gatewayResult.status(), correlation, null);
            }
            boolean approved = gatewayResult.approved();
            String nextStatus = approved ? "EM_PREPARO" : "AGUARDANDO_PAGAMENTO";
            if (!store.changeOrderStatus(orderId, "AGUARDANDO_PAGAMENTO", nextStatus))
                throw new ApiException("CONFLITO_PEDIDO", "Pedido alterado por outra operação.", 409);
            String transactionId = gatewayResult.transactionId();
            store.payment(orderId, ((Number) order.get("total")).doubleValue(), gatewayResult.status(), transactionId);
            store.audit("PAGAMENTO_MOCK",
                    Map.of("pedidoId", orderId, "statusPagamento", gatewayResult.status(),
                            "transacaoId", transactionId, "correlacaoId", correlation), identity.id());
            return new PaymentResult(null, correlation, Map.of(
                    "sucesso", approved,
                    "mensagem", gatewayResult.message(),
                    "transacaoId", transactionId,
                    "statusPedido", nextStatus,
                    "detalhes", store.order(orderId).orElseThrow().get("pagamento")));
        });
        if (result.failure != null) {
            boolean timeout = result.failure.equals("TIMEOUT");
            throw new ApiException(timeout ? "GATEWAY_TIMEOUT" : "GATEWAY_INDISPONIVEL",
                    timeout ? "Tempo de resposta do gateway simulado esgotado." : "Gateway simulado indisponível.",
                    timeout ? 504 : 503, List.of(Map.of("field", "correlacaoId", "issue", result.correlationId)));
        }
        return result.body;
    }

    private record PaymentResult(String failure, String correlationId, Map<String, Object> body) {}

    private static boolean unitScopedRole(Identity identity) {
        return identity.role().equals("ATENDENTE") || identity.role().equals("COZINHA");
    }

    private long assignedUnit(Identity identity) {
        return store.assignedUnit(identity.id()).orElseThrow(
                () -> new ApiException("ACESSO_NEGADO", "Perfil operacional sem unidade vinculada.", 403));
    }

    private void requireOrderUnitAccess(Map<String, Object> order, Identity identity) {
        if (unitScopedRole(identity) &&
                ((Number) order.get("unidadeId")).longValue() != assignedUnit(identity))
            throw new ApiException("ACESSO_NEGADO", "Pedido pertence a outra unidade.", 403);
    }

    public Map<String, Object> changeStatus(String id, Map<String, Object> body,
                                            Identity identity) {
        requireRole(identity, "COZINHA", "ADMIN");
        if (body == null || !Set.of("status").containsAll(body.keySet()) ||
                !(body.get("status") instanceof String next) || !STATUSES.contains(next))
            throw new ApiException("STATUS_INVALIDO", "Status inválido.", 400);
        long orderId = pathId(id);
        return store.transaction(status -> {
            Map<String, Object> order = store.order(orderId).orElseThrow(
                    () -> new ApiException("PEDIDO_NAO_ENCONTRADO", "Pedido não encontrado.", 404));
            requireOrderUnitAccess(order, identity);
            String previous = String.valueOf(order.get("status"));
            br.com.raizesdonordeste.domain.OrderRules.validateTransition(previous, next);
            if (!store.changeOrderStatus(orderId, previous, next))
                throw new ApiException("CONFLITO_PEDIDO", "Pedido alterado por outra operação.", 409);
            if (next.equals("CANCELADO"))
                store.returnStock(((Number) order.get("unidadeId")).longValue(), castItems(order.get("itens")));
            store.audit("ATUALIZACAO_STATUS", Map.of("pedidoId", orderId, "anterior", previous, "novoStatus", next), identity.id());
            return store.order(orderId).orElseThrow();
        });
    }

    private static List<Map<String, Object>> castItems(Object value) {
        if (!(value instanceof List<?> rows)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object row : rows) if (row instanceof Map<?, ?> map) {
            Map<String, Object> item = new HashMap<>();
            map.forEach((key, itemValue) -> item.put(String.valueOf(key), itemValue));
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> privacyNotice() {
        return Map.of("finalidade", PURPOSE, "versaoAviso", NOTICE_VERSION, "demonstracao", true, "opcional", true,
                "titulo", "Preferência de fidelidade — demonstração acadêmica",
                "descricao", "Registrar sua decisão opcional sobre participação na demonstração de fidelidade. Esta versão apenas registra a preferência: não acumula pontos, não concede benefícios e não envia mensagens. Um programa real exigirá novo aviso e nova decisão.",
                "dados", List.of("Identificador do usuário autenticado", "Decisão de aceitar ou revogar", "Versão do aviso", "Data e hora da decisão"),
                "revogacao", "A revogação ocorre com {\"aceito\":false} em PUT /api/privacidade/consentimentos/fidelidade. Pedidos e login permanecem disponíveis.",
                "retencao", "O histórico é mantido para demonstrar as decisões durante o projeto. Não há expurgo automático; o procedimento de encerramento consta na política de privacidade da entrega.",
                "compartilhamento", "Nenhum provedor externo recebe os dados de consentimento nesta demonstração.");
    }

    public Map<String, Object> updateConsent(Map<String, Object> body, Identity identity) {
        if (body == null || !Set.of("aceito", "versaoAviso").containsAll(body.keySet()) ||
                !(body.get("aceito") instanceof Boolean accepted))
            throw new ApiException("DADOS_INVALIDOS", "Os campos aceitos são aceito (booleano) e versaoAviso. A identidade vem do token.", 422);
        if (body.containsKey("versaoAviso") && !(body.get("versaoAviso") instanceof String))
            throw new ApiException("DADOS_INVALIDOS", "versaoAviso deve ser texto.", 422);
        if (accepted && !NOTICE_VERSION.equals(body.get("versaoAviso")))
            throw new ApiException("VERSAO_AVISO_INVALIDA", "O aceite requer a consulta prévia ao aviso atual.", 422);
        return store.transaction(status -> {
            Optional<Map<String, Object>> previous = store.latestConsent(identity.id(), PURPOSE);
            if (previous.isPresent() && Boolean.TRUE.equals(previous.get().get("aceito")) == accepted &&
                    (!accepted || NOTICE_VERSION.equals(previous.get().get("versaoAviso"))))
                return previous.get();
            Map<String, Object> event = store.createConsent(identity.id(), PURPOSE, NOTICE_VERSION, accepted);
            store.audit(accepted ? "CONSENTIMENTO_REGISTRADO" : "CONSENTIMENTO_REVOGADO",
                    Map.of("consentimentoId", event.get("id"), "finalidade", PURPOSE,
                            "versaoAviso", NOTICE_VERSION, "demonstracao", true), identity.id());
            return event;
        });
    }

    public Map<String, Object> readConsents(MultiValueMap<String, String> query, Identity identity) {
        int page = pageNumber(query, "page", 1), limit = pageNumber(query, "limit", 10);
        validatePagination(page, limit);
        return store.transaction(status -> {
            Optional<Map<String, Object>> last = store.latestConsent(identity.id(), PURPOSE);
            RaizesStore.Page result = store.consents(identity.id(), page, limit);
            store.audit("CONSULTA_CONSENTIMENTOS", Map.of("pagina", page, "limite", limit), identity.id());
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("finalidade", PURPOSE);
            current.put("versaoAviso", NOTICE_VERSION);
            current.put("aceito", last.isPresent() && Boolean.TRUE.equals(last.get().get("aceito"))
                    && NOTICE_VERSION.equals(last.get().get("versaoAviso")));
            current.put("demonstracao", true);
            last.ifPresent(value -> current.put("ultimoRegistroId", value.get("id")));
            Map<String, Object> response = new LinkedHashMap<>(result.asMap());
            response.put("atual", current);
            return response;
        });
    }

    public Map<String, Object> audit(MultiValueMap<String, String> query, Identity identity) {
        requireRole(identity, "ADMIN");
        int page = pageNumber(query, "page", 1), limit = pageNumber(query, "limit", 10);
        validatePagination(page, limit);
        String action = optionalStringQuery(query, "acao");
        if (action != null && !AUDIT_ACTION.matcher(action).matches())
            throw new ApiException("PARAMETRO_INVALIDO", "acao deve conter apenas letras maiúsculas e sublinhado (até 64 caracteres).", 422);
        return store.transaction(status -> {
            RaizesStore.Page result = store.auditLogs(action, page, limit);
            List<Map<String, Object>> safe = result.dados().stream().map(RaizesService::safeAudit).toList();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("pagina", page); details.put("limite", limit); details.put("acaoFiltro", action);
            store.audit("CONSULTA_AUDITORIA", details, identity.id());
            return new RaizesStore.Page(safe, result.paginacao()).asMap();
        });
    }

    private static Map<String, Object> safeAudit(Map<String, Object> log) {
        Map<String, Object> safe = new LinkedHashMap<>();
        safe.put("id", log.get("id")); safe.put("acao", log.get("acao"));
        safe.put("timestamp", log.get("timestamp"));
        Object raw = log.get("detalhes");
        try {
            JsonNodeValue parsed = JsonNodeValue.parse(String.valueOf(raw));
            Map<String, Object> filtered = new LinkedHashMap<>();
            if (parsed.value instanceof Map<?, ?> map) for (var entry : map.entrySet())
                if (AUDIT_FIELDS.contains(String.valueOf(entry.getKey())) &&
                        (entry.getValue() instanceof String || entry.getValue() instanceof Number || entry.getValue() instanceof Boolean))
                    filtered.put(String.valueOf(entry.getKey()), entry.getValue());
            safe.put("detalhes", filtered);
        } catch (RuntimeException exception) {
            safe.put("detalhes", Map.of());
        }
        return safe;
    }

    private record JsonNodeValue(Object value) {
        static JsonNodeValue parse(String text) {
            try { return new JsonNodeValue(AUDIT_MAPPER.readValue(text, Object.class)); }
            catch (com.fasterxml.jackson.core.JsonProcessingException exception) { throw new IllegalArgumentException("Invalid audit JSON", exception); }
        }
    }

    private static Long optionalPositiveQuery(MultiValueMap<String, String> query, String key) {
        String value = singleQuery(query, key);
        if (value == null) return null;
        long result = positiveQuery(value, key);
        return result;
    }

    private static String optionalStringQuery(MultiValueMap<String, String> query, String key) {
        String value = singleQuery(query, key);
        return value == null || value.isEmpty() ? null : value;
    }

    private static int pageNumber(MultiValueMap<String, String> query, String key, int fallback) {
        String raw = singleQuery(query, key);
        if (raw == null) return fallback;
        long value = positiveQuery(raw, key);
        if (value > Integer.MAX_VALUE) throw new ApiException("PARAMETRO_INVALIDO", key + " inválido.", 422);
        return (int) value;
    }

    private static String singleQuery(MultiValueMap<String, String> query, String key) {
        List<String> values = query.get(key);
        if (values == null) return null;
        if (values.size() != 1 || values.getFirst().isEmpty())
            throw new ApiException("PARAMETRO_INVALIDO", key + " inválido.", 422);
        return values.getFirst();
    }

    private static long positiveQuery(String raw, String field) {
        if (!raw.matches("^\\d+$")) throw new ApiException("PARAMETRO_INVALIDO", field + " inválido.", 422);
        try {
            long value = Long.parseLong(raw);
            if (value < 1) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new ApiException("PARAMETRO_INVALIDO", field + " inválido.", 422);
        }
    }

    private static long positive(Object raw, String field) {
        if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue()) ||
                number.doubleValue() != Math.rint(number.doubleValue()) || number.doubleValue() < 1 ||
                number.doubleValue() > Long.MAX_VALUE)
            throw new ApiException("PARAMETRO_INVALIDO", field + " deve ser inteiro positivo.", 422);
        return number.longValue();
    }

    private static long pathId(String raw) {
        if (!raw.matches("^\\d+$")) throw new ApiException("PARAMETRO_INVALIDO", "id inválido.", 422);
        return positiveQuery(raw, "id");
    }

    private static void validatePagination(int page, int limit) {
        if (page < 1 || limit < 1 || limit > 100 || ((long) page - 1) * limit > Integer.MAX_VALUE)
            throw new ApiException("PARAMETRO_INVALIDO", "Parâmetros de paginação inválidos.", 422);
    }

    private static void requireRole(Identity identity, String... roles) {
        if (Arrays.stream(roles).noneMatch(identity.role()::equals))
            throw new ApiException("ACESSO_NEGADO", "Perfil sem permissão para esta operação.", 403);
    }
}
