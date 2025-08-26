package org.caixa.dto;

import java.math.BigDecimal;

public record SimulacaoAgregadaDTO(
        Integer codigoProduto,
        String descricaoProduto,
        BigDecimal taxaMediaJuro,
        BigDecimal valorMedioPrestacaoSAC,
        BigDecimal valorMedioPrestacaoPRICE,
        BigDecimal valorTotalDesejado,
        BigDecimal valorTotalCreditoSAC,
        BigDecimal valorTotalCreditoPRICE

) {
}
