package br.com.bibliogest.reservas_api.reserva;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(EstadoReserva atual, EstadoReserva destino) {
        super("Não é possível mudar a reserva de " + atual + " para " + destino + ".");
    }
}
