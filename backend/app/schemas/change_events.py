"""Read schemas for L0 change-detection events."""

from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, ConfigDict, field_validator


class ChangeEventRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    event_date: str
    event_type: str
    direction: str
    signal_json: dict[str, Any]
    source_ids_json: dict[str, Any]
    dedup_key: str
    profile_version: str
    status: str
    detected_at: datetime
    created_at: datetime

    @field_validator("detected_at", mode="before")
    @classmethod
    def sqlite_utc(cls, value):
        return value.replace(tzinfo=timezone.utc) if isinstance(value, datetime) and value.tzinfo is None else value
