package br.com.bibliogest.reservas_api.entities;

import java.util.List;

public enum EstadoReserva {
    AGUARDANDO_RETIRADA, RETIRADA, FINALIZADA, CANCELADA;

    public static final List<EstadoReserva> ATIVOS = List.of(AGUARDANDO_RETIRADA, RETIRADA);
}
