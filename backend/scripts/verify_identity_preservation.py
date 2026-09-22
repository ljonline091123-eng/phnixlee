"""Repeatable read-only audit for issuer imports and additive graph identities.

The report contains counts and column names only, never business row values,
credentials, source payloads, or document content. Both SQLite connections use
mode=ro and a consistent read transaction. No application startup is imported.
"""

import argparse
from collections import Counter
from datetime import date, datetime, timezone
import json
from pathlib import Path
import sqlite3


PROTECTED_TABLES = (
    "stock_symbol", "foundation_entity", "foundation_security", "foundation_listing",
    "foundation_evidence", "foundation_source_identity", "foundation_fact",
    "foundation_fact_evidence", "foundation_fact_review", "foundation_security_classification",
    "classification_definition", "knowledge_base", "knowledge_graph", "knowledge_entity",
    "knowledge_relation", "knowledge_document", "research_report",
    "data_source", "data_interface", "model_provider", "model_instance", "model_route_rule",
    "model_skill", "model_skill_revision", "agent_definition", "agent_data_asset",
    "agent_child_link", "agent_skill_link", "agent_data_asset_link", "agent_knowledge_base_link",
    "agent_data_source_link",
)
IDENTITY_FIELDS = frozenset({
    "canonical_security_id", "canonical_company_id", "security_id", "company_id",
    "stock_symbol_id", "listing_id", "mapping_status", "identity_version",
})
STOCK_REFRESH_FIELDS = {
    ("CN_A", "000002"): frozenset({"last_synced_at", "updated_at"}),
    **{pair: frozenset({"last_synced_at", "updated_at", "list_date", "ext_json"}) for pair in (
        ("CN_A", "689009"), ("HK", "00001"), ("NEEQ", "430019"),
    )},
}
F10_ADDITIONS = frozenset({"f10_profile_fields", "f10_profile_source"})


def quote(value):
    return '"' + value.replace('"', '""') + '"'


def open_read_only(path):
    resolved = Path(path).resolve(strict=True)
    db = sqlite3.connect(resolved.as_uri() + "?mode=ro", uri=True, timeout=30)
    db.execute("PRAGMA query_only = ON")
    db.execute("BEGIN")
    return db


def decoded(value):
    try:
        return json.loads(value) if isinstance(value, str) else value
    except (ValueError, TypeError):
        return value


def expected_stock_refresh(columns, old, current, changed):
    """An explicit exception for four observed drawer-triggered F10 refreshes."""
    original, refreshed = dict(zip(columns, old)), dict(zip(columns, current))
    pair = (original.get("market"), original.get("symbol"))
    if not changed or pair not in STOCK_REFRESH_FIELDS or not set(changed).issubset(STOCK_REFRESH_FIELDS[pair]):
        return False
    try:
        for field in changed:
            if field in {"last_synced_at", "updated_at"}:
                before_time = datetime.fromisoformat(original[field].replace("Z", "+00:00"))
                after_time = datetime.fromisoformat(refreshed[field].replace("Z", "+00:00"))
                before_time = before_time.replace(tzinfo=timezone.utc) if before_time.tzinfo is None else before_time
                after_time = after_time.replace(tzinfo=timezone.utc) if after_time.tzinfo is None else after_time
                if after_time < before_time:
                    return False
            elif field == "list_date":
                if original[field] is not None or date.fromisoformat(refreshed[field]).isoformat() != refreshed[field]:
                    return False
            elif field == "ext_json":
                before_ext, after_ext = decoded(original[field]), decoded(refreshed[field])
                if not isinstance(before_ext, dict) or not isinstance(after_ext, dict):
                    return False
                added_keys = after_ext.keys() - before_ext.keys()
                if not added_keys or not added_keys.issubset(F10_ADDITIONS) or any(
                    key not in after_ext or json.dumps(value, sort_keys=True, allow_nan=False) !=
                    json.dumps(after_ext[key], sort_keys=True, allow_nan=False) for key, value in before_ext.items()
                ):
                    return False
    except (AttributeError, ValueError, TypeError, KeyError):
        return False
    return True


