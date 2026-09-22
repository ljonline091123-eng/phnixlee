"""Bounded, read-only projection of security master, facts and their evidence.

Canonical identities join the existing stores. This module never rebuilds a
legacy graph, changes a review decision, or treats document mentions as causes.
"""

from collections import Counter, deque
from datetime import datetime, timezone
import hashlib
import json

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from app.models.ai_hub import KnowledgeDocument, KnowledgeEntity, KnowledgeGraph
from app.models.company_graph import SecurityClassification, SourceIdentity
from app.models.company_mapping import CompanyMappingState
from app.models.foundation import FoundationEntity, FoundationEvidence, FoundationFact, FoundationFactEvidence, FoundationListing, FoundationSecurity
from app.models.market_data import DataSource, StockSymbol
from app.models.taxonomy import ClassificationDefinition
from app.services import foundation
from app.services.graph_identity import canonical_company_id, canonical_security_id, legacy_stock_pair


VERSION = "KNOWLEDGE_NETWORK_V1"
TYPE_LABELS = {"SECURITY": "证券", "COMPANY": "公司", "MARKET": "交易市场", "INDUSTRY": "行业",
    "THEME": "板块主题", "CLASSIFICATION": "类型标签", "PERSON": "自然人", "ORGANIZATION": "机构",
    "HOLDER_ACCOUNT": "股东账户", "SUPPLY_CHAIN_FACT": "供应链事实", "BUSINESS_FACT": "经营事实",
    "LEGAL_CASE": "司法事项", "PRODUCT": "产品或材料", "EVIDENCE_DOCUMENT": "证据文档", "DATA_SOURCE": "数据来源"}
RELATIONS = {"ISSUED_BY": "发行主体", "LISTED_ON": "上市于", "HOLDS_EQUITY": "直接持股", "CONTROLS": "控制",
    "IN_INDUSTRY": "所属行业", "IN_SECTOR": "所属板块", "MEMBER_OF_THEME": "所属板块", "HAS_CLASSIFICATION": "类型归属",
    "SUB_INDUSTRY_OF": "上级行业", "SUPPLIES_TO": "供应", "PARTNERS_WITH": "合作", "CONTRACT": "合同",
    "GUARANTEES": "担保", "LENDS_TO": "借贷", "COMPETES_WITH": "竞争", "LEGAL_CASE": "涉及司法事项",
    "SUBJECT": "主体", "OBJECT": "关联对象", "INVOLVES_PRODUCT": "涉及产品", "SUPPORTED_BY": "证据支持", "FROM_SOURCE": "来自来源"}
MARKETS = {"CN_A": "中国 A 股", "HK": "香港市场", "NEEQ": "新三板", "NEEQ_INNOVATION": "新三板创新层"}
FACT_NODES = {"SUPPLIES_TO": "SUPPLY_CHAIN_FACT", "PARTNERS_WITH": "BUSINESS_FACT", "CONTRACT": "BUSINESS_FACT",
    "GUARANTEES": "BUSINESS_FACT", "LENDS_TO": "BUSINESS_FACT", "LEGAL_CASE": "LEGAL_CASE"}
CATEGORIES = {"identity", "classification", "ownership", "financial", "market", "disclosure", "business", "legal", "evidence"}
DISCLOSURE_TABLES = {"stock_news", "stock_notice", "research_report", "stock_f10_cache", "stock_context_event"}


def _key(prefix, *parts):
    return prefix + ":" + hashlib.sha256(json.dumps(parts, ensure_ascii=False, sort_keys=True, default=str).encode()).hexdigest()[:28]


def _entity_id(row):
    return canonical_company_id(row.id) if row.entity_type == "COMPANY" else f"entity:{row.id}"


def _status(value):
    return {"ACCEPTED": "VERIFIED", "PENDING": "CANDIDATE"}.get(value, value)


