package br.com.bibliogest.reservas_api.reserva;

public class ExemplarNaoEncontradoException extends RuntimeException {
    public ExemplarNaoEncontradoException() {
        super("Exemplar não encontrado na obra informada.");
    }
}
