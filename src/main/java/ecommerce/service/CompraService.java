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
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
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
				.map(ItemCompra::getQuantidade)
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
		validarEntradaParaCalculo(carrinho, cliente);

		BigDecimal subtotalComDescontos = calcularCustoProdutos(carrinho);
		BigDecimal freteFinal = calcularFreteFinal(carrinho, cliente);

		BigDecimal total = subtotalComDescontos.add(freteFinal);
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	BigDecimal calcularCustoProdutos(CarrinhoDeCompras carrinho) {
		validarCarrinhoParaCalculo(carrinho);

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
		if (quantidade >= 8)
			return new BigDecimal("0.15");
		if (quantidade >= 5)
			return new BigDecimal("0.10");
		if (quantidade >= 3)
			return new BigDecimal("0.05");
		return ZERO;
	}

	private BigDecimal percentualDescontoValorTotal(BigDecimal total) {
		if (total == null)
			return ZERO;

		BigDecimal mil = new BigDecimal("1000.00");
		BigDecimal quinhentos = new BigDecimal("500.00");

		boolean gt500 = total.compareTo(quinhentos) > 0;
		boolean le1000 = total.compareTo(mil) <= 0;

		if (gt500 && le1000) {
			return new BigDecimal("0.10");
		}
		if (total.compareTo(mil) > 0) {
			return new BigDecimal("0.20");
		}
		return ZERO;
	}

	private BigDecimal calcularFreteFinal(CarrinhoDeCompras carrinho, Cliente cliente) {
		BigDecimal pesoTotal = calcularPesoTotalDaCompra(carrinho);

		BigDecimal freteBase = calcularFretePorFaixaDePeso(pesoTotal);
		BigDecimal adicionalFragil = calcularTaxaFragil(carrinho);

		BigDecimal freteComTaxas = freteBase.add(adicionalFragil);

		BigDecimal multiplicador = multiplicadorPorRegiao(cliente.getRegiao());
		BigDecimal freteRegional = freteComTaxas.multiply(multiplicador);

		BigDecimal freteComFidelidade = aplicarFidelidadeNoFrete(freteRegional, cliente.getTipo());

		return freteComFidelidade;
	}

	private BigDecimal aplicarFidelidadeNoFrete(BigDecimal frete, TipoCliente tipoCliente) {
		if (frete == null)
			return ZERO;

		return switch (tipoCliente) {
			case OURO -> ZERO;
			case PRATA -> frete.multiply(new BigDecimal("0.50"));
			case BRONZE -> frete;
		};
	}

	private BigDecimal calcularPesoTotalDaCompra(CarrinhoDeCompras carrinho) {
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
		BigDecimal pesoFisico = produto.getPesoFisico();
		BigDecimal comprimento = produto.getComprimento();
		BigDecimal largura = produto.getLargura();
		BigDecimal altura = produto.getAltura();

		BigDecimal volume = comprimento.multiply(largura).multiply(altura);
		BigDecimal pesoCubico = volume.divide(DIVISOR_PESO_CUBICO, 10, RoundingMode.HALF_UP);

		return max(pesoFisico, pesoCubico);
	}

	private BigDecimal calcularFretePorFaixaDePeso(BigDecimal pesoTotal) {
		if (pesoTotal == null)
			return ZERO;

		if (pesoTotal.compareTo(new BigDecimal("5.00")) <= 0) {
			return ZERO;
		}

		BigDecimal frete;

		if (pesoTotal.compareTo(new BigDecimal("10.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("2.00"));
			return frete.add(TAXA_MINIMA_FRETE);
		}

		if (pesoTotal.compareTo(new BigDecimal("50.00")) <= 0) {
			frete = pesoTotal.multiply(new BigDecimal("4.00"));
			return frete.add(TAXA_MINIMA_FRETE);
		}

		frete = pesoTotal.multiply(new BigDecimal("7.00"));
		return frete.add(TAXA_MINIMA_FRETE);
	}

	private BigDecimal calcularTaxaFragil(CarrinhoDeCompras carrinho) {
		long unidadesFragil = 0;

		for (ItemCompra item : carrinho.getItens()) {
			Produto p = item.getProduto();
			if (Boolean.TRUE.equals(p.isFragil())) {
				unidadesFragil += item.getQuantidade();
			}
		}

		return TAXA_FRAGIL_POR_UNIDADE.multiply(BigDecimal.valueOf(unidadesFragil));
	}

	private BigDecimal multiplicadorPorRegiao(Regiao regiao) {
		if (regiao == null)
			return new BigDecimal("1.00");

		return switch (regiao) {
			case SUDESTE -> new BigDecimal("1.00");
			case SUL -> new BigDecimal("1.05");
			case NORDESTE -> new BigDecimal("1.10");
			case CENTRO_OESTE -> new BigDecimal("1.20");
			case NORTE -> new BigDecimal("1.30");
		};
	}

	private void validarEntradaParaCalculo(CarrinhoDeCompras carrinho, Cliente cliente) {
		validarCarrinhoParaCalculo(carrinho);
		validarClienteParaCalculo(cliente);
	}

	private void validarClienteParaCalculo(Cliente cliente) {
		if (cliente == null) {
			throw new IllegalArgumentException("Cliente não pode ser nulo.");
		}
		if (cliente.getRegiao() == null) {
			throw new IllegalArgumentException("Região do cliente não pode ser nula.");
		}
		if (cliente.getTipo() == null) {
			throw new IllegalArgumentException("Tipo do cliente não pode ser nulo.");
		}
	}

	private void validarCarrinhoParaCalculo(CarrinhoDeCompras carrinho) {
		if (carrinho == null) {
			throw new IllegalArgumentException("Carrinho não pode ser nulo.");
		}
		if (carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException("Carrinho deve possuir pelo menos 1 item.");
		}

		for (ItemCompra item : carrinho.getItens()) {
			if (item == null) {
				throw new IllegalArgumentException("Item do carrinho não pode ser nulo.");
			}

			Long qtd = item.getQuantidade();
			if (qtd == null || qtd <= 0) {
				throw new IllegalArgumentException("Quantidade do item deve ser maior que zero.");
			}

			Produto produto = item.getProduto();
			if (produto == null) {
				throw new IllegalArgumentException("Produto do item não pode ser nulo.");
			}

			if (produto.getTipo() == null) {
				throw new IllegalArgumentException("Tipo do produto não pode ser nulo.");
			}

			BigDecimal preco = produto.getPreco();
			if (preco == null || preco.compareTo(ZERO) < 0) {
				throw new IllegalArgumentException("Preço do produto deve ser maior ou igual a zero.");
			}

			if (produto.getPesoFisico() == null) {
				throw new IllegalArgumentException("Peso físico do produto não pode ser nulo.");
			}
			if (produto.getComprimento() == null || produto.getLargura() == null || produto.getAltura() == null) {
				throw new IllegalArgumentException("Dimensões do produto não podem ser nulas.");
			}
			if (produto.isFragil() == null) {
				throw new IllegalArgumentException("Flag fragil do produto não pode ser nula.");
			}

			validarNaoNegativo(produto.getPesoFisico(), "Peso físico do produto deve ser maior ou igual a zero.");
			validarNaoNegativo(produto.getComprimento(), "Comprimento do produto deve ser maior ou igual a zero.");
			validarNaoNegativo(produto.getLargura(), "Largura do produto deve ser maior ou igual a zero.");
			validarNaoNegativo(produto.getAltura(), "Altura do produto deve ser maior ou igual a zero.");
		}
	}

	private void validarNaoNegativo(BigDecimal valor, String mensagem) {
		if (valor != null && valor.compareTo(ZERO) < 0) {
			throw new IllegalArgumentException(mensagem);
		}
	}

	private static BigDecimal max(BigDecimal a, BigDecimal b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		return a.compareTo(b) >= 0 ? a : b;
	}
}
