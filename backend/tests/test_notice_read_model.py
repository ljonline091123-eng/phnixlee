from types import SimpleNamespace

from app.services.notice_read_model import deduplicate_notice_rows


def _notice(record_id: int, notice_date: str, notice_type: str):
    return SimpleNamespace(
        id=record_id,
        market="HK",
        symbol="02533",
        notice_date=notice_date,
        title="建議授出發行及購回股份之一般授權",
        notice_type=notice_type,
        url="https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0602/2026060202785_c.pdf",
        source_id=9,
        content_json={"NEWS_ID": str(record_id)},
        fetched_at=None,
    )


def test_hkex_cancelled_and_replacement_rows_form_one_version_chain():
    cancelled = _notice(101, "2026-06-02", "(取消 - 標題已被取代及更換) 通函 - [主要交易]")
    replacement = _notice(102, "2026-06-05", "(修改後標題) 通函 - [購回股份]")

    result = deduplicate_notice_rows([cancelled, replacement])

    assert len(result) == 1
    assert result[0].row.id == 102
    assert result[0].version_record_ids == (102, 101)
    assert result[0].version_history[0]["revision_status"] == "CURRENT"
    assert result[0].version_history[1]["revision_status"] == "SUPERSEDED"


def test_unrelated_hkex_pdf_files_are_not_collapsed():
    first = _notice(101, "2026-06-02", "通函 - [主要交易]")
    second = _notice(102, "2026-06-05", "通函 - [購回股份]")
    second.url = "https://www1.hkexnews.hk/listedco/listconews/sehk/2026/0605/2026060500123_c.pdf"

    assert len(deduplicate_notice_rows([first, second])) == 2
