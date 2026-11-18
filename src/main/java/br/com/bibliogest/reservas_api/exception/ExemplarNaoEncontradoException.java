package br.com.bibliogest.reservas_api.exception;

public class ExemplarNaoEncontradoException extends RuntimeException {
    public ExemplarNaoEncontradoException() {
        super("Exemplar não encontrado na obra informada.");
    }
}
