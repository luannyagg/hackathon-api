package org.caixa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.caixa.dto.EnvelopeSimulacaoDTO;
import org.caixa.dto.ResultadoTipoDTO;
import org.caixa.dto.SolicitarSimulacaoDTO;
import org.caixa.entity.Parcela;
import org.caixa.entity.Produto;
import org.caixa.entity.Simulacao;
import org.caixa.entity.enums.TipoSIstema;
import org.caixa.repository.ProdutoRepository;
import org.caixa.repository.SimulacaoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {

    @Inject
    ProdutoRepository produtoRepo;
    @Inject
    SimulacaoRepository simulacaoRepo;

    @Inject
    EventHubPublisher publisher;


    @Transactional
    public EnvelopeSimulacaoDTO simularESalvar(SolicitarSimulacaoDTO req) throws JsonProcessingException {
        BigDecimal valor = req.valorDesejado();
        int prazo = req.prazo();

        if (valor == null || prazo <= 0) throw new IllegalArgumentException("valor/prazo inválidos");

        // Seleciona produto compatível com MENOR taxa
        List<Produto> candidatos = produtoRepo.listAll().stream()
                .filter(p -> valor.compareTo(p.valorMinimo) >= 0 &&
                        (p.valorMaximo == null || valor.compareTo(p.valorMaximo) <= 0) &&
                        prazo >= p.minimoMeses && prazo <= p.maximoMeses)
                .sorted(Comparator.comparing(p -> p.taxaJuros))
                .collect(Collectors.toList());

        if (candidatos.isEmpty())
            throw new NoSuchElementException("Nenhum produto compatível para valor/prazo informados.");

        Produto produto = candidatos.get(0);
        BigDecimal taxa = produto.taxaJuros;

        var sac = CalculoFinanceiro.calcularSAC(valor, taxa, prazo);
        var price = CalculoFinanceiro.calcularPRICE(valor, taxa, prazo);

        Simulacao s = new Simulacao();
        s.produto = produto;
        s.valorSolicitado = valor;
        s.prazoMeses = prazo;
        s.taxaJurosAplicada = taxa;
        s.criadoEm = OffsetDateTime.now();

        for (var p : sac) {
            Parcela par = new Parcela();
            par.simulacao = s;
            par.tipo = TipoSIstema.SAC;
            par.numero = p.numero();
            par.amortizacao = p.valorAmortizacao();
            par.juros = p.valorJuros();
            par.prestacao = p.valorPrestacao();
            s.parcelas.add(par);
        }
        for (var p : price) {
            Parcela par = new Parcela();
            par.simulacao = s;
            par.tipo = TipoSIstema.PRICE;
            par.numero = p.numero();
            par.amortizacao = p.valorAmortizacao();
            par.juros = p.valorJuros();
            par.prestacao = p.valorPrestacao();
            s.parcelas.add(par);
        }

        simulacaoRepo.persist(s);

        EnvelopeSimulacaoDTO envelope = new EnvelopeSimulacaoDTO(
                s.id,
                produto.id,
                produto.nome,
                taxa,
                List.of(
                        new ResultadoTipoDTO("SAC", sac),
                        new ResultadoTipoDTO("PRICE", price)
                )
        );

        String payload = new ObjectMapper().writeValueAsString(envelope);
        publisher.enviarEvento(payload);


        return envelope;

    }

    public List<Map<String,Object>> listarSimulacoes(int pagina, int qtd) {
        PanacheQuery<Simulacao> query = simulacaoRepo.findAll().page(Page.of(pagina-1, qtd));

        List<Simulacao> simulacoes = query.list();
        List<Map<String,Object>> resultado = new ArrayList<>();

        for (Simulacao s : simulacoes) {

            // Agrupa parcelas por tipo
            Map<String, List<Parcela>> parcelasPorTipo = s.parcelas.stream()
                    .collect(Collectors.groupingBy(p -> p.tipo.name()));

            for (var entry : parcelasPorTipo.entrySet()) {
                String tipo = entry.getKey();
                List<Parcela> parcelas = entry.getValue();

                Map<String,Object> map = new LinkedHashMap<>();
                map.put("tipoSistema", tipo);
                map.put("idSimulacao", s.id);
                map.put("codigoProduto", s.produto.id);
                map.put("descricaoProduto", s.produto.nome);
                map.put("taxaJuros", s.taxaJurosAplicada);
                map.put("valor", s.valorSolicitado);
                map.put("prazo", s.prazoMeses);
                map.put("criadoEm", s.criadoEm);
                map.put("valorTotalParcelas", parcelas.stream()
                        .map(p -> p.prestacao)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .doubleValue());

                resultado.add(map);
            }
        }

        return resultado;
    }




    public List<Map<String, Object>> agregacaoDiaria(LocalDate data) {
        List<Map<String,Object>> resultado = new ArrayList<>();

        for (String tipoStr : List.of("SAC", "PRICE")) {
            // converte String para enum
            TipoSIstema tipoEnum = TipoSIstema.valueOf(tipoStr);

            String jpql =
                    "select s.produto.id, s.produto.nome, cast(s.criadoEm as date), " +
                            "count(s), avg(s.valorSolicitado), sum(s.valorSolicitado), " +
                            "avg(s.taxaJurosAplicada), sum(p.prestacao) " +
                            "from Simulacao s " +
                            "join s.parcelas p " +
                            "where p.tipo = :tipo " +
                            (data != null ? "and cast(s.criadoEm as date) = :data " : "") +
                            "group by s.produto.id, s.produto.nome, cast(s.criadoEm as date) " +
                            "order by 3 desc";

            var query = simulacaoRepo.getEntityManager().createQuery(jpql, Object[].class)
                    .setParameter("tipo", tipoEnum); // <- agora passa enum

            if (data != null) query.setParameter("data", data);

            List<Object[]> rows = query.getResultList();

            for (Object[] r : rows) {
                Map<String,Object> map = new LinkedHashMap<>();
                map.put("tipoSistema", tipoStr); //
                map.put("codigoProduto", r[0]);
                map.put("descricaoProduto", r[1]);
                map.put("dia", r[2].toString());
                map.put("quantidade", r[3]);
                map.put("valorMedio", r[4]);
                map.put("valorTotal", r[5]);
                map.put("taxaMedia", r[6]);
                map.put("valorTotalCredito", r[7]);
                resultado.add(map);
            }
        }

        return resultado;
    }



}

