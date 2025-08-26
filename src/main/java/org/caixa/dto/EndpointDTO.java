package org.caixa.dto;

public record EndpointDTO(
        String nomeApi,
        long qtdRequisicoes,
        double tempoMedio,
        double tempoMinimo,
        double tempoMaximo,
        double percentualSucesso
) {}