def table_audit(before, after, table, allow_additive_stock_refresh=False):
    old_schema = before.execute("PRAGMA table_info(" + quote(table) + ")").fetchall()
    new_schema = after.execute("PRAGMA table_info(" + quote(table) + ")").fetchall()
    columns = [row[1] for row in old_schema]
    missing_columns = sorted(set(columns) - {row[1] for row in new_schema})
    keys = [row[1] for row in sorted(old_schema, key=lambda row: row[5]) if row[5]]
    result = {"table": table, "baseline_rows": 0, "current_rows": 0, "added_rows": 0,
        "missing_rows": 0, "changed_rows": 0, "preserved_rows": 0,
        "identity_only_changes": 0, "expected_refresh": [], "changed_columns": {}, "passed": False}
    if not columns or not keys or missing_columns:
        result["schema_problem"] = "missing_table_columns_or_primary_key"
        result["missing_columns"] = missing_columns
        return result
    key_indexes = [columns.index(key) for key in keys]
    query = "SELECT " + ", ".join(map(quote, columns)) + " FROM " + quote(table)
    originals = {tuple(row[index] for index in key_indexes): row for row in before.execute(query)}
    result["baseline_rows"] = len(originals)
    changes = Counter()
    for row in after.execute(query):
        result["current_rows"] += 1
        key = tuple(row[index] for index in key_indexes)
        old = originals.pop(key, None)
        if old is None:
            result["added_rows"] += 1
            continue
        changed = [column for index, column in enumerate(columns) if old[index] != row[index]]
        allowed = set()
        accepted_refresh = table == "stock_symbol" and allow_additive_stock_refresh and expected_stock_refresh(columns, old, row, changed)
        if accepted_refresh:
            allowed.update(changed)
            result["expected_refresh"].append({"market": old[columns.index("market")],
                "symbol": old[columns.index("symbol")], "fields": sorted(changed),
                "reason": "EXPLICITLY_ALLOWED_ADDITIVE_F10_REFRESH"})
        if table == "knowledge_entity" and old[columns.index("entity_type")] == "STOCK":
            props_index = columns.index("properties_json")
            old_props, new_props = decoded(old[props_index]), decoded(row[props_index])
            if isinstance(old_props, dict) and isinstance(new_props, dict):
                old_original = {key: value for key, value in old_props.items() if key not in IDENTITY_FIELDS}
                new_original = {key: value for key, value in new_props.items() if key not in IDENTITY_FIELDS}
                if old_original == new_original and old_props != new_props:
                    allowed.update(("properties_json", "updated_at"))
        unexpected = [column for column in changed if column not in allowed]
        if unexpected:
            result["changed_rows"] += 1
            changes.update(unexpected)
        else:
            result["preserved_rows"] += 1
            if changed and not accepted_refresh:
                result["identity_only_changes"] += 1
    result["missing_rows"] = len(originals)
    result["changed_columns"] = dict(changes)
    result["passed"] = not result["missing_rows"] and not result["changed_rows"]
    return result


def listing_chains(db):
    return {row[0]: row[1:] for row in db.execute(
        "SELECT l.stock_symbol_id, l.id, l.security_id, s.entity_id, l.evidence_id, "
        "s.id IS NOT NULL, e.id IS NOT NULL FROM foundation_listing l "
        "LEFT JOIN foundation_security s ON s.id=l.security_id "
        "LEFT JOIN foundation_entity e ON e.id=s.entity_id")}


