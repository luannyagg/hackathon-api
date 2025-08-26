package org.caixa.dto;

import java.math.BigDecimal;

public record ParcelaDTO(
        Integer numero,
        BigDecimal valorAmortizacao,
        BigDecimal valorJuros,
        BigDecimal valorPrestacao
) {
    @Override
    public Integer numero() {
        return numero;
    }

    @Override
    public BigDecimal valorAmortizacao() {
        return valorAmortizacao;
    }

    @Override
    public BigDecimal valorJuros() {
        return valorJuros;
    }

    @Override
    public BigDecimal valorPrestacao() {
        return valorPrestacao;
    }
}
