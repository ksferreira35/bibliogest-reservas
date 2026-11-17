package br.com.bibliogest.reservas_api.reserva;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReservaTest {
    private final Instant agora = Instant.parse("2026-11-17T12:00:00Z");

    private Reserva nova() {
        return new Reserva("leitor", "obra", "ex1", agora);
    }

    @Test
    void fluxoCompleto() {
        var r = nova();
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
        r.registrarRetirada(agora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.RETIRADA);
        r.finalizar(agora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(r.getRetiradaEm()).isEqualTo(agora);
        assertThat(r.getFinalizadaEm()).isEqualTo(agora);
    }

    @Test
    void cancelarSoAntesDaRetirada() {
        var r = nova();
        r.cancelar(agora);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.CANCELADA);

        var retirada = nova();
        retirada.registrarRetirada(agora);
        assertThatThrownBy(() -> retirada.cancelar(agora)).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void transicoesInvalidasSaoRejeitadas() {
        assertThatThrownBy(() -> nova().finalizar(agora)).isInstanceOf(TransicaoInvalidaException.class);

        var cancelada = nova();
        cancelada.cancelar(agora);
        assertThatThrownBy(() -> cancelada.registrarRetirada(agora)).isInstanceOf(TransicaoInvalidaException.class);

        var finalizada = nova();
        finalizada.registrarRetirada(agora);
        finalizada.finalizar(agora);
        assertThatThrownBy(() -> finalizada.registrarRetirada(agora)).isInstanceOf(TransicaoInvalidaException.class);
    }
}
