package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private static final BigDecimal DIVISOR_PESO_CUBICO = new BigDecimal("6000");

	private static final BigDecimal TAXA_MINIMA_FRETE = new BigDecimal("12.00");
	private static final BigDecimal TAXA_FRAGIL_POR_UNIDADE = new BigDecimal("5.00");

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;
	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

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

		List<Long> produtosIds = carrinho.getItens().stream()
				.map(i -> i.getProduto().getId())
				.collect(Collectors.toList());

		List<Long> produtosQtds = carrinho.getItens().stream()
				.map(i -> i.getQuantidade())
				.collect(Collectors.toList());

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

		return new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");
	}

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Cliente cliente) {
		BigDecimal subtotalComDescontos = calcularCustoProdutos(carrinho);
		BigDecimal freteFinal = calcularFreteFinal(carrinho, cliente);

		BigDecimal total = subtotalComDescontos.add(freteFinal);
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	BigDecimal calcularCustoProdutos(CarrinhoDeCompras carrinho) {
		BigDecimal totalComDescontoPorTipo = calcularTotalComDescontoPorTipo(carrinho);
		BigDecimal totalComDescontoPorValor = calcularTotalComDescontoPorValor(totalComDescontoPorTipo);

		return totalComDescontoPorValor;
	}

	private BigDecimal calcularTotalComDescontoPorValor(BigDecimal total) {
		BigDecimal desconto = percentualDescontoValorTotal(total);
		return aplicarDesconto(total, desconto);
	}

	private BigDecimal calcularTotalComDescontoPorTipo(CarrinhoDeCompras carrinho) {
		Map<TipoProduto, Entry<BigDecimal, Long>> acumuladosPorTipo = acumularPorTipo(carrinho);

		BigDecimal total = ZERO;

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
		if (valor == null)
			return ZERO;
		if (desconto == null)
			return valor;
		return valor.subtract(valor.multiply(desconto));
	}

	private Map<TipoProduto, Entry<BigDecimal, Long>> acumularPorTipo(CarrinhoDeCompras carrinho) {
		Map<TipoProduto, Entry<BigDecimal, Long>> acumulados = new HashMap<>();

		if (carrinho == null || carrinho.getItens() == null) {
			return acumulados;
		}

		for (ItemCompra item : carrinho.getItens()) {
			Produto produto = item.getProduto();
			BigDecimal preco = produto.getPreco();
			TipoProduto tipo = produto.getTipo();
			Long quantidade = item.getQuantidade();

			BigDecimal subtotalItem = preco.multiply(BigDecimal.valueOf(quantidade));

			Entry<BigDecimal, Long> atual = acumulados.get(tipo);
			if (atual == null) {
				atual = new SimpleEntry<>(ZERO, 0L);
			}

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
		return ZERO;
	}

	private BigDecimal percentualDescontoValorTotal(BigDecimal total) {
		if (total == null)
			return ZERO;

		BigDecimal mil = new BigDecimal("1000.00");
		BigDecimal quinhentos = new BigDecimal("500.00");

		if (total.compareTo(mil) >= 0) {
			return new BigDecimal("0.20");
		}
		if (total.compareTo(quinhentos) >= 0 && total.compareTo(mil) < 0) {
			return new BigDecimal("0.10");
		}
		return ZERO;
	}

	private BigDecimal calcularFreteFinal(CarrinhoDeCompras carrinho, Cliente cliente) {
		BigDecimal pesoTotal = calcularPesoTotalDaCompra(carrinho);

		BigDecimal freteBase = calcularFretePorFaixaDePeso(pesoTotal);
		BigDecimal adicionalFragil = calcularTaxaFragil(carrinho);

		BigDecimal freteComTaxas = freteBase.add(adicionalFragil);

		BigDecimal multiplicador = multiplicadorPorRegiao(cliente);
		BigDecimal freteFinal = freteComTaxas.multiply(multiplicador);

		return freteFinal.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calcularPesoTotalDaCompra(CarrinhoDeCompras carrinho) {
		if (carrinho == null || carrinho.getItens() == null) {
			return ZERO;
		}

		BigDecimal total = ZERO;

		for (ItemCompra item : carrinho.getItens()) {
			Produto produto = item.getProduto();
			Long qtd = item.getQuantidade();

			BigDecimal pesoTributavel = calcularPesoTributavel(produto);
			total = total.add(pesoTributavel.multiply(BigDecimal.valueOf(qtd)));
		}

		return total;
	}

	private BigDecimal calcularPesoTributavel(Produto produto) {
		BigDecimal pesoFisico = nvl(produto.getPesoFisico(), ZERO);
		BigDecimal comprimento = nvl(produto.getComprimento(), ZERO);
		BigDecimal largura = nvl(produto.getLargura(), ZERO);
		BigDecimal altura = nvl(produto.getAltura(), ZERO);

		BigDecimal volume = comprimento.multiply(largura).multiply(altura);

		BigDecimal pesoCubico = ZERO;
		if (DIVISOR_PESO_CUBICO.compareTo(ZERO) > 0) {
			pesoCubico = volume.divide(DIVISOR_PESO_CUBICO, 10, RoundingMode.HALF_UP);
		}

		return max(pesoFisico, pesoCubico);
	}

	private BigDecimal calcularFretePorFaixaDePeso(BigDecimal pesoTotal) {
		if (pesoTotal == null)
			return ZERO;

		if (pesoTotal.compareTo(new BigDecimal("5")) <= 0) {
			return ZERO;
		}

		BigDecimal frete;

		if (pesoTotal.compareTo(new BigDecimal("10")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("2.00"));
			return frete.add(TAXA_MINIMA_FRETE);
		}

		if (pesoTotal.compareTo(new BigDecimal("50")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("4.00"));
			return frete.add(TAXA_MINIMA_FRETE);
		}

		frete = pesoTotal.multiply(new BigDecimal("7.00"));
		return frete.add(TAXA_MINIMA_FRETE);
	}

	private BigDecimal calcularTaxaFragil(CarrinhoDeCompras carrinho) {
		if (carrinho == null || carrinho.getItens() == null) {
			return ZERO;
		}

		long unidadesFragil = 0;

		for (ItemCompra item : carrinho.getItens()) {
			if (item.getProduto().isFragil()) {
				unidadesFragil += item.getQuantidade();
			}
		}

		return TAXA_FRAGIL_POR_UNIDADE.multiply(BigDecimal.valueOf(unidadesFragil));
	}

	private BigDecimal multiplicadorPorRegiao(Cliente cliente) {
		if (cliente == null || cliente.getRegiao() == null) {
			return new BigDecimal("1.00");
		}

		String regiao = cliente.getRegiao().toString().toUpperCase();

		return switch (regiao) {
			case "SUL" -> new BigDecimal("1.05");
			case "NORDESTE" -> new BigDecimal("1.10");
			case "CENTRO-OESTE", "CENTRO_OESTE" -> new BigDecimal("1.20");
			case "NORTE" -> new BigDecimal("1.30");
			default -> new BigDecimal("1.00");
		};
	}

	private static BigDecimal nvl(BigDecimal v, BigDecimal padrao) {
		return v == null ? padrao : v;
	}

	private static BigDecimal max(BigDecimal a, BigDecimal b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.compareTo(b) >= 0 ? a : b;
	}
}
