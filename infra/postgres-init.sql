-- Local-dev Postgres bootstrap.
-- Runs once on first container start (via /docker-entrypoint-initdb.d).
-- Creates one user + one database per service. Tables are created later
-- by each service's Spring Boot schema.sql.

CREATE USER orders WITH PASSWORD 'orders_pw';
CREATE USER stock  WITH PASSWORD 'stock_pw';

CREATE DATABASE orders OWNER orders;
CREATE DATABASE stock  OWNER stock;
