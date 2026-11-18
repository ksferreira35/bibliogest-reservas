package br.com.bibliogest.reservas_api.web;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessController {
    @GetMapping("/api/status")
    Map<String, String> status(Authentication authentication) {
        return Map.of("application", "reservas-api", "subject", authentication.getName());
    }

    @GetMapping("/api/admin/status")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public Map<String, String> adminStatus() {
        return Map.of("application", "reservas-api", "access", "BIBLIOTECARIO");
    }
}
