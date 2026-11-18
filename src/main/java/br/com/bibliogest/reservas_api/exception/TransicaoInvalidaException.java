package br.com.bibliogest.reservas_api.exception;

import br.com.bibliogest.reservas_api.entities.EstadoReserva;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(EstadoReserva atual, EstadoReserva destino) {
        super("Não é possível mudar a reserva de " + atual + " para " + destino + ".");
    }
}
