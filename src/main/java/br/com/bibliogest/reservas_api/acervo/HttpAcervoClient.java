package br.com.bibliogest.reservas_api.acervo;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Usa GET {base}/api/v1/obras/{obraId}/exemplares e procura o exemplar na lista.
 * Aceita lista simples ou objeto com a lista em "content", "exemplares" ou "items".
 */
@Component
public class HttpAcervoClient implements AcervoClient {
    private final RestClient client;

    public HttpAcervoClient(@Value("${bibliogest.acervo.base-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public boolean exemplarPertenceAObra(String obraId, String exemplarId, String bearerToken) {
        try {
            return exemplaresDaObra(obraId, bearerToken).contains(exemplarId);
        } catch (ObraNaoEncontradaException e) {
            return false;
        }
    }

    @Override
    public List<String> exemplaresDaObra(String obraId, String bearerToken) {
        Object body;
        try {
            body = client.get()
                .uri("/api/v1/obras/{obraId}/exemplares", obraId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .retrieve()
                .body(Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ObraNaoEncontradaException();
        } catch (RestClientException e) {
            throw new AcervoIndisponivelException("Não foi possível consultar o Acervo.", e);
        }
        return extrairItens(body).stream()
            .filter(item -> item instanceof Map<?, ?> m && m.get("id") != null)
            .map(item -> String.valueOf(((Map<?, ?>) item).get("id")))
            .toList();
    }

    private static List<?> extrairItens(Object body) {
        if (body instanceof List<?> lista) {
            return lista;
        }
        if (body instanceof Map<?, ?> mapa) {
            for (String chave : List.of("content", "exemplares", "items")) {
                if (mapa.get(chave) instanceof List<?> lista) {
                    return lista;
                }
            }
        }
        return List.of();
    }
}
