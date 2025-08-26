package org.caixa.repository;

import java.math.BigDecimal;
import java.util.List;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.caixa.entity.produto.Produto;


@ApplicationScoped
@PersistenceUnit("sqlserver")
public class ProdutoRepository implements PanacheRepository<Produto> {

    public List<Produto> filterProducts(BigDecimal valor, int prazo) {
        return list(
                "valorMinimo <= ?1 and (valorMaximo is null or valorMaximo >= ?1) and minimoMeses <= ?2 and (maximoMeses is null or maximoMeses >= ?2)",
                valor, prazo);
    }
}