class Projection:
    def __init__(self):
        self.nodes, self.edges, self.evidence = {}, {}, {}
        self.truncated = False
        self.notes = []

    def node(self, node_id, kind, label, *, layer="MASTER", status="OBSERVED", properties=None, evidence_ids=None):
        previous = self.nodes.get(node_id, {})
        self.nodes[node_id] = {"id": node_id, "type": kind, "type_label": TYPE_LABELS.get(kind, kind), "label": label,
            "layer": layer, "status": status, "properties": {**previous.get("properties", {}), **(properties or {})},
            "evidence_ids": sorted(set(previous.get("evidence_ids", []) + (evidence_ids or [])))}
        return node_id

    def edge(self, edge_id, source, target, kind, *, layer="MASTER", status="OBSERVED", properties=None,
             evidence_ids=None, method="SOURCE", valid_from=None, valid_to=None, observed_at=None, label=None):
        self.edges[edge_id] = {"id": edge_id, "source": source, "target": target, "type": kind,
            "label": label or RELATIONS.get(kind, kind), "layer": layer, "status": status,
            "verification_status": status, "extraction_method": method, "source_reliability": None,
            "extraction_confidence": None, "valid_from": valid_from, "valid_to": valid_to,
            "observed_at": observed_at, "version": VERSION, "properties": properties or {}, "evidence_ids": evidence_ids or []}

    def proof(self, row: FoundationEvidence):
        proof_id = f"evidence:foundation:{row.id}"
        self.evidence[proof_id] = {"id": proof_id, "title": row.title, "source_name": row.source_name,
            "source_url": row.url, "published_at": row.published_at, "observed_at": row.available_at,
            "excerpt": row.content[:1200], "source_table": "foundation_evidence", "source_record_id": row.id,
            "content_hash": row.content_hash, "version": row.version,
            "detail_url": f"/api/v1/knowledge-network/evidence?source_table=foundation_evidence&record_id={row.id}"}
        return proof_id

    def merge(self, part):
        self.nodes.update((node["id"], node) for node in part.get("nodes", []))
        self.edges.update((edge["id"], edge) for edge in part.get("edges", []))
        self.evidence.update((proof["id"], proof) for proof in part.get("evidence", []))
        self.notes.extend(part.get("warnings", []))
        self.truncated |= bool(part.get("truncated"))


def _resolve_root(db, *, market, symbol, company_id, center_id, graph_id):
    centers, legacy = [], None
    if center_id:
        if market or symbol or company_id:
            raise foundation.FoundationError("中心标识与股票/公司参数不能同时指定", 422)
        if center_id.startswith("security:") and len(center_id.split(":")) == 3:
            _, market, symbol = center_id.split(":")
        elif center_id.startswith("company:"):
            company_id = center_id.removeprefix("company:")
        else:
            raise foundation.FoundationError("请选择证券或公司作为图谱中心", 422)
    if company_id and (market or symbol) or bool(market) != bool(symbol):
        raise foundation.FoundationError("请提供一组完整的市场和股票代码，或一个公司标识", 422)
    if graph_id is not None:
        legacy = db.get(KnowledgeGraph, graph_id)
        if legacy is None:
            raise foundation.FoundationError("原知识图谱不存在", 404)
        rows = db.scalars(select(KnowledgeEntity).where(KnowledgeEntity.graph_id == graph_id,
            KnowledgeEntity.entity_type == "STOCK").order_by(KnowledgeEntity.id).limit(201)).all()
        seen = set()
        for row in rows[:200]:
            pair = legacy_stock_pair(row)
            if pair and pair not in seen:
                centers.append({"id": canonical_security_id(*pair), "market": pair[0], "symbol": pair[1], "label": row.entity_name})
                seen.add(pair)
        if not (market or company_id):
            if not centers:
                raise foundation.FoundationError("该旧图谱没有可定位的证券节点，请从股票或公司进入统一图谱", 404)
            market, symbol = centers[0]["market"], centers[0]["symbol"]
        if market and (market, symbol) not in seen:
            raise foundation.FoundationError("所选证券不在该旧图谱范围内", 404)
        if company_id:
            raise foundation.FoundationError("旧图谱入口请使用图谱内证券作为中心", 422)
    stock, company = None, None
    if company_id:
        company = db.get(FoundationEntity, company_id)
        if company is None or company.entity_type != "COMPANY":
            raise foundation.FoundationError("公司主体不存在", 404)
    else:
        query = select(StockSymbol)
        if market:
            query = query.where(StockSymbol.market == market, StockSymbol.symbol == symbol)
        else:
            query = query.join(FoundationListing, FoundationListing.stock_symbol_id == StockSymbol.id)
        stock = db.scalar(query.order_by(StockSymbol.market, StockSymbol.symbol).limit(1))
        if stock is None:
            raise foundation.FoundationError("未找到股票主数据", 404)
        company = db.scalar(select(FoundationEntity).join(FoundationSecurity, FoundationSecurity.entity_id == FoundationEntity.id)
            .join(FoundationListing, FoundationListing.security_id == FoundationSecurity.id).where(FoundationListing.stock_symbol_id == stock.id))
    return stock, company, centers, legacy


