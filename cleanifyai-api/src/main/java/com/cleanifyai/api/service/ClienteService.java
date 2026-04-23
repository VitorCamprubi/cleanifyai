package com.cleanifyai.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.Cliente;
import com.cleanifyai.api.dto.cliente.ClienteRequest;
import com.cleanifyai.api.dto.cliente.ClienteResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.AgendamentoRepository;
import com.cleanifyai.api.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AgendamentoRepository agendamentoRepository;

    public ClienteService(
            ClienteRepository clienteRepository,
            AgendamentoRepository agendamentoRepository) {
        this.clienteRepository = clienteRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = new Cliente();
        preencherCampos(cliente, request);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return clienteRepository.findAll()
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
        Cliente cliente = buscarEntidade(id);
        if (agendamentoRepository.existsByClienteId(id)) {
            throw new BusinessException("Cliente possui agendamentos vinculados e nao pode ser excluido");
        }
        clienteRepository.delete(cliente);
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado: " + id));
    }

    private void preencherCampos(Cliente cliente, ClienteRequest request) {
        cliente.setNome(normalizarNome(request.nome()));
        cliente.setTelefone(normalizarTelefone(request.telefone()));
        cliente.setEmail(normalizarEmail(request.email()));
        cliente.setVeiculo(normalizarTextoOpcional(request.veiculo()));
        cliente.setPlaca(normalizarPlaca(request.placa()));
        cliente.setObservacoes(normalizarTextoOpcional(request.observacoes()));
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getEmail(),
                cliente.getVeiculo(),
                cliente.getPlaca(),
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

    private String normalizarPlaca(String placa) {
        String valor = normalizarTextoOpcional(placa);
        if (valor == null) {
            return null;
        }

        String normalizada = valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!normalizada.matches("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")) {
            throw new BusinessException("Placa invalida");
        }

        return normalizada;
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null) {
            return null;
        }

        String normalizado = valor.trim().replaceAll("\\s{2,}", " ");
        return normalizado.isBlank() ? null : normalizado;
    }
}

