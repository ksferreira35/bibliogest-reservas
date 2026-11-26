package br.com.bibliogest.reservas_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.bibliogest.reservas_api.client.AcervoClient;
import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import br.com.bibliogest.reservas_api.exception.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ExemplarNaoEncontradoException;
import br.com.bibliogest.reservas_api.exception.ObraNaoEncontradaException;
import br.com.bibliogest.reservas_api.exception.ReservaNaoEncontradaException;
import br.com.bibliogest.reservas_api.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {
    @Mock ReservaRepository repository;
    @Mock AcervoClient acervo;
    ReservaService service;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
        service = new ReservaService(repository, acervo, clock);
    }

    @Test
    void criaReservaQuandoExemplarExiste() {
        when(acervo.exemplarPertenceAObra("o1", "e1", "tok")).thenReturn(true);
        when(repository.saveAndFlush(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        var r = service.criar("leitor", null, "o1", "e1", "tok");

        assertThat(r.getEstado()).isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
        assertThat(r.getLeitorId()).isEqualTo("leitor");
    }

    @Test
    void recusaSemGravarQuandoExemplarNaoExiste() {
        when(acervo.exemplarPertenceAObra("o1", "e1", "tok")).thenReturn(false);

        assertThatThrownBy(() -> service.criar("leitor", null, "o1", "e1", "tok"))
            .isInstanceOf(ExemplarNaoEncontradoException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void recusaSemGravarQuandoAcervoFalha() {
        when(acervo.exemplarPertenceAObra("o1", "e1", "tok"))
            .thenThrow(new AcervoIndisponivelException("fora", new RuntimeException()));

        assertThatThrownBy(() -> service.criar("leitor", null, "o1", "e1", "tok"))
            .isInstanceOf(AcervoIndisponivelException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void indiceUnicoViraExemplarIndisponivel() {
        when(acervo.exemplarPertenceAObra("o1", "e1", "tok")).thenReturn(true);
        when(repository.saveAndFlush(any(Reserva.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> service.criar("leitor", null, "o1", "e1", "tok"))
            .isInstanceOf(ExemplarIndisponivelException.class);
    }

    @Test
    void leitorNaoVeReservaDeOutroLeitor() {
        var r = new Reserva("dono", null, "o1", "e1", Instant.now());
        when(repository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.buscar(r.getId(), "intruso", false))
            .isInstanceOf(ReservaNaoEncontradaException.class);
        assertThat(service.buscar(r.getId(), "dono", false)).isSameAs(r);
        assertThat(service.buscar(r.getId(), "qualquer", true)).isSameAs(r);
    }

    @Test
    void leitorNaoCancelaReservaDeOutro() {
        var r = new Reserva("dono", null, "o1", "e1", Instant.now());
        when(repository.findById(r.getId())).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.cancelar(r.getId(), "intruso", false))
            .isInstanceOf(ReservaNaoEncontradaException.class);
        assertThat(r.getEstado()).isEqualTo(EstadoReserva.AGUARDANDO_RETIRADA);
    }

    @Test
    void donoCancelaEBibliotecarioTambem() {
        var r1 = new Reserva("dono", null, "o1", "e1", Instant.now());
        var r2 = new Reserva("dono", null, "o1", "e2", Instant.now());
        when(repository.findById(r1.getId())).thenReturn(Optional.of(r1));
        when(repository.findById(r2.getId())).thenReturn(Optional.of(r2));
        when(repository.saveAndFlush(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.cancelar(r1.getId(), "dono", false).getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(service.cancelar(r2.getId(), "bib", true).getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void retiradaDeReservaInexistente() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarRetirada(id)).isInstanceOf(ReservaNaoEncontradaException.class);
    }

    @Test
    void disponibilidadeMarcaBloqueados() {
        when(repository.findExemplaresBloqueados(List.of("e1", "e2"), EstadoReserva.ATIVOS)).thenReturn(List.of("e2"));

        assertThat(service.disponibilidade(List.of("e1", "e2")))
            .containsEntry("e1", true).containsEntry("e2", false);
    }

    @Test
    void disponibilidadeDaObraCruzaAcervoComReservasAtivas() {
        when(acervo.exemplaresDaObra("o1", "tok")).thenReturn(List.of("e1", "e2"));
        when(repository.findExemplaresBloqueados(List.of("e1", "e2"), EstadoReserva.ATIVOS)).thenReturn(List.of("e2"));

        assertThat(service.disponibilidadeDaObra("o1", "tok"))
            .containsEntry("e1", true).containsEntry("e2", false);
    }

    @Test
    void obraSemExemplaresNaoConsultaOBanco() {
        when(acervo.exemplaresDaObra("o1", "tok")).thenReturn(List.of());

        assertThat(service.disponibilidadeDaObra("o1", "tok")).isEmpty();
        verify(repository, never()).findExemplaresBloqueados(any(), any());
    }

    @Test
    void obraInexistenteNoAcervoPropagaNaoEncontrada() {
        when(acervo.exemplaresDaObra("o9", "tok")).thenThrow(new ObraNaoEncontradaException());

        assertThatThrownBy(() -> service.disponibilidadeDaObra("o9", "tok"))
            .isInstanceOf(ObraNaoEncontradaException.class);
    }
}
