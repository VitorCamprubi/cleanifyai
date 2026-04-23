package com.cleanifyai.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.dto.servico.ServicoRequest;
import com.cleanifyai.api.dto.servico.ServicoResponse;
import com.cleanifyai.api.service.ServicoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicos")
@Validated
public class ServicoController {

    private final ServicoService servicoService;

    public ServicoController(ServicoService servicoService) {
        this.servicoService = servicoService;
    }

    @PostMapping
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<ServicoResponse>> listar() {
        return ResponseEntity.ok(servicoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServicoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}

