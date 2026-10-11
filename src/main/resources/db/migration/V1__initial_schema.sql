CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    senha TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'CLIENTE',
    consentimentoLGPD BOOLEAN NOT NULL DEFAULT FALSE,
    criadoEm TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS unidades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    cidade TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS produtos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    descricao TEXT NOT NULL,
    preco REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS estoques_unidades (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    unidadeId INTEGER NOT NULL REFERENCES unidades(id),
    produtoId INTEGER NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL DEFAULT 0,
    UNIQUE(unidadeId, produtoId)
);

CREATE TABLE IF NOT EXISTS pedidos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuarioId INTEGER NOT NULL REFERENCES usuarios(id),
    unidadeId INTEGER NOT NULL REFERENCES unidades(id),
    canalPedido TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    total REAL NOT NULL DEFAULT 0,
    criadoEm TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizadoEm TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS itens_pedido (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedidoId INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produtoId INTEGER NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL,
    precoUnitario REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS pagamentos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pedidoId INTEGER NOT NULL UNIQUE REFERENCES pedidos(id),
    valor REAL NOT NULL,
    status TEXT NOT NULL,
    metodo TEXT NOT NULL DEFAULT 'MOCK_EXTERNAL',
    transacaoId TEXT NOT NULL,
    criadoEm TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    acao TEXT NOT NULL,
    detalhes TEXT NOT NULL,
    usuarioId INTEGER REFERENCES usuarios(id) ON DELETE SET NULL,
    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consentimentos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    usuarioId INTEGER NOT NULL REFERENCES usuarios(id),
    finalidade TEXT NOT NULL,
    versaoAviso TEXT NOT NULL,
    aceito BOOLEAN NOT NULL,
    demonstracao BOOLEAN NOT NULL DEFAULT TRUE,
    registradoEm TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS pedidos_usuario_criadoEm_idx
    ON pedidos(usuarioId, criadoEm DESC, id DESC);
CREATE INDEX IF NOT EXISTS pedidos_canal_criadoEm_idx
    ON pedidos(canalPedido, criadoEm DESC, id DESC);
CREATE INDEX IF NOT EXISTS itens_pedido_pedidoId_idx
    ON itens_pedido(pedidoId, id);
CREATE INDEX IF NOT EXISTS audit_logs_acao_id_idx
    ON audit_logs(acao, id DESC);
CREATE INDEX IF NOT EXISTS consentimentos_usuarioId_finalidade_id_idx
    ON consentimentos(usuarioId, finalidade, id);
