package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

public class CompraServiceTest {

	private Cliente clientePadrao() {
		return new Cliente(1L, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
	}

	@Test
	public void calcularCustoTotal() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto produto = new Produto(1L, "Nome_Produto", "Descrição_Produto", new BigDecimal("10.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, produto, 1L));
		carrinho.setItens(itens);

		BigDecimal custoTotal = service.calcularCustoTotal(carrinho, clientePadrao());

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

		Produto fone = new Produto(2L, "Fone", "Fone de ouvido", new BigDecimal("100.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ELETRONICO);

		itens.add(new ItemCompra(1L, arroz, 2L));
		itens.add(new ItemCompra(2L, fone, 1L));
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

		assertThat(custoProdutos).isEqualByComparingTo("325.00");
	}

	@Test
	public void calcularCustoProdutosAoAplicarDescontoPorValorTotalDoCarrinho() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto tv = new Produto(1L, "TV", "TV 50 polegadas", new BigDecimal("200.00"),
				new BigDecimal("1.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ELETRONICO);

		itens.add(new ItemCompra(1L, tv, 8L));
		carrinho.setItens(itens);

		BigDecimal custoProdutos = service.calcularCustoProdutos(carrinho);

		assertThat(custoProdutos)
				.as("8 itens -> 15% por tipo; depois >1000 -> 20% por valor")
				.isEqualByComparingTo("1088.00");
	}

	@Test
	public void calcularFrete_quandoPesoTotalAte5_entaoFreteIsento() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("2.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

		assertThat(total).isEqualByComparingTo("20.00");
	}

	@Test
	public void calcularFrete_quandoPesoEntre5e10_entaoFretePorKgMaisTaxaMinima() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

		assertThat(total).isEqualByComparingTo("44.00");
	}

	@Test
	public void calcularFrete_quandoPesoEntre10e50_entaoFrete4PorKgMaisTaxaMinima() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("6.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

		assertThat(total).isEqualByComparingTo("80.00");
	}

	@Test
	public void calcularFrete_quandoProdutoFragil_entaoSoma5PorUnidade() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				true, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		BigDecimal total = service.calcularCustoTotal(carrinho, clientePadrao());

		assertThat(total).isEqualByComparingTo("54.00");
	}

	@Test
	public void calcularFrete_quandoRegiaoNorte_entaoMultiplicaPor130() {
		CompraService service = new CompraService(null, null, null, null);

		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p = new Produto(1L, "Produto", "Desc", new BigDecimal("10.00"),
				new BigDecimal("3.0"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"),
				false, TipoProduto.ALIMENTO);

		itens.add(new ItemCompra(1L, p, 2L));
		carrinho.setItens(itens);

		Cliente cliente = new Cliente(1L, "C", Regiao.NORTE, TipoCliente.BRONZE);

		BigDecimal total = service.calcularCustoTotal(carrinho, cliente);

		assertThat(total).isEqualByComparingTo("51.20");
	}

	@Test
	public void finalizarCompra_quandoSucesso_entaoAutorizaPagamentoEDaBaixaERetornaSucesso() {
		CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
		ClienteService clienteService = mock(ClienteService.class);
		IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
		IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

		CompraService service = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

		Long carrinhoId = 10L;
		Long clienteId = 20L;

		Cliente cliente = new Cliente(clienteId, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
		CarrinhoDeCompras carrinho = carrinhoBasicoSemFrete();

		when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
		when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

		when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
				.thenReturn(new DisponibilidadeDTO(true, List.of()));

		when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
				.thenReturn(new PagamentoDTO(true, 999L));

		when(estoqueExternal.darBaixa(anyList(), anyList()))
				.thenReturn(new EstoqueBaixaDTO(true));

		CompraDTO retorno = service.finalizarCompra(carrinhoId, clienteId);

		assertThat(retorno).isEqualTo(new CompraDTO(true, 999L, "Compra finalizada com sucesso."));

		ArgumentCaptor<Double> captorValor = ArgumentCaptor.forClass(Double.class);
		verify(pagamentoExternal).autorizarPagamento(eq(clienteId), captorValor.capture());
		assertThat(captorValor.getValue()).isCloseTo(30.00, org.assertj.core.data.Offset.offset(0.0001));

		verify(estoqueExternal, times(1)).verificarDisponibilidade(anyList(), anyList());
		verify(estoqueExternal, times(1)).darBaixa(anyList(), anyList());
		verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
	}

	@Test
	public void finalizarCompra_quandoSemEstoque_entaoLancaExcecaoENaoAutorizaPagamento() {
		CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
		ClienteService clienteService = mock(ClienteService.class);
		IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
		IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

		CompraService service = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

		Long carrinhoId = 1L;
		Long clienteId = 2L;

		Cliente cliente = new Cliente(clienteId, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
		CarrinhoDeCompras carrinho = carrinhoBasicoSemFrete();

		when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
		when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

		when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
				.thenReturn(new DisponibilidadeDTO(false, List.of(1L)));

		assertThatThrownBy(() -> service.finalizarCompra(carrinhoId, clienteId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Itens fora de estoque.");

		verify(pagamentoExternal, never()).autorizarPagamento(anyLong(), anyDouble());
		verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
		verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
	}

	@Test
	public void finalizarCompra_quandoPagamentoNaoAutorizado_entaoLancaExcecaoENaoDaBaixa() {
		CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
		ClienteService clienteService = mock(ClienteService.class);
		IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
		IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

		CompraService service = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

		Long carrinhoId = 1L;
		Long clienteId = 2L;

		Cliente cliente = new Cliente(clienteId, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
		CarrinhoDeCompras carrinho = carrinhoBasicoSemFrete();

		when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
		when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

		when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
				.thenReturn(new DisponibilidadeDTO(true, List.of()));

		when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
				.thenReturn(new PagamentoDTO(false, 123L));

		assertThatThrownBy(() -> service.finalizarCompra(carrinhoId, clienteId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Pagamento não autorizado.");

		verify(estoqueExternal, never()).darBaixa(anyList(), anyList());
		verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
	}

	@Test
	public void finalizarCompra_quandoFalhaAoDarBaixa_entaoCancelaPagamentoELancaExcecao() {
		CarrinhoDeComprasService carrinhoService = mock(CarrinhoDeComprasService.class);
		ClienteService clienteService = mock(ClienteService.class);
		IEstoqueExternal estoqueExternal = mock(IEstoqueExternal.class);
		IPagamentoExternal pagamentoExternal = mock(IPagamentoExternal.class);

		CompraService service = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);

		Long carrinhoId = 1L;
		Long clienteId = 2L;

		Cliente cliente = new Cliente(clienteId, "Cliente", Regiao.SUDESTE, TipoCliente.BRONZE);
		CarrinhoDeCompras carrinho = carrinhoBasicoSemFrete();

		when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
		when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente)).thenReturn(carrinho);

		when(estoqueExternal.verificarDisponibilidade(anyList(), anyList()))
				.thenReturn(new DisponibilidadeDTO(true, List.of()));

		when(pagamentoExternal.autorizarPagamento(eq(clienteId), anyDouble()))
				.thenReturn(new PagamentoDTO(true, 777L));

		when(estoqueExternal.darBaixa(anyList(), anyList()))
				.thenReturn(new EstoqueBaixaDTO(false));

		assertThatThrownBy(() -> service.finalizarCompra(carrinhoId, clienteId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Erro ao dar baixa no estoque.");

		verify(pagamentoExternal, times(1)).cancelarPagamento(clienteId, 777L);
	}

	private CarrinhoDeCompras carrinhoBasicoSemFrete() {
		CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
		List<ItemCompra> itens = new ArrayList<>();

		Produto p1 = new Produto(1L, "Produto 1", "Desc", new BigDecimal("10.00"),
				new BigDecimal("1.0"), new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"),
				false, TipoProduto.ALIMENTO);

		Produto p2 = new Produto(2L, "Produto 2", "Desc", new BigDecimal("20.00"),
				new BigDecimal("1.0"), new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"),
				false, TipoProduto.ELETRONICO);

		itens.add(new ItemCompra(1L, p1, 1L));
		itens.add(new ItemCompra(2L, p2, 1L));

		carrinho.setItens(itens);
		return carrinho;
	}
}
