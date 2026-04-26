package com.cleanifyai.api.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanifyai.api.domain.entity.CategoriaFinanceira;
import com.cleanifyai.api.domain.enums.TipoCategoria;
import com.cleanifyai.api.dto.financeiro.CategoriaFinanceiraRequest;
import com.cleanifyai.api.dto.financeiro.CategoriaFinanceiraResponse;
import com.cleanifyai.api.exception.BusinessException;
import com.cleanifyai.api.exception.ResourceNotFoundException;
import com.cleanifyai.api.repository.CategoriaFinanceiraRepository;
import com.cleanifyai.api.shared.tenant.TenantContext;

@Service
public class CategoriaFinanceiraService {

    private final CategoriaFinanceiraRepository repository;

    public CategoriaFinanceiraService(CategoriaFinanceiraRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CategoriaFinanceiraResponse criar(CategoriaFinanceiraRequest request) {
        Long empresaId = TenantContext.requireEmpresaId();
        String nome = normalizar(request.nome());

        if (repository.existsByEmpresaIdAndNomeAndAtivoTrue(empresaId, nome)) {
            throw new BusinessException("Ja existe uma categoria com este nome");
        }

        CategoriaFinanceira categoria = new CategoriaFinanceira();
        categoria.setEmpresaId(empresaId);
        categoria.setNome(nome);
        categoria.setTipo(request.tipo());
        categoria.setCor(normalizarCor(request.cor()));
        categoria.setAtivo(true);
        return toResponse(repository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaFinanceiraResponse> listar(TipoCategoria filtro) {
        Long empresaId = TenantContext.requireEmpresaId();
        Sort sort = Sort.by(Sort.Direction.ASC, "nome");

        List<CategoriaFinanceira> categorias;
        if (filtro == null) {
            categorias = repository.findAllByEmpresaIdAndAtivoTrue(empresaId, sort);
        } else if (filtro == TipoCategoria.AMBOS) {
            categorias = repository.findAllByEmpresaIdAndAtivoTrue(empresaId, sort);
        } else {
            // Inclui categorias do tipo solicitado + categorias AMBOS
            categorias = repository.findAllByEmpresaIdAndTipoInAndAtivoTrue(empresaId, List.of(filtro, TipoCategoria.AMBOS), sort);
        }

        return categorias.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaFinanceiraResponse buscarPorId(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public CategoriaFinanceiraResponse atualizar(Long id, CategoriaFinanceiraRequest request) {
        CategoriaFinanceira categoria = buscarEntidade(id);
        categoria.setNome(normalizar(request.nome()));
        categoria.setTipo(request.tipo());
        categoria.setCor(normalizarCor(request.cor()));
        return toResponse(repository.save(categoria));
    }

    @Transactional
    public void excluir(Long id) {
        CategoriaFinanceira categoria = buscarEntidade(id);
        categoria.setAtivo(false);
        repository.save(categoria);
    }

    @Transactional(readOnly = true)
    public CategoriaFinanceira buscarEntidade(Long id) {
        return repository.findByIdAndEmpresaIdAndAtivoTrue(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public CategoriaFinanceira buscarEntidadeIncluindoInativos(Long id) {
        return repository.findByIdAndEmpresaId(id, TenantContext.requireEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada: " + id));
    }

    private CategoriaFinanceiraResponse toResponse(CategoriaFinanceira c) {
        return new CategoriaFinanceiraResponse(c.getId(), c.getNome(), c.getTipo(), c.getCor());
    }

    private String normalizar(String valor) {
        return valor.trim().replaceAll("\\s{2,}", " ");
    }

    private String normalizarCor(String cor) {
        if (cor == null || cor.isBlank()) {
            return null;
        }
        String c = cor.trim();
        if (!c.startsWith("#")) {
            c = "#" + c;
        }
        return c.toUpperCase();
    }
}
