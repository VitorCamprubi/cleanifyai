package com.cleanifyai.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cleanifyai.api.domain.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}

