"""Read-only stock classification projection used by the F10 read model.

The stock detail page historically treated industry, exchange board, index
membership and investment themes as one comma separated ``板块`` field.  That
made an index (for example CSI 300) look like an industry/theme.  This module
keeps the provider facts intact and builds a small, explicitly typed display
projection instead.  It deliberately does not infer a classification when no
source record exists.
"""

from __future__ import annotations

import json
import re
import unicodedata
from typing import Any

from sqlalchemy import inspect, select
from sqlalchemy.orm import Session

from app.models.company_graph import SecurityClassification
from app.models.foundation import FoundationEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import StockSymbol
from app.models.taxonomy import ClassificationDefinition


def _split(value: Any) -> list[str]:
    if value in (None, ""):
        return []
    text = str(value)
    return [item.strip() for item in re.split(r"[,，;；|、]+", text) if item.strip()]


def _clean_industry(value: Any) -> list[str]:
    values = []
    for item in _split(value):
        # Symbol-master industry values sometimes carry an alphabetic CSRC
        # prefix (``K 房地产``).  The code is not the user-facing label.
        cleaned = re.sub(r"^[A-Za-z]\s+", "", item).strip()
        if cleaned and cleaned not in values:
            values.append(cleaned)
    return values


def _item(label: Any, *, code: Any = None, source: str = "证券主数据", status: str = "SOURCE",
          definition: str | None = None, criteria: str | None = None,
          source_name: str | None = None, definition_version: str | None = None,
          source_url: str | None = None, level: Any = None) -> dict[str, Any] | None:
    text = str(label or "").strip()
    if not text:
        return None
    row: dict[str, Any] = {"label": text, "name": text, "code": str(code).strip() if code else None,
                           "source": source, "status": status}
    if definition:
        row["definition"] = definition
    if criteria:
        row["criteria"] = criteria
    if source_name:
        row["source_name"] = source_name
    if definition_version:
        row["definition_version"] = definition_version
    if source_url:
        row["source_url"] = source_url
    if level is not None:
        row["level"] = level
    return row


def _group(key: str, title: str, items: list[dict[str, Any]]) -> dict[str, Any] | None:
    unique: dict[tuple[str, str], dict[str, Any]] = {}
    for item in items:
        unique[(str(item.get("code") or ""), str(item.get("label") or ""))] = item
    if not unique:
        return None
    return {"key": key, "title": title, "items": list(unique.values())}


def _looks_like_index(label: Any, code: Any = None) -> bool:
    """Conservatively separate index membership from business themes."""
    text = unicodedata.normalize("NFKC", str(label or "")).strip().upper().replace(" ", "")
    code_text = str(code or "").strip().upper()
    if not text:
        return False
    if any(token in text for token in ("MSCI", "HS300", "沪深", "中证", "上证", "深证", "富时", "恒生", "红利", "指数", "指", "中盘", "精选")):
        return True
    if re.search(r"(?:^|[A-Z])(?:A|R)?(?:50|100|300|500)(?:$|[A-Z])", text):
        return True
    if re.search(r"(?:50|100|300|500)R?$", text):
        return True
    return code_text.startswith(("BK05", "BK06", "BK07", "BK08")) and text.endswith("R")


def _definition_lookup(db: Session | None) -> dict[tuple[str, str], ClassificationDefinition]:
    if db is None:
        return {}
    try:
        rows = list(db.scalars(select(ClassificationDefinition).where(ClassificationDefinition.status == "ACTIVE")))
    except Exception:
        return {}
    result: dict[tuple[str, str], ClassificationDefinition] = {}
    for row in rows:
        result.setdefault((str(row.dimension).upper(), str(row.code).upper()), row)
    return result


