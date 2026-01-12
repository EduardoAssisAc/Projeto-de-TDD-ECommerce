package ecommerce.external;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;

@Service
public class EstoqueExternalStub implements IEstoqueExternal {

    @Override
    public EstoqueBaixaDTO darBaixa(List<Long> produtosIds, List<Long> produtosQuantidades) {
        return new EstoqueBaixaDTO(true);
    }

    @Override
    public DisponibilidadeDTO verificarDisponibilidade(
            List<Long> produtosIds,
            List<Long> produtosQuantidades) {

        return new DisponibilidadeDTO(true, Collections.emptyList());
    }
}
