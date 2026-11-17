package br.com.bibliogest.reservas_api.acervo;

import java.util.List;

/** Integração interna com a API de Acervo. */
public interface AcervoClient {
    /**
     * Confirma que o exemplar existe e pertence à obra.
     *
     * @param bearerToken token do leitor, repassado ao Acervo
     * @return false se a obra ou o exemplar não existem
     * @throws AcervoIndisponivelException se o Acervo não puder confirmar
     */
    boolean exemplarPertenceAObra(String obraId, String exemplarId, String bearerToken);

    /**
     * Ids dos exemplares (cópias físicas) da obra.
     *
     * @param bearerToken token do leitor, repassado ao Acervo
     * @throws ObraNaoEncontradaException se o Acervo não conhece a obra
     * @throws AcervoIndisponivelException se o Acervo não puder responder
     */
    List<String> exemplaresDaObra(String obraId, String bearerToken);
}
