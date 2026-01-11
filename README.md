# Projeto E-commerce

## 1) Objetivo do trabalho

- Implementar e testar o serviço `CompraService`.
- Aplicar técnicas de teste: **partição de equivalência**, **valores-limite**, **tabela de decisão**, **robustez** e **caixa-branca**.
- Medir **cobertura** (JaCoCo) e **qualidade dos testes** por **mutação** (PITest).

---

## 2) Funcionalidades e regras de negócio implementadas

### 2.1 Finalização de compra (`finalizarCompra`)

Fluxo:

1. Busca `Cliente` e `CarrinhoDeCompras`.
2. Monta listas `produtosIds` e `produtosQtds`.
3. Verifica disponibilidade em estoque (`IEstoqueExternal.verificarDisponibilidade`).
4. Calcula custo total (`calcularCustoTotal`).
5. Autoriza pagamento (`IPagamentoExternal.autorizarPagamento`).
6. Dá baixa no estoque (`IEstoqueExternal.darBaixa`).
7. Em falha na baixa, cancela pagamento (`IPagamentoExternal.cancelarPagamento`) e lança exceção.
8. Retorna `CompraDTO` de sucesso.

Regras:

- Se `disponibilidade.disponivel() == false` → lança `IllegalStateException("Itens fora de estoque.")`
- Se `pagamento.autorizado() == false` → lança `IllegalStateException("Pagamento não autorizado.")`
- Se `baixaDTO.sucesso() == false` → cancela pagamento e lança `IllegalStateException("Erro ao dar baixa no estoque.")`

---

### 2.2 Cálculo de custo total (`calcularCustoTotal`)

`total = (subtotalComDescontos + freteFinal)` com **2 casas decimais**.

Pré-condições (validação de entrada):

- `CarrinhoDeCompras` não pode ser nulo, precisa ter itens.
- Cada `ItemCompra` deve ter `produto` não nulo e `quantidade > 0`.
- `Produto` deve ter preço `>= 0`, dimensões e peso não nulos e não negativos, tipo não nulo e flag `fragil` não nula.
- `Cliente` não pode ser nulo, e deve ter `regiao` e `tipo` não nulos.

---

### 2.3 Descontos (`calcularCustoProdutos`)

O desconto é aplicado em duas etapas:

1. **Desconto por tipo (quantidade por `TipoProduto`)**

   - `>= 8` itens do tipo → **15%**
   - `>= 5` itens do tipo → **10%**
   - `>= 3` itens do tipo → **5%**
   - caso contrário → **0%**

2. **Desconto por valor total do carrinho** (**estritamente maior**, conforme enunciado)
   - `total > 1000` → **20%**
   - `total > 500` e `total <= 1000` → **10%**
   - caso contrário → **0%**

---

### 2.4 Frete (`calcularFreteFinal`)

Etapas:

1. Calcula peso total da compra (por item e quantidade).
2. Calcula frete por faixa de peso:
   - `<= 5kg` → frete `0`
   - `<= 10kg` → `peso * 2.00 + TAXA_MINIMA_FRETE (12.00)`
   - `<= 50kg` → `peso * 4.00 + TAXA_MINIMA_FRETE (12.00)`
   - `> 50kg` → `peso * 7.00 + TAXA_MINIMA_FRETE (12.00)`
3. Taxa frágil: `5.00` por unidade marcada como `fragil`.
4. Multiplicador por região do cliente:
   - **SUL** → `1.05`
   - **NORDESTE** → `1.10`
   - **CENTRO_OESTE** → `1.20`
   - **NORTE** → `1.30`
   - **SUDESTE** → `1.00`
5. Fidelidade (aplicada ao frete final):
   - **OURO** → frete **0**
   - **PRATA** → **50%** do frete
   - **BRONZE** → **100%** do frete

Peso tributável do produto:

- `pesoTributavel = max(pesoFisico, pesoCubico)`
- `pesoCubico = (comprimento * largura * altura) / 6000` (escala 10)
- Dimensões em cm e peso em kg.

---

## 3) Tecnologias e ferramentas

- Java
- Maven
- JUnit 5 (JUnit Jupiter)
- Mockito
- AssertJ
- JaCoCo
- PITest

---

## 4) Como executar o projeto

### 4.1 Rodar testes

```bash
mvn clean test
```

### 4.2 Gerar relatório de cobertura (JaCoCo)

```bash
mvn clean test jacoco:report
```

Relatório (padrão):

- `target/site/jacoco/index.html`

### 4.3 Rodar mutation testing (PITest)

```bash
mvn clean test pitest:mutationCoverage
```

Relatório (padrão):

- `target/pit-reports/**/index.html`

---

## 5) Estratégia de testes

Os testes foram estruturados para cobrir:

- **Cenários de sucesso** e **falhas** do fluxo de compra (estoque, pagamento e baixa).
- **Partições de equivalência** (faixas de frete, faixas de desconto).
- **Valores-limite** (transições: 5.00→5.01, 10.00→10.01, 50.00→50.01, 500.00→500.01, 1000.00→1000.01, etc.).
- **Tabela de decisão** (regras de desconto e frete).
- **Robustez** (carrinho nulo, carrinho vazio, item nulo, quantidade inválida, dimensões nulas/negativas, preço nulo/negativo, cliente nulo/região nula/tipo nulo, etc.).


---

## 6) Resultados de qualidade (Cobertura e Mutação)

### 6.1 Cobertura (JaCoCo)

- Cobertura atingida conforme execução local (ver relatório do JaCoCo).
- Resultado observado: **~94%**.

### 6.2 Mutation Score (PITest)

- Mutation Score atual: **94%** (conforme relatório apresentado).

#### Justificativa

- Existirem mutações **sem impacto real** no comportamento observável do caso de uso final (mutantes equivalentes/próximos disso).
- Alguns ramos serem **defensivos** e/ou **não acionados** dentro do fluxo válido do domínio.

---
## 7) Arquivos Auxiliares
- Para a realização dos testes foi criado um arquivo em Excel que reúne as tabelas utilizadas para balizar os testes, onde foi descrito as partições, limites, tabela de decisão e cobertura MC/DC. O arquivo está na raiz do projeto e se chama `tabelas_teste_ecommerce.xls`

---
## 8) Autoria

- Aluno: **Eduardo de Assis Oliveira**
- Disciplina: **Teste de Software**
- Professor: **Eiji Adachi Medeiros Barbosa**
- Instituição: **Universidade Federal do Rio Grande do Norte**