def _add_entity(p, row):
    return p.node(_entity_id(row), row.entity_type, row.name, properties={"company_id": row.id if row.entity_type == "COMPANY" else None,
        "entity_id": row.id, "jurisdiction": row.jurisdiction, "identifier_scheme": row.identifier_scheme,
        "identifier_value": row.identifier_value, **(row.properties_json or {})})


def _add_stock(db, p, stock, listing=None, security=None):
    node_id = canonical_security_id(stock.market, stock.symbol)
    proofs = []
    if listing:
        proof = db.get(FoundationEvidence, listing.evidence_id)
        if proof:
            proofs.append(p.proof(proof))
    state = db.get(CompanyMappingState, stock.id) if not listing else None
    p.node(node_id, "SECURITY", f"{stock.name} · {stock.symbol}", properties={"market": stock.market, "symbol": stock.symbol,
        "stock_symbol_id": stock.id, "exchange": stock.exchange, "listing_status": stock.status, "listed_at": stock.list_date,
        "security_id": security.id if security else None, "listing_id": listing.id if listing else None,
        "company_id": security.entity_id if security else None, "share_class": security.share_class if security else None,
        "mapping_status": "MAPPED" if listing else state.status if state else "UNMAPPED",
        "mapping_reason": state.reason if state else None}, evidence_ids=proofs)
    market_id = "market:" + stock.market + ":" + stock.exchange
    p.node(market_id, "MARKET", MARKETS.get(stock.market, stock.market) + " · " + stock.exchange,
        properties={"market": stock.market, "exchange": stock.exchange})
    source = db.get(DataSource, stock.source_id)
    master_proof = f"evidence:stock_symbol:{stock.id}"
    p.evidence[master_proof] = {"id": master_proof, "title": f"{stock.name}证券主数据", "source_name": source.source_name if source else "证券主数据",
        "source_table": "stock_symbol", "source_record_id": stock.id, "observed_at": stock.last_synced_at,
        "published_at": None, "excerpt": f"{stock.market} {stock.symbol} {stock.exchange}",
        "detail_url": f"/api/v1/knowledge-network/evidence?source_table=stock_symbol&record_id={stock.id}"}
    p.nodes[node_id]["evidence_ids"] = sorted(set(proofs + [master_proof]))
    p.edge("listed:" + node_id, node_id, market_id, "LISTED_ON", evidence_ids=[master_proof], observed_at=stock.last_synced_at)
    if listing and security:
        p.edge("issuer:" + listing.id, node_id, canonical_company_id(security.entity_id), "ISSUED_BY",
            evidence_ids=proofs, observed_at=listing.created_at, properties={"listing_id": listing.id,
                "identity_basis": "SOURCE_IDENTIFIERS", "registry_verified": False})
    return node_id


def _classification_node(p, *, dimension, code, label, taxonomy, version, definition=None, properties=None, node_id=None):
    kind = "INDUSTRY" if dimension == "INDUSTRY" else "THEME" if dimension == "THEME" else "CLASSIFICATION"
    node_id = node_id or _key("classification", taxonomy, dimension, code, version)
    props = {"dimension": dimension, "code": code, "taxonomy": taxonomy, "definition_version": version,
        **(properties or {})}
    if definition:
        props.update(definition=definition.definition, criteria=definition.criteria, definition_source=definition.source_name)
    else:
        props["definition_status"] = "UNREGISTERED"
    p.node(node_id, kind, label, properties=props)
    return node_id


