import json
import sqlite3

import pytest

from scripts.verify_identity_preservation import table_audit


@pytest.fixture
def snapshots():
    old, new = sqlite3.connect(":memory:"), sqlite3.connect(":memory:")
    for db in (old, new):
        db.execute("CREATE TABLE stock_symbol (id INTEGER PRIMARY KEY, market TEXT, symbol TEXT, name TEXT, "
            "list_date TEXT, ext_json TEXT, last_synced_at TEXT, updated_at TEXT)")
        db.executemany("INSERT INTO stock_symbol VALUES (?, ?, ?, ?, ?, ?, ?, ?)", [
            (index, market, symbol, "original name", None, json.dumps({"original": {"keep": True}}),
                "2026-09-21 01:00:00", "2026-09-21 01:00:00")
            for index, (market, symbol) in enumerate([
                ("CN_A", "000002"), ("CN_A", "689009"), ("HK", "00001"), ("NEEQ", "430019"),
                ("CN_A", "000003"),
            ], start=1)])
    yield old, new
    old.close()
    new.close()


def refresh(new, row_id=2):
    new.execute("UPDATE stock_symbol SET list_date=?, ext_json=?, last_synced_at=?, updated_at=? WHERE id=?", (
        "2010-01-01", json.dumps({"original": {"keep": True}, "f10_profile_fields": {"from": "public F10"},
            "f10_profile_source": "public provider"}), "2026-09-22 01:00:00", "2026-09-22 01:00:00", row_id))


def test_strict_default_rejects_refresh_and_explicit_flag_lists_exact_exceptions(snapshots):
    old, new = snapshots
    new.execute("UPDATE stock_symbol SET last_synced_at=?, updated_at=? WHERE id=1", (
        "2026-09-22 01:00:00", "2026-09-22 01:00:00"))
    for row_id in (2, 3, 4):
        refresh(new, row_id)
    strict = table_audit(old, new, "stock_symbol")
    assert not strict["passed"] and strict["changed_rows"] == 4 and not strict["expected_refresh"]
    reviewed = table_audit(old, new, "stock_symbol", allow_additive_stock_refresh=True)
    assert reviewed["passed"] and reviewed["preserved_rows"] == 5 and reviewed["identity_only_changes"] == 0
    assert len(reviewed["expected_refresh"]) == 4
    first = next(row for row in reviewed["expected_refresh"] if row["symbol"] == "000002")
    assert first["fields"] == ["last_synced_at", "updated_at"]
    assert all("public F10" not in json.dumps(row) for row in reviewed["expected_refresh"])


@pytest.mark.parametrize("issue", [
    "unreviewed_security", "time_regressed", "invalid_time", "missing_time", "overwrite_existing_date",
    "invalid_date", "changed_old_ext_value", "changed_old_ext_type", "removed_old_ext_key", "unapproved_ext_key", "changed_name",
    "unreviewed_field_for_000002",
])
def test_flag_never_hides_data_overwrite_or_unreviewed_changes(snapshots, issue):
    old, new = snapshots
    row_id = 5 if issue == "unreviewed_security" else 1 if issue == "unreviewed_field_for_000002" else 2
    refresh(new, row_id)
    if issue == "time_regressed":
        new.execute("UPDATE stock_symbol SET updated_at=? WHERE id=2", ("2026-01-01 00:00:00",))
    elif issue == "invalid_time":
        new.execute("UPDATE stock_symbol SET last_synced_at=? WHERE id=2", ("invalid",))
    elif issue == "missing_time":
        new.execute("UPDATE stock_symbol SET updated_at=NULL WHERE id=2")
    elif issue == "overwrite_existing_date":
        old.execute("UPDATE stock_symbol SET list_date=? WHERE id=2", ("2001-01-01",))
    elif issue == "invalid_date":
        new.execute("UPDATE stock_symbol SET list_date=? WHERE id=2", ("2026-02-30",))
    elif issue == "changed_old_ext_value":
        new.execute("UPDATE stock_symbol SET ext_json=? WHERE id=2", (json.dumps({
            "original": {"keep": False}, "f10_profile_fields": {}}),))
    elif issue == "changed_old_ext_type":
        new.execute("UPDATE stock_symbol SET ext_json=? WHERE id=2", (json.dumps({
            "original": {"keep": 1}, "f10_profile_fields": {}}),))
    elif issue == "removed_old_ext_key":
        new.execute("UPDATE stock_symbol SET ext_json=? WHERE id=2", (json.dumps({"f10_profile_fields": {}}),))
    elif issue == "unapproved_ext_key":
        new.execute("UPDATE stock_symbol SET ext_json=? WHERE id=2", (json.dumps({
            "original": {"keep": True}, "unapproved": {}}),))
    elif issue == "changed_name":
        new.execute("UPDATE stock_symbol SET name=? WHERE id=2", ("replaced original",))
    report = table_audit(old, new, "stock_symbol", allow_additive_stock_refresh=True)
    assert not report["passed"] and report["changed_rows"] == 1 and not report["expected_refresh"]
