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

public class TesteParticao {

    private final CompraService service = new CompraService(null, null, null, null);

    private Cliente clientePadrao() {
        return new Cliente(1L, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
    }

    private CarrinhoDeCompras carrinhoComItens(ItemCompra... itens) {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        List<ItemCompra> lista = new ArrayList<>();
        for (ItemCompra i : itens)
            lista.add(i);
        carrinho.setItens(lista);
        return carrinho;
    }

    private Produto produto(long id, BigDecimal preco, BigDecimal peso, BigDecimal c, BigDecimal l, BigDecimal a,
            boolean fragil, TipoProduto tipo) {
        return new Produto(id, "P" + id, "Desc", preco, peso, c, l, a, fragil, tipo);
    }

    @Test
    public void particao_descontoPorTipo_qtd0a2_entaoSemDesconto() {
        Produto livro = produto(1L, new BigDecimal("50.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.LIVRO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, livro, 2L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("100.00");
    }

    @Test
    public void particao_descontoPorTipo_qtd3a4_entao5porcento() {
        Produto livro = produto(1L, new BigDecimal("50.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.LIVRO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, livro, 3L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("142.50");
    }

    @Test
    public void particao_descontoPorTipo_qtd5a7_entao10porcento() {
        Produto livro = produto(1L, new BigDecimal("50.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.LIVRO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, livro, 5L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("225.00");
    }

    @Test
    public void particao_descontoPorTipo_qtd8Mais_entao15porcento() {
        Produto eletronico = produto(1L, new BigDecimal("10.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ELETRONICO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, eletronico, 8L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("68.00");
    }

    @Test
    public void particao_descontoPorValor_totalMenorOuIgual500_entao0porcento() {
        Produto p = produto(1L, new BigDecimal("500.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("500.00");
    }

    @Test
    public void particao_descontoPorValor_totalEntre500e1000_entao10porcento() {
        Produto p = produto(1L, new BigDecimal("600.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("540.00");
    }

    @Test
    public void particao_descontoPorValor_totalMaiorQue1000_entao20porcento() {
        Produto p = produto(1L, new BigDecimal("1200.00"), new BigDecimal("0.0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("960.00");
    }

    @Test
    public void particao_pesoTributavel_fisicoMaiorQueCubico_entaoUsaFisico() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("24.00");
    }

    @Test
    public void particao_pesoTributavel_cubicoMaiorQueFisico_entaoUsaCubico() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("1.00"),
                new BigDecimal("100"), new BigDecimal("60"), new BigDecimal("50"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("212.00");
    }

    @Test
    public void particao_frete_faixaA_ate5kg_entaoFreteZero() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("4.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("0.00");
    }

    @Test
    public void particao_frete_faixaB_entre5e10kg() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("24.00");
    }

    @Test
    public void particao_frete_faixaC_entre10e50kg() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("12.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("60.00");
    }

    @Test
    public void particao_frete_faixaD_maiorQue50kg() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("60.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("432.00");
    }

    @Test
    public void particao_taxaFragil_comItensFragil_entaoSoma5PorUnidade() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), true, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 2L));

        BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

        assertThat(total).isEqualByComparingTo("70.00");
    }

    @Test
    public void particao_regiaoNorte_entaoMultiplicaPor130() {
        Produto p = produto(1L, BigDecimal.ZERO, new BigDecimal("6.00"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false, TipoProduto.ALIMENTO);

        CarrinhoDeCompras carrinho = carrinhoComItens(new ItemCompra(1L, p, 1L));

        Cliente cliente = new Cliente(1L, "C", Regiao.NORTE, TipoCliente.BRONZE);

        BigDecimal total = service.calcularCustoTotal(carrinho, cliente);

        assertThat(total).isEqualByComparingTo("31.20");
    }
}
