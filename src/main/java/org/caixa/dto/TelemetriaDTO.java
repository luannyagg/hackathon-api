package org.caixa.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TelemetriaDTO(String dataReferencia,
        List<EndpointDTO> listaEndpoints) {

}
