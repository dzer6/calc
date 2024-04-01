CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE calc_history
(
    id         UUID PRIMARY KEY         DEFAULT uuid_generate_v4(),
    input      TEXT,
    output     TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp
);