from typing import Dict, List, Any
from equipment.chiller import ChillerPlant
from equipment.ahu import AirHandlingUnit
from equipment.vav import VariableAirVolumeBox

class SimulatedBuilding:
    """
    Simulated multi-floor commercial building: Nexus Tower (BUILDING-01).
    Topology:
      Chiller Plant (CHILLER-01)
        ├── Floor 1 (AHU-F1) -> VAV-F1-Z1 .. VAV-F1-Z4
        ├── Floor 2 (AHU-F2) -> VAV-F2-Z1 .. VAV-F2-Z4
        ├── Floor 3 (AHU-F3) -> VAV-F3-Z1 .. VAV-F3-Z4
        ├── Floor 4 (AHU-F4) -> VAV-F4-Z1 .. VAV-F4-Z4
        └── Floor 5 (AHU-F5) -> VAV-F5-Z1 .. VAV-F5-Z4
    """
    def __init__(self, building_id: str = "BUILDING-01"):
        self.building_id = building_id
        self.chiller = ChillerPlant("CHILLER-01")
        self.ahus: Dict[str, AirHandlingUnit] = {}
        self.vavs: Dict[str, VariableAirVolumeBox] = {}

        for floor in range(1, 6):
            ahu_id = f"AHU-F{floor}"
            floor_id = f"FLOOR-{floor}"
            self.ahus[ahu_id] = AirHandlingUnit(ahu_id, floor_id)
            
            for zone in range(1, 5):
                zone_id = f"ZONE-{floor}-{zone}"
                vav_id = f"VAV-F{floor}-Z{zone}"
                self.vavs[vav_id] = VariableAirVolumeBox(vav_id, zone_id)

    def step(self, ambient_temp: float, hour_of_day: float) -> List[Dict[str, Any]]:
        """
        Advances the building thermal & airflow physics by 1 simulation step (1 minute).
        Returns a list of normalized canonical telemetry records ready for gateway ingestion.
        """
        telemetry_records = []
        
        # 1. Step AHUs & VAVs
        total_cooling_kw = 0.0
        
        for floor in range(1, 6):
            ahu_id = f"AHU-F{floor}"
            ahu = self.ahus[ahu_id]
            floor_id = f"FLOOR-{floor}"
            
            # Estimate return air temp from the 4 zones of this floor
            floor_zone_temps = []
            for zone in range(1, 5):
                vav_id = f"VAV-F{floor}-Z{zone}"
                floor_zone_temps.append(self.vavs[vav_id].zone_temp)
            avg_return_temp = sum(floor_zone_temps) / len(floor_zone_temps)

            # Step AHU
            ahu_metrics = ahu.step(
                return_air_temp=avg_return_temp,
                chilled_water_supply_temp=self.chiller.chilled_water_supply_temp
            )
            
            for point, val in ahu_metrics.items():
                telemetry_records.append({
                    "buildingId": self.building_id,
                    "floorId": floor_id,
                    "zoneId": None,
                    "equipmentId": ahu_id,
                    "deviceId": f"DEV-{ahu_id}",
                    "pointId": point,
                    "value": val,
                    "unit": "CELSIUS" if "TEMP" in point else "KW" if "POWER" in point else "CFM" if "AIRFLOW" in point else "PASCAL" if "PA" in point else "PERCENT"
                })

            # Step 4 VAVs for this floor
            for zone in range(1, 5):
                zone_id = f"ZONE-{floor}-{zone}"
                vav_id = f"VAV-F{floor}-Z{zone}"
                vav = self.vavs[vav_id]
                
                vav_metrics = vav.step(
                    supply_air_temp=ahu.supply_air_temp,
                    ambient_temp=ambient_temp,
                    hour_of_day=hour_of_day
                )
                
                for point, val in vav_metrics.items():
                    telemetry_records.append({
                        "buildingId": self.building_id,
                        "floorId": floor_id,
                        "zoneId": zone_id,
                        "equipmentId": vav_id,
                        "deviceId": f"DEV-TEMP-F{floor}-Z{zone}" if "TEMP" in point else f"DEV-VAV-F{floor}-Z{zone}",
                        "pointId": point,
                        "value": val,
                        "unit": "CELSIUS" if "TEMP" in point else "OCCUPANTS" if point == "OCCUPANCY" else "PERCENT"
                    })
                
                # Accumulate cooling demand for chiller
                if vav.zone_temp > 22.0:
                    total_cooling_kw += (vav.zone_temp - 22.0) * 8.5

        # 2. Step Central Chiller Plant
        chiller_metrics = self.chiller.step(max(total_cooling_kw, 50.0))
        for point, val in chiller_metrics.items():
            telemetry_records.append({
                "buildingId": self.building_id,
                "floorId": None,
                "zoneId": None,
                "equipmentId": self.chiller.equipment_id,
                "deviceId": f"DEV-{self.chiller.equipment_id}",
                "pointId": point,
                "value": val,
                "unit": "CELSIUS" if "TEMP" in point else "KW" if "POWER" in point else "RATIO"
            })

        return telemetry_records

