package org.caixa.dto;

import java.util.List;

public record ListaSimulacaoDTO(
        Integer pagina,
        Integer qtdRegistros,
        Long qtdTotalPagina,
        List<RegistroDTO> registro
) {
}
