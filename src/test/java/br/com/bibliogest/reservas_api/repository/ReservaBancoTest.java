package br.com.bibliogest.reservas_api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.bibliogest.reservas_api.client.AcervoClient;
import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import br.com.bibliogest.reservas_api.exception.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.service.ReservaService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:tc:postgresql:17.12-bookworm:///bibliogest_reservas",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
    "spring.datasource.username=test",
    "spring.datasource.password=test",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.test/realms/bibliogest",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.example.test/certs"
})
class ReservaBancoTest {
    private static final String OBRA = "o1";
    private static final String TOKEN = "tok";

    @Autowired ReservaService service;
    @Autowired ReservaRepository repository;
    @MockitoBean AcervoClient acervo;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void preparar() {
        repository.deleteAll();
        when(acervo.exemplarPertenceAObra(any(), any(), any())).thenReturn(true);
        when(acervo.exemplaresDaObra(OBRA, TOKEN)).thenReturn(List.of("e1", "e2"));
    }

    @Test
    void mesmaCopiaBloqueiaSegundaReservaEOutraCopiaContinuaLivre() {
        service.criar("leitor-a", OBRA, "e1", TOKEN);

        assertThatThrownBy(() -> service.criar("leitor-b", OBRA, "e1", TOKEN))
            .isInstanceOf(ExemplarIndisponivelException.class);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.disponibilidadeDaObra(OBRA, TOKEN))
            .containsEntry("e1", false).containsEntry("e2", true);
        assertThat(service.criar("leitor-b", OBRA, "e2", TOKEN).getEstado())
            .isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
    }

    @Test
    void copiaVoltaAFicarDisponivelDepoisDoCancelamento() {
        var reserva = service.criar("leitor-a", OBRA, "e1", TOKEN);
        assertThat(service.disponibilidadeDaObra(OBRA, TOKEN)).containsEntry("e1", false);

        service.cancelar(reserva.getId(), "leitor-a", false);

        assertThat(service.disponibilidadeDaObra(OBRA, TOKEN)).containsEntry("e1", true);
        assertThat(service.criar("leitor-b", OBRA, "e1", TOKEN).getEstado())
            .isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
    }

    @Test
    void copiaContinuaBloqueadaNaRetiradaEVoltaAFicarDisponivelDepoisDaDevolucao() {
        var reserva = service.criar("leitor-a", OBRA, "e1", TOKEN);

        service.registrarRetirada(reserva.getId());
        assertThat(service.disponibilidadeDaObra(OBRA, TOKEN)).containsEntry("e1", false);
        assertThatThrownBy(() -> service.criar("leitor-b", OBRA, "e1", TOKEN))
            .isInstanceOf(ExemplarIndisponivelException.class);

        service.finalizar(reserva.getId());

        assertThat(service.disponibilidadeDaObra(OBRA, TOKEN)).containsEntry("e1", true);
        assertThat(service.criar("leitor-b", OBRA, "e1", TOKEN).getEstado())
            .isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
    }

    @Test
    void duasPessoasAoMesmoTempoUmaConsegueEAOutraRecebeIndisponivel() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var largada = new CountDownLatch(1);
        try {
            List<Future<Object>> tentativas = new ArrayList<>();
            for (var leitor : List.of("leitor-a", "leitor-b")) {
                tentativas.add(pool.submit(() -> {
                    largada.await();
                    try {
                        return service.criar(leitor, OBRA, "e1", TOKEN);
                    } catch (ExemplarIndisponivelException e) {
                        return e;
                    }
                }));
            }
            largada.countDown();

            int criadas = 0;
            int recusadas = 0;
            for (var tentativa : tentativas) {
                var resultado = tentativa.get(30, TimeUnit.SECONDS);
                if (resultado instanceof Reserva) {
                    criadas++;
                } else if (resultado instanceof ExemplarIndisponivelException) {
                    recusadas++;
                }
            }
            assertThat(criadas).isEqualTo(1);
            assertThat(recusadas).isEqualTo(1);
            assertThat(repository.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
