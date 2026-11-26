package br.com.bibliogest.reservas_api.entities;

import br.com.bibliogest.reservas_api.exception.TransicaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    private UUID id;

    @Column(name = "leitor_id", nullable = false, updatable = false)
    private String leitorId;

    @Column(name = "leitor_nome", length = 255, updatable = false)
    private String leitorNome;

    @Column(name = "obra_id", nullable = false, updatable = false)
    private String obraId;

    @Column(name = "exemplar_id", nullable = false, updatable = false)
    private String exemplarId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Column(name = "retirada_em")
    private Instant retiradaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @Column(name = "cancelada_em")
    private Instant canceladaEm;

    @Version
    @Column(name = "versao")
    private Long versao;

    protected Reserva() {
    }

    public Reserva(String leitorId, String leitorNome, String obraId, String exemplarId, Instant agora) {
        this.id = UUID.randomUUID();
        this.leitorId = leitorId;
        this.leitorNome = leitorNome;
        this.obraId = obraId;
        this.exemplarId = exemplarId;
        this.estado = EstadoReserva.AGUARDANDO_RETIRADA;
        this.criadaEm = agora;
    }

    public void cancelar(Instant agora) {
        exigir(EstadoReserva.AGUARDANDO_RETIRADA, EstadoReserva.CANCELADA);
        estado = EstadoReserva.CANCELADA;
        canceladaEm = agora;
    }

    public void registrarRetirada(Instant agora) {
        exigir(EstadoReserva.AGUARDANDO_RETIRADA, EstadoReserva.RETIRADA);
        estado = EstadoReserva.RETIRADA;
        retiradaEm = agora;
    }

    public void finalizar(Instant agora) {
        exigir(EstadoReserva.RETIRADA, EstadoReserva.FINALIZADA);
        estado = EstadoReserva.FINALIZADA;
        finalizadaEm = agora;
    }

    private void exigir(EstadoReserva esperado, EstadoReserva destino) {
        if (estado != esperado) {
            throw new TransicaoInvalidaException(estado, destino);
        }
    }

    public UUID getId() { return id; }
    public String getLeitorNome() { return leitorNome; }
    public String getLeitorId() { return leitorId; }
    public String getObraId() { return obraId; }
    public String getExemplarId() { return exemplarId; }
    public EstadoReserva getEstado() { return estado; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getRetiradaEm() { return retiradaEm; }
    public Instant getFinalizadaEm() { return finalizadaEm; }
    public Instant getCanceladaEm() { return canceladaEm; }
}