def _classifications(db, p, listings, include_candidates):
    ids = [listing.security_id for listing in listings]
    by_security = {listing.security_id: listing for listing in listings}
    rows = list(db.scalars(select(SecurityClassification).where(SecurityClassification.security_id.in_(ids),
        SecurityClassification.status.in_(["ACCEPTED", "PENDING"] if include_candidates else ["ACCEPTED"]))
        .order_by(SecurityClassification.created_at.desc()).limit(161))) if ids else []
    p.truncated |= len(rows) > 160
    definitions = list(db.scalars(select(ClassificationDefinition).where(ClassificationDefinition.status == "ACTIVE")))
    for row in rows[:160]:
        taxonomy = row.properties_json.get("taxonomy")
        matching = [item for item in definitions if item.dimension == row.dimension and item.code == row.code
            and item.definition_version == row.definition_version and (not taxonomy or item.taxonomy == taxonomy)]
        definition = matching[0] if len(matching) == 1 else None
        taxonomy = taxonomy or (definition.taxonomy if definition else row.method)
        node_id = _classification_node(p, dimension=row.dimension, code=row.code, label=row.label,
            taxonomy=taxonomy, version=row.definition_version, definition=definition, properties=row.properties_json)
        listing = by_security[row.security_id]
        proof = db.get(FoundationEvidence, row.evidence_id)
        proof_ids = [p.proof(proof)] if proof else []
        p.nodes[node_id]["evidence_ids"] = sorted(set(p.nodes[node_id]["evidence_ids"] + proof_ids))
        p.edge("classification:" + row.id, canonical_security_id(listing.market, listing.symbol), node_id,
            "IN_INDUSTRY" if row.dimension == "INDUSTRY" else "IN_SECTOR" if row.dimension == "THEME" else "HAS_CLASSIFICATION",
            status=_status(row.status), evidence_ids=proof_ids, method=row.method, valid_from=row.valid_from,
            valid_to=row.valid_to, observed_at=row.created_at, properties={"assignment_id": row.id, **row.properties_json})
    # Read literal provider classification fields already archived with mappings.
    # This makes new bulk-mapped companies usable without rewriting reviewed facts.
    from app.connectors.company_master_sources import parse_master_record
    for listing in listings:
        proof = db.get(FoundationEvidence, listing.evidence_id)
        if proof is None or proof.source_name != "EASTMONEY":
            continue
        try:
            raw = json.loads(proof.content)
            record = parse_master_record(raw, listing.market, listing.symbol, foundation.aware(proof.available_at).isoformat())
        except (ValueError, TypeError, KeyError):
            continue
        if not record:
            continue
        proof_id = p.proof(proof)
        previous = None
        for dimension, values in (("INDUSTRY", record.get("industries", [])), ("THEME", record.get("themes", []))):
            for item in values[:40]:
                code, taxonomy, version = item["code"], item["taxonomy"], item["taxonomy_version"]
                matching = [value for value in definitions if value.dimension == dimension and value.code == code
                    and value.taxonomy == taxonomy and value.definition_version == version]
                alias = db.scalar(select(SourceIdentity).where(SourceIdentity.namespace == "EASTMONEY",
                    SourceIdentity.external_id == item.get("source_issuer_id"), SourceIdentity.entity_type == dimension))
                entity = db.get(FoundationEntity, alias.entity_id) if alias else None
                node_id = _classification_node(p, dimension=dimension, code=code, label=item["name"], taxonomy=taxonomy,
                    version=version, definition=matching[0] if len(matching) == 1 else None,
                    node_id=_entity_id(entity) if entity else None, properties={"provider_reported": True, "level": item.get("level")})
                sid = canonical_security_id(listing.market, listing.symbol)
                kind = "IN_INDUSTRY" if dimension == "INDUSTRY" else "IN_SECTOR"
                if not any(edge["source"] == sid and edge["target"] == node_id and edge["type"] == kind for edge in p.edges.values()):
                    p.edge(_key("source-classification", listing.id, dimension, code), sid, node_id, kind,
                        evidence_ids=[proof_id], observed_at=proof.available_at, properties={"taxonomy": taxonomy,
                            "taxonomy_version": version, "scope": "PROVIDER_SECURITY_CLASSIFICATION"})
                p.nodes[node_id]["evidence_ids"] = sorted(set(p.nodes[node_id]["evidence_ids"] + [proof_id]))
                if dimension == "INDUSTRY":
                    if previous:
                        p.edge(_key("industry-parent", node_id, previous, proof_id), node_id, previous, "SUB_INDUSTRY_OF",
                            evidence_ids=[proof_id], observed_at=proof.available_at)
                    previous = node_id
            p.truncated |= len(values) > 40


