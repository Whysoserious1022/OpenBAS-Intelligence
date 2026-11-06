# ADR-001: Selection of TimescaleDB for Industrial Telemetry

## Context
Industrial building operations generate continuous time-series telemetry from BACnet, MQTT, and OPC UA protocols. A 5-floor building with 20 zones and ~30 mechanical assets produces 150–200 data points per minute (approx. 288,000 measurements/day).

## Decision
We chose **TimescaleDB** (PostgreSQL extension) for time-series telemetry storage rather than a separate time-series database (e.g. InfluxDB) or general NoSQL store (MongoDB).

## Rationale
1. **Relational Integrity with SQL Joins**: Telemetry events can be seamlessly joined with relational equipment hierarchy tables (`buildings`, `floors`, `zones`, `equipment`, `devices`) in single ACID transactions.
2. **Hypertables and Chunking**: Automated 1-day chunking prevents B-tree index bloat on high-velocity inserts.
3. **Analytical Aggregations**: Supports standard SQL window functions, rollups, and time-bucket aggregations for rolling moving averages and anomaly baseline calculation.
4. **Zero Extra Operational Surface**: Retains single PostgreSQL cluster management rather than maintaining dual database clusters.

