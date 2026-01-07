package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoProduto;

public class CompraServiceTest {

	@Test
	public void calcularCustoTotal() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto produto = new Produto(1l, "Nome_Produto", "Descrição_Produto", BigDecimal.valueOf(10l),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		ItemCompra item1 = new ItemCompra(1l, produto, 1l);

		itens.add(item1);
		carrinho.setItens(itens);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, null);

		BigDecimal esperado = BigDecimal.valueOf(10l);
		assertEquals(0, custoTotal.compareTo(esperado), "Valor calculado incorreto: " + custoTotal);

		assertThat(custoTotal).as("Custo Total da Compra").isEqualByComparingTo("10.00");
	}

	@Test
	public void calcularSubTotalSimples() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto arroz = new Produto(1L, "Arroz", "Arroz 5kg", new BigDecimal("50.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		ItemCompra item1 = new ItemCompra(1L, arroz, 2L);

		Produto fone = new Produto(2L, "Fone", "Fone de ouvido", new BigDecimal("100.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ELETRONICO);

		ItemCompra item2 = new ItemCompra(2L, fone, 1L);

		itens.add(item1);
		itens.add(item2);
		carrinho.setItens(itens);

		BigDecimal custoProdutos = service.calcularCustoProdutos(carrinho);

		assertThat(custoProdutos).as("Custo dos produtos sem descontos").isEqualByComparingTo("200.00");
	}

	@Test
	public void calcularCustoProdutosAoAplicarDescontoPorTipo() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto livroA = new Produto(1L, "Livro A", "Livro A", new BigDecimal("50.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.LIVRO);

		Produto livroB = new Produto(2L, "Livro B", "Livro B", new BigDecimal("50.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.LIVRO);

		ItemCompra item1 = new ItemCompra(1L, livroA, 3L);
		ItemCompra item2 = new ItemCompra(2L, livroB, 2L);

		Produto fone = new Produto(3L, "Fone", "Fone de ouvido", new BigDecimal("100.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ELETRONICO);

		ItemCompra item3 = new ItemCompra(3L, fone, 1L);

		itens.add(item1);
		itens.add(item2);
		itens.add(item3);
		carrinho.setItens(itens);

		BigDecimal custoProdutos = service.calcularCustoProdutos(carrinho);

		assertThat(custoProdutos)
				.as("Custo dos produtos com desconto por tipo")
				.isEqualByComparingTo("325.00");
	}

	@Test
	public void calcularCustoProdutosAoAplicarDescontoPorValorTotalDoCarrinho() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto tv = new Produto(1L, "TV", "TV 50 polegadas", new BigDecimal("200.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ELETRONICO);

		ItemCompra item = new ItemCompra(1L, tv, 8L);

		itens.add(item);
		carrinho.setItens(itens);

		BigDecimal custoProdutos = service.calcularCustoProdutos(carrinho);

		assertThat(custoProdutos)
				.as("Custo dos produtos com desconto por valor total")
				.isEqualByComparingTo("1088.00");
	}

	
	@Test
	public void calcularFrete_quandoPesoTotalAte5_entaoFreteIsento() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso tributável = 2 kg, qtd = 2 => 4 kg (faixa A)
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("2.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, null);

		// sem descontos, sem frete
		assertThat(total).isEqualByComparingTo("20.00");
	}

	@Test
	public void calcularFrete_quandoPesoEntre5e10_entaoFretePorKgMaisTaxaMinima() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso = 3 kg, qtd 2 => 6 kg (faixa B)
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, null);

		// subtotal = 20.00
		// frete = (6 * 2.00) + 12.00 = 24.00
		assertThat(total).isEqualByComparingTo("44.00");
	}

	@Test
	public void calcularFrete_quandoPesoEntre10e50_entaoFrete4PorKgMaisTaxaMinima() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso = 6 kg, qtd 2 => 12 kg (faixa C)
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("6.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, null);

		// subtotal = 20.00
		// frete = (12 * 4.00) + 12.00 = 60.00
		assertThat(total).isEqualByComparingTo("80.00");
	}

	@Test
	public void calcularFrete_quandoProdutoFragil_entaoSoma5PorUnidadeAntesDeRegiaoEFidelidade() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso = 3 kg, qtd 2 => 6 kg (faixa B) => 24.00
		// frágil: 2 unidades => +10.00
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				true, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, null);

		// subtotal 20.00 + frete (24.00 + 10.00) = 54.00
		assertThat(total).isEqualByComparingTo("54.00");
	}

	@Test
	public void calcularFrete_quandoRegiaoNorte_entaoMultiplicaPor130() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso = 3 kg, qtd 2 => 6 kg => frete base 24.00
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		Cliente cliente = new Cliente();
		cliente.setRegiao(Regiao.NORTE);
		BigDecimal total = service.calcularCustoTotal(carrinho, cliente);

		// subtotal 20.00 + frete (24 * 1.30) = 31.20 => total 51.20
		assertThat(total).isEqualByComparingTo("51.20");
	}

	@Test
	public void calcularFrete_quandoFidelidadeOuro_entaoFreteZerado() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		// peso = 3 kg, qtd 2 => 6 kg => frete base 24.00
		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		Cliente cliente = new Cliente();
		cliente.setRegiao(Regiao.NORTE);

		BigDecimal total = service.calcularCustoTotal(carrinho, cliente);

		// subtotal 20.00 + frete 0.00
		assertThat(total).isEqualByComparingTo("20.00");
	}
}
