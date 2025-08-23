package org.caixa.dto;

import jakarta.persistence.criteria.CriteriaBuilder;

import java.math.BigDecimal;
import java.util.List;

public record EnvelopeSimulacaoDTO(Integer idSimulacao,
                                   Integer codigoProduto,
                                   String descricaoProduto,
                                   BigDecimal taxaJuros,
                                   List<ResultadoTipoDTO> resultadoSimulacao) {
}
