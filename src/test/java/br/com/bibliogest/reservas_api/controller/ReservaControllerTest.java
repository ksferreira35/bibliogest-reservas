package br.com.bibliogest.reservas_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.bibliogest.reservas_api.config.ClockConfiguration;
import br.com.bibliogest.reservas_api.config.SecurityConfiguration;
import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.entities.Reserva;
import br.com.bibliogest.reservas_api.exception.ExemplarIndisponivelException;
import br.com.bibliogest.reservas_api.exception.ReservaNaoEncontradaException;
import br.com.bibliogest.reservas_api.exception.TransicaoInvalidaException;
import br.com.bibliogest.reservas_api.service.ReservaService;
import br.com.bibliogest.reservas_api.web.ApiExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservaController.class, properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.test/realms/bibliogest",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.example.test/certs"
})
@Import({SecurityConfiguration.class, ApiExceptionHandler.class, ClockConfiguration.class})
class ReservaControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean ReservaService service;

    @BeforeEach
    void tokens() {
        when(decoder.decode("reader")).thenReturn(token("LEITOR"));
        when(decoder.decode("librarian")).thenReturn(token("BIBLIOTECARIO"));
    }

    @Test
    void anonimoNaoCria() throws Exception {
        mvc.perform(post("/api/v1/reservas").contentType(MediaType.APPLICATION_JSON)
            .content("{\"obraId\":\"1\",\"exemplarId\":\"2\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void leitorCriaUsandoSubEToken() throws Exception {
        var r = new Reserva("user-123", null, "1", "2", Instant.now());
        when(service.criar("user-123", null, "1", "2", "reader")).thenReturn(r);

        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer reader")
                .contentType(MediaType.APPLICATION_JSON).content("{\"obraId\":\"1\",\"exemplarId\":\"2\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("AGUARDANDO_RETIRADA"));
    }

    @Test
    void bibliotecarioNaoCria() throws Exception {
        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer librarian")
                .contentType(MediaType.APPLICATION_JSON).content("{\"obraId\":\"1\",\"exemplarId\":\"2\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void corpoInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer reader")
                .contentType(MediaType.APPLICATION_JSON).content("{\"obraId\":\"../x\",\"exemplarId\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors").isArray());
        verify(service, never()).criar(any(), any(), any(), any(), any());
    }

    @Test
    void exemplarJaReservadoRetorna409() throws Exception {
        when(service.criar(any(), any(), any(), any(), any())).thenThrow(new ExemplarIndisponivelException());

        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer reader")
                .contentType(MediaType.APPLICATION_JSON).content("{\"obraId\":\"1\",\"exemplarId\":\"2\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void leitorNaoRegistraRetiradaNemDevolucaoNemListaTudo() throws Exception {
        var id = UUID.randomUUID();
        mvc.perform(post("/api/v1/reservas/" + id + "/retirada").header("Authorization", "Bearer reader"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reservas/" + id + "/devolucao").header("Authorization", "Bearer reader"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reservas").header("Authorization", "Bearer reader"))
            .andExpect(status().isForbidden());
    }

    @Test
    void bibliotecarioRegistraRetiradaEDevolucao() throws Exception {
        var r = new Reserva("x", null, "1", "2", Instant.now());
        r.registrarRetirada(Instant.now());
        when(service.registrarRetirada(r.getId())).thenReturn(r);

        mvc.perform(post("/api/v1/reservas/" + r.getId() + "/retirada").header("Authorization", "Bearer librarian"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RETIRADA"));
    }

    @Test
    void transicaoInvalidaRetorna409() throws Exception {
        var id = UUID.randomUUID();
        when(service.finalizar(id)).thenThrow(
            new TransicaoInvalidaException(EstadoReserva.AGUARDANDO_RETIRADA, EstadoReserva.FINALIZADA));

        mvc.perform(post("/api/v1/reservas/" + id + "/devolucao").header("Authorization", "Bearer librarian"))
            .andExpect(status().isConflict());
    }

    @Test
    void leitorCancelaPassandoSuaIdentidade() throws Exception {
        var r = new Reserva("user-123", null, "1", "2", Instant.now());
        r.cancelar(Instant.now());
        when(service.cancelar(eq(r.getId()), eq("user-123"), eq(false))).thenReturn(r);

        mvc.perform(post("/api/v1/reservas/" + r.getId() + "/cancelar").header("Authorization", "Bearer reader"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void reservaDeOutroLeitorRetorna404() throws Exception {
        var id = UUID.randomUUID();
        when(service.buscar(eq(id), eq("user-123"), eq(false))).thenThrow(new ReservaNaoEncontradaException());

        mvc.perform(get("/api/v1/reservas/" + id).header("Authorization", "Bearer reader"))
            .andExpect(status().isNotFound());
    }

    @Test
    void minhasListaDoLeitorAutenticado() throws Exception {
        when(service.minhas("user-123", 0, 20)).thenReturn(List.of(new Reserva("user-123", null, "1", "2", Instant.now())));

        mvc.perform(get("/api/v1/reservas/minhas").header("Authorization", "Bearer reader"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].leitorId").value("user-123"));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer reader")
                .contentType(MediaType.APPLICATION_JSON).content("{\"obraId\":"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
        verify(service, never()).criar(any(), any(), any(), any(), any());
    }

    @Test
    void idDeReservaInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/v1/reservas/id-invalido").header("Authorization", "Bearer reader"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    static Stream<Arguments> nomesDoToken() {
        return Stream.of(
            Arguments.of(Map.of("name", "  Kaiky   Ferreira  "), "Kaiky Ferreira"),
            Arguments.of(Map.of("given_name", "Kaiky", "family_name", "Ferreira"), "Kaiky Ferreira"),
            Arguments.of(Map.of("given_name", "Kaiky"), "Kaiky"),
            Arguments.of(Map.of("preferred_username", "email@example.test"), null),
            Arguments.of(Map.of("name", List.of("invalido"), "given_name", "Kaiky"), "Kaiky"),
            Arguments.of(Map.of("name", " ", "family_name", "Ferreira"), "Ferreira"),
            Arguments.of(Map.of("name", "😀".repeat(256)), "😀".repeat(255))
        );
    }

    @ParameterizedTest
    @MethodSource("nomesDoToken")
    void nomeVemDoTokenValidadoENaoDoCorpo(Map<String, Object> claims, String esperado) throws Exception {
        var builder = Jwt.withTokenValue("reader").header("alg", "RS256").subject("user-123")
            .claim("realm_access", Map.of("roles", List.of("LEITOR")));
        claims.forEach(builder::claim);
        when(decoder.decode("reader")).thenReturn(builder.build());
        when(service.criar(any(), org.mockito.ArgumentMatchers.nullable(String.class), any(), any(), any()))
            .thenAnswer(call -> new Reserva(call.getArgument(0), call.getArgument(1),
                call.getArgument(2), call.getArgument(3), Instant.now()));

        mvc.perform(post("/api/v1/reservas").header("Authorization", "Bearer reader")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"obraId\":\"1\",\"exemplarId\":\"2\",\"leitorId\":\"outro\",\"leitorNome\":\"Nome forjado\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.leitorId").value("user-123"))
            .andExpect(jsonPath("$.leitorNome").value(esperado));
    }

    @Test
    void bibliotecarioConsultaONomeDoDonoDaReservaSemSubstituirPeloSeu() throws Exception {
        var reserva = new Reserva("outro-leitor", "Kaiky Ferreira", "1", "2", Instant.now());
        when(service.buscar(reserva.getId(), "user-123", true)).thenReturn(reserva);
        when(service.todas(null, 0, 20)).thenReturn(List.of(reserva));
        mvc.perform(get("/api/v1/reservas/" + reserva.getId()).header("Authorization", "Bearer librarian"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.leitorNome").value("Kaiky Ferreira"));
        mvc.perform(get("/api/v1/reservas").header("Authorization", "Bearer librarian"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].leitorNome").value("Kaiky Ferreira"));
    }

    private Jwt token(String role) {
        return Jwt.withTokenValue(role.equals("LEITOR") ? "reader" : "librarian").header("alg", "RS256")
            .subject("user-123").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
            .claim("realm_access", Map.of("roles", List.of(role))).build();
    }
}
