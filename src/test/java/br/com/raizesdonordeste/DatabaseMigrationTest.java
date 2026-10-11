package br.com.raizesdonordeste;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest {
    @TempDir Path directory;

    private Flyway flyway(String url) {
        return Flyway.configure().dataSource(url, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("0").load();
    }

    @Test
    void newDatabaseMigratesOnceAndValidatesChecksumsOnRestart() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("new.db");
        Flyway migration = flyway(url);
        assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(migration.migrate().migrationsExecuted).isZero();
        migration.validate();
        try (var connection = DriverManager.getConnection(url);
             var result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN " +
                             "('usuarios','unidades','produtos','estoques_unidades','pedidos','itens_pedido','pagamentos','audit_logs','consentimentos')")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(9);
        }
    }

    @Test
    void existingDatabaseIsBaselinedWithoutDeletingUserData() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("existing.db");
        try (var connection = DriverManager.getConnection(url)) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V1__initial_schema.sql"));
            connection.createStatement().executeUpdate(
                    "INSERT INTO usuarios(nome,email,senha) VALUES('Existente','existing@test.local','hash-preservado')");
        }
        Flyway migration = flyway(url);
        assertThat(migration.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(migration.migrate().migrationsExecuted).isZero();
        migration.validate();
        try (var connection = DriverManager.getConnection(url);
             var result = connection.createStatement().executeQuery("SELECT nome,senha FROM usuarios WHERE email='existing@test.local'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("nome")).isEqualTo("Existente");
            assertThat(result.getString("senha")).isEqualTo("hash-preservado");
        }
    }
}
