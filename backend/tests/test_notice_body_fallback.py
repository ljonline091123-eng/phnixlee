from app.models.market_data import StockNotice
from app.services import document_event_governance as governor


def test_linked_cninfo_notice_reconstructs_pdf_url(monkeypatch):
    requested = []

    class Response:
        def __enter__(self):
            return self

        def __exit__(self, *_):
            return False

        def raise_for_status(self):
            return None

        def iter_bytes(self):
            yield b"%PDF-test"

    class Client:
        def __init__(self, *args, **kwargs):
            pass

        def __enter__(self):
            return self

        def __exit__(self, *_):
            return False

        def stream(self, method, url):
            requested.append(url)
            return Response()

    monkeypatch.setattr(governor.httpx, "Client", Client)
    monkeypatch.setattr(governor, "extract_pdf_text", lambda _: {"content": "document body " * 20})
    detail_url = (
        "https://www.cninfo.com.cn/new/disclosure/detail?"
        "announcementId=1225635942&announcementTime=2026-10-29&orgId=gfbj0836990"
    )
    row = StockNotice(
        market="CN_A", symbol="688627", notice_date="2026-10-29",
        title="2026 Q3 report", url=detail_url, source_id=1,
        content_json={"disclosure_entry": {"url": detail_url}},
    )

    body, _ = governor.official_notice_body(row)

    assert body.startswith("document body")
    assert requested == ["https://static.cninfo.com.cn/finalpage/2026-10-29/1225635942.PDF"]
