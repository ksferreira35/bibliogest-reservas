package br.com.bibliogest.reservas_api.client;

import br.com.bibliogest.reservas_api.dto.ExemplarAcervoResponse;
import br.com.bibliogest.reservas_api.exception.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ObraNaoEncontradaException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        } catch (ObraNaoEncontradaException exception) {
            return false;
        }
    }

    @Override
    public List<String> exemplaresDaObra(String obraId, String bearerToken) {
        List<ExemplarAcervoResponse> exemplares;
        try {
            exemplares = client.get()
                    .uri("/api/v1/obras/{obraId}/exemplares", obraId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ExemplarAcervoResponse>>() {});
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ObraNaoEncontradaException();
        } catch (RestClientException exception) {
            throw new AcervoIndisponivelException("Não foi possível consultar o Acervo.", exception);
        }

        if (exemplares == null || exemplares.stream().anyMatch(exemplar ->
                exemplar == null || exemplar.id() == null || exemplar.id().isBlank()
                        || !obraId.equals(exemplar.obraId()))) {
            throw new AcervoIndisponivelException(
                    "O Acervo retornou uma resposta inválida.", null);
        }

        return exemplares.stream().map(ExemplarAcervoResponse::id).toList();
    }
}
