# ============================================================
# OpenBAS Intelligence — Building Simulator Dockerfile
# Physics-based HVAC/BAS simulator publishing MQTT telemetry
# ============================================================
FROM python:3.12-slim AS base

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy simulator source
COPY . .

# Create non-root user
RUN useradd --no-create-home --shell /bin/false simuser \
    && chown -R simuser:simuser /app
USER simuser

# Default environment (can be overridden via docker-compose or CLI)
ENV MQTT_BROKER_HOST=mqtt
ENV MQTT_BROKER_PORT=1883
ENV SIMULATION_SCENARIO=normal_operation
ENV SIMULATION_STEP_INTERVAL_SECONDS=5
ENV SIMULATION_TOTAL_STEPS=0

# Run simulator
CMD ["python", "main.py"]

