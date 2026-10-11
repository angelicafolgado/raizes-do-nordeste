package br.com.raizesdonordeste.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Component
public class SeedRunner implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final boolean enabled;
    private final Environment environment;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SeedRunner(JdbcTemplate jdbc, @Value("${app.seed.enabled:true}") boolean enabled, Environment environment) {
        this.jdbc = jdbc;
        this.enabled = enabled;
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        if (!enabled || environment.acceptsProfiles(Profiles.of("production", "prod"))) return;
        List<String[]> users = List.of(
                new String[]{"Maria Cliente", "maria@cliente.com", "CLIENTE"},
                new String[]{"José Cliente", "jose@cliente.com", "CLIENTE"},
                new String[]{"Atendente", "atendente@raizes.com", "ATENDENTE"},
                new String[]{"Cozinha", "cozinha@raizes.com", "COZINHA"},
                new String[]{"Administrador", "admin@raizes.com", "ADMIN"});
        for (String[] user : users)
            jdbc.update("INSERT INTO usuarios(nome,email,senha,role) VALUES(?,?,?,?) ON CONFLICT(email) DO NOTHING",
                    user[0], user[1], encoder.encode("123456"), user[2]);

        long recifeId = addUnit("Raízes Recife", "Recife");
        long joaoPessoaId = addUnit("Raízes João Pessoa", "João Pessoa");
        long baiaoId = addProduct("Baião de dois", "Arroz, feijão-verde e temperos nordestinos", 38.50);
        long tapiocaId = addProduct("Tapioca", "Tapioca recheada preparada na hora", 24.00);
        long sucoId = addProduct("Suco de cajá", "Suco natural de cajá", 14.50);
        int[][] stock = {{24, 18, 30}, {10, 22, 25}};
        long[] units = {recifeId, joaoPessoaId};
        long[] products = {baiaoId, tapiocaId, sucoId};
        for (int unit = 0; unit < units.length; unit++) for (int product = 0; product < products.length; product++)
            jdbc.update("INSERT INTO estoques_unidades(unidadeId,produtoId,quantidade) VALUES(?,?,?) " +
                    "ON CONFLICT(unidadeId,produtoId) DO NOTHING", units[unit], products[product], stock[unit][product]);
    }

    private long addUnit(String name, String city) {
        jdbc.update("INSERT INTO unidades(nome,cidade) SELECT ?,? WHERE NOT EXISTS (SELECT 1 FROM unidades WHERE nome=?)",
                name, city, name);
        return jdbc.queryForObject("SELECT id FROM unidades WHERE nome=?", Long.class, name);
    }

    private long addProduct(String name, String description, double price) {
        jdbc.update("INSERT INTO produtos(nome,descricao,preco) SELECT ?,?,? WHERE NOT EXISTS (SELECT 1 FROM produtos WHERE nome=?)",
                name, description, price, name);
        return jdbc.queryForObject("SELECT id FROM produtos WHERE nome=?", Long.class, name);
    }
}
