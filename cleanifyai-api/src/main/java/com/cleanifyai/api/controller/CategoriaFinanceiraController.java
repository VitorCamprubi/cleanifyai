package com.cleanifyai.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.domain.enums.TipoCategoria;
import com.cleanifyai.api.dto.financeiro.CategoriaFinanceiraRequest;
import com.cleanifyai.api.dto.financeiro.CategoriaFinanceiraResponse;
import com.cleanifyai.api.service.CategoriaFinanceiraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/financeiro/categorias")
public class CategoriaFinanceiraController {

    private final CategoriaFinanceiraService service;

    public CategoriaFinanceiraController(CategoriaFinanceiraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CategoriaFinanceiraResponse> criar(@Valid @RequestBody CategoriaFinanceiraRequest request) {
        CategoriaFinanceiraResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/api/financeiro/categorias/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaFinanceiraResponse>> listar(@RequestParam(name = "tipo", required = false) TipoCategoria tipo) {
        return ResponseEntity.ok(service.listar(tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaFinanceiraResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaFinanceiraResponse> atualizar(@PathVariable Long id, @Valid @RequestBody CategoriaFinanceiraRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
