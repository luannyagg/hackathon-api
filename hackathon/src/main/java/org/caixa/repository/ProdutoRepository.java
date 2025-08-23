package org.caixa.repository;



import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.caixa.entity.Produto;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class ProdutoRepository implements PanacheRepository<Produto> {

    public List<Produto> produtosCompativeis(BigDecimal valor, int prazo){
        return find("(?1 between valorMinimo and coalesce(valorMaximo, 9999999999999)) " + "and (?2 between minimoMeses and maximoMeses)",
                valor, prazo).list();
    }


}
