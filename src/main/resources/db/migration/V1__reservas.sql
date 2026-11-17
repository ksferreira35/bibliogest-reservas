CREATE TABLE reserva (
    id              UUID         PRIMARY KEY,
    leitor_id       VARCHAR(64)  NOT NULL,
    obra_id         VARCHAR(64)  NOT NULL,
    exemplar_id     VARCHAR(64)  NOT NULL,
    estado          VARCHAR(20)  NOT NULL,
    criada_em       TIMESTAMPTZ  NOT NULL,
    retirada_em     TIMESTAMPTZ,
    finalizada_em   TIMESTAMPTZ,
    cancelada_em    TIMESTAMPTZ,
    versao          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('AGUARDANDO_RETIRADA', 'RETIRADA', 'FINALIZADA', 'CANCELADA'))
);

-- No máximo uma reserva ativa (AGUARDANDO_RETIRADA ou RETIRADA) por exemplar.
CREATE UNIQUE INDEX ux_reserva_exemplar_ativa
    ON reserva (exemplar_id)
    WHERE estado IN ('AGUARDANDO_RETIRADA', 'RETIRADA');

CREATE INDEX ix_reserva_leitor ON reserva (leitor_id, criada_em DESC);
CREATE INDEX ix_reserva_estado ON reserva (estado, criada_em DESC);
