from __future__ import annotations

from datetime import date, datetime, timedelta
from typing import Any

from pydantic import BaseModel, ConfigDict, Field


class DataSourceCreate(BaseModel):
    source_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_]+$")
    source_name: str = Field(min_length=1, max_length=128)
    source_type: str = "MARKET_DATA"
    adapter_type: str = Field(min_length=1, max_length=64)
    priority: int = Field(default=100, ge=1, le=9999)
    enabled: bool = True
    config_json: dict[str, Any] = Field(default_factory=dict)
    description: str | None = None


class DataSourceUpdate(BaseModel):
    source_name: str | None = Field(default=None, min_length=1, max_length=128)
    source_type: str | None = None
    adapter_type: str | None = None
    priority: int | None = Field(default=None, ge=1, le=9999)
    enabled: bool | None = None
    config_json: dict[str, Any] | None = None
    description: str | None = None


class DataSourceRead(DataSourceCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime
    updated_at: datetime


class DataInterfaceCreate(BaseModel):
    source_id: int
    interface_code: str = Field(min_length=2, max_length=64, pattern=r"^[A-Z0-9_]+$")
    interface_name: str = Field(min_length=1, max_length=128)
    data_category: str = Field(min_length=1, max_length=32)
    request_mode: str = Field(default="SYNC", pattern=r"^(SYNC|ON_DEMAND)$")
    adapter_method: str = Field(min_length=1, max_length=128)
    supported_markets: list[str] = Field(default_factory=list)
    input_schema: dict[str, Any] = Field(default_factory=dict)
    output_schema: dict[str, Any] = Field(default_factory=dict)
    enabled: bool = True
    description: str | None = None


class DataInterfaceUpdate(BaseModel):
    interface_name: str | None = Field(default=None, min_length=1, max_length=128)
    data_category: str | None = None
    request_mode: str | None = Field(default=None, pattern=r"^(SYNC|ON_DEMAND)$")
    adapter_method: str | None = None
    supported_markets: list[str] | None = None
    input_schema: dict[str, Any] | None = None
    output_schema: dict[str, Any] | None = None
    enabled: bool | None = None
    description: str | None = None


class DataInterfaceRead(DataInterfaceCreate):
    model_config = ConfigDict(from_attributes=True)

    id: int
    created_at: datetime
    updated_at: datetime


class DataSourceTestResponse(BaseModel):
    source_code: str
    adapter_type: str
    status: str
    message: str
    capabilities: list[str] = Field(default_factory=list)


class StockSyncRequest(BaseModel):
    source_code: str = "AKSHARE"
    market: str = Field(default="ALL", pattern=r"^(CN_A|HK|NEEQ|NEEQ_INNOVATION|ALL)$")
    enable_fallback: bool = True


class StockSyncResponse(BaseModel):
    sync_log_ids: list[int]
    attempted_source_codes: list[str]
    status: str
    message: str


class StockSymbolRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    exchange: str
    name: str
    asset_type: str
    status: str
    list_date: str | None
    source_id: int
    ext_json: dict[str, Any]
    raw_payload: dict[str, Any]
    last_synced_at: datetime


class StockSymbolPage(BaseModel):
    items: list[StockSymbolRead]
    total: int
    page: int
    page_size: int


class WatchlistItemRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    market: str
    symbol: str
    note: str | None = None
    name: str | None = None
    current_price: float | None = None
    change_pct: float | None = None
    created_at: datetime


class WatchlistItemCreate(BaseModel):
    market: str = Field(pattern=r"^(CN_A|HK|NEEQ|NEEQ_INNOVATION)$")
    symbol: str = Field(min_length=1, max_length=32)
    note: str | None = Field(default=None, max_length=256)


class IpoCalendarItemRead(BaseModel):
    market: str
    symbol: str
    name: str
    apply_date: str
    apply_end_date: str | None = None
    listing_date: str | None = None
    price: str | None = None
    lot_size: str | None = None
    entry_fee: str | None = None
    industry: str | None = None
    sponsor: str | None = None
    prospectus_url: str | None = None
    detail_url: str | None = None
    issue_total: str | None = None
    online_issue: str | None = None
    apply_limit: str | None = None
    pe_ratio: str | None = None
    raw: dict[str, Any] = Field(default_factory=dict)
    source: str


class IpoCalendarSourceRead(BaseModel):
    source: str
    market: str
    status: str
    message: str
    total_count: int = 0
    item_count: int = 0
    fetched_at: datetime


class IpoCalendarResponse(BaseModel):
    items: list[IpoCalendarItemRead]
    sources: list[IpoCalendarSourceRead]
    errors: list[str] = Field(default_factory=list)
    window_start: str
    window_end: str
    updated_at: datetime


class DataSyncLogRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    source_id: int
    interface_code: str
    market: str
    status: str
    total_count: int
    inserted_count: int
    updated_count: int
    failed_count: int
    error_message: str | None
    detail_json: dict[str, Any]
    started_at: datetime
    completed_at: datetime | None


def default_start_date() -> date:
    return date.today() - timedelta(days=365)


class KlineFetchRequest(BaseModel):
    source_code: str = "AKSHARE"
    period: str = Field(default="daily", pattern=r"^(daily|weekly|monthly)$")
    adjust: str = Field(default="", pattern=r"^(|qfq|hfq)$")
    start_date: date = Field(default_factory=default_start_date)
    end_date: date = Field(default_factory=date.today)
    persist: bool = True


class FinancialFetchRequest(BaseModel):
    source_code: str = "AKSHARE"
    indicator: str | None = Field(default=None, max_length=32)
    persist: bool = True


class NoticeFetchRequest(BaseModel):
    source_code: str = "AKSHARE"
    start_date: date = Field(default_factory=lambda: date.today() - timedelta(days=90))
    end_date: date = Field(default_factory=date.today)
    persist: bool = True


class QuoteFetchRequest(BaseModel):
    source_code: str = "AKSHARE"
    persist: bool = True


class NewsFetchRequest(BaseModel):
    source_code: str = "AKSHARE"
    persist: bool = True


class OnDemandFetchResponse(BaseModel):
    fetch_log_id: int
    source_code: str
    status: str
    total_count: int
    persisted_count: int
    items: list[dict[str, Any]]


class StockKlineRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    period: str
    adjust: str
    trade_date: str
    open_price: float | None
    high_price: float | None
    low_price: float | None
    close_price: float | None
    volume: float | None
    amount: float | None
    turnover_rate: float | None
    source_id: int
    fetched_at: datetime


class StockFinancialReportRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    indicator: str
    report_period: str
    currency: str | None
    data_json: dict[str, Any]
    url: str | None = None
    source_id: int
    fetched_at: datetime


class StockNoticeRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    notice_date: str
    title: str
    notice_type: str | None
    url: str | None
    content_json: dict[str, Any]
    source_id: int
    fetched_at: datetime


class StockQuoteRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    quote_time: str | None
    current_price: float | None
    previous_close_price: float | None
    open_price: float | None
    high_price: float | None
    low_price: float | None
    volume: float | None
    amount: float | None
    change_amount: float | None
    change_pct: float | None
    turnover_rate: float | None
    source_id: int
    fetched_at: datetime
    raw_payload: dict[str, Any]


class StockNewsRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    news_time: str
    title: str
    content: str | None
    source_name: str | None
    url: str | None
    content_json: dict[str, Any]
    source_id: int
    fetched_at: datetime


class StockNoticePage(BaseModel):
    items: list[StockNoticeRead]
    total: int
    page: int
    page_size: int


class StockNewsPage(BaseModel):
    items: list[StockNewsRead]
    total: int
    page: int
    page_size: int


class StockF10CacheRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    market: str
    symbol: str
    section: str
    source_id: int
    payload_json: dict[str, Any]
    fetched_at: datetime


class DataFetchLogRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    source_id: int
    interface_code: str
    market: str
    symbol: str
    request_json: dict[str, Any]
    status: str
    total_count: int
    persisted_count: int
    error_message: str | None
    started_at: datetime
    completed_at: datetime | None


class StockF10Read(BaseModel):
    symbol: StockSymbolRead
    realtime_quote: StockQuoteRead | None = None
    recent_klines: list[StockKlineRead]
    financial_reports: list[StockFinancialReportRead]
    notices: list[StockNoticeRead]
    news: list[StockNewsRead] = Field(default_factory=list)
    notice_total: int = 0
    notice_page: int = 1
    news_total: int = 0
    news_page: int = 1
    published_reports: dict[str, Any] = Field(default_factory=dict)
    profile: dict[str, Any] = Field(default_factory=dict)
    holders: dict[str, Any] = Field(default_factory=dict)
    fund_flow: dict[str, Any] = Field(default_factory=dict)
    financial_summary: dict[str, Any] = Field(default_factory=dict)
    financial_statements: dict[str, Any] = Field(default_factory=dict)
    business_composition: dict[str, Any] = Field(default_factory=dict)
