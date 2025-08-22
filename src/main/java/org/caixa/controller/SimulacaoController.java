package org.caixa.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.caixa.dto.AgregacaoDiariaDTO;
import org.caixa.dto.EnvelopeSimulacaoDTO;
import org.caixa.dto.ListaSimulacaoDTO;
import org.caixa.dto.SolicitarSimulacaoDTO;
import org.caixa.entity.Simulacao;
import org.caixa.repository.SimulacaoRepository;
import org.caixa.service.SimulacaoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/simulacoes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulacaoController {

    @Inject
    SimulacaoService service;

    @Inject
    SimulacaoRepository repository;

    @POST
    public Response criar(SolicitarSimulacaoDTO req) throws JsonProcessingException {
        EnvelopeSimulacaoDTO out = service.simularESalvar(req);
        return Response.status(Response.Status.CREATED).entity(out).build();
    }



    @GET
    public Response listar(@QueryParam("pagina") @DefaultValue("1") int pagina,
                           @QueryParam("qtd") @DefaultValue("200") int qtd) {

        if (pagina < 1) pagina = 1;
        if (qtd < 1) qtd = 1;
        if (qtd > 200) qtd = 200;

        List<Map<String,Object>> itens = service.listarSimulacoes(pagina, qtd);
        long total = repository.count();

        return Response.ok(new ListaSimulacaoDTO(pagina, qtd, total, itens)).build();
    }


    @GET
    @Path("/diario")
    public Response agregacaoDiaria(@QueryParam("data") String dataIso) {

        LocalDate data = dataIso != null ? LocalDate.parse(dataIso) : null;

        List<Map<String,Object>> simulacoes = service.agregacaoDiaria(data);

        String dataReferencia = dataIso != null ? dataIso : "todas";

        return Response.ok(new AgregacaoDiariaDTO(dataReferencia, simulacoes)).build();
    }



}
