package ecommerce.service;

import java.math.BigDecimal;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService {
	private final CarrinhoDeComprasService carrinhoService;

	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;

	private final IPagamentoExternal pagamentoExternal;

	@Autowired
	public CompraService(
			CarrinhoDeComprasService carrinhoService,
			ClienteService clienteService,
			IEstoqueExternal estoqueExternal,
			IPagamentoExternal pagamentoExternal) {
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;
		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId) {
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel()) {
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente);

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado()) {
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso()) {
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Cliente cliente) {
		BigDecimal custoProdutos = calcularCustoProdutos(carrinho);

		return custoProdutos;
	}

	BigDecimal calcularCustoProdutos(CarrinhoDeCompras carrinho) {
		BigDecimal totalComDescontoPorTipo = calcularTotalComDescontoPorTipo(carrinho);
		BigDecimal totalComDescontoPorValor = calcularTotalComDescontoPorValor(totalComDescontoPorTipo);

		return totalComDescontoPorValor;
	}

	private BigDecimal calcularTotalComDescontoPorValor(BigDecimal total) {
		BigDecimal desconto = percentualDescontoValorTotal(total);
		BigDecimal resultado = aplicarDesconto(total, desconto);

		return resultado;
	}

	private BigDecimal calcularTotalComDescontoPorTipo(CarrinhoDeCompras carrinho) {
		Map<TipoProduto, Entry<BigDecimal, Long>> acumuladosPorTipo = acumularPorTipo(carrinho);

		BigDecimal total = BigDecimal.ZERO;

		for (Entry<TipoProduto, Entry<BigDecimal, Long>> entry : acumuladosPorTipo.entrySet()) {
			Entry<BigDecimal, Long> acumulado = entry.getValue();

			BigDecimal subtotalTipo = acumulado.getKey();
			Long quantidadeTipo = acumulado.getValue();

			BigDecimal descontoTipo = percentualDescontoPorQuantidade(quantidadeTipo);
			BigDecimal subtotalComDesconto = aplicarDesconto(subtotalTipo, descontoTipo);

			total = total.add(subtotalComDesconto);
		}

		return total;
	}

	private BigDecimal aplicarDesconto(BigDecimal valor, BigDecimal desconto) {
		return valor.subtract(valor.multiply(desconto));
	}

	private Map<TipoProduto, Entry<BigDecimal, Long>> acumularPorTipo(CarrinhoDeCompras carrinho) {
		Map<TipoProduto, Entry<BigDecimal, Long>> acumulados = new HashMap<>();

		for (ItemCompra item : carrinho.getItens()) {
			Produto produto = item.getProduto();
			BigDecimal preco = produto.getPreco();
			TipoProduto tipo = produto.getTipo();
			Long quantidade = item.getQuantidade();

			BigDecimal subtotalItem = preco.multiply(BigDecimal.valueOf(quantidade));

			Entry<BigDecimal, Long> atual = acumulados.get(tipo);

			BigDecimal novoSubtotal = atual.getKey().add(subtotalItem);
			Long novaQuantidade = atual.getValue() + quantidade;
			acumulados.put(tipo, new SimpleEntry<>(novoSubtotal, novaQuantidade));

		}

		return acumulados;
	}

	private BigDecimal percentualDescontoPorQuantidade(Long quantidade) {
		if (quantidade >= 8) {
			return new BigDecimal("0.15");
		}
		if (quantidade >= 5) {
			return new BigDecimal("0.10");
		}
		if (quantidade >= 3) {
			return new BigDecimal("0.05");
		}

		return BigDecimal.ZERO;
	}

	private BigDecimal percentualDescontoValorTotal(BigDecimal total) {
		if (total.compareTo(new BigDecimal("1000.00")) > 0) {
			return new BigDecimal("0.20");
		}
		if (total.compareTo(new BigDecimal("500.00")) > 0) {
			return new BigDecimal("0.10");
		}

		return BigDecimal.ZERO;
	}

}
