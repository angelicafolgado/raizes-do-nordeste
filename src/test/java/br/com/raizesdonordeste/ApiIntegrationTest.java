package br.com.raizesdonordeste;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.core.env.Environment;
import br.com.raizesdonordeste.config.SeedRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.jwt.secret=integration-test-secret-with-more-than-32-characters",
        "app.seed.enabled=false",
        "spring.datasource.url=jdbc:sqlite:target/test-api.db"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired Environment environment;

    private String maria;
    private String jose;
    private String atendente;
    private String cozinha;
    private String admin;

    @BeforeEach
    void resetDatabase() throws Exception {
        jdbc.update("DELETE FROM audit_logs");
        jdbc.execute("DROP TRIGGER IF EXISTS reject_consent_audit");
        jdbc.update("DELETE FROM consentimentos");
        jdbc.update("DELETE FROM pagamentos");
        jdbc.update("DELETE FROM itens_pedido");
        jdbc.update("DELETE FROM pedidos");
        jdbc.update("DELETE FROM estoques_unidades");
        jdbc.update("DELETE FROM produtos");
        jdbc.update("DELETE FROM usuarios");
        jdbc.update("DELETE FROM unidades");
        jdbc.update("DELETE FROM sqlite_sequence");
        String hash = new BCryptPasswordEncoder().encode("123456");
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES('Maria','maria@test.local',?,'CLIENTE')", hash);
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES('José','jose@test.local',?,'CLIENTE')", hash);
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES('Atendente','atendente@test.local',?,'ATENDENTE')", hash);
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES('Cozinha','cozinha@test.local',?,'COZINHA')", hash);
        jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES('Admin','admin@test.local',?,'ADMIN')", hash);
        jdbc.update("INSERT INTO unidades(nome,cidade) VALUES('Recife','Recife')");
        jdbc.update("INSERT INTO unidades(nome,cidade) VALUES('João Pessoa','João Pessoa')");
        jdbc.update("UPDATE usuarios SET unidadeId=1 WHERE id IN (3,4)");
        jdbc.update("INSERT INTO produtos(nome,descricao,preco) VALUES('Baião','Prato',38.50)");
        jdbc.update("INSERT INTO produtos(nome,descricao,preco) VALUES('Tapioca','Lanche',24.00)");
        jdbc.update("INSERT INTO estoques_unidades(unidadeId,produtoId,quantidade) VALUES(1,1,4)");
        jdbc.update("INSERT INTO estoques_unidades(unidadeId,produtoId,quantidade) VALUES(1,2,3)");
        jdbc.update("INSERT INTO estoques_unidades(unidadeId,produtoId,quantidade) VALUES(2,1,4)");
        jdbc.update("INSERT INTO estoques_unidades(unidadeId,produtoId,quantidade) VALUES(2,2,3)");
        maria = token("maria@test.local");
        jose = token("jose@test.local");
        atendente = token("atendente@test.local");
        cozinha = token("cozinha@test.local");
        admin = token("admin@test.local");
    }

    @Test
    void loginAndProtectedEndpointsReturnConsistentErrors() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@test.local\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIAIS_INVALIDAS"));
        mvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        mvc.perform(get("/api/pedidos").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void catalogPaginationAndStockAreReturned() throws Exception {
        mvc.perform(get("/api/produtos").param("page", "1").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].estoques[0].quantidade").value(4))
                .andExpect(jsonPath("$.paginacao.total").value(2));
        mvc.perform(get("/api/produtos").param("page", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("PARAMETRO_INVALIDO"));
        mvc.perform(get("/api/produtos").param("page", "1").param("page", "2"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/produtos").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(0))
                .andExpect(jsonPath("$.paginacao.total").value(2));
    }

    @Test
    void orderValidationAndRepeatedProductsUseAggregatedQuantity() throws Exception {
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":0}]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"INVALID\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":999,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":999,\"quantidade\":1}]}"))
                .andExpect(status().isNotFound());
        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":2},{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].quantidade").value(3))
                .andExpect(jsonPath("$.total").value(115.5))
                .andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class)).isEqualTo(1);
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(maria))
                        .param("canalPedido", "TOTEM"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").value(0));
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(maria))
                        .param("canalPedido", "INVALID"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE id=?", Integer.class, orderId)).isEqualTo(1);
    }

    @Test
    void orderPaymentAndStatusTransitionsPreserveOwnershipRules() throws Exception {
        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"TOTEM\",\"itens\":[{\"produtoId\":1,\"quantidade\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(77.0))
                .andExpect(jsonPath("$.itens[0].quantidade").value(2))
                .andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class)).isEqualTo(2);

        mvc.perform(get("/api/pedidos").header("Authorization", bearer(jose)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(0));
        mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(jose))
                        .contentType("application/json").content("{\"statusSimulado\":\"APROVADO\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"statusSimulado\":\"RECUSADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sucesso").value(false));
        mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"statusSimulado\":\"APROVADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusPedido").value("EM_PREPARO"));
        mvc.perform(patch("/api/pedidos/" + orderId + "/status").header("Authorization", bearer(cozinha))
                        .contentType("application/json").content("{\"status\":\"PRONTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PRONTO"));
        mvc.perform(patch("/api/pedidos/" + orderId + "/status").header("Authorization", bearer(cozinha))
                        .contentType("application/json").content("{\"status\":\"EM_PREPARO\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void failedOrderRollsBackReservationsAndCancellationReturnsStockOnce() throws Exception {
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1},{\"produtoId\":2,\"quantidade\":99}]}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class)).isZero();

        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/pedidos/" + orderId + "/status").header("Authorization", bearer(cozinha))
                        .contentType("application/json").content("{\"status\":\"CANCELADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/pedidos/" + orderId + "/status").header("Authorization", bearer(cozinha))
                        .contentType("application/json").content("{\"status\":\"CANCELADO\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class)).isEqualTo(4);
    }

    @Test
    void consentIsIdempotentAndAuditIsRestrictedToAdministrators() throws Exception {
        mvc.perform(get("/api/privacidade/aviso"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.demonstracao").value(true));
        mvc.perform(get("/api/privacidade/consentimentos").header("Authorization", bearer(maria)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atual.aceito").value(false))
                .andExpect(jsonPath("$.dados.length()").value(0));
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":true}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":true,\"versaoAviso\":\"1.0\",\"usuarioId\":2}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":true,\"versaoAviso\":\"1.0\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aceito").value(true));
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":true,\"versaoAviso\":\"1.0\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consentimentos", Integer.class)).isEqualTo(1);
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aceito").value(false));
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"maria@test.local\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(maria)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").isNotEmpty());
    }

    @Test
    void rootRedirectsToOpenApiAndUnknownRoutesUseStandardErrorEnvelope() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/api-docs"));
        mvc.perform(get("/api/rota-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROTA_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        mvc.perform(get("/api-docs.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/pedidos'].get.security[0].bearerAuth").isEmpty())
                .andExpect(jsonPath("$.paths['/api/pedidos'].post.requestBody.content['application/json'].schema['$ref']").value("#/components/schemas/PedidoRequest"))
                .andExpect(jsonPath("$.components.schemas.PedidoRequest.required").value(org.hamcrest.Matchers.hasItems("unidadeId", "canalPedido", "itens")))
                .andExpect(jsonPath("$.components.schemas.PedidoRequest.properties.canalPedido.enum").value(org.hamcrest.Matchers.hasItems("APP", "TOTEM", "BALCAO", "PICKUP", "WEB")))
                .andExpect(jsonPath("$.paths['/api/pedidos'].post.responses['409'].content['application/json'].schema['$ref']").value("#/components/schemas/Erro"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['200'].content['application/json'].example.expiresIn").value("8h"))
                .andExpect(jsonPath("$.paths['/api/pedidos/{id}/pagamento'].post.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/api/pedidos/{id}/pagamento'].post.responses['504']").exists())
                .andExpect(jsonPath("$.paths['/'].get.responses['302'].headers.Location.example").value("/api-docs"));
        mvc.perform(get("/api-docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));
    }

    @Test
    void oversizedJsonBodyUsesTheDocumented413Envelope() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("x".repeat(100 * 1024 + 1)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("CORPO_MUITO_GRANDE"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void technicalPaymentFailuresPreserveOrderStockAndPreviousPayment() throws Exception {
        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"statusSimulado\":\"RECUSADO\"}"))
                .andExpect(status().isOk());
        for (var failure : java.util.List.of(
                new String[]{"INDISPONIVEL", "503", "GATEWAY_INDISPONIVEL"},
                new String[]{"TIMEOUT", "504", "GATEWAY_TIMEOUT"})) {
            mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(maria))
                            .contentType("application/json").content("{\"statusSimulado\":\"" + failure[0] + "\"}"))
                    .andExpect(status().is(Integer.parseInt(failure[1])))
                    .andExpect(jsonPath("$.error").value(failure[2]))
                    .andExpect(jsonPath("$.details[0].field").value("correlacaoId"));
        }
        assertThat(jdbc.queryForObject("SELECT status FROM pedidos WHERE id=?", String.class, orderId))
                .isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(jdbc.queryForObject("SELECT status FROM pagamentos WHERE pedidoId=?", String.class, orderId))
                .isEqualTo("RECUSADO");
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void seedIsIdempotentAndDoesNotOverwriteLiveOrdersOrStock() throws Exception {
        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"WEB\",\"itens\":[{\"produtoId\":1,\"quantidade\":2}]}"))
                .andExpect(status().isCreated()).andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        SeedRunner seed = new SeedRunner(jdbc, true, environment);
        seed.run();
        int usersAfterFirstRun = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        int productsAfterFirstRun = jdbc.queryForObject("SELECT COUNT(*) FROM produtos", Integer.class);
        int unitsAfterFirstRun = jdbc.queryForObject("SELECT COUNT(*) FROM unidades", Integer.class);
        seed.run();

        assertThat(usersAfterFirstRun).isGreaterThan(0);
        assertThat(productsAfterFirstRun).isGreaterThan(0);
        assertThat(unitsAfterFirstRun).isGreaterThan(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(usersAfterFirstRun);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM produtos", Integer.class)).isEqualTo(productsAfterFirstRun);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM unidades", Integer.class)).isEqualTo(unitsAfterFirstRun);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos WHERE id=?", Integer.class, orderId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM estoques_unidades e JOIN produtos p ON p.id=e.produtoId " +
                "WHERE p.nome='Baião de dois'", Integer.class)).isEqualTo(2);
    }

    @Test
    void consentAuditFailureRollsBackTheConsentEvent() throws Exception {
        jdbc.execute("CREATE TRIGGER reject_consent_audit BEFORE INSERT ON audit_logs " +
                "WHEN NEW.acao='CONSENTIMENTO_REGISTRADO' BEGIN SELECT RAISE(ABORT,'blocked'); END");
        mvc.perform(put("/api/privacidade/consentimentos/fidelidade").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"aceito\":true,\"versaoAviso\":\"1.0\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("ERRO_INTERNO_SERVIDOR"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM consentimentos", Integer.class)).isZero();
    }

    @Test
    void concurrentPaymentOnlyTransitionsTheOrderOnce() throws Exception {
        MvcResult created = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long orderId = mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> payAfter(start, orderId));
            Future<Integer> second = executor.submit(() -> payAfter(start, orderId));
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos WHERE pedidoId=?", Integer.class, orderId)).isEqualTo(1);
    }

    @Test
    void concurrentOrderRequestsCannotReserveTheLastUnitTwice() throws Exception {
        jdbc.update("UPDATE estoques_unidades SET quantidade=1 WHERE unidadeId=1 AND produtoId=1");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> createAfter(start));
            Future<Integer> second = executor.submit(() -> createAfter(start));
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("SELECT quantidade FROM estoques_unidades WHERE unidadeId=1 AND produtoId=1", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pedidos", Integer.class)).isEqualTo(1);
    }

    private int createAfter(CountDownLatch start) throws Exception {
        start.await();
        return mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void attendantCanCreateAndListWhileKitchenCannotCreate() throws Exception {
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(cozinha))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"BALCAO\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/pedidos").header("Authorization", bearer(atendente))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"BALCAO\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(atendente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").value(1));
    }

    @Test
    void operationalUsersAreRestrictedToTheirAssignedUnit() throws Exception {
        MvcResult recifeOrder = mvc.perform(post("/api/pedidos").header("Authorization", bearer(maria))
                        .contentType("application/json")
                        .content("{\"unidadeId\":1,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long recifeOrderId = mapper.readTree(recifeOrder.getResponse().getContentAsString()).get("id").asLong();
        MvcResult joaoPessoaOrder = mvc.perform(post("/api/pedidos").header("Authorization", bearer(jose))
                        .contentType("application/json")
                        .content("{\"unidadeId\":2,\"canalPedido\":\"APP\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isCreated()).andReturn();
        long joaoPessoaOrderId = mapper.readTree(joaoPessoaOrder.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/pedidos").header("Authorization", bearer(atendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].unidadeId").value(1));
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(cozinha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados.length()").value(1))
                .andExpect(jsonPath("$.dados[0].unidadeId").value(1));
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dados.length()").value(2));

        mvc.perform(post("/api/pedidos").header("Authorization", bearer(atendente))
                        .contentType("application/json")
                        .content("{\"unidadeId\":2,\"canalPedido\":\"BALCAO\",\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
        mvc.perform(post("/api/pedidos/" + joaoPessoaOrderId + "/pagamento")
                        .header("Authorization", bearer(atendente)).contentType("application/json")
                        .content("{\"statusSimulado\":\"APROVADO\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
        mvc.perform(patch("/api/pedidos/" + joaoPessoaOrderId + "/status")
                        .header("Authorization", bearer(cozinha)).contentType("application/json")
                        .content("{\"status\":\"CANCELADO\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
        mvc.perform(post("/api/pedidos/" + recifeOrderId + "/pagamento")
                        .header("Authorization", bearer(atendente)).contentType("application/json")
                        .content("{\"statusSimulado\":\"APROVADO\"}"))
                .andExpect(status().isOk());

        jdbc.update("UPDATE usuarios SET unidadeId=NULL WHERE id=3");
        mvc.perform(get("/api/pedidos").header("Authorization", bearer(atendente)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
        assertThat(jdbc.queryForObject("SELECT status FROM pedidos WHERE id=?", String.class, joaoPessoaOrderId))
                .isEqualTo("AGUARDANDO_PAGAMENTO");
    }

    @Test
    void auditProjectionHidesUnapprovedFieldsAndMalformedPayloads() throws Exception {
        jdbc.update("INSERT INTO audit_logs(acao,detalhes,usuarioId) VALUES(?,?,?)",
                "TESTE_SEGURO", "{\"pagina\":1,\"senha\":\"nao-expor\",\"email\":\"privado@test.local\"}", 1);
        jdbc.update("INSERT INTO audit_logs(acao,detalhes,usuarioId) VALUES(?,?,?)",
                "TESTE_INVALIDO", "{not-json", 1);
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(admin))
                        .param("acao", "TESTE_SEGURO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].detalhes.pagina").value(1))
                .andExpect(jsonPath("$.dados[0].detalhes.senha").doesNotExist())
                .andExpect(jsonPath("$.dados[0].detalhes.email").doesNotExist());
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(admin))
                        .param("acao", "TESTE_INVALIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados[0].detalhes").isEmpty());
        mvc.perform(get("/api/auditoria").header("Authorization", bearer(admin))
                        .param("acao", "invalid"))
                .andExpect(status().isUnprocessableEntity());
    }

    private int payAfter(CountDownLatch start, long orderId) throws Exception {
        start.await();
        return mvc.perform(post("/api/pedidos/" + orderId + "/pagamento").header("Authorization", bearer(maria))
                        .contentType("application/json").content("{\"statusSimulado\":\"APROVADO\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void twentyConcurrentCatalogRequestsSucceed() throws Exception {
        int clients = 20;
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(clients)) {
            Future<Integer>[] responses = new Future[clients];
            for (int i = 0; i < clients; i++) {
                responses[i] = executor.submit(() -> {
                    start.await();
                    return mvc.perform(get("/api/produtos").param("page", "1").param("limit", "10"))
                            .andReturn().getResponse().getStatus();
                });
            }
            start.countDown();
            for (Future<Integer> response : responses) assertThat(response.get()).isEqualTo(200);
        }
    }

    private String token(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(mapper.writeValueAsString(java.util.Map.of("email", email, "senha", "123456"))))
                .andExpect(status().isOk()).andReturn();
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private static String bearer(String token) { return "Bearer " + token; }
}
