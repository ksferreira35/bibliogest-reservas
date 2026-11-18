package br.com.bibliogest.reservas_api.client;

import java.util.List;

public interface AcervoClient {
    boolean exemplarPertenceAObra(String obraId, String exemplarId, String bearerToken);

    List<String> exemplaresDaObra(String obraId, String bearerToken);
}
