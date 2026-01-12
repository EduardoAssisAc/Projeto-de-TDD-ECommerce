package ecommerce.external;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import ecommerce.dto.PagamentoDTO;

@Service
public class PagamentoExternalStub implements IPagamentoExternal {

    @Override
    public PagamentoDTO autorizarPagamento(Long clienteId, Double custoTotal) {
        long transacaoId = ThreadLocalRandom.current().nextLong(1_000_000, 9_999_999);
        return new PagamentoDTO(true, transacaoId);
    }

    @Override
    public void cancelarPagamento(Long clienteId, Long pagamentoTransacaoId) {
    }
}
