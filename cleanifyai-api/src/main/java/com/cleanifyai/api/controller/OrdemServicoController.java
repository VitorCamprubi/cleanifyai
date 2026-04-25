package com.cleanifyai.api.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.domain.enums.StatusOrdem;
import com.cleanifyai.api.dto.ordem.AtualizarStatusOrdemRequest;
import com.cleanifyai.api.dto.ordem.OrdemServicoRequest;
import com.cleanifyai.api.dto.ordem.OrdemServicoResponse;
import com.cleanifyai.api.service.OrdemServicoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ordens")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(OrdemServicoService ordemServicoService) {
        this.ordemServicoService = ordemServicoService;
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody OrdemServicoRequest request) {
        OrdemServicoResponse response = ordemServicoService.criar(request);
        return ResponseEntity.created(URI.create("/api/ordens/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrdemServicoResponse>> listar(@RequestParam(name = "status", required = false) StatusOrdem status) {
        return ResponseEntity.ok(ordemServicoService.listar(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdemServicoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrdemServicoRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrdemServicoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusOrdemRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizarStatus(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<OrdemServicoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.cancelar(id));
    }
}
