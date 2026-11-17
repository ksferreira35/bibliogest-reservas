package br.com.bibliogest.reservas_api.reserva;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bibliogest.reservas_api.acervo.AcervoIndisponivelException;
import br.com.bibliogest.reservas_api.acervo.ObraNaoEncontradaException;
import br.com.bibliogest.reservas_api.config.ClockConfiguration;
import br.com.bibliogest.reservas_api.config.SecurityConfiguration;
import br.com.bibliogest.reservas_api.web.ApiExceptionHandler;
import br.com.bibliogest.reservas_api.web.DisponibilidadeController;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DisponibilidadeController.class, properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.test/realms/bibliogest",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.example.test/certs"
})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class, ClockConfiguration.class})
class DisponibilidadeControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean ReservaService service;

    @BeforeEach
    void tokens() {
        when(decoder.decode("reader")).thenReturn(token("LEITOR"));
    }

    @Test
    void anonimoNaoConsulta() throws Exception {
        mvc.perform(get("/api/v1/obras/o1/disponibilidade")).andExpect(status().isUnauthorized());
    }

    @Test
    void leitorVeQuaisCopiasDaObraEstaoLivres() throws Exception {
        var mapa = new LinkedHashMap<String, Boolean>();
        mapa.put("e1", true);
        mapa.put("e2", false);
        when(service.disponibilidadeDaObra("o1", "reader")).thenReturn(mapa);

        mvc.perform(get("/api/v1/obras/o1/disponibilidade").header("Authorization", "Bearer reader"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.obraId").value("o1"))
            .andExpect(jsonPath("$.exemplares[0].exemplarId").value("e1"))
            .andExpect(jsonPath("$.exemplares.length()").value(1));
    }

    @Test
    void obraInexistenteRetorna404() throws Exception {
        when(service.disponibilidadeDaObra("o9", "reader")).thenThrow(new ObraNaoEncontradaException());

        mvc.perform(get("/api/v1/obras/o9/disponibilidade").header("Authorization", "Bearer reader"))
            .andExpect(status().isNotFound());
    }

    @Test
    void acervoForaDoArRetorna503() throws Exception {
        when(service.disponibilidadeDaObra("o1", "reader"))
            .thenThrow(new AcervoIndisponivelException("fora", new RuntimeException()));

        mvc.perform(get("/api/v1/obras/o1/disponibilidade").header("Authorization", "Bearer reader"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void obraIdInvalidoRetorna400SemConsultarOAcervo() throws Exception {
        mvc.perform(get("/api/v1/obras/a.b/disponibilidade").header("Authorization", "Bearer reader"))
            .andExpect(status().isBadRequest());
        verify(service, never()).disponibilidadeDaObra(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    }

    private Jwt token(String role) {
        return Jwt.withTokenValue("reader").header("alg", "RS256")
            .subject("user-123").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
            .claim("realm_access", Map.of("roles", List.of(role))).build();
    }
}