def _master(db, p, stock, company, *, depth, include_candidates, budget):
    entities = {company.id: company} if company else {}
    facts = {}
    frontier = {company.id} if company else set()
    statuses = ["ACCEPTED", "PENDING"] if include_candidates else ["ACCEPTED"]
    for _ in range(depth):
        if not frontier or len(facts) >= budget:
            break
        rows = list(db.scalars(select(FoundationFact).where(FoundationFact.status.in_(statuses),
            or_(FoundationFact.subject_entity_id.in_(frontier), FoundationFact.object_entity_id.in_(frontier)))
            .order_by(FoundationFact.created_at.desc(), FoundationFact.id).limit(budget + 1)))
        p.truncated |= len(rows) > budget
        following = set()
        for row in rows[:budget]:
            if row.id in facts:
                continue
            if len(facts) >= budget:
                p.truncated = True
                break
            facts[row.id] = row
            for entity_id in (row.subject_entity_id, row.object_entity_id):
                if entity_id and entity_id not in entities:
                    entity = db.get(FoundationEntity, entity_id)
                    if entity:
                        entities[entity_id] = entity
                        if entity.entity_type in {"COMPANY", "ORGANIZATION", "PERSON", "HOLDER_ACCOUNT"}:
                            following.add(entity_id)
        frontier = following
    for entity in entities.values():
        _add_entity(p, entity)
    listings = list(db.scalars(select(FoundationListing).join(FoundationSecurity)
        .where(FoundationSecurity.entity_id.in_(list(entities))).order_by(FoundationListing.market, FoundationListing.symbol).limit(41))) if entities else []
    p.truncated |= len(listings) > 40
    listings = listings[:40]
    root_listing = db.scalar(select(FoundationListing).where(FoundationListing.stock_symbol_id == stock.id)) if stock else None
    if root_listing and root_listing not in listings:
        listings.insert(0, root_listing)
    for listing in listings:
        item = db.get(StockSymbol, listing.stock_symbol_id)
        _add_stock(db, p, item, listing, db.get(FoundationSecurity, listing.security_id))
    if stock and root_listing is None:
        _add_stock(db, p, stock)
    for fact in facts.values():
        source = _entity_id(entities[fact.subject_entity_id])
        target = _entity_id(entities[fact.object_entity_id]) if fact.object_entity_id else None
        proofs = [p.proof(proof) for proof in foundation.fact_evidence(db, fact.id)]
        state = _status(fact.status)
        props = {**fact.properties_json, "fact_id": fact.id, "title": fact.title}
        common = {"status": state, "properties": props, "evidence_ids": proofs, "valid_from": fact.valid_from,
            "valid_to": fact.valid_to, "observed_at": fact.created_at, "layer": "FACT"}
        if fact.fact_type in FACT_NODES:
            fact_id = "fact:" + fact.id
            p.node(fact_id, FACT_NODES[fact.fact_type], fact.title, layer="FACT", status=state, properties=props, evidence_ids=proofs)
            p.edge("subject:" + fact.id, source, fact_id, fact.fact_type, **common)
            if target:
                p.edge("object:" + fact.id, fact_id, target, "OBJECT", **common)
            product = fact.properties_json.get("product")
            if isinstance(product, str) and product.strip():
                # A product mention is scoped to its fact until separately resolved.
                product_id = _key("product-mention", fact.id, product)
                p.node(product_id, "PRODUCT", product[:128], properties={"identity_scope": "FACT_LOCAL_MENTION"}, evidence_ids=proofs)
                p.edge("product:" + fact.id, fact_id, product_id, "INVOLVES_PRODUCT", **common)
        elif target:
            label = RELATIONS.get(fact.fact_type, fact.fact_type)
            if fact.fact_type == "HOLDS_EQUITY" and fact.properties_json.get("ratio") is not None:
                label += f" {fact.properties_json['ratio']}%"
            p.edge("fact:" + fact.id, source, target, fact.fact_type, label=label, **common)
    _classifications(db, p, listings, include_candidates)
    return listings, list(facts.values())


def _attach_evidence(p):
    # Side-panel references stay available even when the canvas budget omits proof nodes.
    items = list(p.nodes.values()) + list(p.edges.values())
    for proof in list(p.evidence.values()):
        if proof["id"] not in p.nodes:
            p.node(proof["id"], "EVIDENCE_DOCUMENT", proof["title"], layer="EVIDENCE", properties={key: value for key, value in proof.items()
                if key not in {"id", "title", "excerpt"}}, evidence_ids=[proof["id"]])
        source_id = proof.get("source_id") or _key("source", proof.get("source_name", "来源未标明"))
        if source_id not in p.nodes:
            p.node(source_id, "DATA_SOURCE", proof.get("source_name") or "来源未标明", layer="EVIDENCE")
        p.edge("from:" + proof["id"], proof["id"], source_id, "FROM_SOURCE", layer="EVIDENCE", evidence_ids=[proof["id"]])
    for item in items:
        anchor = item.get("source") or item["id"]
        for proof_id in item.get("evidence_ids", []):
            if proof_id in p.evidence and anchor != proof_id:
                p.edge(_key("proof", anchor, proof_id), anchor, proof_id, "SUPPORTED_BY", layer="EVIDENCE", evidence_ids=[proof_id])


