from scripts.validate_board_details import balance_checks


def test_actual_a_share_rows_include_periods_and_detect_accounting_mismatch():
    rows = [{"指标": "资产合计", "值": 100}, {"指标": "负债合计", "值": 70},
            {"指标": "所有者权益合计", "值": 30}]
    result = balance_checks({"report_date": "2026-06-30", "rows": rows,
                            "periods": [{"报告日": "2025-12-31", "数据": [*rows[:2], {"指标": "所有者权益合计", "值": 20}]}]})
    assert {r["period"]: r["status"] for r in result} == {"2026-06-30": "MATCH", "2025-12-31": "MISMATCH"}


def test_missing_hk_totals_are_not_a_pass_or_fabricated_from_current_liabilities():
    result = balance_checks({"report_date": "2026-06-30", "rows": [
        {"指标": "总资产", "值": 100}, {"指标": "流动负债合计", "值": 50},
        {"指标": "股东权益合计", "值": 30}]})
    assert result[0]["status"] == "MISSING_TOTALS"
    assert result[0]["missing"] == ["liabilities"]


def test_matrix_layout_zero_values_and_distinct_periods_are_supported():
    result = balance_checks({"rows": [
        {"metric_code": "SUMASSET", "data": {"2026-06-30": 100, "2025-12-31": 200}},
        {"metric_code": "SUMLIAB", "data": {"2026-06-30": 0, "2025-12-31": 100}},
        {"metric_code": "SUMSHEQUITY", "data": {"2026-06-30": 100, "2025-12-31": 100}}]})
    assert len(result) == 2 and all(r["status"] == "MATCH" for r in result)
