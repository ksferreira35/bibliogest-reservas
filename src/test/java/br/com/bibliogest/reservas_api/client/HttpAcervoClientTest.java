package br.com.bibliogest.reservas_api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.bibliogest.reservas_api.exception.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ObraNaoEncontradaException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpAcervoClientTest {
    private HttpServer server;
    private HttpAcervoClient client;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void iniciarServidor() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new HttpAcervoClient("http://127.0.0.1:" + server.getAddress().getPort() + "/acervo");
        server.start();
    }

    @AfterEach
    void pararServidor() {
        server.stop(0);
    }

    @Test
    void consultaRotaDoAcervoERepassaToken() {
        responder(200, """
                [{"id":"e1","obraId":"o1","codigoTombo":"MNG-001"}]
                """);

        assertThat(client.exemplaresDaObra("o1", "token-de-teste")).containsExactly("e1");
        assertThat(authorization.get()).isEqualTo("Bearer token-de-teste");
    }

    @Test
    void aceitaListaVaziaDeUmaObraExistente() {
        responder(200, "[]");

        assertThat(client.exemplaresDaObra("o1", "token-de-teste")).isEmpty();
    }

    @Test
    void obraInexistenteRetornaNaoEncontrada() {
        responder(404, "{}");

        assertThatThrownBy(() -> client.exemplaresDaObra("o1", "token-de-teste"))
                .isInstanceOf(ObraNaoEncontradaException.class);
    }

    @Test
    void falhaDoAcervoImpedeConfirmacaoDaReserva() {
        responder(503, "{}");

        assertThatThrownBy(() -> client.exemplarPertenceAObra("o1", "e1", "token-de-teste"))
                .isInstanceOf(AcervoIndisponivelException.class);
    }

    @Test
    void respostaComFormatoErradoNaoViraListaVazia() {
        responder(200, "{\"items\":[]}");

        assertThatThrownBy(() -> client.exemplaresDaObra("o1", "token-de-teste"))
                .isInstanceOf(AcervoIndisponivelException.class);
    }

    @Test
    void exemplarSemIdERecusado() {
        responder(200, "[{\"obraId\":\"o1\"}]");

        assertThatThrownBy(() -> client.exemplaresDaObra("o1", "token-de-teste"))
                .isInstanceOf(AcervoIndisponivelException.class);
    }

    @Test
    void exemplarDeOutraObraERecusado() {
        responder(200, "[{\"id\":\"e1\",\"obraId\":\"o2\"}]");

        assertThatThrownBy(() -> client.exemplaresDaObra("o1", "token-de-teste"))
                .isInstanceOf(AcervoIndisponivelException.class);
    }

    private void responder(int status, String body) {
        server.createContext("/acervo/api/v1/obras/o1/exemplares", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
    }
}
