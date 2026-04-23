package com.cleanifyai.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.dto.agendamento.AgendamentoRequest;
import com.cleanifyai.api.dto.agendamento.AgendamentoResponse;
import com.cleanifyai.api.dto.agendamento.AtualizarStatusAgendamentoRequest;
import com.cleanifyai.api.service.AgendamentoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/agendamentos")
@Validated
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> listar() {
        return ResponseEntity.ok(agendamentoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AgendamentoResponse> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusAgendamentoRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizarStatus(id, request));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.cancelar(id));
    }
}

