package br.com.bibliogest.reservas_api.dto;

import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import java.time.Instant;
import java.util.UUID;

public record ReservaResponse(
    UUID id, String leitorId, String obraId, String exemplarId, EstadoReserva estado,
    Instant criadaEm, Instant retiradaEm, Instant finalizadaEm, Instant canceladaEm
) {
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getLeitorId(), r.getObraId(), r.getExemplarId(), r.getEstado(),
            r.getCriadaEm(), r.getRetiradaEm(), r.getFinalizadaEm(), r.getCanceladaEm());
    }
}
