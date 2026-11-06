import os
import sys
import time
import json
import logging
from datetime import datetime, timezone

# Add simulator folder to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from building.building_model import SimulatedBuilding
from scenarios.scenario_runner import ScenarioRunner

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s - %(message)s")
logger = logging.getLogger("OpenBAS-Simulator")

def run_simulation(
    scenario: str = "normal_operation",
    steps: int = 0,
    interval_seconds: float = 2.0,
    mqtt_host: str = "localhost",
    mqtt_port: int = 1883,
    publish_mqtt: bool = True
):
    logger.info("======================================================")
    logger.info(f" Starting OpenBAS Building Simulator: {scenario}")
    logger.info(f" Simulating Nexus Tower (BUILDING-01) - 5 Floors, 20 Zones")
    logger.info(f" Mode: {'MQTT Telemetry Stream' if publish_mqtt else 'Standalone'}")
    logger.info(f" Steps: {'Continuous (infinite)' if steps <= 0 else f'{steps} steps'}")
    logger.info("======================================================")

    building = SimulatedBuilding("BUILDING-01")
    runner = ScenarioRunner(building)
    runner.apply_scenario(scenario)

    client = None
    if publish_mqtt:
        try:
            import paho.mqtt.client as mqtt
            # Support both paho-mqtt v1 and v2 CallbackAPIVersion
            try:
                client = mqtt.Client(
                    mqtt.CallbackAPIVersion.VERSION2,
                    client_id="openbas-simulator-publisher"
                )
            except AttributeError:
                client = mqtt.Client(client_id="openbas-simulator-publisher")

            # Retry loop for MQTT connection (in case broker container is starting up)
            connected = False
            for attempt in range(1, 6):
                try:
                    client.connect(mqtt_host, mqtt_port, 60)
                    client.loop_start()
                    connected = True
                    logger.info(f"Connected to MQTT broker at {mqtt_host}:{mqtt_port}")
                    break
                except Exception as ex:
                    logger.warning(f"Connection attempt {attempt}/5 to MQTT {mqtt_host}:{mqtt_port} failed ({ex}). Retrying in 2s...")
                    time.sleep(2)

            if not connected:
                logger.warning(f"Could not connect to MQTT broker after retries. Operating in standalone output mode.")
                client = None

        except ImportError:
            logger.warning("paho-mqtt not installed. Operating in standalone output mode.")
            client = None
        except Exception as e:
            logger.warning(f"Failed to initialize MQTT client ({e}). Operating in standalone output mode.")
            client = None

    step_num = 0
    try:
        while True:
            step_num += 1
            if steps > 0 and step_num > steps:
                break

            # Ambient temperature curve: 22°C morning, peaks at 31°C at 2pm
            hour = 8.0 + ((step_num % 48) * (14.0 / 48.0))
            ambient_temp = 24.0 + 7.0 * (1.0 - abs(hour - 14.0) / 6.0)

            telemetry_batch = building.step(ambient_temp, hour)
            timestamp = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")

            # Publish or log
            published_count = 0
            for item in telemetry_batch:
                item["timestamp"] = timestamp
                item["quality"] = "SIMULATED"
                item["sourceProtocol"] = "MQTT" if client else "SIMULATED"

                if client:
                    topic = f"openbas/telemetry/{item['buildingId']}/{item.get('floorId') or 'BLDG'}/{item.get('zoneId') or 'ALL'}/{item['equipmentId']}/{item['deviceId']}/{item['pointId']}"
                    payload = json.dumps({
                        "value": item["value"],
                        "unit": item["unit"],
                        "timestamp": timestamp,
                        "quality": "SIMULATED"
                    })
                    client.publish(topic, payload, qos=1)
                    published_count += 1

            ahu3_temp = building.ahus["AHU-F3"].supply_air_temp
            z31_temp = building.vavs["VAV-F3-Z1"].zone_temp
            step_display = f"{step_num:03d}" if steps <= 0 else f"{step_num:02d}/{steps:02d}"
            logger.info(
                f"Step {step_display} | Hour {hour:04.1f}:00 | Amb: {ambient_temp:.1f}°C | "
                f"AHU-F3 Supply: {ahu3_temp:.1f}°C | Zone 3-1: {z31_temp:.1f}°C | Points Published: {len(telemetry_batch)}"
            )
            time.sleep(interval_seconds)

    except KeyboardInterrupt:
        logger.info("Simulation interrupted by operator.")
    finally:
        if client:
            client.loop_stop()
            client.disconnect()
            logger.info("Disconnected from MQTT broker.")

    logger.info("Simulation finished successfully.")

if __name__ == "__main__":
    scenario = os.getenv("SIMULATION_SCENARIO", os.getenv("SCENARIO", "normal_operation"))
    steps = int(os.getenv("SIMULATION_TOTAL_STEPS", os.getenv("STEPS", "10")))
    interval = float(os.getenv("SIMULATION_STEP_INTERVAL_SECONDS", os.getenv("INTERVAL_SECONDS", "1.0")))
    mqtt_host = os.getenv("MQTT_BROKER_HOST", os.getenv("MQTT_HOST", "localhost"))
    mqtt_port = int(os.getenv("MQTT_BROKER_PORT", os.getenv("MQTT_PORT", "1883")))
    publish_mqtt = os.getenv("PUBLISH_MQTT", "true").lower() in ("true", "1", "yes")

    run_simulation(
        scenario=scenario,
        steps=steps,
        interval_seconds=interval,
        mqtt_host=mqtt_host,
        mqtt_port=mqtt_port,
        publish_mqtt=publish_mqtt
    )
