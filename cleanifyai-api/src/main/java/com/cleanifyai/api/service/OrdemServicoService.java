package com.cleanifyai.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Agendamento;
import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.ItemOrdem;
import com.cleanifyai.api.domain.entity.Lancamento;
import com.cleanifyai.api.domain.entity.OrdemServico;
import com.cleanifyai.api.domain.entity.Servico;
import com.cleanifyai.api.domain.entity.Veiculo;
import com.cleanifyai.api.domain.enums.FormaPagamento;
import com.cleanifyai.api.domain.enums.StatusAgendamento;
import com.cleanifyai.api.domain.enums.StatusOrdem;
import com.cleanifyai.api.domain.enums.TipoLancamento;
import com.cleanifyai.api.dto.ordem.AtualizarStatusOrdemRequest;
import com.cleanifyai.api.dto.ordem.ItemOrdemRequest;
import com.cleanifyai.api.dto.ordem.ItemOrdemResponse;
import com.cleanifyai.api.dto.ordem.OrdemServicoRequest;
import com.cleanifyai.api.dto.ordem.OrdemServicoResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.LancamentoRepository;
import com.cleanifyai.api.repository.OrdemServicoRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class OrdemServicoService {

    private final OrdemServicoRepository ordemRepository;
    private final ClienteService clienteService;
    private final ServicoService servicoService;
    private final AgendamentoService agendamentoService;
    private final VeiculoService veiculoService;
    private final LancamentoRepository lancamentoRepository;

    public OrdemServicoService(
            OrdemServicoRepository ordemRepository,
            ClienteService clienteService,
            ServicoService servicoService,
            AgendamentoService agendamentoService,
            VeiculoService veiculoService,
            LancamentoRepository lancamentoRepository) {
        this.ordemRepository = ordemRepository;
        this.clienteService = clienteService;
        this.servicoService = servicoService;
        this.agendamentoService = agendamentoService;
        this.veiculoService = veiculoService;
        this.lancamentoRepository = lancamentoRepository;
    }

    @Transactional
    public OrdemServicoResponse criar(OrdemServicoRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Veiculo veiculo = veiculoService.buscarEntidade(request.veiculoId());
        validarVeiculoDoCliente(veiculo, cliente);

        OrdemServico ordem = new OrdemServico();
        ordem.setEmpresaId(TenantContext.requireEmpresaId());
        ordem.setCliente(cliente);
        ordem.setVeiculo(veiculo);
        ordem.setStatus(StatusOrdem.ABERTA);
        ordem.setAbertaEm(Instant.now());
        ordem.setObservacoes(normalizarTextoOpcional(request.observacoes()));

        if (request.agendamentoId() != null) {
            Agendamento agendamento = agendamentoService.buscarEntidade(request.agendamentoId());
            validarAgendamentoDaOrdem(agendamento, cliente, veiculo);
            ordem.setAgendamento(agendamento);
        }

        if (request.itens() != null) {
            for (ItemOrdemRequest itemRequest : request.itens()) {
                ordem.adicionarItem(montarItem(itemRequest));
            }
        }

        recalcularValorTotal(ordem);
        return toResponse(ordemRepository.save(ordem));
    }

    @Transactional
    public OrdemServicoResponse criarAPartirDeAgendamento(Long agendamentoId) {
        Long empresaId = TenantContext.requireEmpresaId();
        if (ordemRepository.existsByEmpresaIdAndAgendamentoId(empresaId, agendamentoId)) {
            throw new BusinessException("Este agendamento ja possui OS vinculada");
        }

        Agendamento agendamento = agendamentoService.buscarEntidade(agendamentoId);
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO
                && agendamento.getStatus() != StatusAgendamento.EM_ANDAMENTO) {
            throw new BusinessException("OS deve nascer de um agendamento confirmado ou em execucao");
        }
        if (agendamento.getVeiculo() == null) {
            throw new BusinessException("Agendamento precisa de veiculo vinculado para gerar OS");
        }

        OrdemServico ordem = new OrdemServico();
        ordem.setEmpresaId(empresaId);
        ordem.setCliente(agendamento.getCliente());
        ordem.setVeiculo(agendamento.getVeiculo());
        ordem.setAgendamento(agendamento);
        ordem.setStatus(StatusOrdem.ABERTA);
        ordem.setAbertaEm(Instant.now());
        ordem.setObservacoes(normalizarTextoOpcional(agendamento.getObservacoes()));

        ItemOrdem item = new ItemOrdem();
        item.setServico(agendamento.getServico());
        item.setDescricao(agendamento.getServico().getNome());
        item.setQuantidade(1);
        item.setValorUnitario(agendamento.getServico().getPreco().setScale(2, RoundingMode.HALF_UP));
        item.setValorTotal(item.calcularValorTotal().setScale(2, RoundingMode.HALF_UP));
        ordem.adicionarItem(item);

        recalcularValorTotal(ordem);
        return toResponse(ordemRepository.save(ordem));
    }

    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar(StatusOrdem status) {
        Long empresaId = TenantContext.requireEmpresaId();
        Sort sort = Sort.by(Sort.Direction.DESC, "abertaEm");

        List<OrdemServico> ordens = status != null
                ? ordemRepository.findAllByEmpresaIdAndStatus(empresaId, status, sort)
                : ordemRepository.findAllByEmpresaId(empresaId, sort);

        return ordens.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrdemServicoResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public OrdemServicoResponse atualizar(Long id, OrdemServicoRequest request) {
        OrdemServico ordem = buscarEntidade(id);
        if (!ordem.getStatus().permiteEdicaoDeItens()) {
            throw new BusinessException("Nao e possivel editar uma ordem com status " + ordem.getStatus());
        }

        Cliente cliente = clienteService.buscarEntidade(request.clienteId());
        Veiculo veiculo = veiculoService.buscarEntidade(request.veiculoId());
        validarVeiculoDoCliente(veiculo, cliente);
        ordem.setCliente(cliente);
        ordem.setVeiculo(veiculo);
        ordem.setObservacoes(normalizarTextoOpcional(request.observacoes()));

        if (request.agendamentoId() != null) {
            Agendamento agendamento = agendamentoService.buscarEntidade(request.agendamentoId());
            validarAgendamentoDaOrdem(agendamento, cliente, veiculo);
            ordem.setAgendamento(agendamento);
        } else {
            ordem.setAgendamento(null);
        }

        ordem.getItens().clear();
        if (request.itens() != null) {
            List<ItemOrdem> novos = new ArrayList<>();
            for (ItemOrdemRequest itemRequest : request.itens()) {
                novos.add(montarItem(itemRequest));
            }
            for (ItemOrdem item : novos) {
                ordem.adicionarItem(item);
            }
        }

        recalcularValorTotal(ordem);
        return toResponse(ordemRepository.save(ordem));
    }

    @Transactional
    public OrdemServicoResponse atualizarStatus(Long id, AtualizarStatusOrdemRequest request) {
        OrdemServico ordem = buscarEntidade(id);
        validarTransicaoStatus(ordem.getStatus(), request.status());
        aplicarNovoStatus(ordem, request.status());
        OrdemServico ordemSalva = ordemRepository.save(ordem);
        registrarReceitaSeEntregue(ordemSalva);
        return toResponse(ordemSalva);
    }

    @Transactional
    public OrdemServicoResponse cancelar(Long id) {
        OrdemServico ordem = buscarEntidade(id);
        validarTransicaoStatus(ordem.getStatus(), StatusOrdem.CANCELADA);
        aplicarNovoStatus(ordem, StatusOrdem.CANCELADA);
        return toResponse(ordemRepository.save(ordem));
    }

    @Transactional(readOnly = true)
    public OrdemServico buscarEntidade(Long id) {
        return ordemRepository.findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de servico nao encontrada: " + id));
    }

    private ItemOrdem montarItem(ItemOrdemRequest request) {
        Servico servico = servicoService.buscarEntidade(request.servicoId());
        ItemOrdem item = new ItemOrdem();
        item.setServico(servico);
        item.setDescricao(montarDescricao(request, servico));
        item.setQuantidade(request.quantidade());
        item.setValorUnitario(request.valorUnitario().setScale(2, RoundingMode.HALF_UP));
        item.setValorTotal(item.calcularValorTotal().setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    private String montarDescricao(ItemOrdemRequest request, Servico servico) {
        String descricao = normalizarTextoOpcional(request.descricao());
        return descricao != null ? descricao : servico.getNome();
    }

    private void recalcularValorTotal(OrdemServico ordem) {
        BigDecimal total = ordem.getItens().stream()
                .map(ItemOrdem::getValorTotal)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ordem.setValorTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private void aplicarNovoStatus(OrdemServico ordem, StatusOrdem destino) {
        ordem.setStatus(destino);
        if (destino.ehFinal() || destino == StatusOrdem.CONCLUIDA) {
            if (ordem.getFechadaEm() == null) {
                ordem.setFechadaEm(Instant.now());
            }
        }
    }

    private void registrarReceitaSeEntregue(OrdemServico ordem) {
        if (ordem.getStatus() != StatusOrdem.ENTREGUE || ordem.getValorTotal() == null || ordem.getValorTotal().signum() <= 0) {
            return;
        }

        Long empresaId = TenantContext.requireEmpresaId();
        boolean jaRegistrado = lancamentoRepository.existsByEmpresaIdAndOrdemIdAndTipo(
                empresaId,
                ordem.getId(),
                TipoLancamento.ENTRADA);
        if (jaRegistrado) {
            return;
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setEmpresaId(empresaId);
        lancamento.setTipo(TipoLancamento.ENTRADA);
        lancamento.setValor(ordem.getValorTotal().setScale(2, RoundingMode.HALF_UP));
        lancamento.setFormaPagamento(FormaPagamento.OUTROS);
        lancamento.setDataLancamento(LocalDate.now());
        lancamento.setDescricao("Recebimento OS #" + ordem.getId() + " - " + ordem.getCliente().getNome());
        lancamento.setOrdemId(ordem.getId());
        lancamentoRepository.save(lancamento);
    }

    private void validarTransicaoStatus(StatusOrdem atual, StatusOrdem destino) {
        if (atual == destino) {
            return;
        }

        boolean valida = switch (atual) {
            case ABERTA -> destino == StatusOrdem.EM_EXECUCAO || destino == StatusOrdem.CANCELADA;
            case EM_EXECUCAO -> destino == StatusOrdem.CONCLUIDA || destino == StatusOrdem.CANCELADA;
            case CONCLUIDA -> destino == StatusOrdem.ENTREGUE || destino == StatusOrdem.CANCELADA;
            case ENTREGUE, CANCELADA -> false;
        };

        if (!valida) {
            throw new BusinessException("Transicao de status invalida para a ordem de servico");
        }
    }

    private void validarVeiculoDoCliente(Veiculo veiculo, Cliente cliente) {
        if (!veiculo.getClienteId().equals(cliente.getId())) {
            throw new BusinessException("Veiculo nao pertence ao cliente informado");
        }
    }

    private void validarAgendamentoDaOrdem(Agendamento agendamento, Cliente cliente, Veiculo veiculo) {
        if (!agendamento.getCliente().getId().equals(cliente.getId())) {
            throw new BusinessException("Agendamento nao pertence ao cliente informado");
        }
        if (agendamento.getVeiculo() == null || !agendamento.getVeiculo().getId().equals(veiculo.getId())) {
            throw new BusinessException("Agendamento nao pertence ao veiculo informado");
        }
    }

    private OrdemServicoResponse toResponse(OrdemServico ordem) {
        List<ItemOrdemResponse> itens = ordem.getItens().stream()
                .map(item -> new ItemOrdemResponse(
                        item.getId(),
                        item.getServico().getId(),
                        item.getServico().getNome(),
                        item.getDescricao(),
                        item.getQuantidade(),
                        item.getValorUnitario(),
                        item.getValorTotal()))
                .toList();

        Veiculo veiculo = ordem.getVeiculo();
        String veiculoDescricao = veiculo != null
                ? (veiculo.getMarca() + " " + veiculo.getModelo()).trim()
                : null;

        return new OrdemServicoResponse(
                ordem.getId(),
                ordem.getCliente().getId(),
                ordem.getCliente().getNome(),
                veiculo != null ? veiculo.getId() : null,
                veiculoDescricao,
                veiculo != null ? veiculo.getPlaca() : null,
                ordem.getAgendamento() != null ? ordem.getAgendamento().getId() : null,
                ordem.getStatus(),
                ordem.getValorTotal(),
                ordem.getAbertaEm(),
                ordem.getFechadaEm(),
                ordem.getObservacoes(),
                itens);
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().replaceAll("\\s{2,}", " ");
        return normalizado.isBlank() ? null : normalizado;
    }
}
