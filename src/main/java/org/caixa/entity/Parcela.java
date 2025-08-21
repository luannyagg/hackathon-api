package org.caixa.entity;

import org.caixa.entity.enums.TipoSIstema;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "PARCELA")
public class Parcela extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PARCELA")
    public Integer id;

    @ManyToOne(optional = false) @JoinColumn(name = "ID_SIMULACAO")
    public Simulacao simulacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TP_SISTEMA", length = 10, nullable = false)
    public TipoSIstema tipo;

    @Column(name = "NU_PARCELA", nullable = false)
    public Integer numero;

    @Column(name = "VR_AMORTIZACAO", nullable = false, precision = 18, scale = 2)
    public BigDecimal amortizacao;

    @Column(name = "VR_JUROS", nullable = false, precision = 18, scale = 2)
    public BigDecimal juros;

    @Column(name = "VR_PRESTACAO", nullable = false, precision = 18, scale = 2)
    public BigDecimal prestacao;
}
