package br.com.bibliogest.reservas_api.dto;

import java.util.List;
import java.util.Map;

public record DisponibilidadeResponse(String obraId, List<Exemplar> exemplares) {
    public record Exemplar(String exemplarId, boolean disponivel) {
    }

    public static DisponibilidadeResponse de(String obraId, Map<String, Boolean> disponibilidade) {
        return new DisponibilidadeResponse(obraId, disponibilidade.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(e -> new Exemplar(e.getKey(), true)).toList());
    }
}
