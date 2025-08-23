package org.caixa.controller;


import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.caixa.dto.EnvelopeSimulacaoDTO;
import org.caixa.dto.SolicitarSimulacaoDTO;
import org.caixa.entity.Simulacao;
import org.caixa.repository.SimulacaoRepository;
import org.caixa.service.SimulacaoService;
import org.locationtech.jts.geom.Envelope;

import java.time.LocalDate;
import java.time.ZoneOffset;
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
    public Response criar(SolicitarSimulacaoDTO req){
        EnvelopeSimulacaoDTO out = service.simularESalvar(req);
        return Response.status(Response.Status.CREATED).entity(out).build();
    }

    @GET
    public Response listar(@QueryParam("pagina") @DefaultValue("1") int pagina,
                           @QueryParam("qtd") @DefaultValue("200") int qtd){
        if(pagina < 1 ) pagina = 1;
        if(qtd < 1) qtd= 1;
        if(qtd > 200) qtd = 200;

        PanacheQuery<Simulacao> q = repository.findAll().page(Page.of(pagina-1, qtd));
        var itens = q.list().stream().map(s ->
                Map.of(
                        "idSimulacao", s.id,
                        "codigoProduto", s.produto.id,
                        "descricaoProduto", s.produto.nome,
                        "taxaJuros", s.taxaJurosAplicada,
                        "valor", s.valorSolicitado,
                        "prazo", s.prazoMeses,
                        "criadoEm", s.criadoEm
                )
        ).toList();

        return Response.ok(Map.of(
                "pagina", pagina,
                "qtd", qtd,
                "total", q.count(),
                "itens", itens

        )).build();
    }

    @GET

    @Path("/diario")

    public Response agregacaoDiaria(@QueryParam("data") String dataIso){

        // filtro opcional por data YYYY-MM-DD (utc)

        String jpql =
                "select s.produto.id, s.produto.nome, cast(s.criadoEm as date), count(s), avg(s.valorSolicitado), sum(s.valorSolicitado) " +
                        "from Simulacao s " +
                        (dataIso != null ? "where cast(s.criadoEm as date) = ?1 " : "") +
                        "group by s.produto.id, s.produto.nome, cast(s.criadoEm as date) " +
                        "order by 3 desc";



        List<Object[]> rows;

        if(dataIso != null){

            LocalDate d = LocalDate.parse(dataIso);
            rows = repository.getEntityManager().createQuery(jpql, Object[].class)
                    .setParameter(1, d)
                    .getResultList();

        } else {

            rows = repository.getEntityManager().createQuery(jpql, Object[].class).getResultList();

        }



        var itens = rows.stream().map(r -> Map.of(

                "codigoProduto", r[0],

                "descricaoProduto", r[1],

                "dia", r[2].toString(),

                "quantidade", r[3],

                "valorMedio", r[4],

                "valorTotal", r[5]

        )).toList();



        return Response.ok(itens).build();

    }

}

