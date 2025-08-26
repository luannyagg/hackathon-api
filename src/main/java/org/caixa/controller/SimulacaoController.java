
package org.caixa.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.caixa.dto.AgregacaoDiariaDTO;
import org.caixa.dto.ListaSimulacaoDTO;
import org.caixa.dto.SimulacaoRequest;
import org.caixa.dto.SimulacaoResponse;
import jakarta.ws.rs.core.Response;
import org.caixa.service.SimulacaoService;

import java.time.LocalDate;
import java.util.Map;

@Path("/simulacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulacaoController {

    @Inject
    SimulacaoService service;

    @POST
    public Response simular(SimulacaoRequest req) {

        try {
            SimulacaoResponse res = service.simular(req);
            return Response.ok(res).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("erro", ex.getMessage()))
                    .build();
        }
    }



    @GET()
    @Path("/all")
    public ListaSimulacaoDTO getAll(
            @QueryParam("pagina") Integer pagina,
            @QueryParam("qtdRegistrosPagina") Integer qtdRegistrosPagina) {

        if (pagina == null || pagina < 1) {
            pagina = 1;
        }
        if (qtdRegistrosPagina == null || qtdRegistrosPagina < 1) {
            qtdRegistrosPagina = 10;
        }

        ListaSimulacaoDTO response = service.getAllSimulacoes(pagina, qtdRegistrosPagina);
        return response;
    }



    @GET
    public AgregacaoDiariaDTO porProdutoDia(@QueryParam("dia") String diaReq) {

        LocalDate dia = (diaReq == null || diaReq.isBlank()) ? LocalDate.now() : LocalDate.parse(diaReq);
        AgregacaoDiariaDTO response = service.getSimulacoesPorProduto(dia);
        return response;
    }
}