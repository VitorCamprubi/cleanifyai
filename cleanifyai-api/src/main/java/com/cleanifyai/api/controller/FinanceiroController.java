package com.cleanifyai.api.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cleanifyai.api.dto.financeiro.LancamentoRequest;
import com.cleanifyai.api.dto.financeiro.LancamentoResponse;
import com.cleanifyai.api.dto.financeiro.ResumoFinanceiroResponse;
import com.cleanifyai.api.service.FinanceiroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/financeiro")
public class FinanceiroController {

    private final FinanceiroService financeiroService;

    public FinanceiroController(FinanceiroService financeiroService) {
        this.financeiroService = financeiroService;
    }

    @PostMapping("/lancamentos")
    public ResponseEntity<LancamentoResponse> registrar(@Valid @RequestBody LancamentoRequest request) {
        LancamentoResponse response = financeiroService.registrar(request);
        return ResponseEntity.created(URI.create("/api/financeiro/lancamentos/" + response.id())).body(response);
    }

    @GetMapping("/lancamentos")
    public ResponseEntity<List<LancamentoResponse>> listar(
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(financeiroService.listar(inicio, fim));
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoFinanceiroResponse> resumo(
            @RequestParam(name = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(financeiroService.resumo(inicio, fim));
    }

    @DeleteMapping("/lancamentos/{id}")
    public ResponseEntity<Void> estornar(@PathVariable Long id) {
        financeiroService.estornar(id);
        return ResponseEntity.noContent().build();
    }
}
