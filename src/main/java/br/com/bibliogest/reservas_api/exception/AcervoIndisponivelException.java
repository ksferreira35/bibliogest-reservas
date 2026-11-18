package br.com.bibliogest.reservas_api.exception;

public class AcervoIndisponivelException extends RuntimeException {
    public AcervoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
