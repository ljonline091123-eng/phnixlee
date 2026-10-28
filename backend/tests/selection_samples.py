"""Small, explicitly synthetic scorecard for publication gate tests."""
def valid_scorecard():
    codes = ["FUNDAMENTAL_QUALITY", "VALUATION_EXPECTATION", "EVENT_DISCLOSURE", "PRICE_RISK",
             "CAPITAL_BEHAVIOR", "GOVERNANCE_STABILITY", "INDUSTRY_MOMENTUM", "SUPPLY_CHAIN_RESILIENCE"]
    return {"trusted_score": 60, "dimensions": [{"dimension_code": code, "weight": 12.5,
            "score": 60, "status": "SUPPORTED"} for code in codes]}
