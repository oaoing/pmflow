CREATE TABLE equipment (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    period      NUMERIC(10,2) NOT NULL,
    unit        VARCHAR(10)  NOT NULL,          -- UNIT enum 이름
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(50)  NOT NULL,
    modified_at TIMESTAMPTZ  NOT NULL,
    modified_by VARCHAR(50)  NOT NULL
);