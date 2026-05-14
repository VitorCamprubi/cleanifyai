CREATE TABLE IF NOT EXISTS empresas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(160) NOT NULL,
    cnpj VARCHAR(18),
    telefone VARCHAR(20),
    email VARCHAR(160),
    ativa BIT(1) NOT NULL DEFAULT b'1',
    criada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_empresas_cnpj (cnpj)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_empresa (empresa_id),
    CONSTRAINT fk_users_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    email VARCHAR(120),
    observacoes VARCHAR(500),
    ativo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_clientes_empresa_ativo_nome (empresa_id, ativo, nome),
    CONSTRAINT fk_clientes_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS servicos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    preco DECIMAL(10, 2) NOT NULL,
    duracao_minutos INT NOT NULL,
    ativo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_servicos_empresa_ativo_nome (empresa_id, ativo, nome),
    CONSTRAINT fk_servicos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS veiculos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    cliente_id BIGINT NOT NULL,
    marca VARCHAR(60) NOT NULL,
    modelo VARCHAR(80) NOT NULL,
    placa VARCHAR(10),
    cor VARCHAR(30),
    ano_modelo INT,
    observacoes VARCHAR(500),
    ativo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_veiculos_empresa_ativo (empresa_id, ativo),
    KEY idx_veiculos_empresa_cliente_ativo (empresa_id, cliente_id, ativo),
    KEY idx_veiculos_placa (placa),
    CONSTRAINT fk_veiculos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id),
    CONSTRAINT fk_veiculos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agendamentos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    cliente_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    veiculo_id BIGINT,
    data DATE NOT NULL,
    horario TIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    observacoes VARCHAR(500),
    PRIMARY KEY (id),
    KEY idx_agendamentos_empresa_data_status (empresa_id, data, status),
    KEY idx_agendamentos_cliente (cliente_id),
    KEY idx_agendamentos_servico (servico_id),
    KEY idx_agendamentos_veiculo (veiculo_id),
    CONSTRAINT fk_agendamentos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id),
    CONSTRAINT fk_agendamentos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_agendamentos_servico FOREIGN KEY (servico_id) REFERENCES servicos (id),
    CONSTRAINT fk_agendamentos_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ordens_servico (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    cliente_id BIGINT NOT NULL,
    agendamento_id BIGINT,
    veiculo_id BIGINT,
    status VARCHAR(20) NOT NULL,
    valor_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    aberta_em DATETIME(6) NOT NULL,
    fechada_em DATETIME(6),
    observacoes VARCHAR(500),
    PRIMARY KEY (id),
    KEY idx_ordens_empresa_status_aberta (empresa_id, status, aberta_em),
    KEY idx_ordens_cliente (cliente_id),
    KEY idx_ordens_agendamento (agendamento_id),
    KEY idx_ordens_veiculo (veiculo_id),
    CONSTRAINT fk_ordens_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id),
    CONSTRAINT fk_ordens_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_ordens_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamentos (id),
    CONSTRAINT fk_ordens_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS itens_ordem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ordem_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    quantidade INT NOT NULL,
    valor_unitario DECIMAL(12, 2) NOT NULL,
    valor_total DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_itens_ordem_ordem (ordem_id),
    KEY idx_itens_ordem_servico (servico_id),
    CONSTRAINT fk_itens_ordem_ordem FOREIGN KEY (ordem_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_itens_ordem_servico FOREIGN KEY (servico_id) REFERENCES servicos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categorias_financeiras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    nome VARCHAR(80) NOT NULL,
    tipo VARCHAR(12) NOT NULL,
    cor VARCHAR(9),
    ativo BIT(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (id),
    KEY idx_categorias_empresa_ativo_nome (empresa_id, ativo, nome),
    KEY idx_categorias_empresa_tipo_ativo (empresa_id, tipo, ativo),
    CONSTRAINT fk_categorias_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS lancamentos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id BIGINT NOT NULL,
    criado_em DATETIME(6),
    atualizado_em DATETIME(6),
    tipo VARCHAR(10) NOT NULL,
    valor DECIMAL(12, 2) NOT NULL,
    forma_pagamento VARCHAR(12) NOT NULL,
    data_lancamento DATE NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    ordem_id BIGINT,
    categoria_id BIGINT,
    registrado_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lancamentos_empresa_data (empresa_id, data_lancamento),
    KEY idx_lancamentos_empresa_ordem_tipo (empresa_id, ordem_id, tipo),
    KEY idx_lancamentos_categoria (categoria_id),
    CONSTRAINT fk_lancamentos_empresa FOREIGN KEY (empresa_id) REFERENCES empresas (id),
    CONSTRAINT fk_lancamentos_ordem FOREIGN KEY (ordem_id) REFERENCES ordens_servico (id),
    CONSTRAINT fk_lancamentos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_financeiras (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
