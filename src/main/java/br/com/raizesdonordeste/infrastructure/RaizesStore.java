package br.com.raizesdonordeste.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.raizesdonordeste.domain.ApiException;
import br.com.raizesdonordeste.domain.Identity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class RaizesStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper;
    // SQLite has one writer; serialize local read-modify-write transactions to preserve compare-and-set behavior.
    private final ReentrantLock sqliteWriteLock = new ReentrantLock(true);

    public RaizesStore(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.mapper = mapper;
    }

    public <T> T transaction(org.springframework.transaction.support.TransactionCallback<T> work) {
        sqliteWriteLock.lock();
        try {
            return transaction.execute(work);
        } finally {
            sqliteWriteLock.unlock();
        }
    }

    public Optional<Map<String, Object>> userByEmail(String email) {
        return one("SELECT * FROM usuarios WHERE email = ?", USER, email);
    }

    public Optional<Map<String, Object>> userById(long id) {
        return one("SELECT * FROM usuarios WHERE id = ?", USER, id);
    }

    public Optional<Long> assignedUnit(long userId) {
        List<Long> units = jdbc.query("SELECT unidadeId FROM usuarios WHERE id=?",
                (rs, row) -> {
                    long unitId = rs.getLong("unidadeId");
                    return rs.wasNull() ? null : unitId;
                }, userId);
        return units.stream().filter(java.util.Objects::nonNull).findFirst();
    }

    public Optional<Map<String, Object>> unit(long id) {
        return one("SELECT * FROM unidades WHERE id = ?", UNIT, id);
    }

    public Optional<Map<String, Object>> product(long id) {
        return one("SELECT * FROM produtos WHERE id = ?", PRODUCT, id);
    }

    public record Page(List<Map<String, Object>> dados, Map<String, Object> paginacao) {
        public Map<String, Object> asMap() { return Map.of("dados", dados, "paginacao", paginacao); }
    }

    public Page units(int page, int limit) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT id,nome,cidade FROM unidades ORDER BY id LIMIT ? OFFSET ?",
                UNIT, limit, offset(page, limit));
        return page(rows, jdbc.queryForObject("SELECT COUNT(*) FROM unidades", Long.class), page, limit);
    }

    public Page products(Long unitId, int page, int limit) {
        if (unitId == null) {
            List<Map<String, Object>> products = jdbc.query("SELECT id,nome,descricao,preco FROM produtos ORDER BY id LIMIT ? OFFSET ?",
                    PRODUCT, limit, offset(page, limit));
            products.forEach(product -> product.put("estoques", stockRows(number(product.get("id")), null)));
            return page(products, jdbc.queryForObject("SELECT COUNT(*) FROM produtos", Long.class), page, limit);
        }
        String where = " FROM produtos p JOIN estoques_unidades e ON e.produtoId=p.id WHERE e.unidadeId=? ";
        List<Map<String, Object>> products = jdbc.query("SELECT p.id,p.nome,p.descricao,p.preco " + where +
                        "ORDER BY p.id LIMIT ? OFFSET ?",
                PRODUCT, unitId, limit, offset(page, limit));
        products.forEach(product -> product.put("estoques", stockRows(number(product.get("id")), unitId)));
        long total = jdbc.queryForObject("SELECT COUNT(*)" + where, Long.class, unitId);
        return page(products, total, page, limit);
    }

    private List<Map<String, Object>> stockRows(long productId, Long unitId) {
        String sql = "SELECT e.id,e.unidadeId,e.produtoId,e.quantidade,u.id AS unit_id,u.nome AS unit_name,u.cidade AS unit_city " +
                "FROM estoques_unidades e JOIN unidades u ON u.id=e.unidadeId WHERE e.produtoId=?" +
                (unitId == null ? "" : " AND e.unidadeId=?") + " ORDER BY e.unidadeId";
        return unitId == null
                ? jdbc.query(sql, (rs, row) -> stock(rs), productId)
                : jdbc.query(sql, (rs, row) -> stock(rs), productId, unitId);
    }

    private static Map<String, Object> stock(ResultSet rs) throws SQLException {
        return Map.of("id", rs.getLong("id"), "unidadeId", rs.getLong("unidadeId"),
                "produtoId", rs.getLong("produtoId"), "quantidade", rs.getInt("quantidade"),
                "unidade", Map.of("id", rs.getLong("unit_id"), "nome", rs.getString("unit_name"),
                        "cidade", rs.getString("unit_city")));
    }

    public List<Map<String, Object>> orderItems(long orderId) {
        return jdbc.query("SELECT i.id,i.pedidoId,i.produtoId,i.quantidade,i.precoUnitario," +
                        "p.nome AS produtoNome,p.descricao AS produtoDescricao,p.preco AS produtoPreco " +
                        "FROM itens_pedido i JOIN produtos p ON p.id=i.produtoId WHERE i.pedidoId=? ORDER BY i.id",
                (rs, row) -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", rs.getLong("id")); item.put("pedidoId", rs.getLong("pedidoId"));
                    item.put("produtoId", rs.getLong("produtoId")); item.put("quantidade", rs.getInt("quantidade"));
                    item.put("precoUnitario", rs.getDouble("precoUnitario"));
                    item.put("produto", Map.of("id", rs.getLong("produtoId"), "nome", rs.getString("produtoNome"),
                            "descricao", rs.getString("produtoDescricao"), "preco", rs.getDouble("produtoPreco")));
                    return item;
                }, orderId);
    }

    public Optional<Map<String, Object>> order(long id) {
        return one("SELECT * FROM pedidos WHERE id=?", ORDER, id).map(order -> {
            order.put("itens", orderItems(id));
            one("SELECT id,pedidoId,valor,status,metodo,transacaoId,criadoEm FROM pagamentos WHERE pedidoId=?",
                    PAYMENT, id).ifPresent(payment -> order.put("pagamento", payment));
            return order;
        });
    }

    public Page orders(Identity identity, Long assignedUnitId, String channel, int page, int limit) {
        boolean client = identity.role().equals("CLIENTE");
        boolean unitScoped = !client && !identity.role().equals("ADMIN");
        StringBuilder where = new StringBuilder(client ? " WHERE usuarioId=? " : unitScoped ? " WHERE unidadeId=? " : "");
        List<Object> args = new ArrayList<>();
        if (client) args.add(identity.id());
        else if (unitScoped) args.add(assignedUnitId);
        if (channel != null) { where.append(" AND canalPedido=? "); args.add(channel); }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM pedidos" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(limit); pageArgs.add(offset(page, limit));
        List<Map<String, Object>> orders = jdbc.query("SELECT * FROM pedidos" + where +
                        " ORDER BY criadoEm DESC,id DESC LIMIT ? OFFSET ?", ORDER, pageArgs.toArray());
        orders.forEach(order -> {
            long id = number(order.get("id"));
            order.put("itens", orderItems(id));
            one("SELECT id,pedidoId,valor,status,metodo,transacaoId,criadoEm FROM pagamentos WHERE pedidoId=?",
                    PAYMENT, id).ifPresent(payment -> order.put("pagamento", payment));
        });
        return page(orders, total, page, limit);
    }

    public long createOrder(long userId, long unitId, String channel, double total, List<Map<String, Object>> items) {
        jdbc.update("INSERT INTO pedidos(usuarioId,unidadeId,canalPedido,total,atualizadoEm) VALUES(?,?,?,?,CURRENT_TIMESTAMP)",
                userId, unitId, channel, total);
        long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        for (Map<String, Object> item : items)
            jdbc.update("INSERT INTO itens_pedido(pedidoId,produtoId,quantidade,precoUnitario) VALUES(?,?,?,?)",
                    id, item.get("produtoId"), item.get("quantidade"), item.get("precoUnitario"));
        return id;
    }

    public boolean reserve(long unitId, long productId, int quantity) {
        return jdbc.update("UPDATE estoques_unidades SET quantidade=quantidade-? " +
                "WHERE unidadeId=? AND produtoId=? AND quantidade>=?", quantity, unitId, productId, quantity) == 1;
    }

    public void returnStock(long unitId, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items)
            jdbc.update("UPDATE estoques_unidades SET quantidade=quantidade+? WHERE unidadeId=? AND produtoId=?",
                    item.get("quantidade"), unitId, item.get("produtoId"));
    }

    public boolean changeOrderStatus(long id, String from, String to) {
        return jdbc.update("UPDATE pedidos SET status=?,atualizadoEm=CURRENT_TIMESTAMP WHERE id=? AND status=?",
                to, id, from) == 1;
    }

    public void payment(long orderId, double amount, String status, String transactionId) {
        jdbc.update("INSERT INTO pagamentos(pedidoId,valor,status,metodo,transacaoId) VALUES(?,?,?,?,?) " +
                        "ON CONFLICT(pedidoId) DO UPDATE SET valor=excluded.valor,status=excluded.status," +
                        "metodo=excluded.metodo,transacaoId=excluded.transacaoId,criadoEm=CURRENT_TIMESTAMP",
                orderId, amount, status, "SIMULACAO_GATEWAY", transactionId);
    }

    public void audit(String action, Object details, Long userId) {
        try {
            jdbc.update("INSERT INTO audit_logs(acao,detalhes,usuarioId) VALUES(?,?,?)",
                    action, mapper.writeValueAsString(details), userId);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível serializar os detalhes de auditoria.", exception);
        }
    }

    public Optional<Map<String, Object>> latestConsent(long userId, String purpose) {
        return one("SELECT * FROM consentimentos WHERE usuarioId=? AND finalidade=? ORDER BY id DESC LIMIT 1",
                CONSENT, userId, purpose);
    }

    public Map<String, Object> createConsent(long userId, String purpose, String version, boolean accepted) {
        jdbc.update("INSERT INTO consentimentos(usuarioId,finalidade,versaoAviso,aceito,demonstracao) VALUES(?,?,?,?,TRUE)",
                userId, purpose, version, accepted);
        long id = jdbc.queryForObject("SELECT last_insert_rowid()", Long.class);
        return one("SELECT * FROM consentimentos WHERE id=?", CONSENT, id).orElseThrow();
    }

    public Page consents(long userId, int page, int limit) {
        List<Map<String, Object>> rows = jdbc.query("SELECT id,usuarioId,finalidade,versaoAviso,aceito,demonstracao,registradoEm " +
                "FROM consentimentos WHERE usuarioId=? ORDER BY id DESC LIMIT ? OFFSET ?",
                CONSENT, userId, limit, offset(page, limit));
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM consentimentos WHERE usuarioId=?", Long.class, userId);
        return page(rows, total, page, limit);
    }

    public Page auditLogs(String action, int page, int limit) {
        String where = action == null ? "" : " WHERE acao=?";
        Object[] countArgs = action == null ? new Object[0] : new Object[]{action};
        List<Map<String, Object>> rows = jdbc.query("SELECT id,acao,detalhes,usuarioId,timestamp FROM audit_logs" + where +
                " ORDER BY id DESC LIMIT ? OFFSET ?", AUDIT,
                action == null ? new Object[]{limit, offset(page, limit)} : new Object[]{action, limit, offset(page, limit)});
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs" + where, Long.class, countArgs);
        return page(rows, total, page, limit);
    }

    public long count(String table) {
        if (!List.of("usuarios", "unidades", "produtos", "pedidos", "itens_pedido", "pagamentos",
                "audit_logs", "consentimentos").contains(table)) throw new IllegalArgumentException("Unknown table");
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    private <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... args) {
        return jdbc.query(sql, mapper, args).stream().findFirst();
    }

    private static long offset(int page, int limit) { return (long) (page - 1) * limit; }

    private static Page page(List<Map<String, Object>> rows, long total, int page, int limit) {
        long pages = total == 0 ? 0 : (total + limit - 1) / limit;
        return new Page(rows, Map.of("pagina", page, "limite", limit, "total", total, "totalPaginas", pages));
    }

    private static long number(Object value) { return ((Number) value).longValue(); }

    private static Map<String, Object> row(ResultSet rs, List<String> fields) throws SQLException {
        Map<String, Object> value = new LinkedHashMap<>();
        for (String field : fields) {
            Object fieldValue = rs.getObject(field);
            if (field.equals("aceito") || field.equals("demonstracao") || field.equals("consentimentoLGPD"))
                fieldValue = rs.getBoolean(field);
            if (field.equals("preco") || field.equals("precoUnitario") || field.equals("total") || field.equals("valor"))
                fieldValue = rs.getDouble(field);
            if (field.equals("quantidade")) fieldValue = rs.getInt(field);
            if (field.equals("id") || field.equals("usuarioId") || field.equals("pedidoId") ||
                    field.equals("unidadeId") || field.equals("produtoId")) fieldValue = rs.getLong(field);
            value.put(field, fieldValue);
        }
        return value;
    }

    private static final RowMapper<Map<String, Object>> USER = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "nome", "email", "senha", "role", "consentimentoLGPD", "criadoEm"));
    private static final RowMapper<Map<String, Object>> UNIT = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "nome", "cidade"));
    private static final RowMapper<Map<String, Object>> PRODUCT = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "nome", "descricao", "preco"));
    private static final RowMapper<Map<String, Object>> ORDER = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "usuarioId", "unidadeId", "canalPedido", "status", "total", "criadoEm", "atualizadoEm"));
    private static final RowMapper<Map<String, Object>> PAYMENT = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "pedidoId", "valor", "status", "metodo", "transacaoId", "criadoEm"));
    private static final RowMapper<Map<String, Object>> CONSENT = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "usuarioId", "finalidade", "versaoAviso", "aceito", "demonstracao", "registradoEm"));
    private static final RowMapper<Map<String, Object>> AUDIT = (rs, row) ->
            RaizesStore.row(rs, List.of("id", "acao", "detalhes", "usuarioId", "timestamp"));

    public static double money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static long number(Map<String, Object> value, String key) {
        Object raw = value.get(key);
        if (!(raw instanceof Number number)) throw new ApiException("PARAMETRO_INVALIDO", key + " inválido.", 422);
        long converted = number.longValue();
        if (number.doubleValue() != converted || converted < 1)
            throw new ApiException("PARAMETRO_INVALIDO", key + " deve ser inteiro positivo.", 422);
        return converted;
    }
}
