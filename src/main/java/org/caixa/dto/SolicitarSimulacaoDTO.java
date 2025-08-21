package org.caixa.dto;


import java.math.BigDecimal;

public record SolicitarSimulacaoDTO(BigDecimal valorDesejado,
                                    Integer prazo) {
}
