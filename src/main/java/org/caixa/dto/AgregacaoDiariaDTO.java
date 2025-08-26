package org.caixa.dto;

import java.time.LocalDate;
import java.util.List;

public record AgregacaoDiariaDTO(
        LocalDate dataReferencia,
        List<SimulacaoAgregadaDTO> simulacoes
) {
}
