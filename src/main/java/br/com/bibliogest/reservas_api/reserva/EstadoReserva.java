package br.com.bibliogest.reservas_api.reserva;

import java.util.List;

public enum EstadoReserva {
    AGUARDANDO_RETIRADA, RETIRADA, FINALIZADA, CANCELADA;

    /** Estados que bloqueiam o exemplar. */
    public static final List<EstadoReserva> ATIVOS = List.of(AGUARDANDO_RETIRADA, RETIRADA);
}
