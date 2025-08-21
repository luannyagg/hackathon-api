package org.caixa.dto;

import java.math.BigDecimal;

public record ParcelaDTO(Integer numero,
                         BigDecimal valorAmortizacao,
                         BigDecimal valorJuros,
                         BigDecimal valorPrestacao) {
}
