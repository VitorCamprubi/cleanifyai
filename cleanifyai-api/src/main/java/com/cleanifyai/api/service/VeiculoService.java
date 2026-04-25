package com.cleanifyai.api.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.domain.entity.Veiculo;
import com.cleanifyai.api.dto.veiculo.VeiculoRequest;
import com.cleanifyai.api.dto.veiculo.VeiculoResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.VeiculoRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class VeiculoService {

    private final VeiculoRepository veiculoRepository;
    private final ClienteService clienteService;

    public VeiculoService(VeiculoRepository veiculoRepository, ClienteService clienteService) {
        this.veiculoRepository = veiculoRepository;
        this.clienteService = clienteService;
    }

    @Transactional
    public VeiculoResponse criar(VeiculoRequest request) {
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());

        Veiculo veiculo = new Veiculo();
        veiculo.setEmpresaId(TenantContext.requireEmpresaId());
        veiculo.setClienteId(cliente.getId());
        veiculo.setAtivo(true);
        preencherCampos(veiculo, request);
        return toResponse(veiculoRepository.save(veiculo), cliente);
    }

    @Transactional(readOnly = true)
    public List<VeiculoResponse> listar(Long clienteId) {
        Long empresaId = TenantContext.requireEmpresaId();
        Sort sort = Sort.by(Sort.Direction.ASC, "marca", "modelo");

        List<Veiculo> veiculos = clienteId != null
                ? veiculoRepository.findAllByEmpresaIdAndClienteIdAndAtivoTrue(empresaId, clienteId, sort)
                : veiculoRepository.findAllByEmpresaIdAndAtivoTrue(empresaId, sort);

        return veiculos.stream()
                .map(v -> toResponse(v, clienteService.buscarEntidadeIncluindoInativos(v.getClienteId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        Cliente cliente = clienteService.buscarEntidadeIncluindoInativos(veiculo.getClienteId());
        return toResponse(veiculo, cliente);
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Veiculo veiculo = buscarEntidade(id);
        Cliente cliente = clienteService.buscarEntidade(request.clienteId());

        if (!veiculo.getClienteId().equals(cliente.getId())) {
            // Permitir transferencia explicita de veiculo entre clientes (caso real: revenda).
            veiculo.setClienteId(cliente.getId());
        }

        preencherCampos(veiculo, request);
        return toResponse(veiculoRepository.save(veiculo), cliente);
    }

    @Transactional
    public void excluir(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        veiculo.setAtivo(false);
        veiculoRepository.save(veiculo);
    }

    @Transactional(readOnly = true)
    public Veiculo buscarEntidade(Long id) {
        return veiculoRepository.findByIdAndEmpresaIdAndAtivoTrue(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Veiculo buscarEntidadeIncluindoInativos(Long id) {
        return veiculoRepository.findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Veiculo nao encontrado: " + id));
    }

    private void preencherCampos(Veiculo veiculo, VeiculoRequest request) {
        veiculo.setMarca(normalizarObrigatorio(request.marca()));
        veiculo.setModelo(normalizarObrigatorio(request.modelo()));
        veiculo.setPlaca(normalizarPlaca(request.placa()));
        veiculo.setCor(normalizarOpcional(request.cor()));
        veiculo.setAnoModelo(request.anoModelo());
        veiculo.setObservacoes(normalizarOpcional(request.observacoes()));
    }

    private VeiculoResponse toResponse(Veiculo veiculo, Cliente cliente) {
        return new VeiculoResponse(
                veiculo.getId(),
                veiculo.getClienteId(),
                cliente != null ? cliente.getNome() : null,
                veiculo.getMarca(),
                veiculo.getModelo(),
                veiculo.getPlaca(),
                veiculo.getCor(),
                veiculo.getAnoModelo(),
                veiculo.getObservacoes());
    }

    private String normalizarObrigatorio(String valor) {
        return valor.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String n = valor.trim().replaceAll("\\s{2,}", " ");
        return n.isBlank() ? null : n;
    }

    private String normalizarPlaca(String placa) {
        String valor = normalizarOpcional(placa);
        if (valor == null) {
            return null;
        }
        String normalizada = valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!normalizada.matches("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")) {
            throw new BusinessException("Placa invalida");
        }
        return normalizada;
    }
}
