-- We use a single database with isolated schemas to support a single Debezium Connector
\c default_db;

CREATE SCHEMA IF NOT EXISTS orchestrator_schema;
CREATE SCHEMA IF NOT EXISTS flight_schema;
CREATE SCHEMA IF NOT EXISTS hotel_schema;
CREATE SCHEMA IF NOT EXISTS car_schema;

-- Set search path for shedlock creation so it goes into orchestrator_schema
SET search_path TO orchestrator_schema;

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
