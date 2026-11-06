import math
from typing import Dict, Any

class ChillerPlant:
    """
    Central Chiller Plant model.
    Produces chilled water at ~6.7°C (44°F) for AHU cooling coils.
    Calculates power consumption based on refrigeration tons and COP.
    """
    def __init__(self, equipment_id: str = "CHILLER-01", rated_tons: float = 300.0, nominal_cop: float = 5.2):
        self.equipment_id = equipment_id
        self.rated_tons = rated_tons
        self.nominal_cop = nominal_cop
        self.chilled_water_supply_temp = 6.8  # °C
        self.chilled_water_return_temp = 12.2 # °C
        self.flow_rate_lps = 45.0             # liters per sec
        self.status = "NORMAL"

    def step(self, total_building_cooling_demand_kw: float) -> Dict[str, float]:
        # Part-load efficiency curve
        cooling_tons = min(total_building_cooling_demand_kw / 3.517, self.rated_tons)
        pl_ratio = max(cooling_tons / self.rated_tons, 0.15)
        current_cop = self.nominal_cop * (0.8 + 0.5 * pl_ratio - 0.3 * (pl_ratio ** 2))
        electrical_power_kw = (cooling_tons * 3.517) / current_cop
        
        self.chilled_water_return_temp = self.chilled_water_supply_temp + (total_building_cooling_demand_kw / (self.flow_rate_lps * 4.184))
        
        return {
            "CHILLED_WATER_SUPPLY_TEMP": round(self.chilled_water_supply_temp, 2),
            "CHILLED_WATER_RETURN_TEMP": round(self.chilled_water_return_temp, 2),
            "CHILLER_POWER_KW": round(electrical_power_kw, 2),
            "CHILLER_COP": round(current_cop, 2)
        }

