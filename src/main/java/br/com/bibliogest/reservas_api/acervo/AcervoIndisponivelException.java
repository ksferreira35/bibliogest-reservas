package br.com.bibliogest.reservas_api.acervo;

public class AcervoIndisponivelException extends RuntimeException {
    public AcervoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
