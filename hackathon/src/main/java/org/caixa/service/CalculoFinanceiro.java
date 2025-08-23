package org.caixa.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.quarkus.core.CamelConfig;
import org.caixa.dto.ParcelaDTO;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class CalculoFinanceiro {
    private static final MathContext MC = new MathContext(20, RoundingMode.UP);
    private static final int SCALE = 2;

    public static List<ParcelaDTO> calcularSAC(BigDecimal principal, BigDecimal taxaMensal, int n){
        List<ParcelaDTO> out = new ArrayList<>();
        BigDecimal amortConst = principal.divide(BigDecimal.valueOf(n), MC);
        amortConst = amortConst.setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal saldo = principal;
        for(int k = 1; k <= n; k++){
            BigDecimal juros = saldo.multiply(taxaMensal, MC).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal prest = amortConst.add(juros).setScale(SCALE, RoundingMode.HALF_UP);
            out.add(new ParcelaDTO(k, amortConst, juros, prest));
            saldo = saldo.subtract(amortConst, MC);

        }

        return out;
    }

    public static List<ParcelaDTO> calcularPRICE(BigDecimal principal, BigDecimal taxaMensal, int n){
        List<ParcelaDTO> out = new ArrayList<>();

        BigDecimal i = taxaMensal;
        BigDecimal umMaisI = BigDecimal.ONE.add(i, MC);
        BigDecimal fator = BigDecimal.ONE.divide(umMaisI.pow(n, MC), MC);
        BigDecimal denominador = BigDecimal.ONE.subtract(fator, MC);
        BigDecimal pmt = principal.multiply(i, MC).divide(denominador, MC).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal saldo = principal;
        for(int k = 1; k <= n; k++){
            BigDecimal juros = saldo.multiply(i, MC).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal amort = pmt.subtract(juros, MC).setScale(SCALE, RoundingMode.HALF_UP);
            out.add(new ParcelaDTO(k, amort, juros, pmt));
            saldo = saldo.subtract(amort, MC);
        }

        return out;

    }
}