def _definition_for(
    definitions: dict[tuple[str, str], ClassificationDefinition],
    dimension: str,
) -> ClassificationDefinition | None:
    candidates = {
        "INDUSTRY": ("EASTMONEY_LEVEL1", "CSRC_LEVEL1"),
        "THEME": ("PROVIDER_CONCEPT",),
        "INDEX": ("INDEX_MEMBERSHIP", "PROVIDER_CONCEPT"),
        "BOARD": ("LISTED_BOARD", "EASTMONEY_LEVEL1"),
        "TYPE": ("SECURITY_TYPE", "A_SHARE", "H_SHARE"),
    }.get(dimension.upper(), ())
    return next((definitions.get((dimension.upper(), code)) for code in candidates if definitions.get((dimension.upper(), code))), None)


def _attach_definition(
    item: dict[str, Any], definitions: dict[tuple[str, str], ClassificationDefinition], dimension: str,
) -> dict[str, Any]:
    """Attach glossary metadata while preserving a fact's precise definition.

    ``SecurityClassification.properties_json`` may contain a definition for a
    concrete provider label (for example a particular theme).  The built-in
    dimension definition explains only the taxonomy as a whole and must not
    overwrite that more specific fact.  Keep both values explicit so callers
    can show the precise definition first and the master-data explanation as a
    fallback.
    """
    definition = _definition_for(definitions, dimension)
    if definition is not None:
        # Preserve concrete fact-level values.  The ``master_*`` fields are
        # intentionally separate and versioned, making provenance clear to
        # both the UI and downstream model consumers.
        concrete_definition = item.get("definition")
        if concrete_definition in (None, ""):
            item["definition"] = definition.definition
        elif concrete_definition != definition.definition:
            item.setdefault("fact_definition", concrete_definition)
        concrete_criteria = item.get("criteria")
        if concrete_criteria in (None, ""):
            item["criteria"] = definition.criteria
        elif concrete_criteria != definition.criteria:
            item.setdefault("fact_criteria", concrete_criteria)
        if item.get("source_name") in (None, ""):
            item["source_name"] = definition.source_name
        item.setdefault("source_url", definition.source_url)
        item.setdefault("definition_version", definition.definition_version)
        item["master_definition"] = definition.definition
        item["master_criteria"] = definition.criteria
        item["master_source_name"] = definition.source_name
        item["master_source_url"] = definition.source_url
        item["master_definition_version"] = definition.definition_version
        item["dimension"] = definition.dimension
        item["taxonomy"] = definition.taxonomy
    return item


def _foundation_classifications(db: Session, stock: StockSymbol) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    """Read provider classifications already imported into the company graph.

    Some older test databases do not contain foundation tables.  In that case
    the function simply returns empty lists and the F10 provider fields remain
    the source of truth.
    """
    try:
        tables = set(inspect(db.connection()).get_table_names())
        required = {FoundationListing.__tablename__, FoundationSecurity.__tablename__,
                    FoundationEvidence.__tablename__, SecurityClassification.__tablename__}
        if not required.issubset(tables):
            return [], [], []
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        if listing is None:
            return [], [], []
        rows = list(db.scalars(select(SecurityClassification).where(
            SecurityClassification.security_id == listing.security_id,
            SecurityClassification.status.in_(("ACCEPTED", "PENDING")),
        ).order_by(SecurityClassification.created_at.desc())))
        industries: list[dict[str, Any]] = []
        themes: list[dict[str, Any]] = []
        indexes: list[dict[str, Any]] = []
        for row in rows:
            target = industries if row.dimension.upper() == "INDUSTRY" else themes if row.dimension.upper() == "THEME" else None
            if target is None:
                continue
            props = row.properties_json if isinstance(row.properties_json, dict) else {}
            value = _item(row.label, code=row.code, source="公司/证券分类主数据", status=row.status,
                          definition=props.get("definition"), level=props.get("level"))
            if value:
                target.append(value)
        # The imported evidence contains the complete Eastmoney board/theme
        # hierarchy even when classifications were not reviewed yet.
        evidence = db.get(FoundationEvidence, listing.evidence_id)
        raw = json.loads(evidence.content) if evidence and evidence.content else {}
        if isinstance(raw, dict):
            for level in (1, 2, 3):
                label, code = raw.get(f"BOARD_NAME_{level}LEVEL"), raw.get(f"BOARD_CODE_BK_{level}LEVEL")
                value = _item(label, code=code, source="东方财富公司主数据", status="SOURCE", level=level)
                if value:
                    industries.append(value)
            labels = _split(raw.get("BLGAINIAN"))
            codes = _split(raw.get("BLGAINIAN_CODE"))
            for index, label in enumerate(labels):
                value = _item(label, code=codes[index] if index < len(codes) else None,
                              source="东方财富主题板块", status="SOURCE")
                if value:
                    (indexes if _looks_like_index(label, value.get("code")) else themes).append(value)
        return industries, themes, indexes
    except (ValueError, TypeError, json.JSONDecodeError):
        return [], [], []


