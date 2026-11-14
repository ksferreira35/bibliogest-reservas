package br.com.bibliogest.reservas_api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import br.com.bibliogest.reservas_api.config.SecurityConfiguration;
import br.com.bibliogest.reservas_api.web.AccessController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AccessController.class, properties = {
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.example.test/realms/bibliogest",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.example.test/certs"
})
@Import(SecurityConfiguration.class)
class ReservasApiApplicationTests {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;

    @BeforeEach
    void tokens() {
        when(decoder.decode("reader")).thenReturn(token("LEITOR"));
        when(decoder.decode("librarian")).thenReturn(token("BIBLIOTECARIO"));
        when(decoder.decode("other")).thenReturn(token("UNRELATED"));
    }

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/status")).andExpect(status().isUnauthorized());
    }

    @Test
    void readerCanReadButCannotAdminister() throws Exception {
        mvc.perform(get("/api/status").header("Authorization", "Bearer reader"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/status").header("Authorization", "Bearer reader"))
            .andExpect(status().isForbidden());
    }

    @Test
    void librarianCanReadAndAdminister() throws Exception {
        mvc.perform(get("/api/status").header("Authorization", "Bearer librarian"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/status").header("Authorization", "Bearer librarian"))
            .andExpect(status().isOk());
    }

    @Test
    void unrelatedRolesAreRejected() throws Exception {
        mvc.perform(get("/api/status").header("Authorization", "Bearer other"))
            .andExpect(status().isForbidden());
    }

    private Jwt token(String role) {
        return Jwt.withTokenValue(role).header("alg", "RS256").subject("user-123")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
            .claim("realm_access", Map.of("roles", List.of(role))).build();
    }
}
