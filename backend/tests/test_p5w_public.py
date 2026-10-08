import httpx
import pytest

from app.connectors.p5w_public import P5WClient, identity, parse_notices, parse_qa, parse_news

COMPANY = {"symbol": "430005", "name": "原子高科", "pid": "issuer-1"}
PROOF = {"sha256": "proof", "observed_at": "2026-10-08T15:00:00Z"}


def test_company_page_and_question_reject_other_issuers_and_unknown_empty():
    with pytest.raises(ValueError):
        identity("<title>安徽凤凰（920000）投资者关系互动平台</title>", "430005")
    with pytest.raises(ValueError):
        identity("<title>访问验证</title>", "430005")
    with pytest.raises(ValueError):
        parse_qa({"code": 0, "data": None, "message": "参数错误"}, COMPANY, {}, PROOF)
    raw = {"companyCode": "430005", "companyPid": "wrong", "questionId": "1", "content": "问题", "questionDate": "2026-10-01"}
    with pytest.raises(ValueError):
        parse_qa({"code": 1, "data": [raw]}, COMPANY, {}, PROOF)


def test_pending_reply_and_explicit_provider_eof_preserve_scope():
    raw = {"companyCode": "430005", "companyPid": "issuer-1", "questionId": "1", "content": "<p>问题</p>", "questionDate": "2026-10-01"}
    page = parse_qa({"code": 1, "data": [raw]}, COMPANY, {"offset": 5}, PROOF)
    assert not page.complete and page.cursor["offset"] == 6
    assert page.rows[0]["answer"] is None
    assert page.rows[0]["status"] == "PENDING_REPLY"
    empty = parse_qa({"code": 0, "data": [], "message": "没有更多数据了"}, COMPANY, page.cursor, PROOF)
    assert empty.complete and not empty.rows


def test_report_period_not_saved_as_publication_date_and_bad_total_ignored():
    row = {"s1": 1, "s3": "原子高科:2026年半年度报告", "s4": "2026-06-30", "s2": "https://www.neeq.com.cn/disclosure/2026/2026-08-28/file.pdf"}
    records = parse_notices({"code": 1, "total": 0, "rows": [row]}, COMPANY, "NEEQ", PROOF)
    assert records[0].notice_date == "2026-08-28"
    assert records[0].content_json["source_list_date"] == "2026-06-30"
    assert records[0].content_json["date_status"] == "LIST_DATE_CONFLICT_ATTACHMENT_PATH"
    with pytest.raises(ValueError):
        parse_notices({"code": 1, "rows": [{**row, "s3": "其他公司:公告"}]}, COMPANY, "NEEQ", PROOF)
    with pytest.raises(ValueError):
        parse_notices({"code": 1, "rows": [{**row, "s2": "https://unknown.example/file.pdf"}]}, COMPANY, "NEEQ", PROOF)


def test_repeated_notice_page_is_failure_not_complete():
    row = {"s1": 1, "s3": "原子高科:公告", "s4": "2026-09-01", "s2": None}
    client = P5WClient()
    client.client.close()
    def handler(request):
        if request.url.path.endswith("430005.html"):
            return httpx.Response(200, text="<title>原子高科（430005）投资者关系互动平台</title>")
        return httpx.Response(200, json={"code": 1, "total": 0, "rows": [row]})
    client.client = httpx.Client(transport=httpx.MockTransport(handler))
    with client, pytest.raises(ValueError, match="重复分页"):
        client.fetch_notices("NEEQ", "430005", "2026-01-01", "2026-10-08")


def test_news_code_identity_original_date_and_summary_are_preserved():
    raw = {"DOCID": 1, "STOCKCODE": ["430005"], "DOCTITLE": "原子高科公告",
           "DOCABSTRACT": "<p>原始摘要</p>", "DOCRELTIME": "2013-11-07 18:02:00"}
    row = parse_news({"total": 1, "data": [raw]}, COMPANY, "NEEQ", PROOF)[0]
    assert row.news_time == "2013-11-07 18:02:00" and row.content == "原始摘要"
    assert row.content_json["content_status"] == "SUMMARY_ONLY"
    with pytest.raises(ValueError):
        parse_news({"total": 1, "data": [{**raw, "STOCKCODE": ["430006"]}]}, COMPANY, "NEEQ", PROOF)