def _scope_category(p, category, root_id, issuer_id, include_evidence):
    """Keep a domain and real connecting paths before applying the canvas budget.

    Source counts remain separate from this bounded projection. Category cards
    are a UI navigation aid, so no synthetic business relations are created.
    """
    kinds = {
        "identity": {"SECURITY", "COMPANY", "MARKET"},
        "classification": {"INDUSTRY", "THEME", "CLASSIFICATION"},
        "financial": {"FINANCIAL_OBSERVATION"},
        "market": {"MARKET_OBSERVATION", "MARKET_EVENT"},
        "business": {"SUPPLY_CHAIN_FACT", "BUSINESS_FACT", "PRODUCT"},
        "legal": {"LEGAL_CASE"},
        "evidence": {"EVIDENCE_DOCUMENT", "DATA_SOURCE"},
    }
    predicates = {
        "ownership": {"HOLDS_EQUITY", "CONTROLS"},
        "business": {"SUPPLIES_TO", "PARTNERS_WITH", "CONTRACT", "GUARANTEES", "LENDS_TO", "COMPETES_WITH", "INVOLVES_PRODUCT"},
    }
    selected = {node["id"] for node in p.nodes.values() if node["type"] in kinds.get(category, set())}
    for node in p.nodes.values():
        props = node.get("properties", {})
        if category == "disclosure" and props.get("source_table") in DISCLOSURE_TABLES:
            selected.add(node["id"])
        if node["type"] == "CANDIDATE_EVENT" and (
            category == "legal" and props.get("event_type") == "LEGAL"
            or category == "ownership" and props.get("event_type") == "SHAREHOLDER"
            or category == "business" and props.get("event_type") in {"SUPPLY_CHAIN", "CONTRACT", "RAW_MATERIAL"}
        ):
            selected.add(node["id"])
    for edge in p.edges.values():
        if edge["type"] in predicates.get(category, set()):
            selected.update((edge["source"], edge["target"]))
    # Context paths must describe actual relationships, rather than shortcuts
    # through two documents that happen to use the same provider.
    adjacency = {}
    for edge in p.edges.values():
        if edge["source"] not in p.nodes or edge["target"] not in p.nodes:
            continue
        if category != "evidence" and edge["type"] == "FROM_SOURCE":
            continue
        if category not in {"disclosure", "evidence"} and edge.get("layer") == "EVIDENCE":
            continue
        adjacency.setdefault(edge["source"], []).append(edge["target"])
        adjacency.setdefault(edge["target"], []).append(edge["source"])
    parents = {root_id: None}
    queue = deque([root_id])
    while queue:
        source = queue.popleft()
        for target in adjacency.get(source, []):
            if target not in parents:
                parents[target] = source
                queue.append(target)
    keep = {root_id}
    for node_id in selected | ({issuer_id} if issuer_id else set()):
        while node_id in parents and node_id not in keep:
            keep.add(node_id)
            node_id = parents[node_id]
    if include_evidence and category != "evidence":
        scoped_items = [p.nodes[key] for key in keep if key in p.nodes] + [edge for edge in p.edges.values()
            if edge["source"] in keep and edge["target"] in keep]
        proofs = {key for item in scoped_items for key in item.get("evidence_ids", [])}
        keep.update(proofs & p.nodes.keys())
        keep.update(edge["target"] for edge in p.edges.values() if edge["source"] in proofs and edge["type"] == "FROM_SOURCE")
    p.nodes = {key: node for key, node in p.nodes.items() if key in keep}
    p.edges = {key: edge for key, edge in p.edges.items() if edge["source"] in keep and edge["target"] in keep}
    return selected - {root_id, issuer_id}


