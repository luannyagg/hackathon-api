package org.caixa.dto;

import java.math.BigDecimal;

public record RegistroDTO(Long idSimulacao,
                         BigDecimal valorDesejado,
                         Integer prazo,
                         BigDecimal valorTotalParcelasSAC,
                          BigDecimal valorTotalParcelasPRICE
                    ) {
}
