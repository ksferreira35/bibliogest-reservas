package br.com.bibliogest.reservas_api.service;

import br.com.bibliogest.reservas_api.client.AcervoClient;
import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import br.com.bibliogest.reservas_api.exception.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ExemplarNaoEncontradoException;
import br.com.bibliogest.reservas_api.exception.ReservaNaoEncontradaException;
import br.com.bibliogest.reservas_api.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {
    private final ReservaRepository repository;
    private final AcervoClient acervo;
    private final Clock clock;

    public ReservaService(ReservaRepository repository, AcervoClient acervo, Clock clock) {
        this.repository = repository;
        this.acervo = acervo;
        this.clock = clock;
    }

    public Reserva criar(String leitorId, String leitorNome, String obraId, String exemplarId, String bearerToken) {
        if (!acervo.exemplarPertenceAObra(obraId, exemplarId, bearerToken)) {
            throw new ExemplarNaoEncontradoException();
        }
        try {
            return repository.saveAndFlush(new Reserva(leitorId, leitorNome, obraId, exemplarId, agora()));
        } catch (DataIntegrityViolationException e) {
            throw new ExemplarIndisponivelException();
        }
    }

    @Transactional(readOnly = true)
    public Reserva buscar(UUID id, String leitorId, boolean bibliotecario) {
        var reserva = repository.findById(id).orElseThrow(ReservaNaoEncontradaException::new);
        if (!bibliotecario && !reserva.getLeitorId().equals(leitorId)) {
            throw new ReservaNaoEncontradaException();
        }
        return reserva;
    }

    @Transactional
    public Reserva cancelar(UUID id, String leitorId, boolean bibliotecario) {
        var reserva = buscar(id, leitorId, bibliotecario);
        reserva.cancelar(agora());
        return repository.saveAndFlush(reserva);
    }

    @Transactional
    public Reserva registrarRetirada(UUID id) {
        var reserva = repository.findById(id).orElseThrow(ReservaNaoEncontradaException::new);
        reserva.registrarRetirada(agora());
        return repository.saveAndFlush(reserva);
    }

    @Transactional
    public Reserva finalizar(UUID id) {
        var reserva = repository.findById(id).orElseThrow(ReservaNaoEncontradaException::new);
        reserva.finalizar(agora());
        return repository.saveAndFlush(reserva);
    }

    @Transactional(readOnly = true)
    public List<Reserva> minhas(String leitorId, int pagina, int tamanho) {
        return repository.findByLeitorIdOrderByCriadaEmDesc(leitorId, PageRequest.of(pagina, tamanho));
    }

    @Transactional(readOnly = true)
    public List<Reserva> todas(EstadoReserva estado, int pagina, int tamanho) {
        var page = PageRequest.of(pagina, tamanho);
        return estado == null
            ? repository.findAllByOrderByCriadaEmDesc(page)
            : repository.findByEstadoOrderByCriadaEmDesc(estado, page);
    }

    public Map<String, Boolean> disponibilidadeDaObra(String obraId, String bearerToken) {
        var exemplarIds = acervo.exemplaresDaObra(obraId, bearerToken);
        return exemplarIds.isEmpty() ? Map.of() : disponibilidade(exemplarIds);
    }

    @Transactional(readOnly = true)
    public Map<String, Boolean> disponibilidade(Collection<String> exemplarIds) {
        var bloqueados = new HashSet<>(repository.findExemplaresBloqueados(exemplarIds, EstadoReserva.ATIVOS));
        var resultado = new LinkedHashMap<String, Boolean>();
        exemplarIds.forEach(id -> resultado.put(id, !bloqueados.contains(id)));
        return resultado;
    }

    private Instant agora() {
        return Instant.now(clock);
    }
}
