package org.caixa.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import io.quarkus.hibernate.orm.PersistenceUnit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.caixa.dto.*;
import org.caixa.entity.produto.Produto;
import org.caixa.entity.simulacao.Simulacao;
import org.caixa.repository.ProdutoRepository;
import org.caixa.repository.SimulacaoRepository;

@ApplicationScoped
public class SimulacaoService {

    @Inject
    @PersistenceUnit("sqlserver")
    private ProdutoRepository produtoRepository;

    @Inject
    private CacheService cacheService;

    @Inject
    @PersistenceUnit("postgres")
    private SimulacaoRepository simulacaoRepository;

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);

    // ----------------- Cálculo de parcelas -----------------
    private List<ParcelaDTO> calcularSAC(BigDecimal principal, BigDecimal taxaMensal, int meses) {
        if (meses <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Prazo e valor desejado devem ser maiores que zero");

        List<ParcelaDTO> parcelas = new ArrayList<>(meses);
        BigDecimal amortizacaoConst = principal.divide(BigDecimal.valueOf(meses), MC).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldo = principal;

        for (int n = 1; n <= meses; n++) {
            BigDecimal juros = saldo.multiply(taxaMensal, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal prestacao = amortizacaoConst.add(juros).setScale(2, RoundingMode.HALF_UP);
            parcelas.add(new ParcelaDTO(n, amortizacaoConst, juros, prestacao));
            saldo = saldo.subtract(amortizacaoConst, MC);
        }

        return parcelas;
    }

    private List<ParcelaDTO> calcularPRICE(BigDecimal principal, BigDecimal taxaMensal, int meses) {
        if (meses <= 0 || principal.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Prazo e valor desejado devem ser maiores que zero");

        List<ParcelaDTO> parcelas = new ArrayList<>(meses);
        BigDecimal i = taxaMensal;
        BigDecimal um = BigDecimal.ONE;
        BigDecimal fator = um.add(i, MC).pow(meses, MC);
        BigDecimal pmt = principal.multiply(i, MC).divide(um.subtract(um.divide(fator, MC), MC), MC).setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldo = principal;
        for (int n = 1; n <= meses; n++) {
            BigDecimal juros = saldo.multiply(i, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amortizacao = pmt.subtract(juros, MC).setScale(2, RoundingMode.HALF_UP);
            parcelas.add(new ParcelaDTO(n, amortizacao, juros, pmt));
            saldo = saldo.subtract(amortizacao, MC);
        }

        return parcelas;
    }

    // ----------------- Busca de produto -----------------
    private Produto getProduto(SimulacaoRequest req) {
        if (req.valorDesejado().compareTo(BigDecimal.ZERO) <= 0 || req.prazo() <= 0)
            throw new IllegalArgumentException("Valor desejado e prazo devem ser maiores que zero");

        String cacheKey = String.format("valorDesejado:%s-prazo:%d", req.valorDesejado(), req.prazo());
        var produtoCached = cacheService.get(cacheKey);
        if (produtoCached.isPresent()) return (Produto) produtoCached.get();

        List<Produto> produtos = produtoRepository.filterProducts(req.valorDesejado(), req.prazo());
        if (produtos.isEmpty()) throw new IllegalArgumentException("Não temos produtos disponíveis para sua solicitação 😓");

        // Escolha o produto com menor taxa de juros
        Produto escolhido = produtos.stream().min(Comparator.comparing(Produto::getTaxaJurosMensal)).get();
        cacheService.put(cacheKey, escolhido);
        return escolhido;
    }

    // ----------------- Simulação -----------------
    public SimulacaoResponse simular(SimulacaoRequest req) {
        Produto produto = getProduto(req);
        BigDecimal taxa = produto.taxaJurosMensal;

        List<ParcelaDTO> sac = calcularSAC(req.valorDesejado(), taxa, req.prazo());
        List<ParcelaDTO> price = calcularPRICE(req.valorDesejado(), taxa, req.prazo());

        List<ResultadoTipoDTO> resultados = List.of(
                new ResultadoTipoDTO("SAC", sac),
                new ResultadoTipoDTO("PRICE", price)
        );

        BigDecimal totalSac = sac.stream()
                .map(ParcelaDTO::valorPrestacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPrice = price.stream()
                .map(ParcelaDTO::valorPrestacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Simulacao simulacao = new Simulacao();
        simulacao.codigoProduto = produto.codigo;
        simulacao.nomeProduto = produto.nome;
        simulacao.taxaJuros = taxa;
        simulacao.valorDesejado = req.valorDesejado();
        simulacao.prazo = req.prazo();
        simulacao.dataReferencia = LocalDate.now();
        simulacao.valorTotalParcelasSAC = totalSac;
        simulacao.valorTotalParcelasPRICE = totalPrice;

        salvarSimulacaoPostgres(simulacao);

        return new SimulacaoResponse(
                simulacao.id,
                produto.codigo,
                produto.nome,
                taxa,
                resultados
        );
    }

    @Transactional
    protected Simulacao salvarSimulacaoPostgres(Simulacao simulacao) {
        simulacao.persist();
        return simulacao;
    }

    // ----------------- Agregação diária -----------------
    @Transactional
    public AgregacaoDiariaDTO getSimulacoesPorProduto(LocalDate dia) {
        List<Simulacao> sims = simulacaoRepository.findByDataReferencia(dia);
        Map<Integer, List<Simulacao>> porProduto = sims.stream()
                .collect(Collectors.groupingBy(Simulacao::getCodigoProduto));

        List<SimulacaoAgregadaDTO> simulacoes = new ArrayList<>();

        for (var entry : porProduto.entrySet()) {
            Integer codigoProduto = entry.getKey();
            List<Simulacao> lista = entry.getValue();

            BigDecimal taxaMediaJuro = lista.stream()
                    .map(Simulacao::getTaxaJuros)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(lista.size()), MC);

            BigDecimal valorMedioPrestacaoSAC = lista.stream()
                    .map(sim -> sim.getValorTotalParcelasSAC().divide(BigDecimal.valueOf(sim.getPrazo()), MC))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(lista.size()), MC);

            BigDecimal valorMedioPrestacaoPRICE = lista.stream()
                    .map(sim -> sim.getValorTotalParcelasPRICE().divide(BigDecimal.valueOf(sim.getPrazo()), MC))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(lista.size()), MC);

            BigDecimal valorTotalDesejado = lista.stream()
                    .map(Simulacao::getValorDesejado)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorTotalCreditoSAC = lista.stream()
                    .map(Simulacao::getValorTotalParcelasSAC)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal valorTotalCreditoPRICE = lista.stream()
                    .map(Simulacao::getValorTotalParcelasPRICE)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            simulacoes.add(new SimulacaoAgregadaDTO(
                    codigoProduto,
                    lista.get(0).nomeProduto,
                    taxaMediaJuro.stripTrailingZeros(),
                    valorMedioPrestacaoSAC.setScale(2, RoundingMode.HALF_UP),
                    valorMedioPrestacaoPRICE.setScale(2, RoundingMode.HALF_UP),
                    valorTotalDesejado.setScale(2, RoundingMode.HALF_UP),
                    valorTotalCreditoSAC.setScale(2, RoundingMode.HALF_UP),
                    valorTotalCreditoPRICE.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        return new AgregacaoDiariaDTO(dia, simulacoes);
    }

    // ----------------- Listagem paginada -----------------
    @Transactional
    public ListaSimulacaoDTO getAllSimulacoes(Integer pagina, Integer qtdRegistrosPagina) {
        if (pagina == null || pagina <= 0) pagina = 1;
        if (qtdRegistrosPagina == null || qtdRegistrosPagina <= 0) qtdRegistrosPagina = 10;

        List<Simulacao> simulacoes = simulacaoRepository.findAll()
                .page(pagina - 1, qtdRegistrosPagina)
                .list();

        List<RegistroDTO> registros = simulacoes.stream()
                .map(s -> new RegistroDTO(
                        s.id,
                        s.valorDesejado,
                        s.prazo,
                        s.valorTotalParcelasSAC,
                        s.valorTotalParcelasPRICE
                ))
                .collect(Collectors.toList());

        return new ListaSimulacaoDTO(pagina, (int) simulacaoRepository.count(), (long) registros.size(), registros);
    }
}
