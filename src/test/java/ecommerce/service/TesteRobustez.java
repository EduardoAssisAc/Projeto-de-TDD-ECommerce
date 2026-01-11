package ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

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

public class TesteRobustez {

        private final CompraService service = new CompraService(null, null, null, null);

        private CarrinhoDeCompras carrinhoNuloItens() {
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
                carrinho.setItens(null);
                return carrinho;
        }

        private CarrinhoDeCompras carrinhoVazio() {
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
                carrinho.setItens(new ArrayList<>());
                return carrinho;
        }

        private CarrinhoDeCompras carrinhoComUmItem(Produto p, Long qtd) {
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
                List<ItemCompra> itens = new ArrayList<>();
                itens.add(new ItemCompra(1L, p, qtd));
                carrinho.setItens(itens);
                return carrinho;
        }

        private CarrinhoDeCompras carrinhoComItemNulo() {
                CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
                List<ItemCompra> itens = new ArrayList<>();
                itens.add(null);
                carrinho.setItens(itens);
                return carrinho;
        }

        private Produto produto(BigDecimal preco, BigDecimal pesoFisico,
                        BigDecimal c, BigDecimal l, BigDecimal a,
                        Boolean fragil) {
                return new Produto(1L, "P", "D", preco, pesoFisico, c, l, a, fragil, TipoProduto.ALIMENTO);
        }

        private Cliente clienteValido() {
                return new Cliente(1L, "Cliente", Regiao.NORTE, TipoCliente.BRONZE);
        }

        private Cliente clienteComRegiaoNula() {
                return new Cliente(1L, "Cliente", null, TipoCliente.BRONZE);
        }

        private Cliente clienteComTipoNulo() {
                return new Cliente(1L, "Cliente", Regiao.NORTE, null);
        }

        @Test
        public void robustez_quandoCarrinhoNulo_entaoLancaExcecao() {
                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(null, clienteValido()));
        }

        @Test
        public void robustez_quandoCarrinhoComListaItensNula_entaoLancaExcecao() {
                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoNuloItens(), clienteValido()));
        }

        @Test
        public void robustez_quandoCarrinhoVazio_entaoLancaExcecao() {
                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoVazio(), clienteValido()));
        }

        @Test
        public void robustez_quandoItemNuloNoCarrinho_entaoLancaExcecao() {
                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComItemNulo(), clienteValido()));
        }

        @Test
        public void robustez_quandoProdutoNulo_entaoLancaExcecao() {
                CarrinhoDeCompras carrinho = carrinhoComUmItem(null, 1L);
                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho, clienteValido()));
        }

        @Test
        public void robustez_quandoQuantidadeNula_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                CarrinhoDeCompras carrinho = carrinhoComUmItem(p, null);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinho, clienteValido()));
        }

        @Test
        public void robustez_quandoQuantidadeZero_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 0L), clienteValido()));
        }

        @Test
        public void robustez_quandoQuantidadeNegativa_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, -1L), clienteValido()));
        }

        @Test
        public void robustez_quandoPrecoNulo_entaoLancaExcecao() {
                Produto p = produto(null, new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoPrecoNegativo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("-10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoPesoFisicoNulo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), null,
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoPesoFisicoNegativo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("-1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoDimensoesNulas_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                null, new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoDimensoesNegativas_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("-10"), new BigDecimal("10"), new BigDecimal("10"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoFragilNulo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), null);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteValido()));
        }

        @Test
        public void robustez_quandoClienteNulo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), null));
        }

        @Test
        public void robustez_quandoClienteComRegiaoNula_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteComRegiaoNula()));
        }

        @Test
        public void robustez_quandoClienteComTipoNulo_entaoLancaExcecao() {
                Produto p = produto(new BigDecimal("10.00"), new BigDecimal("1.0"),
                                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"), false);

                assertThrows(IllegalArgumentException.class,
                                () -> service.calcularCustoTotal(carrinhoComUmItem(p, 1L), clienteComTipoNulo()));
        }
}
