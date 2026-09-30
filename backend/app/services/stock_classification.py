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
from app.models.market_data import StockRealtimeQuote, StockSymbol
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
          source_url: str | None = None, level: Any = None,
          classification_dimension: str | None = None) -> dict[str, Any] | None:
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
    if classification_dimension:
        row["classification_dimension"] = str(classification_dimension).upper()
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
    # Eastmoney's ``BLGAINIAN`` feed mixes investable themes with index,
    # eligibility and trading-status labels.  The latter must not leak into
    # the concept/industry panels.  Keep this list deliberately conservative:
    # it removes well-known index families and status labels while preserving
    # business themes such as ``智能家居`` and ``养老概念``.
    if any(token in text for token in (
        "MSCI", "HS300", "沪深", "中证", "上证", "深证", "深成指", "国证", "巨潮",
        "富时", "恒生", "标准普尔", "央视", "指数", "深股通", "深市精选", "成分",
        "昨日涨停", "昨日首板", "高振幅", "破净", "热股", "多板",
        "风格",
    )):
        return True
    if re.search(r"(?:^|[A-Z])(?:A|R)?(?:50|100|300|500)(?:$|[A-Z])", text):
        return True
    if re.search(r"(?:50|100|300|500)R?$", text):
        return True
    return code_text.startswith(("BK05", "BK06", "BK07", "BK08")) and text.endswith("R")


def enrich_classification_groups_with_members(
    db: Session,
    groups: list[dict[str, Any]],
    *,
    limit_per_group: int = 30,
) -> list[dict[str, Any]]:
    """Attach source-backed member stocks and an observed group trend.

    The classification table is versioned independently from ``stock_symbol``
    and older imports can point to superseded security identities.  The
    immutable Eastmoney evidence payload therefore remains the safest join
    key for this read model: it carries the source security code/name beside
    the classification fact.  This function only enriches the response and
    never promotes a theme into a business relationship.
    """
    if not groups:
        return groups
    wanted: dict[str, set[str]] = {}
    for group in groups:
        for item in group.get("items") or []:
            code = str(item.get("code") or "").strip()
            if code:
                wanted.setdefault(code, set()).add(str(group.get("key") or ""))
    if not wanted:
        return groups
    try:
        rows = db.execute(
            select(SecurityClassification, FoundationEvidence)
            .join(FoundationEvidence, FoundationEvidence.id == SecurityClassification.evidence_id)
            .where(
                SecurityClassification.code.in_(list(wanted)),
                SecurityClassification.status.in_(("ACCEPTED", "PENDING")),
            )
            .order_by(SecurityClassification.updated_at.desc())
        ).all()
    except Exception:
        return groups

    members: dict[str, dict[tuple[str, str], dict[str, Any]]] = {}
    for classification, evidence in rows:
        try:
            payload = json.loads(evidence.content or "{}")
        except (TypeError, ValueError, json.JSONDecodeError):
            continue
        if not isinstance(payload, dict):
            continue
        raw_code = payload.get("SECURITY_CODE") or payload.get("security_code")
        raw_name = payload.get("SECURITY_NAME_ABBR") or payload.get("SECURITY_NAME") or payload.get("ORG_NAME")
        if not raw_code or not raw_name:
            continue
        code = str(raw_code).strip().upper()
        secu_code = str(payload.get("SECUCODE") or "").upper()
        market = "HK" if secu_code.endswith(".HK") else "CN_A"
        symbol = code.zfill(5 if market == "HK" else 6) if code.isdigit() else code
        member = {
            "market": market,
            "symbol": symbol,
            "name": str(raw_name).strip(),
            "classification_code": str(classification.code),
            "source_name": evidence.source_name,
            "source_url": evidence.url,
        }
        bucket = members.setdefault(str(classification.code), {})
        bucket.setdefault((market, symbol), member)

    # Latest quote is optional enrichment; no quote means the trend remains
    # visibly unavailable rather than being inferred from the theme label.
    quote_map: dict[tuple[str, str], float] = {}
    pairs = [(market, symbol) for bucket in members.values() for market, symbol in bucket]
    if pairs:
        try:
            quotes = db.scalars(
                select(StockRealtimeQuote).where(
                    StockRealtimeQuote.market.in_({pair[0] for pair in pairs}),
                    StockRealtimeQuote.symbol.in_({pair[1] for pair in pairs}),
                )
            ).all()
            for quote in quotes:
                if quote.change_pct is not None:
                    quote_map[(quote.market, quote.symbol)] = float(quote.change_pct)
        except Exception:
            quote_map = {}

    for group in groups:
        for item in group.get("items") or []:
            code = str(item.get("code") or "").strip()
            bucket = list(members.get(code, {}).values())[:limit_per_group] if code else []
            for member in bucket:
                member["change_pct"] = quote_map.get((member["market"], member["symbol"]))
            item["related_stocks"] = bucket
            item["member_count"] = len(members.get(code, {})) if code else 0
            changes = [member["change_pct"] for member in bucket if member.get("change_pct") is not None]
            item["trend_pct"] = round(sum(changes) / len(changes), 2) if changes else None
    return groups


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
    code: Any = None,
) -> ClassificationDefinition | None:
    normalized_dimension = str(dimension or "").upper()
    normalized_code = str(code or "").upper()
    # Concrete definitions (for example SIZE/LARGE_CAP or
    # LEGAL_LISTING_CLASS/A_SHARE) are more precise than the dimension-level
    # fallback.  Prefer them whenever the fact carries a code.
    if normalized_code:
        exact = definitions.get((normalized_dimension, normalized_code))
        if exact is not None:
            return exact
    candidates = {
        "INDUSTRY": ("EASTMONEY_LEVEL1", "CSRC_LEVEL1"),
        "THEME": ("PROVIDER_CONCEPT",),
        "INDEX": ("INDEX_MEMBERSHIP", "PROVIDER_CONCEPT"),
        "BOARD": ("LISTED_BOARD", "EASTMONEY_LEVEL1"),
        "TYPE": ("SECURITY_TYPE", "A_SHARE", "H_SHARE"),
    }.get(normalized_dimension, ())
    return next((definitions.get((normalized_dimension, candidate)) for candidate in candidates
                 if definitions.get((normalized_dimension, candidate))), None)


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
    definition = _definition_for(definitions, dimension, item.get("code"))
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
        # ``dimension`` is the master-data dimension for legacy callers.  A
        # typed fact can retain its original dimension separately so SIZE,
        # STYLE and LEGAL_LISTING_CLASS remain auditable inside the combined
        # display group.
        fact_dimension = item.get("classification_dimension")
        item["dimension"] = str(fact_dimension or definition.dimension)
        item["master_dimension"] = definition.dimension
        item["taxonomy"] = definition.taxonomy
    return item


