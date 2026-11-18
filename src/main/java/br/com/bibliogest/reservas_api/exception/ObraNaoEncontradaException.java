package br.com.bibliogest.reservas_api.exception;

public class ObraNaoEncontradaException extends RuntimeException {
    public ObraNaoEncontradaException() {
        super("Obra não encontrada.");
    }
}
