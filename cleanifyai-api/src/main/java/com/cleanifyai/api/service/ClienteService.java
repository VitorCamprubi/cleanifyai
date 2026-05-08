package com.cleanifyai.api.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.dto.cliente.ClienteRequest;
import com.cleanifyai.api.dto.cliente.ClienteResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.ClienteRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = new Cliente();
        cliente.setEmpresaId(TenantContext.requireEmpresaId());
        cliente.setAtivo(true);
        preencherCampos(cliente, request);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository
                .findAllByEmpresaIdAndAtivoTrue(TenantContext.requireEmpresaId(), Sort.by(Sort.Direction.ASC, "nome"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        preencherCampos(cliente, request);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public void excluir(Long id) {
        // Soft-delete: marcamos como inativo. Agendamentos e OS historicas continuam validos
        // e podem referenciar este cliente, mas ele desaparece das listagens e novas operacoes.
        Cliente cliente = buscarEntidade(id);
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidade(Long id) {
        return clienteRepository.findByIdAndEmpresaIdAndAtivoTrue(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    /**
     * Para uso interno por OS/Agendamento: localiza o cliente mesmo se estiver inativo,
     * desde que pertenca a empresa atual. Garante que historico mantenha a referencia
     * legivel.
     */
    @Transactional(readOnly = true)
    public Cliente buscarEntidadeIncluindoInativos(Long id) {
        return clienteRepository.findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    private void preencherCampos(Cliente cliente, ClienteRequest request) {
        cliente.setNome(normalizarNome(request.nome()));
        cliente.setTelefone(normalizarTelefone(request.telefone()));
        cliente.setEmail(normalizarEmail(request.email()));
        cliente.setObservacoes(normalizarTextoOpcional(request.observacoes()));
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getObservacoes());
    }

    private String normalizarNome(String nome) {
        return nome.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");

        if (digitos.length() == 10 || digitos.length() == 11) {
            digitos = "55" + digitos;
        }

        if (digitos.length() < 12 || digitos.length() > 13) {
            throw new BusinessException("Telefone deve conter DDD e numero validos");
        }

        return digitos;
    }

    private String normalizarEmail(String email) {
        String valor = normalizarTextoOpcional(email);
        return valor != null ? valor.toLowerCase() : null;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().replaceAll("\\s{2,}", " ");
        return normalizado.isBlank() ? null : normalizado;
    }
}
