package org.caixa.dto;

import java.util.List;
import java.util.Map;

public record AgregacaoDiariaDTO(
        String dataReferencia,
        List<Map<String, Object>> simulacoes
) {}