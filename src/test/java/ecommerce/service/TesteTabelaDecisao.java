package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

public class TesteTabelaDecisao {

    private final CompraService service = new CompraService(null, null, null, null);

    private Cliente clientePadrao() {
        return new Cliente(1L, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
    }

    private CarrinhoDeCompras carrinhoComUmProduto(Produto p, long qtd) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        List<ItemCompra> itens = new ArrayList<>();
        itens.add(new ItemCompra(1L, p, qtd));
        carrinho.setItens(itens);
        return carrinho;
    }

    private Produto produto(BigDecimal preco, BigDecimal peso, BigDecimal c, BigDecimal l, BigDecimal a, boolean fragil,
            TipoProduto tipo) {
        return new Produto(1L, "P", "D", preco, peso, c, l, a, fragil, tipo);
    }

    @Test
    public void decisao_descontoValor_regraD3_totalMenorOuIgual500_entao0porcento() {
        Produto p = produto(new BigDecimal("500.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), clientePadrao());

        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    public void decisao_descontoValor_regraD2_totalEntre500e1000_entao10porcento() {
        Produto p = produto(new BigDecimal("600.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), clientePadrao());

        assertThat(total).isEqualByComparingTo("540.00");
    }

    @Test
    public void decisao_descontoValor_regraD1_totalMaiorQue1000_entao20porcento() {
        Produto p = produto(new BigDecimal("1200.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), clientePadrao());

        assertThat(total).isEqualByComparingTo("960.00");
    }

    @Test
    public void decisao_frete_regraF1_faixaA_semFragil_entaoFreteZero() {
        Produto p = produto(BigDecimal.ZERO, new BigDecimal("4.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), clientePadrao());

        assertThat(total).isEqualByComparingTo("0.00");
    }

    @Test
    public void decisao_frete_regraF1_faixaA_comFragil_entaoSomaTaxaFragil() {
        Produto p = produto(BigDecimal.ZERO, new BigDecimal("2.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), true, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 2L), clientePadrao());

        // frete base 0 + frágil 2*5 = 10
        assertThat(total).isEqualByComparingTo("10.00");
    }

    @Test
    public void decisao_frete_regraF2_faixaB_entao2PorKgMaisTaxaMinima() {
        Produto p = produto(BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), clientePadrao());

        assertThat(total).isEqualByComparingTo("24.00");
    }

    @Test
    public void decisao_frete_regiaoNorte_entaoMultiplica() {
        Produto p = produto(BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        Cliente cliente = new Cliente(1L, "C", Regiao.NORTE, TipoCliente.BRONZE);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), cliente);

        assertThat(total).isEqualByComparingTo("31.20");
    }

    @Test
    public void decisao_frete_fidelidadeOuro_isentaFrete() {
        Produto p = produto(BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        Cliente cliente = new Cliente(1L, "C", Regiao.SUDESTE, TipoCliente.OURO);

        BigDecimal total = service.calcularCustoTotal(carrinhoComUmProduto(p, 1L), cliente);

        assertThat(total).isEqualByComparingTo("0.00");
    }
}