def identity_audit(db):
    mappings = {(row[0], row[1]): row[2:] for row in db.execute(
        "SELECT l.market, l.symbol, l.security_id, s.entity_id, l.id, l.stock_symbol_id "
        "FROM foundation_listing l JOIN foundation_security s ON s.id=l.security_id")}
    counts = {"stock_nodes": 0, "identity_complete": 0, "mapped_company_nodes": 0,
        "unmapped_company_nodes": 0, "identity_mismatch": 0}
    for (raw,) in db.execute("SELECT properties_json FROM knowledge_entity WHERE entity_type='STOCK'"):
        counts["stock_nodes"] += 1
        props = decoded(raw)
        if not isinstance(props, dict) or not props.get("market") or not props.get("symbol"):
            counts["identity_mismatch"] += 1
            continue
        pair = (props["market"].strip().upper(), props["symbol"].strip().upper())
        mapping = mappings.get(pair)
        expected = {"canonical_security_id": f"security:{pair[0]}:{pair[1]}",
            "identity_version": "SECURITY_COMPANY_V1",
            "canonical_company_id": f"company:{mapping[1]}" if mapping else None,
            "security_id": mapping[0] if mapping else None,
            "company_id": mapping[1] if mapping else None,
            "listing_id": mapping[2] if mapping else None,
            "mapping_status": "MAPPED" if mapping else "UNMAPPED"}
        if mapping:
            expected["stock_symbol_id"] = mapping[3]
        if all(field in props and props[field] == value for field, value in expected.items()):
            counts["identity_complete"] += 1
            counts["mapped_company_nodes" if mapping else "unmapped_company_nodes"] += 1
        else:
            counts["identity_mismatch"] += 1
    counts["passed"] = not counts["identity_mismatch"]
    return counts


def audit(baseline, current, require_identity=False, allow_additive_stock_refresh=False):
    old = open_read_only(baseline)
    new = open_read_only(current)
    try:
        reports = [table_audit(old, new, table, allow_additive_stock_refresh) for table in PROTECTED_TABLES]
        old_chains, new_chains = listing_chains(old), listing_chains(new)
        missing = sum(key not in new_chains for key in old_chains)
        changed = sum(key in new_chains and value != new_chains[key] for key, value in old_chains.items())
        broken = sum(not row[-1] or not row[-2] for row in new_chains.values())
        chains = {"baseline_mappings": len(old_chains), "current_mappings": len(new_chains),
            "original_distinct_companies": len({row[2] for row in old_chains.values()}),
            "missing_original_mappings": missing, "changed_original_uuid_chains": changed,
            "broken_current_mappings": broken, "passed": not (missing or changed or broken)}
        integrity_errors = sum(row[0] != "ok" for row in new.execute("PRAGMA quick_check"))
        old_foreign_keys = Counter(tuple(row) for row in old.execute("PRAGMA foreign_key_check"))
        new_foreign_keys = Counter(tuple(row) for row in new.execute("PRAGMA foreign_key_check"))
        introduced_foreign_keys = sum((new_foreign_keys - old_foreign_keys).values())
        identity = identity_audit(new)
        refreshes = [item for table in reports for item in table["expected_refresh"]]
        return {"passed": all(row["passed"] for row in reports) and chains["passed"] and
            not integrity_errors and not introduced_foreign_keys and (identity["passed"] or not require_identity),
            "read_only": True, "identity_required": require_identity,
            "expected_refresh": {"enabled": allow_additive_stock_refresh, "count": len(refreshes), "items": refreshes},
            "integrity_errors": integrity_errors, "new_foreign_key_violations": introduced_foreign_keys,
            "original_security_company_mappings": chains, "legacy_identity": identity, "tables": reports}
    finally:
        old.close()
        new.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--current", required=True)
    parser.add_argument("--require-identity", action="store_true", help="Also require canonical aliases on every legacy STOCK node")
    parser.add_argument("--allow-additive-stock-refresh", action="store_true",
        help="Explicitly allow and report only the four reviewed F10 refreshes; strict comparison remains the default")
    parser.add_argument("--report")
    args = parser.parse_args()
    if args.report and Path(args.report).resolve() in {Path(args.baseline).resolve(), Path(args.current).resolve()}:
        parser.error("The report path must differ from both database paths")
    result = audit(args.baseline, args.current, args.require_identity, args.allow_additive_stock_refresh)
    if args.report:
        Path(args.report).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key != "tables"}, ensure_ascii=True))
    print(json.dumps({"tables_checked": len(result["tables"]),
        "changed_tables": [row for row in result["tables"] if not row["passed"]],
        "added_rows": {row["table"]: row["added_rows"] for row in result["tables"] if row["added_rows"]},
        "identity_only_changes": sum(row["identity_only_changes"] for row in result["tables"])}, ensure_ascii=True))
    raise SystemExit(0 if result["passed"] else 1)


if __name__ == "__main__":
    main()
