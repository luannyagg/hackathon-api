package org.caixa.entity.simulacao;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "simulacao", schema = "dbo")
public class Simulacao extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SIMULACAO", nullable = false, unique = true)
    public Long id;

    @Column(name = "CO_PRODUTO", nullable = false)
    public Integer codigoProduto;

    @Column(name = "NO_PRODUTO", nullable = false, length = 200)
    public String nomeProduto;

    @Column(name = "PC_TAXA_JUROS", nullable = false, precision = 10, scale = 9)
    public BigDecimal taxaJuros;

    @Column(name = "VALOR_DESEJADO", nullable = false, precision = 18, scale = 2)
    public BigDecimal valorDesejado;

    @Column(name = "PRAZO", nullable = false)
    public Integer prazo;

    @Column(name = "DATA_REFERENCIA", nullable = false)
    public LocalDate dataReferencia;

    @Column(name = "VALOR_TOTAL_SAC", nullable = false, precision = 18, scale = 2)
    public BigDecimal valorTotalParcelasSAC;

    @Column(name = "VALOR_TOTAL_PRICE", nullable = false, precision = 18, scale = 2)
    public BigDecimal valorTotalParcelasPRICE;


    public Long getId() {
        return id;
    }

    public Integer getCodigoProduto() {
        return codigoProduto;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public BigDecimal getTaxaJuros() {
        return taxaJuros;
    }

    public BigDecimal getValorDesejado() {
        return valorDesejado;
    }

    public Integer getPrazo() {
        return prazo;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public BigDecimal getValorTotalParcelasSAC() {
        return valorTotalParcelasSAC;
    }

    public BigDecimal getValorTotalParcelasPRICE() {
        return valorTotalParcelasPRICE;
    }
}
