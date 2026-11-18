package br.com.bibliogest.reservas_api.exception;

public class ExemplarIndisponivelException extends RuntimeException {
    public ExemplarIndisponivelException() {
        super("O exemplar já está reservado ou retirado.");
    }
}
