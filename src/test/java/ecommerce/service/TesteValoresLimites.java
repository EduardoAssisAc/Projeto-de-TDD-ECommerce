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

public class TesteValoresLimites {

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

        private Produto produto(BigDecimal preco, BigDecimal pesoFisico, BigDecimal c, BigDecimal l, BigDecimal a,
                        boolean fragil, TipoProduto tipo) {
                return new Produto(1L, "P", "D", preco, pesoFisico, c, l, a, fragil, tipo);
        }

        @Test
        public void limite_descontoPorTipo_transicao2Para3() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.LIVRO);

                BigDecimal total2 = service.calcularCustoTotal(carrinhoComUmProduto(p, 2L), clientePadrao());
                BigDecimal total3 = service.calcularCustoTotal(carrinhoComUmProduto(p, 3L), clientePadrao());

                assertThat(total2).isEqualByComparingTo("20.00");
                assertThat(total3).isEqualByComparingTo("28.50");
        }

        @Test
        public void limite_descontoPorTipo_transicao4Para5() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.LIVRO);

                BigDecimal total4 = service.calcularCustoTotal(carrinhoComUmProduto(p, 4L), clientePadrao());
                BigDecimal total5 = service.calcularCustoTotal(carrinhoComUmProduto(p, 5L), clientePadrao());

                assertThat(total4).isEqualByComparingTo("38.00");
                assertThat(total5).isEqualByComparingTo("45.00");
        }

        @Test
        public void limite_descontoPorTipo_transicao7Para8() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.LIVRO);

                BigDecimal total7 = service.calcularCustoTotal(carrinhoComUmProduto(p, 7L), clientePadrao());
                BigDecimal total8 = service.calcularCustoTotal(carrinhoComUmProduto(p, 8L), clientePadrao());

                assertThat(total7).isEqualByComparingTo("63.00");
                assertThat(total8).isEqualByComparingTo("68.00");
        }

        @Test
        public void limite_descontoPorValor_transicao500_00Para500_01() {
                Produto p500 = produto(new BigDecimal("500.00"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                Produto p50001 = produto(new BigDecimal("500.01"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                BigDecimal t500 = service.calcularCustoTotal(carrinhoComUmProduto(p500, 1L), clientePadrao());
                BigDecimal t50001 = service.calcularCustoTotal(carrinhoComUmProduto(p50001, 1L), clientePadrao());

                assertThat(t500).isEqualByComparingTo("500.00");
                assertThat(t50001).isEqualByComparingTo("450.01"); // 500.01 -10%
        }

        @Test
        public void limite_descontoPorValor_transicao1000_00Para1000_01() {
                Produto p1000 = produto(new BigDecimal("1000.00"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                Produto p100001 = produto(new BigDecimal("1000.01"), new BigDecimal("0.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                BigDecimal t1000 = service.calcularCustoTotal(carrinhoComUmProduto(p1000, 1L), clientePadrao());
                BigDecimal t100001 = service.calcularCustoTotal(carrinhoComUmProduto(p100001, 1L), clientePadrao());

                // 1000.00 é >500 e <=1000 => 10%
                assertThat(t1000).isEqualByComparingTo("900.00");
                // 1000.01 é >1000 => 20%
                assertThat(t100001).isEqualByComparingTo("800.01");
        }

        @Test
        public void limite_frete_transicao5_00Para5_01() {
                Produto p5 = produto(BigDecimal.ZERO, new BigDecimal("5.00"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                Produto p501 = produto(BigDecimal.ZERO, new BigDecimal("5.01"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                BigDecimal t5 = service.calcularCustoTotal(carrinhoComUmProduto(p5, 1L), clientePadrao());
                BigDecimal t501 = service.calcularCustoTotal(carrinhoComUmProduto(p501, 1L), clientePadrao());

                assertThat(t5).isEqualByComparingTo("0.00");
                assertThat(t501).isEqualByComparingTo("22.02"); // 5.01*2 + 12 = 22.02
        }

        @Test
        public void limite_frete_transicao10_00Para10_01() {
                Produto p10 = produto(BigDecimal.ZERO, new BigDecimal("10.00"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                Produto p1001 = produto(BigDecimal.ZERO, new BigDecimal("10.01"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                BigDecimal t10 = service.calcularCustoTotal(carrinhoComUmProduto(p10, 1L), clientePadrao());
                BigDecimal t1001 = service.calcularCustoTotal(carrinhoComUmProduto(p1001, 1L), clientePadrao());

                assertThat(t10).isEqualByComparingTo("32.00"); // 10*2 + 12
                assertThat(t1001).isEqualByComparingTo("52.04"); // 10.01*4 + 12 = 52.04
        }

        @Test
        public void limite_frete_transicao50_00Para50_01() {
                Produto p50 = produto(BigDecimal.ZERO, new BigDecimal("50.00"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                Produto p5001 = produto(BigDecimal.ZERO, new BigDecimal("50.01"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false,
                                TipoProduto.ALIMENTO);

                BigDecimal t50 = service.calcularCustoTotal(carrinhoComUmProduto(p50, 1L), clientePadrao());
                BigDecimal t5001 = service.calcularCustoTotal(carrinhoComUmProduto(p5001, 1L), clientePadrao());

                assertThat(t50).isEqualByComparingTo("212.00"); // 50*4+12
                assertThat(t5001).isEqualByComparingTo("362.07"); // 50.01*7+12 = 362.07
        }
}