def explore(db: Session, *, market=None, symbol=None, company_id=None, center_id=None, graph_id=None,
            depth=1, max_nodes=160, max_edges=300, include_candidates=False, include_evidence=True, per_category=3, category=None):
    if category is not None and category not in CATEGORIES:
        raise foundation.FoundationError("未知图谱分类", 422)
    stock, company, centers, legacy = _resolve_root(db, market=market, symbol=symbol, company_id=company_id,
        center_id=center_id, graph_id=graph_id)
    p = Projection()
    listings, facts = _master(db, p, stock, company, depth=depth, include_candidates=include_candidates, budget=min(60, max_edges // 3))
    root_id = canonical_security_id(stock.market, stock.symbol) if stock else canonical_company_id(company.id)
    from app.services.knowledge_network_dynamic import build_dynamic_projection
    targets = [stock] if stock else [db.get(StockSymbol, listing.stock_symbol_id) for listing in listings
        if db.get(FoundationSecurity, listing.security_id).entity_id == company.id][:4]
    coverage_counts, sampled_counts = Counter(), Counter()
    for target in targets:
        part = build_dynamic_projection(db, target.market, target.symbol, company_id=company.id if company else None,
            per_category=per_category, include_evidence=include_evidence or category in {"disclosure", "evidence"}, graph_id=graph_id)
        p.merge(part)
        for key, item in part.get("coverage", {}).items():
            coverage_counts[key] += item["total_records"]
            sampled_counts[key] += item.get("shown_records", 0)
            p.truncated |= item.get("truncated", False)
    if stock is None and len([item for item in listings if db.get(FoundationSecurity, item.security_id).entity_id == company.id]) > 4:
        p.truncated = True
        p.notes.append("公司动态资料最多展开四条上市记录，可双击具体证券查看其完整范围。")
    if legacy:
        p.notes.append("以所选旧图谱中的证券为中心，补充当前公司主数据及已入库动态资料；旧图谱和原文保持不变。")
    if not include_candidates:
        candidates = {node["id"] for node in p.nodes.values() if node.get("status") in {"CANDIDATE", "PENDING"}}
        p.nodes = {key: node for key, node in p.nodes.items() if key not in candidates}
        p.edges = {key: edge for key, edge in p.edges.items() if edge.get("status") not in {"CANDIDATE", "PENDING"}
            and edge["source"] not in candidates and edge["target"] not in candidates}
    if include_evidence or category == "evidence":
        _attach_evidence(p)
    else:
        # In the disclosure domain the documents themselves are the objects
        # being inspected. Hiding auxiliary provenance must not hide all news.
        p.nodes = {key: node for key, node in p.nodes.items() if node["layer"] != "EVIDENCE"
            or category == "disclosure" and node.get("properties", {}).get("source_table") in DISCLOSURE_TABLES}
        p.edges = {key: edge for key, edge in p.edges.items() if edge["source"] in p.nodes and edge["target"] in p.nodes}
    # Coverage describes the full bounded neighborhood, even while a single
    # domain is expanded. Source-record totals are counted before row sampling.
    by_type = Counter(node["type"] for node in p.nodes.values())
    # Reserve the center and its issuer, then show a connected bounded projection.
    issuer_id = canonical_company_id(company.id) if company else None
    category_node_ids = _scope_category(p, category, root_id, issuer_id, include_evidence) if category else set()
    priority = {root_id: -2, issuer_id: -1}
    ordered = sorted(p.nodes.values(), key=lambda node: (priority.get(node["id"], {"MASTER": 0, "FACT": 1, "EVIDENCE": 2}.get(node["layer"], 1)),
        node["type"], node["id"]))
    before_nodes, before_edges = len(ordered), len(p.edges)
    nodes = ordered[:max_nodes]
    ids = {node["id"] for node in nodes}
    edges = sorted((edge for edge in p.edges.values() if edge["source"] in ids and edge["target"] in ids),
        key=lambda edge: (edge["type"] not in {"ISSUED_BY", "LISTED_ON"}, edge.get("layer") == "EVIDENCE", edge["id"]))[:max_edges]
    connected = {root_id}
    while True:
        additions = {endpoint for edge in edges if edge["source"] in connected or edge["target"] in connected
            for endpoint in (edge["source"], edge["target"])} - connected
        if not additions:
            break
        connected |= additions
    nodes = [node for node in nodes if node["id"] in connected]
    edges = [edge for edge in edges if edge["source"] in connected and edge["target"] in connected]
    dimensions = [("identity", "证券身份", int(stock is not None or bool(listings))), ("company", "公司主体", int(company is not None)),
        ("industry", "行业", by_type["INDUSTRY"]), ("theme", "板块主题", by_type["THEME"]),
        ("classification", "类型标签", by_type["CLASSIFICATION"]),
        ("ownership", "股权与控制", sum(f.fact_type in {"HOLDS_EQUITY", "CONTROLS"} for f in facts)),
        ("supply_chain", "供应链", sum(f.fact_type == "SUPPLIES_TO" for f in facts)),
        ("business", "经营往来", sum(f.fact_type in {"CONTRACT", "PARTNERS_WITH", "GUARANTEES", "LENDS_TO"} for f in facts)),
        ("legal", "司法事项", sum(f.fact_type == "LEGAL_CASE" for f in facts))]
    dynamic_labels = {"financial": "财务观测", "market": "日线量价", "realtime": "实时行情", "news": "新闻", "notices": "公告", "research": "研究报告",
        "f10": "F10资料", "external_events": "外部事件", "shareholder_actions": "股东行为"}
    dimensions += [(key, dynamic_labels.get(key, key), count) for key, count in coverage_counts.items()]
    coverage = [{"key": key, "label": label, "status": "PRESENT" if count else "NOT_COLLECTED", "count": count}
        for key, label, count in dimensions]
    coverage_scope = {key: {"count_unit": "SOURCE_RECORDS" if key in coverage_counts else
        "PROJECTED_FACTS" if key in {"ownership", "supply_chain", "business", "legal"} else "PROJECTED_NODES",
        "count_scope": "SELECTED_SECURITIES" if key in coverage_counts else "BOUNDED_NEIGHBORHOOD",
        **({"sampled_records": sampled_counts[key]} if key in coverage_counts else {})} for key, _, _ in dimensions}
    used_proofs = {proof for item in [*nodes, *edges] for proof in item.get("evidence_ids", [])}
    evidence = [proof for key, proof in p.evidence.items() if key in used_proofs]
    return {"center_id": root_id, "centers": centers, "nodes": nodes, "edges": edges, "evidence": evidence,
        "coverage": coverage, "coverage_scope": coverage_scope, "stats": {"nodes": len(nodes), "edges": len(edges), "evidence": len(evidence),
            "available_nodes": before_nodes, "available_edges": before_edges, "covered_dimensions": sum(item["count"] > 0 for item in coverage),
            "total_dimensions": len(coverage), "candidate_nodes": sum(node.get("status") == "CANDIDATE" for node in nodes)},
        "truncated": p.truncated or len(nodes) < before_nodes or len(edges) < before_edges,
        "notes": list(dict.fromkeys(p.notes)), "version": VERSION, "snapshot_at": datetime.now(timezone.utc),
        "governance_status": "PARTIAL", "scope": "CURRENT_STORED_DATA", "legacy_graph_id": graph_id,
        "legacy_graph_name": legacy.graph_name if legacy else None, "include_candidates": include_candidates,
        "category": category, "category_node_ids": sorted(category_node_ids & {node["id"] for node in nodes}),
        "limits": {"max_nodes": max_nodes, "max_edges": max_edges, "per_category": per_category, "depth": depth, "category": category},
        "absence_is_not_negative_fact": True}


def evidence_detail(db: Session, source_table: str, record_id: str) -> dict:
    """Only explicitly whitelisted business records; never arbitrary SQL tables."""
    from app.services.knowledge_network_dynamic import SUPPORTED_SOURCE_TABLES
    models = {**SUPPORTED_SOURCE_TABLES, "foundation_evidence": FoundationEvidence, "stock_symbol": StockSymbol,
        "knowledge_document": KnowledgeDocument}
    model = models.get(source_table)
    if model is None:
        raise foundation.FoundationError("该来源不支持图谱原文读取", 422)
    try:
        key = record_id if source_table == "foundation_evidence" else int(record_id)
    except ValueError:
        raise foundation.FoundationError("原始记录编号格式错误", 422)
    row = db.get(model, key)
    if row is None:
        raise foundation.FoundationError("原始证据记录不存在", 404)
    payload = {column.name: getattr(row, column.name) for column in model.__table__.columns}
    content = payload.get("content") or payload.get("report_markdown")
    if not isinstance(content, str):
        content = json.dumps(payload.get("data_json") or payload.get("content_json") or payload.get("payload_json") or payload,
            ensure_ascii=False, indent=2, default=str, allow_nan=False)
    source = db.get(DataSource, payload["source_id"]) if payload.get("source_id") else None
    return {"id": f"evidence:{'foundation' if source_table == 'foundation_evidence' else source_table}:{record_id}",
        "source_table": source_table, "source_record_id": record_id,
        "title": payload.get("title") or f"{source_table} · {record_id}",
        "content": content[:2_000_000], "content_truncated": len(content) > 2_000_000,
        "source_name": payload.get("source_name") or (source.source_name if source else None),
        "source_url": payload.get("url"), "published_at": payload.get("published_at"),
        "observed_at": payload.get("fetched_at") or payload.get("available_at") or payload.get("created_at"),
        "metadata": {key: value for key, value in payload.items() if key not in {"content", "report_markdown", "data_json",
            "content_json", "payload_json", "raw_payload", "ext_json", "agent_snapshot_json"}}}
