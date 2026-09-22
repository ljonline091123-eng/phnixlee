from app.services.knowledge_network import _scope_category


class _Projection:
    def __init__(self):
        self.nodes = {
            "security:CN_A:000001": {"id": "security:CN_A:000001", "type": "SECURITY", "properties": {}, "evidence_ids": []},
            "company:1": {"id": "company:1", "type": "COMPANY", "properties": {}, "evidence_ids": []},
            "industry:1": {"id": "industry:1", "type": "INDUSTRY", "properties": {}, "evidence_ids": []},
            "financial:1": {"id": "financial:1", "type": "FINANCIAL_OBSERVATION", "properties": {}, "evidence_ids": []},
            "news:1": {"id": "news:1", "type": "EVIDENCE_DOCUMENT", "properties": {"source_table": "stock_news"}, "evidence_ids": []},
        }
        self.edges = {
            "issuer": {"id": "issuer", "source": "security:CN_A:000001", "target": "company:1", "type": "ISSUED_BY", "layer": "MASTER", "evidence_ids": []},
            "industry": {"id": "industry", "source": "security:CN_A:000001", "target": "industry:1", "type": "IN_INDUSTRY", "layer": "MASTER", "evidence_ids": []},
            "financial": {"id": "financial", "source": "security:CN_A:000001", "target": "financial:1", "type": "HAS_FINANCIAL_OBSERVATION", "layer": "FACT", "evidence_ids": []},
            "news": {"id": "news", "source": "security:CN_A:000001", "target": "news:1", "type": "HAS_NEWS", "layer": "EVIDENCE", "evidence_ids": []},
        }


def test_category_scope_keeps_center_and_only_requested_domain():
    projection = _Projection()
    selected = _scope_category(projection, "financial", "security:CN_A:000001", "company:1", False)
    assert selected == {"financial:1"}
    assert set(projection.nodes) == {"security:CN_A:000001", "company:1", "financial:1"}
    assert set(projection.edges) == {"issuer", "financial"}


def test_disclosure_scope_selects_documents_by_source_table():
    projection = _Projection()
    selected = _scope_category(projection, "disclosure", "security:CN_A:000001", "company:1", True)
    assert selected == {"news:1"}
    assert "news:1" in projection.nodes
    assert "financial:1" not in projection.nodes
