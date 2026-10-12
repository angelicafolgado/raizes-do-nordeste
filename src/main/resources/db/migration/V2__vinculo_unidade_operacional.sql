ALTER TABLE usuarios
    ADD COLUMN unidadeId INTEGER REFERENCES unidades(id);

CREATE INDEX IF NOT EXISTS usuarios_unidadeId_idx
    ON usuarios(unidadeId);
