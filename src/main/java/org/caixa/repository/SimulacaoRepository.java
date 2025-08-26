package org.caixa.repository;


import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.caixa.entity.simulacao.Simulacao;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
@PersistenceUnit("postgres")
public class SimulacaoRepository implements PanacheRepository<Simulacao> {
    public List<Simulacao> findByDataReferencia(LocalDate dia) {
        return list("dataReferencia", dia);
    }
}
