package br.com.bibliogest.reservas_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class NomeLeitorMigrationTest {
    @Test
    void adicionaNomeSemPerderReservasExistentes() throws Exception {
        String url = "jdbc:tc:postgresql:17.12-bookworm:///migration-nome-leitor";
        UUID id = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "test", "test")) {
            Flyway.configure().dataSource(url, "test", "test").target("1").load().migrate();
            try (var insert = connection.prepareStatement(
                    "INSERT INTO reserva (id, leitor_id, obra_id, exemplar_id, estado, criada_em) "
                    + "VALUES (?, 'reader', 'obra', 'exemplar', 'AGUARDANDO_RETIRADA', CURRENT_TIMESTAMP)")) {
                insert.setObject(1, id);
                insert.executeUpdate();
            }
            Flyway.configure().dataSource(url, "test", "test").load().migrate();
            try (var query = connection.prepareStatement("SELECT leitor_id, leitor_nome, estado FROM reserva WHERE id = ?")) {
                query.setObject(1, id);
                try (var row = query.executeQuery()) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getString("leitor_id")).isEqualTo("reader");
                    assertThat(row.getString("leitor_nome")).isNull();
                    assertThat(row.getString("estado")).isEqualTo("AGUARDANDO_RETIRADA");
                    assertThat(row.next()).isFalse();
                }
            }
        }
    }
}