def _foundation_classifications(
    db: Session,
    stock: StockSymbol,
) -> tuple[
    list[dict[str, Any]],  # industries
    list[dict[str, Any]],  # themes
    list[dict[str, Any]],  # indexes
    list[dict[str, Any]],  # listed boards
    list[dict[str, Any]],  # security/type classifications
]:
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
            return [], [], [], [], []
        listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id))
        if listing is None:
            return [], [], [], [], []
        rows = list(db.scalars(select(SecurityClassification).where(
            SecurityClassification.security_id == listing.security_id,
            SecurityClassification.status.in_(("ACCEPTED", "PENDING")),
        ).order_by(SecurityClassification.created_at.desc())))
        industries: list[dict[str, Any]] = []
        themes: list[dict[str, Any]] = []
        indexes: list[dict[str, Any]] = []
        boards: list[dict[str, Any]] = []
        type_values: list[dict[str, Any]] = []
        for row in rows:
            dimension = str(row.dimension or "").upper()
            if dimension == "INDUSTRY":
                target = industries
            elif dimension == "THEME":
                target = themes
            elif dimension == "INDEX":
                target = indexes
            elif dimension == "BOARD":
                target = boards
            elif dimension in {"SIZE", "STYLE", "QUALITY", "LEGAL_LISTING_CLASS", "LIQUIDITY", "RISK", "TYPE"}:
                target = type_values
            else:
                target = None
            if target is None:
                continue
            props = row.properties_json if isinstance(row.properties_json, dict) else {}
            value = _item(
                row.label,
                code=row.code,
                source="公司/证券分类主数据",
                status=row.status,
                definition=props.get("definition"),
                criteria=props.get("criteria"),
                source_name=props.get("source_name"),
                source_url=props.get("source_url"),
                definition_version=row.definition_version,
                level=props.get("level"),
                classification_dimension=dimension,
            )
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
                    boards.append(value)
            labels = _split(raw.get("BLGAINIAN"))
            codes = _split(raw.get("BLGAINIAN_CODE"))
            for index, label in enumerate(labels):
                value = _item(label, code=codes[index] if index < len(codes) else None,
                              source="东方财富主题板块", status="SOURCE")
                if value:
                    (indexes if _looks_like_index(label, value.get("code")) else themes).append(value)
        return industries, themes, indexes, boards, type_values
    except (ValueError, TypeError, json.JSONDecodeError):
        return [], [], [], [], []


