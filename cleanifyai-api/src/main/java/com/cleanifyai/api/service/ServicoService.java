package com.cleanifyai.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.dto.servico.ServicoRequest;
import com.cleanifyai.api.dto.servico.ServicoResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ServicoRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final AgendamentoRepository agendamentoRepository;

    public ServicoService(
            ServicoRepository servicoRepository,
            AgendamentoRepository agendamentoRepository) {
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = new Servico();
        servico.setEmpresaId(TenantContext.requireEmpresaId());
        preencherCampos(servico, request);
        return toResponse(servicoRepository.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoResponse> listar() {
        return servicoRepository.findAllByEmpresaId(TenantContext.requireEmpresaId(), Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarEntidade(id);
        preencherCampos(servico, request);
        return toResponse(servicoRepository.save(servico));
    }

    @Transactional
    public void excluir(Long id) {
        Long empresaId = TenantContext.requireEmpresaId();
        Servico servico = buscarEntidade(id);
        if (agendamentoRepository.existsByEmpresaIdAndServicoId(empresaId, id)) {
            throw new BusinessException("Servico possui agendamentos vinculados e nao pode ser excluido");
        }
        servicoRepository.delete(servico);
    }

    @Transactional(readOnly = true)
    public Servico buscarEntidade(Long id) {
        return servicoRepository.findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Servico nao encontrado: " + id));
    }

    private void preencherCampos(Servico servico, ServicoRequest request) {
        validarValores(request.preco(), request.duracaoMinutos());
        servico.setNome(normalizarTextoObrigatorio(request.nome()));
        servico.setDescricao(normalizarTextoOpcional(request.descricao()));
        servico.setPreco(request.preco().setScale(2, RoundingMode.HALF_UP));
        servico.setDuracaoMinutos(request.duracaoMinutos());
        servico.setAtivo(request.ativo());
    }

    private ServicoResponse toResponse(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getDuracaoMinutos(),
                servico.getAtivo());
    }

    private void validarValores(BigDecimal preco, Integer duracaoMinutos) {
        if (preco == null || preco.signum() <= 0) {
            throw new BusinessException("Preco deve ser maior que zero");
        }

        if (duracaoMinutos == null || duracaoMinutos <= 0) {
            throw new BusinessException("Duracao deve ser maior que zero");
        }
    }

    private String normalizarTextoObrigatorio(String valor) {
        return valor.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().replaceAll("\\s{2,}", " ");
        return normalizado.isBlank() ? null : normalizado;
    }
}
