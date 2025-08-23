package org.caixa.dto;

import java.util.Map;

public record TelemetriaDTO(Map<String, Long> volumePorEndpoint,
                            Map<String, Double> tempoMedioMsPorEndpoint,
                            Map<String, Double> sucessoPctPorEndpoint
) {
}
