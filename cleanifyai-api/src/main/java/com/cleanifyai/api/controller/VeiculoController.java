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

import com.cleanifyai.api.dto.veiculo.VeiculoRequest;
import com.cleanifyai.api.dto.veiculo.VeiculoResponse;
import com.cleanifyai.api.service.VeiculoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/veiculos")
public class VeiculoController {

    private final VeiculoService veiculoService;

    public VeiculoController(VeiculoService veiculoService) {
        this.veiculoService = veiculoService;
    }

    @PostMapping
    public ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request) {
        VeiculoResponse response = veiculoService.criar(request);
        return ResponseEntity.created(URI.create("/api/veiculos/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<VeiculoResponse>> listar(@RequestParam(name = "clienteId", required = false) Long clienteId) {
        return ResponseEntity.ok(veiculoService.listar(clienteId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VeiculoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(veiculoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VeiculoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody VeiculoRequest request) {
        return ResponseEntity.ok(veiculoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        veiculoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
