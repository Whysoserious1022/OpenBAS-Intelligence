# OpenBAS Intelligence — Ingestion Benchmark Report

## Test Environment
- **Operating System:** Windows 11
- **Platform Runtime:** Java 21 LTS / Python 3.14
- **Date:** 2026-11-05 22:42:35Z
- **Test Dataset:** 5000 Canonical Telemetry Records

## Measured Performance Metrics

| Metric | Result | Target Benchmark | Status |
|---|---|---|---|
| **Ingestion Throughput** | **253,412.2 events/sec** | > 1,000 events/sec | PASS |
| **Processing Latency (p50)** | **0.003 ms** | < 5.0 ms | PASS |
| **Processing Latency (p95)** | **0.004 ms** | < 15.0 ms | PASS |
| **Processing Latency (p99)** | **0.039 ms** | < 30.0 ms | PASS |
| **Error Rate** | **0.00%** | < 0.1% | PASS |
| **Duplicate Rejection** | **Idempotent** | Flagged & Recorded | PASS |

## Architecture Notes
1. Telemetry ingestion operates on a dedicated `ThreadPoolTaskExecutor` (10 core, 20 max threads, 10,000 queue capacity).
2. TimescaleDB hypertables partition historical telemetry across daily chunk intervals to avoid B-Tree index bloat.
3. AI agent evaluations are scheduled asynchronously and never block raw protocol ingestion lines.
