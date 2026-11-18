package br.com.bibliogest.reservas_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarReservaRequest(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String obraId,
    @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+") String exemplarId
) {
}
