package com.cleanifyai.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.CategoriaFinanceira;
import com.cleanifyai.api.domain.entity.Lancamento;
import com.cleanifyai.api.domain.entity.OrdemServico;
import com.cleanifyai.api.domain.enums.FormaPagamento;
import com.cleanifyai.api.domain.enums.TipoLancamento;
import com.cleanifyai.api.dto.financeiro.LancamentoRequest;
import com.cleanifyai.api.dto.financeiro.LancamentoResponse;
import com.cleanifyai.api.dto.financeiro.ResumoFinanceiroResponse;
import com.cleanifyai.api.dto.financeiro.TotalPorFormaResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class FinanceiroService {

    private final LancamentoRepository lancamentoRepository;
    private final OrdemServicoService ordemServicoService;
    private final CategoriaFinanceiraService categoriaService;

    public FinanceiroService(
            LancamentoRepository lancamentoRepository,
            OrdemServicoService ordemServicoService,
            CategoriaFinanceiraService categoriaService) {
        this.lancamentoRepository = lancamentoRepository;
        this.ordemServicoService = ordemServicoService;
        this.categoriaService = categoriaService;
    }

    @Transactional
    public LancamentoResponse registrar(LancamentoRequest request) {
        validarValor(request.valor());
        validarData(request.dataLancamento());

        Lancamento lancamento = new Lancamento();
        lancamento.setEmpresaId(TenantContext.requireEmpresaId());
        lancamento.setTipo(request.tipo());
        lancamento.setValor(request.valor().setScale(2, RoundingMode.HALF_UP));
        lancamento.setFormaPagamento(request.formaPagamento());
        lancamento.setDataLancamento(request.dataLancamento());
        lancamento.setDescricao(normalizarTexto(request.descricao()));

        if (request.ordemId() != null) {
            OrdemServico ordem = ordemServicoService.buscarEntidade(request.ordemId());
            lancamento.setOrdemId(ordem.getId());
        }

        if (request.categoriaId() != null) {
            CategoriaFinanceira categoria = categoriaService.buscarEntidade(request.categoriaId());
            if (!categoria.getTipo().aceitaTipoLancamento(request.tipo())) {
                throw new BusinessException("Categoria " + categoria.getNome() + " nao aceita lancamentos do tipo " + request.tipo());
            }
            lancamento.setCategoriaId(categoria.getId());
        }

        return toResponse(lancamentoRepository.save(lancamento));
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listar(LocalDate inicio, LocalDate fim) {
        LocalDate de = inicio != null ? inicio : LocalDate.now();
        LocalDate ate = fim != null ? fim : de;
        if (ate.isBefore(de)) {
            throw new BusinessException("Periodo invalido: data final anterior a inicial");
        }

        return lancamentoRepository
                .findAllByEmpresaIdAndDataLancamentoBetween(
                        TenantContext.requireEmpresaId(),
                        de,
                        ate,
                        Sort.by(Sort.Direction.DESC, "dataLancamento", "registradoEm"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiroResponse resumo(LocalDate inicio, LocalDate fim) {
        LocalDate de = inicio != null ? inicio : LocalDate.now();
        LocalDate ate = fim != null ? fim : de;
        if (ate.isBefore(de)) {
            throw new BusinessException("Periodo invalido: data final anterior a inicial");
        }

        List<Lancamento> lancamentos = lancamentoRepository
                .findAllByEmpresaIdAndDataLancamentoBetween(
                        TenantContext.requireEmpresaId(),
                        de,
                        ate,
                        Sort.by(Sort.Direction.ASC, "dataLancamento"));

        BigDecimal totalEntradas = BigDecimal.ZERO;
        BigDecimal totalSaidas = BigDecimal.ZERO;

        Map<FormaPagamento, BigDecimal[]> porForma = new EnumMap<>(FormaPagamento.class);
        for (FormaPagamento forma : FormaPagamento.values()) {
            porForma.put(forma, new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO });
        }

        for (Lancamento lancamento : lancamentos) {
            BigDecimal[] acc = porForma.get(lancamento.getFormaPagamento());
            if (lancamento.getTipo() == TipoLancamento.ENTRADA) {
                totalEntradas = totalEntradas.add(lancamento.getValor());
                acc[0] = acc[0].add(lancamento.getValor());
            } else {
                totalSaidas = totalSaidas.add(lancamento.getValor());
                acc[1] = acc[1].add(lancamento.getValor());
            }
        }

        List<TotalPorFormaResponse> porFormaResponse = new ArrayList<>();
        for (FormaPagamento forma : FormaPagamento.values()) {
            BigDecimal[] acc = porForma.get(forma);
            if (acc[0].signum() == 0 && acc[1].signum() == 0) {
                continue;
            }
            porFormaResponse.add(new TotalPorFormaResponse(
                    forma,
                    acc[0].setScale(2, RoundingMode.HALF_UP),
                    acc[1].setScale(2, RoundingMode.HALF_UP)));
        }

        return new ResumoFinanceiroResponse(
                de,
                ate,
                totalEntradas.setScale(2, RoundingMode.HALF_UP),
                totalSaidas.setScale(2, RoundingMode.HALF_UP),
                totalEntradas.subtract(totalSaidas).setScale(2, RoundingMode.HALF_UP),
                lancamentos.size(),
                porFormaResponse);
    }

    @Transactional
    public void estornar(Long id) {
        Lancamento lancamento = lancamentoRepository
                .findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Lancamento nao encontrado: " + id));

        if (!lancamento.getDataLancamento().equals(LocalDate.now())) {
            throw new BusinessException("Estorno permitido apenas no mesmo dia do lancamento");
        }

        lancamentoRepository.delete(lancamento);
    }

    private void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new BusinessException("Valor deve ser maior que zero");
        }
    }

    private void validarData(LocalDate data) {
        if (data == null) {
            throw new BusinessException("Data e obrigatoria");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new BusinessException("Nao e permitido lancar em data futura");
        }
    }

    private String normalizarTexto(String valor) {
        return valor.trim().replaceAll("\\s{2,}", " ");
    }

    private LancamentoResponse toResponse(Lancamento lancamento) {
        String categoriaNome = null;
        String categoriaCor = null;
        if (lancamento.getCategoriaId() != null) {
            try {
                CategoriaFinanceira c = categoriaService.buscarEntidadeIncluindoInativos(lancamento.getCategoriaId());
                categoriaNome = c.getNome();
                categoriaCor = c.getCor();
            } catch (Exception ignorada) {
                // Categoria removida fisicamente: lancamento mantem o id mas sem dados resolvidos.
            }
        }

        return new LancamentoResponse(
                lancamento.getId(),
                lancamento.getTipo(),
                lancamento.getValor(),
                lancamento.getFormaPagamento(),
                lancamento.getDataLancamento(),
                lancamento.getDescricao(),
                lancamento.getOrdemId(),
                lancamento.getCategoriaId(),
                categoriaNome,
                categoriaCor,
                lancamento.getRegistradoEm());
    }
}
