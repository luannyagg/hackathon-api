package org.caixa.dto;

import java.util.List;
import java.util.Map;

public record ListaSimulacaoDTO(
        int pagina,
        int qtd,
        long total,
        List<Map<String,Object>> itens
) {}
