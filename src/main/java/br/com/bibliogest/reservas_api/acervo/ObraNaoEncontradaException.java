package br.com.bibliogest.reservas_api.acervo;

public class ObraNaoEncontradaException extends RuntimeException {
    public ObraNaoEncontradaException() {
        super("Obra não encontrada.");
    }
}
