package org.caixa.service;

import io.quarkus.hibernate.orm.panache.Panache;
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
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class SimulacaoService {

    @Inject
    ProdutoRepository produtoRepo;
    @Inject
    SimulacaoRepository simulacaoRepo;

    @Transactional
    public EnvelopeSimulacaoDTO simularESalvar(SolicitarSimulacaoDTO req){
        BigDecimal valor = req.valorDesejado();
        int prazo = req.prazo();

        if(valor == null || prazo <= 0) throw new IllegalArgumentException("valor/prazo inválidos");

        // Seleciona produto compatível com MENOR taxa
        List<Produto> candidatos = produtoRepo.listAll().stream()
                .filter(p -> valor.compareTo(p.valorMinimo) >= 0 &&
                        (p.valorMaximo == null || valor.compareTo(p.valorMaximo) <= 0) &&
                        prazo >= p.minimoMeses && prazo <= p.maximoMeses)
                .sorted(Comparator.comparing(p -> p.taxaJuros))
                .collect(Collectors.toList());

        if(candidatos.isEmpty()) throw new NoSuchElementException("Nenhum produto compatível para valor/prazo informados.");

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

        for(var p : sac){
            Parcela par = new Parcela();
            par.simulacao = s;
            par.tipo = TipoSIstema.SAC;
            par.numero = p.numero();
            par.amortizacao = p.valorAmortizacao();
            par.juros = p.valorJuros();
            par.prestacao = p.valorPrestacao();
            s.parcelas.add(par);
        }
        for(var p : price){
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

        return new EnvelopeSimulacaoDTO(
                s.id,
                produto.id,
                produto.nome,
                taxa,
                List.of(
                        new ResultadoTipoDTO("SAC", sac),
                        new ResultadoTipoDTO("PRICE", price)
                )
        );
    }


//// Publica no Event Hub (best effort)
//        publisher.publish(envelope);
//
//        return envelope;
//    }
}