package org.caixa.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public record SimulacaoRequest(
        @NotNull(message = "Informe o valor desejado.") @Min(value = 1, message = "O valor desejado deve ser maior ou igual a 1.") BigDecimal valorDesejado,
        @Min(value = 1, message = "O prazo informado deve ser maior ou igual a 1.") int prazo) {
}