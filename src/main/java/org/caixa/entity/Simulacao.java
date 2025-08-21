package org.caixa.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SIMULACAO")
public class Simulacao extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SIMULACAO")
    public Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CO_PRODUTO")
    public Produto produto;

    @Column(name = "VR_SOLICITADO", nullable = false, precision = 18, scale = 2)
    public BigDecimal valorSolicitado;

    @Column(name = "NUM_MESES", nullable = false)
    public Integer prazoMeses;

    @Column(name = "PC_TAXA_JU", nullable = false, precision = 10, scale = 9)
    public BigDecimal taxaJurosAplicada;

    @Column(name = "DATA_CRIACAO", nullable = false)
    public OffsetDateTime criadoEm;

    @OneToMany(mappedBy = "simulacao", cascade =CascadeType.ALL, orphanRemoval = true)
    public List<Parcela> parcelas = new ArrayList<>();

}
