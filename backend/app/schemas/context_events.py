from datetime import datetime, timezone
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, field_validator


class ContextEventCreate(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True, extra="forbid")
    market: Literal["CN_A", "HK", "NEEQ", "NEEQ_INNOVATION"]
    symbol: str = Field(min_length=1, max_length=32)
    event_type: Literal["POLICY", "RAW_MATERIAL", "SUPPLY_CHAIN", "SHAREHOLDER", "CONTRACT", "OTHER"]
    title: str = Field(min_length=1, max_length=512)
    content: str = Field(min_length=10, max_length=200000)
    source_name: str = Field(min_length=1, max_length=256)
    url: HttpUrl = Field(max_length=2048)
    published_at: datetime
    related_entity: str | None = Field(default=None, max_length=256)

    @field_validator("published_at")
    @classmethod
    def valid_publication(cls, value: datetime) -> datetime:
        if value.tzinfo is None:
            raise ValueError("发布时间必须包含时区")
        if value > datetime.now(timezone.utc):
            raise ValueError("发布时间不能晚于当前时间")
        return value


class ContextEventRead(ContextEventCreate):
    model_config = ConfigDict(from_attributes=True)
    id: int
    created_at: datetime
    updated_at: datetime

    @field_validator("published_at", mode="before")
    @classmethod
    def sqlite_utc(cls, value):
        return value.replace(tzinfo=timezone.utc) if isinstance(value, datetime) and value.tzinfo is None else value
