package br.com.bibliogest.reservas_api.controller;

import br.com.bibliogest.reservas_api.dto.CriarReservaRequest;
import br.com.bibliogest.reservas_api.dto.ReservaResponse;
import br.com.bibliogest.reservas_api.entities.EstadoReserva;
import br.com.bibliogest.reservas_api.service.ReservaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {
    private static final int MAX_TAMANHO = 100;

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@Valid @RequestBody CriarReservaRequest req, Authentication auth) {
        var jwt = (Jwt) auth.getPrincipal();
        var reserva = service.criar(jwt.getSubject(), nomeDoLeitor(jwt), req.obraId(), req.exemplarId(), jwt.getTokenValue());
        return ResponseEntity.created(URI.create("/reservas/api/v1/reservas/" + reserva.getId()))
            .body(ReservaResponse.de(reserva));
    }

    @GetMapping("/minhas")
    public List<ReservaResponse> minhas(Authentication auth,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 @RequestParam(defaultValue = "20") int tamanho) {
        return service.minhas(auth.getName(), pagina(pagina), tamanho(tamanho))
            .stream().map(ReservaResponse::de).toList();
    }

    @GetMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public List<ReservaResponse> todas(@RequestParam(required = false) EstadoReserva estado,
                                @RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "20") int tamanho) {
        return service.todas(estado, pagina(pagina), tamanho(tamanho)).stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse buscar(@PathVariable UUID id, Authentication auth) {
        return ReservaResponse.de(service.buscar(id, auth.getName(), bibliotecario(auth)));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable UUID id, Authentication auth) {
        return ReservaResponse.de(service.cancelar(id, auth.getName(), bibliotecario(auth)));
    }

    @PostMapping("/{id}/retirada")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ReservaResponse retirada(@PathVariable UUID id) {
        return ReservaResponse.de(service.registrarRetirada(id));
    }

    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ReservaResponse devolucao(@PathVariable UUID id) {
        return ReservaResponse.de(service.finalizar(id));
    }

    private static String nomeDoLeitor(Jwt jwt) {
        String nome = texto(jwt, "name");
        if (nome.isEmpty()) {
            nome = (texto(jwt, "given_name") + " " + texto(jwt, "family_name")).strip();
        }
        if (nome.isEmpty()) {
            return null;
        }
        int fim = nome.offsetByCodePoints(0, Math.min(255, nome.codePointCount(0, nome.length())));
        return nome.substring(0, fim);
    }

    private static String texto(Jwt jwt, String claim) {
        Object valor = jwt.getClaims().get(claim);
        return valor instanceof String texto ? texto.strip().replaceAll("\\s+", " ") : "";
    }

    private static boolean bibliotecario(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BIBLIOTECARIO"));
    }

    private static int pagina(int p) {
        return Math.max(p, 0);
    }

    private static int tamanho(int t) {
        return Math.min(Math.max(t, 1), MAX_TAMANHO);
    }
}
