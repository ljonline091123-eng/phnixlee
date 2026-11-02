from app.models.market_data import StockNotice
from app.services import document_event_governance as governor


def test_readable_source_text_rejects_broken_pdf_font_mapping():
    broken = "\u0e3f\u0294\u036a\u0fc1\u03aa\u0349\u02ae\u0453" * 20
    readable = "Black Sesame International Holding Limited 核心员工拟增持本公司股份。" * 5

    assert governor.readable_source_text(broken) is False
    assert governor.readable_source_text(readable) is True


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


def test_hkex_unreadable_chinese_pdf_uses_validated_official_english_pair(monkeypatch):
    requested = []

    class Response:
        def __init__(self, body):
            self.body = body

        def __enter__(self):
            return self

        def __exit__(self, *_):
            return False

        def raise_for_status(self):
            return None

        def iter_bytes(self):
            yield self.body

    class Client:
        def __init__(self, *args, **kwargs):
            pass

        def __enter__(self):
            return self

        def __exit__(self, *_):
            return False

        def stream(self, method, url):
            requested.append(url)
            return Response(b"%PDF-english" if url.endswith("2026013000067.pdf") else b"%PDF-chinese")

    broken = "\u0e3f\u0294\u036a\u0fc1\u03aa\u0349\u02ae\u0453" * 20
    english = (
        "Black Sesame International Holding Limited (Stock Code: 2533). "
        "Mr. Liu Weihong has been re-designated from an executive Director to a non-executive Director "
        "and has ceased to serve as the president of the Company, effective from January 29, 2026. "
    ) * 3
    monkeypatch.setattr(governor.httpx, "Client", Client)
    monkeypatch.setattr(governor, "extract_pdf_text", lambda body: {
        "content": english if body == b"%PDF-english" else broken,
        "page_count": 2,
    })
    row = StockNotice(
        market="HK", symbol="02533", notice_date="2026-01-30", title="董事調任",
        notice_type="更換董事或重要行政職能或職責的變更",
        url="https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0130/2026013000068_c.pdf",
        source_id=1, content_json={},
    )

    body, metadata = governor.official_notice_body(row)

    assert "Liu Weihong" in body
    assert metadata["source_url"].endswith("2026013000067.pdf")
    assert metadata["paired_source_url"].endswith("2026013000068_c.pdf")
    assert metadata["source_language"] == "en"
    assert requested == [
        "https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0130/2026013000068_c.pdf",
        "https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0130/2026013000067.pdf",
    ]
