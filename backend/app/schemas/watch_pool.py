"""Request schemas for watch-pool operations."""

from pydantic import BaseModel, ConfigDict, Field


class RefreshRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    market: str | None = Field(default=None, max_length=32, description="仅刷新该市场，如 CN_A / HK")
    business_date: str | None = Field(
        default=None, pattern=r"^\d{4}-\d{2}-\d{2}$", description="刷新截止日（默认今天，上海时区）"
    )
    delay_seconds: float = Field(default=0.5, ge=0, le=10, description="逐只之间的礼貌间隔（秒）")


class DetectRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    market: str | None = Field(default=None, max_length=32, description="仅扫描该市场")
    business_date: str | None = Field(
        default=None, pattern=r"^\d{4}-\d{2}-\d{2}$", description="只用该日期及之前的行情（确定性回放）"
    )