def build_classification_groups(db: Session | None, stock: StockSymbol,
                                profile_fields: dict[str, Any] | None = None) -> list[dict[str, Any]]:
    fields = profile_fields if isinstance(profile_fields, dict) else {}
    groups: list[dict[str, Any]] = []
    definitions = _definition_lookup(db)
    industry_values, theme_values, index_values, board_values, type_values = (
        _foundation_classifications(db, stock) if db is not None else ([], [], [], [], [])
    )

    if not industry_values:
        for index, value in enumerate(_clean_industry(fields.get("所属行业") or fields.get("行业"))):
            item = _item(value, source="公司资料 / F10", status="SOURCE", level=index + 1,
                         classification_dimension="INDUSTRY")
            if item:
                industry_values.append(item)
    industry_values = [_attach_definition(item, definitions, "INDUSTRY") for item in industry_values]
    if industry_values:
        # Keep only the hierarchy labels in the industry group; themes are
        # displayed separately below and indexes never enter either group.
        groups.append(_group("industry", "所属行业", industry_values))

    board_values = board_values or []
    board_values.extend(
        _item(value, source="证券主数据 / F10", status="SOURCE", classification_dimension="BOARD")
        for value in _split(fields.get("板块") or fields.get("上市板块") or fields.get("所属市场"))
    )
    board_items = [item for item in board_values if item]
    board_items = [item for item in board_items if item]
    if board_items:
        groups.append(_group("board", "上市板块", board_items))

    if not theme_values:
        # ``所属概念`` is accepted only as a theme fallback.  ``入选指数`` is
        # intentionally not read here, since an index is its own dimension.
        for value in _split(fields.get("所属概念") or fields.get("概念")):
            item = _item(value, source="F10 概念字段", status="SOURCE", classification_dimension="THEME")
            if item:
                theme_values.append(item)
    theme_values = [_attach_definition(item, definitions, "THEME") for item in theme_values if not _looks_like_index(item.get("label"), item.get("code"))]
    if theme_values:
        groups.append(_group("theme", "主题板块", theme_values))

    index_items = [_attach_definition(item, definitions, "INDEX") for item in index_values]
    index_items.extend(_attach_definition(_item(value, source="F10 入选指数", status="SOURCE", classification_dimension="INDEX"), definitions, "INDEX") for value in _split(fields.get("入选指数")))
    index_items = [item for item in index_items if item]
    if index_items:
        groups.append(_group("index", "入选指数", index_items))

    type_items: list[dict[str, Any]] = list(type_values)
    if stock.asset_type:
        item = _attach_definition(_item(stock.asset_type, source="证券主数据", status="SOURCE",
                                        classification_dimension="TYPE"), definitions, "TYPE")
        if item:
            type_items.append(item)
    type_items = [_attach_definition(item, definitions, item.get("classification_dimension") or "TYPE")
                  for item in type_items if item]
    if type_items:
        groups.append(_group("type", "证券类型", type_items))
    # Enrich every emitted label with the versioned glossary.  Applying this
    # at the final projection boundary also covers legacy foundation facts and
    # avoids mutating their source payloads.
    for group in groups:
        for item in group.get("items", []):
            # A combined display group (notably ``type``) can contain SIZE,
            # STYLE, LEGAL_LISTING_CLASS, etc. facts.  Resolve the glossary
            # using each fact's original dimension instead of overwriting its
            # master metadata with the UI group dimension.
            dimension = str(item.get("classification_dimension") or "").upper()
            if not dimension:
                dimension = {"industry": "INDUSTRY", "board": "BOARD", "theme": "THEME",
                             "index": "INDEX", "type": "TYPE"}.get(str(group.get("key")), "")
            if dimension:
                _attach_definition(item, definitions, dimension)
    return [item for item in groups if item]
