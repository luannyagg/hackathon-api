package org.caixa.dto;

import java.util.List;

public record ResultadoTipoDTO(String tipo,
                               List<ParcelaDTO> parcelas) {
}
