package br.com.bibliogest.reservas_api.web;

import br.com.bibliogest.reservas_api.reserva.ReservaService;
import java.util.regex.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/obras")
public class DisponibilidadeController {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final ReservaService service;

    public DisponibilidadeController(ReservaService service) {
        this.service = service;
    }

    /** Somente as cópias da obra no Acervo que estão livres (sem reserva ativa). */
    @GetMapping("/{obraId}/disponibilidade")
    public DisponibilidadeResponse disponibilidade(@PathVariable String obraId, Authentication auth) {
        if (!ID.matcher(obraId).matches()) {
            throw new IllegalArgumentException("obraId inválido.");
        }
        var jwt = (Jwt) auth.getPrincipal();
        return DisponibilidadeResponse.de(obraId, service.disponibilidadeDaObra(obraId, jwt.getTokenValue()));
    }
}
