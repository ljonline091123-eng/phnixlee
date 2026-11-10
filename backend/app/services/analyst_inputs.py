"""Validated user images and stock-scoped local document references."""
from __future__ import annotations

import base64
from hashlib import sha256
from io import BytesIO
import warnings

from PIL import Image
from sqlalchemy import select, func, literal, union_all

from app.models.market_data import DataSource, StockNews, StockNotice

MAX_IMAGE_BYTES = 4 * 1024 * 1024
MAX_TOTAL_IMAGE_BYTES = 12 * 1024 * 1024


def validate_image(data_url: str) -> tuple[str, bytes, int, int]:
    try:
        header, encoded = data_url.split(",", 1)
        mime = header.removeprefix("data:").removesuffix(";base64")
        if header != f"data:{mime};base64" or mime not in {"image/png", "image/jpeg", "image/webp"}:
            raise ValueError()
        binary = base64.b64decode(encoded, validate=True)
        if not binary or len(binary) > MAX_IMAGE_BYTES:
            raise ValueError("每张图片最大4MB，请缩小图片后重试")
        with warnings.catch_warnings():
            warnings.simplefilter("error", Image.DecompressionBombWarning)
            with Image.open(BytesIO(binary)) as image:
                width, height = image.size
                if width * height > 16_000_000 or min(width, height) < 1:
                    raise ValueError("图片分辨率过大，请缩小后重试")
                if Image.MIME.get(image.format) != mime:
                    raise ValueError()
                image.verify()
        return mime, binary, width, height
    except ValueError as exc:
        if str(exc):
            raise
        raise ValueError("图片格式不正确，仅支持PNG、JPEG、WebP") from exc
    except Exception as exc:
        raise ValueError("图片无法读取，请重新复制或选择图片") from exc


def image_record(image, index: int) -> dict:
    mime, binary, width, height = validate_image(image.data_url)
    digest = sha256(binary).hexdigest()
    return {"evidence_id": f"user-image:{digest}", "name": image.name or f"粘贴图片{index + 1}",
            "data_url": image.data_url, "mime_type": mime, "sha256": digest, "size": len(binary),
            "width": width, "height": height}


def supports_images(model_code: str, config: dict | None = None) -> bool:
    config = config or {}
    for key in ("supports_images", "supports_vision"):
        if isinstance(config.get(key), bool):
            return config[key]
    code = model_code.lower()
    return any(value in code for value in ("gemini", "gpt-4o", "gpt-4.1", "gpt-5", "claude", "qwen-vl", "qwen2.5-vl", "qwen3-vl", "glm-4v", "glm-4.1v", "glm-4.5v", "glm-4.6v"))


def document_evidence(db, analysis, evidence_id: str, *, body_limit=4200) -> dict:
    prefix, _, raw_id = evidence_id.partition(":")
    model = {"news": StockNews, "notice": StockNotice}.get(prefix)
    if model is None or not raw_id.isdigit():
        raise ValueError("请选择有效的新闻或公告")
    row = db.get(model, int(raw_id))
    if row is None or (row.market, row.symbol) != (analysis.market, analysis.symbol):
        raise ValueError("所选新闻或公告不属于当前股票，或已不存在")
    from app.services.document_event_governance import document_body
    body = document_body(row) or ""
    source = db.get(DataSource, row.source_id)
    return {"evidence_id": evidence_id, "category": "NEWS" if model is StockNews else "NOTICE",
            "category_name": "新闻" if model is StockNews else "公告", "title": row.title,
            "observed_at": row.news_time if model is StockNews else row.notice_date,
            "source": (row.source_name if model is StockNews else None) or (source.source_name if source else prefix),
            "excerpt": body[:body_limit] or None,
            "data": {"url": row.url, "content_status": "LOCAL_TEXT" if body else "TITLE_ONLY",
                     "content_length": len(body), "content_sha256": sha256(body.encode("utf-8")).hexdigest(),
                     "truncated": len(body) > body_limit,
                     "fetched_at": row.fetched_at.isoformat() if row.fetched_at else None}}


def local_documents(db, analysis, category: str, keyword: str, offset: int, limit: int) -> dict:
    statements = []
    for model, label, date_col in ((StockNews, "NEWS", StockNews.news_time), (StockNotice, "NOTICE", StockNotice.notice_date)):
        if category not in {"ANY", label}:
            continue
        statement = select(model.id.label("id"), literal(label).label("category"), model.title.label("title"), date_col.label("observed_at")).where(
            model.market == analysis.market, model.symbol == analysis.symbol)
        if keyword.strip():
            statement = statement.where(model.title.contains(keyword.strip(), autoescape=True))
        statements.append(statement)
    combined = union_all(*statements).subquery()
    total = db.scalar(select(func.count()).select_from(combined)) or 0
    rows = db.execute(select(combined).order_by(combined.c.observed_at.desc(), combined.c.id.desc(), combined.c.category)
                      .offset(offset).limit(limit)).mappings().all()
    items = [document_evidence(db, analysis, f"{'news' if row['category'] == 'NEWS' else 'notice'}:{row['id']}", body_limit=300) for row in rows]
    return {"items": items, "total": total, "offset": offset, "limit": limit}
