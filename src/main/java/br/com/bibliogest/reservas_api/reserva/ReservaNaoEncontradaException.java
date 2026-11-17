package br.com.bibliogest.reservas_api.reserva;

public class ReservaNaoEncontradaException extends RuntimeException {
    public ReservaNaoEncontradaException() {
        super("Reserva não encontrada.");
    }
}
