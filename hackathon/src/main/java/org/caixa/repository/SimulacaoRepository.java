package org.caixa.repository;



import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.caixa.entity.Simulacao;


@ApplicationScoped
public class SimulacaoRepository implements PanacheRepository<Simulacao> {

}