def build_classification_groups(db: Session | None, stock: StockSymbol,
                                profile_fields: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    fields = profile_fields if isinstance(profile_fields, dict) else {}
    groups: list[dict[str, Any]] = []
    definitions = _definition_lookup(db)
    industry_values, theme_values, index_values = _foundation_classifications(db, stock) if db is not None else ([], [], [])

    if not industry_values:
        for index, value in enumerate(_clean_industry(fields.get("所属行业") or fields.get("行业"))):
            item = _item(value, source="公司资料 / F10", status="SOURCE", level=index + 1)
            if item:
                industry_values.append(item)
    industry_values = [_attach_definition(item, definitions, "INDUSTRY") for item in industry_values]
    if industry_values:
        # Keep only the hierarchy labels in the industry group; themes are
        # displayed separately below and indexes never enter either group.
        groups.append(_group("industry", "所属行业", industry_values))

    board_values = _split(fields.get("板块") or fields.get("上市板块") or fields.get("所属市场"))
    board_items = [_item(value, source="证券主数据 / F10", status="SOURCE") for value in board_values]
    board_items = [item for item in board_items if item]
    if board_items:
        groups.append(_group("board", "上市板块", board_items))

    if not theme_values:
        # ``所属概念`` is accepted only as a theme fallback.  ``入选指数`` is
        # intentionally not read here, since an index is its own dimension.
        for value in _split(fields.get("所属概念") or fields.get("概念")):
            item = _item(value, source="F10 概念字段", status="SOURCE")
            if item:
                theme_values.append(item)
    theme_values = [_attach_definition(item, definitions, "THEME") for item in theme_values if not _looks_like_index(item.get("label"), item.get("code"))]
    if theme_values:
        groups.append(_group("theme", "主题板块", theme_values))

    index_items = [_attach_definition(item, definitions, "INDEX") for item in index_values]
    index_items.extend(_attach_definition(_item(value, source="F10 入选指数", status="SOURCE"), definitions, "INDEX") for value in _split(fields.get("入选指数")))
    index_items = [item for item in index_items if item]
    if index_items:
        groups.append(_group("index", "入选指数", index_items))

    type_items: list[dict[str, Any]] = []
    if stock.asset_type:
        item = _attach_definition(_item(stock.asset_type, source="证券主数据", status="SOURCE"), definitions, "TYPE")
        if item:
            type_items.append(item)
    if stock.market:
        item = _attach_definition(_item({"CN_A": "A股", "HK": "港股", "NEEQ": "新三板", "NEEQ_INNOVATION": "创新层"}.get(stock.market, stock.market),
                     code=stock.market, source="证券主数据", status="SOURCE"), definitions, "TYPE")
        if item:
            type_items.append(item)
    if type_items:
        groups.append(_group("type", "证券类型", type_items))
    # Enrich every emitted label with the versioned glossary.  Applying this
    # at the final projection boundary also covers legacy foundation facts and
    # avoids mutating their source payloads.
    for group in groups:
        dimension = {"industry": "INDUSTRY", "board": "BOARD", "theme": "THEME",
                     "index": "INDEX", "type": "TYPE"}.get(str(group.get("key")), "")
        for item in group.get("items", []):
            if dimension:
                _attach_definition(item, definitions, dimension)
    return [item for item in groups if item]
