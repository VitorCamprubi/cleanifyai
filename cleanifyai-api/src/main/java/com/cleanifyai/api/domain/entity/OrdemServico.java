package com.cleanifyai.api.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.cleanifyai.api.domain.enums.StatusOrdem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "ordens_servico")
public class OrdemServico extends EntidadeTenantBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrdem status;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "aberta_em", nullable = false)
    private Instant abertaEm;

    @Column(name = "fechada_em")
    private Instant fechadaEm;

    @Column(length = 500)
    private String observacoes;

    @OneToMany(mappedBy = "ordem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<ItemOrdem> itens = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (abertaEm == null) {
            abertaEm = Instant.now();
        }
        if (status == null) {
            status = StatusOrdem.ABERTA;
        }
        if (valorTotal == null) {
            valorTotal = BigDecimal.ZERO;
        }
    }

    public void adicionarItem(ItemOrdem item) {
        item.setOrdem(this);
        itens.add(item);
    }

    public void removerItem(ItemOrdem item) {
        itens.remove(item);
        item.setOrdem(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public void setAgendamento(Agendamento agendamento) {
        this.agendamento = agendamento;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public StatusOrdem getStatus() {
        return status;
    }

    public void setStatus(StatusOrdem status) {
        this.status = status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }

    public void setAbertaEm(Instant abertaEm) {
        this.abertaEm = abertaEm;
    }

    public Instant getFechadaEm() {
        return fechadaEm;
    }

    public void setFechadaEm(Instant fechadaEm) {
        this.fechadaEm = fechadaEm;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public List<ItemOrdem> getItens() {
        return itens;
    }

    public void setItens(List<ItemOrdem> itens) {
        this.itens = itens;
    }
}
