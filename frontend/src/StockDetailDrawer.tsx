import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from "react";
import { Building2 } from "lucide-react";
import { CompanyGraphDialog } from "./CompanyGraphWorkbench";

import {
  api,
  type ResearchFundamentalAgent,
  type ResearchTechnicalAgent,
  type StockF10,
  type StockKline,
  type StockNewsItem,
  type StockNotice,
  type StockSymbol,
} from "./api";

type JsonRecord = Record<string, unknown>;
type PrimaryTab = "精选" | "新闻" | "公告" | "资金" | "F10" | "研究";
type F10Tab = "财务" | "股东" | "简况" | "财报";

const marketLabel: Record<string, string> = {
  CN_A: "A股",
  HK: "港股",
  NEEQ: "新三板",
  NEEQ_INNOVATION: "创新层",
};

const marketClass: Record<string, string> = {
  CN_A: "cn",
  HK: "hk",
  NEEQ: "cn",
  NEEQ_INNOVATION: "cn",
};

function asRecord(value: unknown): JsonRecord {
  return value && typeof value === "object" && !Array.isArray(value) ? (value as JsonRecord) : {};
}

function asArray(value: unknown): JsonRecord[] {
  return Array.isArray(value) ? value.filter((item) => item && typeof item === "object").map((item) => item as JsonRecord) : [];
}

function toNumber(value: unknown): number | null {
  if (typeof value === "number") return Number.isFinite(value) ? value : null;
  if (typeof value === "string") {
    const cleaned = value.replace(/[,，%\s]/g, "");
    if (!cleaned) return null;
    const parsed = Number(cleaned);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function valueText(value: unknown): string {
  if (value === null || value === undefined || value === "") return "--";
  if (typeof value === "number") return formatNumber(value);
  if (typeof value === "boolean") return value ? "是" : "否";
  if (Array.isArray(value)) return value.map(valueText).join("、");
  if (typeof value === "object") {
    const record = asRecord(value);
    return Object.entries(record)
      .slice(0, 6)
      .map(([key, item]) => `${key}: ${valueText(item)}`)
      .join(" · ");
  }
  return String(value);
}

function formatNumber(value?: number | null, digits = 2): string {
  if (value === undefined || value === null || !Number.isFinite(value)) return "--";
  return new Intl.NumberFormat("zh-CN", { maximumFractionDigits: digits }).format(value);
}

function formatMoney(value?: number | null): string {
  if (value === undefined || value === null || !Number.isFinite(value)) return "--";
  const abs = Math.abs(value);
  if (abs >= 100000000) return `${formatNumber(value / 100000000, 2)}亿`;
  if (abs >= 10000) return `${formatNumber(value / 10000, 2)}万`;
  return formatNumber(value, 2);
}

function formatPercent(value?: number | null): string {
  if (value === undefined || value === null || !Number.isFinite(value)) return "--";
  return `${formatNumber(value, 2)}%`;
}

function formatDate(value?: string | null): string {
  if (!value) return "--";
  const text = String(value);
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  if (/^\d{8}$/.test(text)) return `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6, 8)}`;
  return text;
}

function pickValue(record: JsonRecord, keys: string[]): unknown {
  for (const key of keys) {
    if (record[key] !== undefined && record[key] !== null && record[key] !== "") return record[key];
  }
  return undefined;
}

function findValue(record: JsonRecord, aliases: string[]): unknown {
  const direct = pickValue(record, aliases);
  if (direct !== undefined) return direct;
  const normalizedAliases = aliases.map((key) => key.toLowerCase().replace(/[\s_()（）-]/g, ""));
  for (const [key, value] of Object.entries(record)) {
    const normalized = key.toLowerCase().replace(/[\s_()（）-]/g, "");
    if (normalizedAliases.some((alias) => normalized.includes(alias) || alias.includes(normalized))) {
      if (value !== undefined && value !== null && value !== "") return value;
    }
  }
  return undefined;
}

function findNumber(record: JsonRecord, aliases: string[]): number | null {
  return toNumber(findValue(record, aliases));
}

function displayByLabel(label: string, value: unknown): string {
  const numeric = toNumber(value);
  if (numeric === null) return valueText(value);
  if (/(市值|金额|成交额|资产|负债|收入|利润|现金|资本|持股市值|净流入|净额|发行量|股本)/.test(label)) {
    return formatMoney(numeric);
  }
  if (/(市盈率|市净率|\bPE\b|\bPB\b)/i.test(label)) {
    return formatNumber(numeric);
  }
  if (/(率|幅|占比|比例|ROE|毛利|净利|pe|pb|市盈|市净)/i.test(label)) {
    return formatPercent(numeric);
  }
  return formatNumber(numeric, Math.abs(numeric) >= 1000 ? 0 : 4);
}

function getRowLabel(row: JsonRecord, index = 0): string {
  const value = pickValue(row, ["metric", "指标", "name", "名称", "项目", "label", "title", "report_name", "indicator"]);
  return value ? String(value) : `项目 ${index + 1}`;
}

function getRowValue(row: JsonRecord, periods: string[] = []): unknown {
  const direct = pickValue(row, ["value", "值", "data", "数据", "amount", "金额", "content"]);
  if (direct && typeof direct === "object" && !Array.isArray(direct)) {
    const data = asRecord(direct);
    for (const period of periods) {
      if (data[period] !== undefined && data[period] !== null && data[period] !== "") return data[period];
    }
    const first = Object.values(data).find((item) => item !== undefined && item !== null && item !== "");
    return first;
  }
  return direct ?? row;
}

function buildRows(rows: unknown, periods: string[] = [], limit = 12) {
  const list = Array.isArray(rows)
    ? rows
    : Object.entries(asRecord(rows)).map(([metric, data]) => ({ metric, data }));
  return list
    .filter((row) => row !== null && row !== undefined)
    .slice(0, limit)
    .map((row, index) => {
      const record = asRecord(row);
      const label = getRowLabel(record, index);
      return { label, value: getRowValue(record, periods) };
    });
}

function findMetricFromRows(rows: unknown, aliases: string[], periods: string[] = []): number | null {
  const normalizedAliases = aliases.map((alias) => alias.toLowerCase().replace(/[\s_()（）-]/g, ""));
  const match = buildRows(rows, periods, 120).find((row) => {
    const normalized = row.label.toLowerCase().replace(/[\s_()（）-]/g, "");
    return normalizedAliases.some((alias) => normalized.includes(alias) || alias.includes(normalized));
  });
  return match ? toNumber(match.value) : null;
}

function shareholderRows(rows: unknown): JsonRecord[] {
  if (Array.isArray(rows)) {
    return rows.map((row) => asRecord(row)).filter((row) => Object.keys(row).length > 0);
  }
  return Object.entries(asRecord(rows)).map(([label, value]) => ({
    项目: label,
    值: value,
  }));
}

function shareholderField(row: JsonRecord, aliases: string[]): unknown {
  return findValue(row, aliases);
}

function ma(values: number[], size: number): number | null {
  if (values.length === 0) return null;
  const slice = values.slice(-Math.min(size, values.length));
  return slice.reduce((sum, value) => sum + value, 0) / slice.length;
}

function storageKey(market: string, symbol: string) {
  return `gemini-quant-agent:research-report:${market}:${symbol}`;
}

function normaliseQuoteParts(raw: JsonRecord) {
  const parts = raw.parts;
  if (!Array.isArray(parts)) return [];
  return parts.map((part) => String(part ?? "").trim());
}

function isPositivePrice(value?: string) {
  const numeric = Number(value);
  return Number.isFinite(numeric) && numeric > 0;
}

function parseTencentOrderbook(raw: JsonRecord) {
  const parts = normaliseQuoteParts(raw);
  if (!parts.length) return { asks: [] as JsonRecord[], bids: [] as JsonRecord[] };

  const buildRows = (labels: string[], startIndex: number, side: "ask" | "bid") =>
    labels
      .map((label, index) => {
        const price = parts[startIndex + index * 2];
        const volume = parts[startIndex + index * 2 + 1];
        return { label, price, volume, side };
      })
      .filter((row) => isPositivePrice(row.price));

  return {
    bids: buildRows(["买一", "买二", "买三", "买四", "买五"], 9, "bid"),
    asks: buildRows(["卖一", "卖二", "卖三", "卖四", "卖五"], 19, "ask"),
  };
}

function parseTencentTrades(raw: JsonRecord) {
  const parts = normaliseQuoteParts(raw);
  if (!parts.length) return [];

  const snapshot = String(parts[35] || "").split("/");
  const price = snapshot[0] || parts[3];
  if (!isPositivePrice(price)) return [];

  return [
    {
      time: String(parts[30] || raw.fetched_at || "最新"),
      price: String(price),
      volume: String(snapshot[1] || parts[36] || "--"),
      amount: String(snapshot[2] || "--"),
      side: Number(parts[31]) >= 0 ? "up" : "down",
    },
  ];
}

function StockKlinePanel({
  klines,
  quote,
  profileFields,
}: {
  klines: StockKline[];
  quote: StockF10["realtime_quote"];
  profileFields: JsonRecord;
}) {
  const validKlines = useMemo(
    () =>
      [...klines]
        .filter((item) => toNumber(item.close_price) !== null)
        .sort((a, b) => String(a.trade_date).localeCompare(String(b.trade_date))),
    [klines],
  );
  const [windowSize, setWindowSize] = useState(120);
  const [crosshair, setCrosshair] = useState<{ x: number; y: number; index: number } | null>(null);
  const visible = validKlines.slice(-Math.min(windowSize, validKlines.length || windowSize));
  const closes = visible.map((item) => Number(item.close_price));
  const highs = visible.map((item) => Number(item.high_price ?? item.close_price));
  const lows = visible.map((item) => Number(item.low_price ?? item.close_price));
  const volumes = visible.map((item) => Number(item.volume ?? 0));
  const priceMaxRaw = Math.max(...highs, ...closes);
  const priceMinRaw = Math.min(...lows, ...closes);
  const padding = Math.max((priceMaxRaw - priceMinRaw) * 0.08, priceMaxRaw * 0.002, 0.01);
  const priceMax = priceMaxRaw + padding;
  const priceMin = priceMinRaw - padding;
  const priceRange = priceMax - priceMin || 1;
  const volumeMax = Math.max(...volumes, 1);
  const priceTop = 18;
  const priceBottom = 210;
  const volumeTop = 226;
  const volumeBottom = 292;
  const width = 740;
  const candleWidth = Math.max(3, Math.min(9, (width - 58) / Math.max(visible.length, 1) * 0.58));
  const x = (index: number) => 28 + (index * 686) / Math.max(visible.length - 1, 1);
  const y = (value: number) => priceTop + ((priceMax - value) / priceRange) * (priceBottom - priceTop);
  const volumeY = (value: number) => volumeBottom - (value / volumeMax) * (volumeBottom - volumeTop);
  const issuePrice = findValue(profileFields, ["发行价", "挂牌价", "每股面值", "发行价格"]);
  const lotSize = findValue(profileFields, ["每手股数", "每手股", "交易单位", "lot_size"]);
  const rawQuotePayload = asRecord(quote?.raw_payload);
  const parsedOrderbook = parseTencentOrderbook(rawQuotePayload);
  const fallbackTrades = parseTencentTrades(rawQuotePayload);
  const intradayTradesRaw = asArray(findValue(rawQuotePayload, ["trades", "ticks", "分时成交", "intraday"]));
  const intradayTrades = intradayTradesRaw.length ? intradayTradesRaw : fallbackTrades;
  const orderbook = asRecord(findValue(rawQuotePayload, ["orderbook", "盘口", "five_level"]));
  const askRowsRaw = asArray(orderbook.asks);
  const bidRowsRaw = asArray(orderbook.bids);
  const askRows = (askRowsRaw.length ? askRowsRaw : parsedOrderbook.asks).slice(0, 5);
  const bidRows = (bidRowsRaw.length ? bidRowsRaw : parsedOrderbook.bids).slice(0, 5);
  const handleChartMove = (event: ReactMouseEvent<SVGSVGElement>) => {
    if (!visible.length) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const chartX = Math.max(28, Math.min(714, ((event.clientX - rect.left) / rect.width) * width));
    const chartY = Math.max(priceTop, Math.min(volumeBottom, ((event.clientY - rect.top) / rect.height) * 310));
    const index = Math.max(
      0,
      Math.min(visible.length - 1, Math.round(((chartX - 28) / 686) * Math.max(visible.length - 1, 1))),
    );
    setCrosshair({ x: chartX, y: chartY, index });
  };

  if (!visible.length) {
    return (
      <div className="market-chart-shell restored-market-chart">
        <div className="market-chart-main">
          <p className="empty-state">暂无 K 线数据</p>
        </div>
        <aside className="market-order-panel">
          <h4>挂牌资料</h4>
          <div className="detail-data-row compact-row"><span>挂牌/发行价</span><strong>{valueText(issuePrice)}</strong></div>
          <div className="detail-data-row compact-row"><span>每手股数</span><strong>{valueText(lotSize)}</strong></div>
        </aside>
      </div>
    );
  }

  return (
    <div className="market-chart-shell restored-market-chart">
      <div className="market-chart-main">
        <svg
          className="market-kline"
          viewBox="0 0 740 310"
          role="img"
          aria-label="日K线、成交量与均线"
          onMouseMove={handleChartMove}
          onMouseLeave={() => setCrosshair(null)}
        >
          {[0, 1, 2, 3].map((index) => (
            <line key={index} className="chart-grid-line" x1="24" x2="718" y1={priceTop + index * 48} y2={priceTop + index * 48} />
          ))}
          <line className="chart-divider-line" x1="24" x2="718" y1={priceBottom + 8} y2={priceBottom + 8} />
          {visible.map((item, index) => {
            const open = Number(item.open_price ?? item.close_price);
            const close = Number(item.close_price);
            const high = Number(item.high_price ?? Math.max(open, close));
            const low = Number(item.low_price ?? Math.min(open, close));
            const rising = close >= open;
            const bodyTop = Math.min(y(open), y(close));
            const bodyHeight = Math.max(2, Math.abs(y(open) - y(close)));
            const volTop = volumeY(Number(item.volume ?? 0));
            return (
              <g key={`${item.trade_date}-${index}`}>
                <rect className={`volume-bar ${rising ? "rising" : "falling"}`} x={x(index) - candleWidth / 2} y={volTop} width={candleWidth} height={Math.max(1, volumeBottom - volTop)} />
                <line className={`candle-wick ${rising ? "rising" : "falling"}`} x1={x(index)} x2={x(index)} y1={y(high)} y2={y(low)} />
                <rect className={`candle-body ${rising ? "rising" : "falling"}`} x={x(index) - candleWidth / 2} y={bodyTop} width={candleWidth} height={bodyHeight} rx="1" />
              </g>
            );
          })}
          {crosshair && visible[crosshair.index] ? (
            <g className="chart-crosshair" aria-hidden="true">
              <line x1={crosshair.x} x2={crosshair.x} y1={priceTop} y2={volumeBottom} />
              <line x1="24" x2="718" y1={crosshair.y} y2={crosshair.y} />
              <circle cx={crosshair.x} cy={crosshair.y < priceBottom ? crosshair.y : volumeY(Number(visible[crosshair.index].volume ?? 0))} r="4" />
              <rect
                x={Math.min(Math.max(crosshair.x + 9, 34), 562)}
                y="25"
                width="165"
                height="38"
                rx="4"
              />
              <text className="chart-tooltip-title" x={Math.min(Math.max(crosshair.x + 17, 42), 570)} y="41">
                {formatDate(visible[crosshair.index].trade_date)}
              </text>
              <text x={Math.min(Math.max(crosshair.x + 17, 42), 570)} y="55">
                收 {formatNumber(toNumber(visible[crosshair.index].close_price))} · 量 {formatNumber(toNumber(visible[crosshair.index].volume), 0)}
              </text>
            </g>
          ) : null}
          <text className="chart-axis-label" x="28" y="14">{formatNumber(priceMaxRaw)}</text>
          <text className="chart-axis-label" x="28" y="210">{formatNumber(priceMinRaw)}</text>
          <text className="chart-axis-label month" x="28" y="306">{formatDate(visible[0]?.trade_date)}</text>
          <text className="chart-axis-label month" x="630" y="306">{formatDate(visible[visible.length - 1]?.trade_date)}</text>
        </svg>
        <div className="chart-formulas">
          <span>MA5: {formatNumber(ma(closes, 5))}</span>
          <span>MA20: {formatNumber(ma(closes, 20))}</span>
          <span>MA60: {formatNumber(ma(closes, 60))}</span>
          <span>VOL: {formatNumber(volumes[volumes.length - 1], 0)}</span>
        </div>
        <div className="kline-zoom-controls">
          <button type="button" className="zoom-button" onClick={() => { setCrosshair(null); setWindowSize((size) => Math.max(20, size - 40)); }} disabled={windowSize <= 20}>−</button>
          <span>{windowSize >= validKlines.length ? `上市以来 · ${validKlines.length} 个交易日` : `近 ${Math.min(windowSize, validKlines.length)} 个交易日`}</span>
          <button type="button" className="zoom-button" onClick={() => { setCrosshair(null); setWindowSize((size) => Math.min(validKlines.length, size + 80)); }} disabled={windowSize >= validKlines.length}>+</button>
        </div>
      </div>
      <aside className="market-order-panel">
        <h4>挂牌资料</h4>
        <div className="detail-data-row compact-row"><span>挂牌/发行价</span><strong>{valueText(issuePrice)}</strong></div>
        <div className="detail-data-row compact-row"><span>每手股数</span><strong>{valueText(lotSize)}</strong></div>
        <div className="detail-data-row compact-row"><span>最新成交量</span><strong>{formatNumber(toNumber(quote?.volume), 0)}</strong></div>
        <h4 className="trade-title">五档盘口</h4>
        {askRows.length || bidRows.length ? (
          <div className="orderbook">
                {[...askRows].reverse().concat(bidRows).slice(0, 10).map((row, index) => (
                  <div key={index} className={`orderbook-row ${String(asRecord(row).side || asRecord(row).label || "").includes("ask") || String(asRecord(row).side || asRecord(row).label || "").includes("卖") ? "ask" : "bid"}`}>
                <span>{String(findValue(row, ["label", "档位"]) || (index < askRows.length ? "卖盘" : "买盘"))}</span>
                <strong>{formatNumber(toNumber(findValue(row, ["price", "价格"])))}</strong>
                <em>{formatNumber(toNumber(findValue(row, ["volume", "数量"])), 0)}</em>
              </div>
            ))}
          </div>
        ) : (
          <p className="empty-state compact-empty">暂无真实盘口数据</p>
        )}
        <h4 className="trade-title">分时成交量价</h4>
        {intradayTrades.length ? (
          <div className="trade-list">
            {intradayTrades.slice(0, 8).map((row, index) => (
              <p key={index}>
                <span>{String(findValue(row, ["time", "成交时间", "时间"]) || "--")}</span>
                <strong>{formatNumber(toNumber(findValue(row, ["price", "成交价", "价格"])))}</strong>
                <em>{formatNumber(toNumber(findValue(row, ["volume", "成交量", "数量"])), 0)}</em>
              </p>
            ))}
          </div>
        ) : (
          <p className="empty-state compact-empty">暂无真实分时成交数据</p>
        )}
      </aside>
    </div>
  );
}

function MetricRows({ rows, periods, limit = 12 }: { rows: unknown; periods?: string[]; limit?: number }) {
  const list = buildRows(rows, periods || [], limit);
  return (
    <div className="financial-value-list">
      {list.length ? (
        list.map((row, index) => (
          <div className="financial-value-row" key={`${row.label}-${index}`}>
            <span>{row.label}</span>
            <strong>{displayByLabel(row.label, row.value)}</strong>
          </div>
        ))
      ) : (
        <p className="empty-state compact-empty">暂无数据</p>
      )}
    </div>
  );
}

function FinancialHighlights({ summary, quote, profileFields }: { summary: JsonRecord; quote: StockF10["realtime_quote"]; profileFields: JsonRecord }) {
  const periods = Array.isArray(summary.periods) ? summary.periods.map(String) : [];
  const rows = buildRows(summary.rows, periods, 80);
  const preferred = ["市盈率", "市净率", "总市值", "流通市值", "营业总收入", "净利润", "净资产收益率", "销售净利率", "资产负债率", "每股收益"];
  const selected = preferred
    .map((keyword) => rows.find((row) => row.label.includes(keyword)))
    .filter(Boolean)
    .slice(0, 6) as Array<{ label: string; value: unknown }>;
  const profileMetrics = [
    ["市盈率", findValue(profileFields, ["市盈率", "滚动市盈率", "PE", "pe_ttm"])],
    ["市净率", findValue(profileFields, ["市净率", "PB", "pb"])],
    ["总市值", findValue(profileFields, ["总市值", "总市值(元)", "总市值(港元)"])],
    ["流通市值", findValue(profileFields, ["流通市值", "流通市值(元)", "港股市值"])],
  ]
    .filter(([, value]) => value !== undefined && value !== null && value !== "")
    .map(([label, value]) => ({ label: String(label), value }));
  const fallback = [...profileMetrics, ...selected.filter((row) => !profileMetrics.some((item) => item.label === row.label))].slice(0, 8);
  const numericValues = fallback.map((row) => Math.abs(toNumber(row.value) ?? 0));
  const max = Math.max(...numericValues, 1);

  return (
    <div className="financial-block metrics-block">
      <div className="financial-block-heading">
        <div>
          <span className="financial-section-mark" />
          <h3>核心指标</h3>
        </div>
        <span className="financial-period">报告期 {periods[0] || "最新"} · 行情 {formatDate(quote?.quote_time || quote?.fetched_at)}</span>
      </div>
      <div className="insight-metric-grid">
        {fallback.map((row, index) => {
          const width = Math.max(8, Math.min(100, ((Math.abs(toNumber(row.value) ?? 0) || 0) / max) * 100));
          return (
            <article className="insight-metric-card" key={`${row.label}-${index}`}>
              <span>{row.label}</span>
              <strong>{displayByLabel(row.label, row.value)}</strong>
              <i style={{ width: `${width}%` }} />
            </article>
          );
        })}
      </div>
    </div>
  );
}

function scoreTone(score: number | null | undefined) {
  if (score === null || score === undefined || !Number.isFinite(score)) return "neutral";
  if (score >= 70) return "up";
  if (score <= 45) return "down";
  return "neutral";
}

function trendTone(value: number | null | undefined) {
  if (value === null || value === undefined || !Number.isFinite(value) || value === 0) return "neutral";
  return value > 0 ? "up" : "down";
}

function cleanResearchLine(line: string) {
  return line
    .replace(/^[-*]\s*/, "")
    .replace(/^\d+[.)]\s*/, "")
    .replace(/^>\s*/, "")
    .replace(/\*\*/g, "")
    .trim();
}

function extractReportScore(report: string): number | null {
  const scoreMatch = report.match(/(?:评分|得分|score|评级)[^\d]{0,12}(\d{1,3})\s*\/\s*100/i);
  if (scoreMatch) return Math.min(100, Math.max(0, Number(scoreMatch[1])));
  const looseMatch = report.match(/(\d{1,3})\s*\/\s*100/);
  return looseMatch ? Math.min(100, Math.max(0, Number(looseMatch[1]))) : null;
}

function extractReportRating(report: string) {
  const match = report.match(/评级[：:\s*]*([A-D][+-]?|买入|增持|持有|中性|谨慎|回避|卖出|看多|看空)/i);
  return match ? match[1].toUpperCase() : "";
}

function extractResearchLines(report: string, keywords: string[], limit = 3) {
  const results: string[] = [];
  for (const rawLine of report.split("\n")) {
    const line = cleanResearchLine(rawLine);
    if (!line || line.length < 4 || line.length > 110) continue;
    if (keywords.some((keyword) => line.includes(keyword))) {
      results.push(line);
    }
    if (results.length >= limit) break;
  }
  return results;
}

function keyLevelsText(levels: Record<string, number | null | undefined> | undefined, klines: StockKline[]) {
  const support =
    toNumber(levels?.support) ??
    toNumber(levels?.support_level) ??
    toNumber(levels?.ma20) ??
    toNumber(levels?.MA20);
  const resistance =
    toNumber(levels?.resistance) ??
    toNumber(levels?.resistance_level) ??
    toNumber(levels?.ma60) ??
    toNumber(levels?.MA60);
  if (support !== null || resistance !== null) {
    return `支撑 ${formatNumber(support)} / 压力 ${formatNumber(resistance)}`;
  }
  const recent = klines.slice(0, 20);
  const lows = recent.map((item) => toNumber(item.low_price)).filter((item): item is number => item !== null);
  const highs = recent.map((item) => toNumber(item.high_price)).filter((item): item is number => item !== null);
  if (lows.length || highs.length) {
    return `近20日低 ${formatNumber(lows.length ? Math.min(...lows) : null)} / 高 ${formatNumber(highs.length ? Math.max(...highs) : null)}`;
  }
  return "待补充";
}

function ResearchInsightSummary({
  report,
  running,
  stage,
  agentSnapshots,
  quote,
  summary,
  profileFields,
  klines,
}: {
  report: string;
  running: boolean;
  stage: string;
  agentSnapshots: Array<ResearchFundamentalAgent | ResearchTechnicalAgent>;
  quote: StockF10["realtime_quote"];
  summary: JsonRecord;
  profileFields: JsonRecord;
  klines: StockKline[];
}) {
  const fundamental = agentSnapshots.find((item): item is ResearchFundamentalAgent => "rating" in item);
  const technical = agentSnapshots.find((item): item is ResearchTechnicalAgent => "trend" in item || "capital_intent" in item);
  const reportScore = extractReportScore(report);
  const score =
    fundamental?.score ??
    (technical?.score !== undefined && reportScore !== null ? Math.round((technical.score + reportScore) / 2) : reportScore);
  const rating = fundamental?.rating || extractReportRating(report) || (score !== null && score !== undefined ? (score >= 70 ? "偏积极" : score <= 45 ? "谨慎" : "中性") : "待生成");
  const changePct = toNumber(quote?.change_pct);
  const pe =
    findNumber(profileFields, ["市盈率", "滚动市盈率", "PE", "pe_ttm"]) ??
    findMetricFromRows(summary.rows, ["市盈率", "滚动市盈率", "PE"], Array.isArray(summary.periods) ? summary.periods.map(String) : []);
  const pb =
    findNumber(profileFields, ["市净率", "PB", "pb"]) ??
    findMetricFromRows(summary.rows, ["市净率", "PB"], Array.isArray(summary.periods) ? summary.periods.map(String) : []);
  const opportunityLines = [
    ...(fundamental?.positive_factors || []),
    ...(technical?.positive_factors || []),
    ...extractResearchLines(report, ["看多", "利好", "机会", "改善", "增长", "支撑"], 3),
  ].filter(Boolean).slice(0, 3);
  const riskLines = [
    ...(fundamental?.negative_factors || []),
    ...(technical?.negative_factors || []),
    ...(fundamental?.data_gaps || []),
    ...(technical?.data_gaps || []),
    ...extractResearchLines(report, ["风险", "看空", "承压", "下滑", "谨慎", "不足"], 4),
  ].filter(Boolean).slice(0, 4);
  const cards = [
    {
      label: "综合研判",
      value: score !== null && score !== undefined ? `${score}/100` : rating,
      hint: rating,
      tone: scoreTone(score),
    },
    {
      label: "行情动量",
      value: formatPercent(changePct),
      hint: changePct !== null ? (changePct > 0 ? "短线走强" : changePct < 0 ? "短线承压" : "横盘") : "暂无行情",
      tone: trendTone(changePct),
    },
    {
      label: "资金/趋势",
      value: technical?.trend || "--",
      hint: technical?.capital_intent || "待 AI 分析",
      tone: /UP|上行|流入|吸筹|看多/i.test(`${technical?.trend || ""}${technical?.capital_intent || ""}`)
        ? "up"
        : /DOWN|下行|流出|派发|看空/i.test(`${technical?.trend || ""}${technical?.capital_intent || ""}`)
          ? "down"
          : "neutral",
    },
    {
      label: "关键价位",
      value: keyLevelsText(technical?.key_levels, klines),
      hint: "支撑/压力",
      tone: "neutral",
    },
    {
      label: "估值观察",
      value: `PE ${formatNumber(pe)} / PB ${formatNumber(pb)}`,
      hint: pe !== null || pb !== null ? "结合行业比较" : "暂无估值",
      tone: "neutral",
    },
  ];

  return (
    <section className="research-insight-summary">
      <div className="research-insight-head">
        <div>
          <span className="financial-section-mark" />
          <h3>AI 研判摘要</h3>
        </div>
        <span>{running ? "研报生成中..." : stage || (report ? "已提炼最新研报" : "基于本地数据快照")}</span>
      </div>
      <div className="research-signal-grid">
        {cards.map((card) => (
          <article className={`research-signal-card ${card.tone}`} key={card.label}>
            <span>{card.label}</span>
            <strong>{card.value}</strong>
            <small>{card.hint}</small>
          </article>
        ))}
      </div>
      <div className="research-brief-grid">
        <article>
          <h4>看点</h4>
          {opportunityLines.length ? (
            opportunityLines.map((line, index) => <p key={`${line}-${index}`}>{line}</p>)
          ) : (
            <p>暂无明确看多信号，建议先在“研究”页签生成 AI 研报。</p>
          )}
        </article>
        <article>
          <h4>风险</h4>
          {riskLines.length ? (
            riskLines.map((line, index) => <p key={`${line}-${index}`}>{line}</p>)
          ) : (
            <p>暂无明显风险项，仍需结合公告、财报和成交量验证。</p>
          )}
        </article>
      </div>
    </section>
  );
}

function NewsList({
  title,
  items,
  total,
  page,
  pageSize,
  loading,
  dateKey,
  onPageChange,
}: {
  title: string;
  items: Array<StockNewsItem | StockNotice | JsonRecord>;
  total?: number;
  page: number;
  pageSize: number;
  loading: boolean;
  dateKey: "news_time" | "notice_date" | "report_date" | "report_period";
  onPageChange?: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil((total || items.length) / pageSize));
  return (
    <section className="detail-list-section">
      <div className="detail-list-heading">
        <h3>{title}</h3>
        <span>{loading ? "加载中..." : `${total || items.length} 条`}</span>
      </div>
      {items.length ? (
        items.map((item, index) => {
          const record = asRecord(item);
          const date = findValue(record, [dateKey, "notice_date", "news_time", "report_date", "report_period", "published_at"]);
          const source = findValue(record, ["source_name", "source", "来源"]) || "数据源";
          const headline = findValue(record, ["title", "report_name", "公告标题", "新闻标题", "indicator"]) || "查看资料";
          const url = findValue(record, ["url", "detail_url", "链接"]);
          const content = findValue(record, ["content", "summary", "message", "公告内容"]);
          const contentText = typeof content === "object" ? valueText(content) : String(content || headline);
          return (
            <article className="detail-news-row" key={`${String(date)}-${index}`}>
              <div>
                <strong>{String(headline)}</strong>
                <small>{formatDate(String(date || ""))} · {String(source)}</small>
              </div>
              {url ? (
                <a href={String(url)} target="_blank" rel="noreferrer">查看详情</a>
              ) : (
                <details>
                  <summary>查看详情</summary>
                  <p>{contentText}</p>
                </details>
              )}
            </article>
          );
        })
      ) : (
        <p className="empty-state">暂无{title}</p>
      )}
      {onPageChange && (
        <div className="pagination-row detail-pagination">
          <span>第 {page} / {totalPages} 页</span>
          <div className="pagination-actions">
            <button className="quiet-button" type="button" disabled={page <= 1 || loading} onClick={() => onPageChange(page - 1)}>上一页</button>
            <button className="quiet-button" type="button" disabled={page >= totalPages || loading} onClick={() => onPageChange(page + 1)}>下一页</button>
          </div>
        </div>
      )}
    </section>
  );
}

function ShareholderTable({ rows, title }: { rows: unknown; title: string }) {
  const list = shareholderRows(rows);
  const columns = [
    { key: "rank", label: "序号", aliases: ["序号", "排名", "rank", "编号"] },
    { key: "name", label: "股东名称", aliases: ["股东名称", "股东", "名称", "name", "股东全称"] },
    { key: "shares", label: "持股数量", aliases: ["持股数量", "持股数", "股份数", "shares", "数量"] },
    { key: "ratio", label: "持股比例", aliases: ["持股比例", "持股比", "占比", "ratio", "percent"] },
    { key: "nature", label: "股本性质", aliases: ["股本性质", "股份性质", "nature", "性质"] },
    { key: "date", label: "截止日期", aliases: ["截止日期", "截至日期", "报告期", "日期", "date"] },
  ];
  return (
    <div className="shareholder-table-block">
      <div className="shareholder-table-title">{title}</div>
      {list.length ? (
        <div className="shareholder-table-wrap">
          <table className="shareholder-table">
            <thead>
              <tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr>
            </thead>
            <tbody>
              {list.map((row, index) => (
                <tr key={`${String(shareholderField(row, columns[1].aliases) || "")}-${index}`}>
                  <td className="shareholder-rank">{valueText(shareholderField(row, columns[0].aliases) ?? index + 1)}</td>
                  <td className="shareholder-name">{valueText(shareholderField(row, columns[1].aliases) ?? `项目 ${index + 1}`)}</td>
                  <td>{displayByLabel(columns[2].label, shareholderField(row, columns[2].aliases))}</td>
                  <td>{displayByLabel(columns[3].label, shareholderField(row, columns[3].aliases))}</td>
                  <td>{valueText(shareholderField(row, columns[4].aliases))}</td>
                  <td>{formatDate(String(shareholderField(row, columns[5].aliases) || ""))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="empty-state compact-empty">暂无{title}数据</p>
      )}
    </div>
  );
}

function FundFlowPanel({ data }: { data: JsonRecord }) {
  const rows = asArray(data.rows);
  const latest = rows[0] || {};
  const isHongKongMode = String(data.mode || data.source || "").includes("港股") || findValue(latest, ["持股市值"]) !== undefined;
  const netKeyAliases = isHongKongMode
    ? ["持股市值变化-1日", "持股市值变动-1日", "市值变化-1日"]
    : ["主力净流入-净额", "主力净流入", "主力净额", "主力资金净流入"];
  const mainValue = isHongKongMode ? findNumber(latest, ["持股市值", "持股市值(元)"]) : findNumber(latest, netKeyAliases);
  const chartRows = rows.slice(0, 5).reverse();
  const chartValues = chartRows.map((row) => findNumber(row, netKeyAliases) ?? 0);
  const max = Math.max(...chartValues.map((value) => Math.abs(value)), 1);
  const heroTone = (mainValue ?? 0) > 0 ? "positive" : (mainValue ?? 0) < 0 ? "negative" : "neutral";
  const cards = isHongKongMode
    ? [
        ["持股市值", findValue(latest, ["持股市值"])],
        ["1日变化", findValue(latest, ["持股市值变化-1日"])],
        ["5日变化", findValue(latest, ["持股市值变化-5日"])],
        ["持股占比", findValue(latest, ["持股数量占A股百分比", "持股比例"])],
      ]
    : [
        ["主力净流入", findValue(latest, netKeyAliases)],
        ["主力净占比", findValue(latest, ["主力净流入-净占比", "主力净占比"])],
        ["超大单净流入", findValue(latest, ["超大单净流入-净额", "超大单净流入"])],
        ["大单净流入", findValue(latest, ["大单净流入-净额", "大单净流入"])],
      ];

  return (
    <section className="f10-section inner-section">
      <div className="f10-section-head">
        <div>
          <h3>{isHongKongMode ? "港股通持股 / 资金参考" : "资金流向"}</h3>
          <span>{String(data.source || "本地资金数据")}</span>
        </div>
      </div>
      {rows.length ? (
        <>
          <div className="fund-flow-overview">
            <article className="fund-flow-hero">
              <div className="fund-flow-hero-head">
                <span className="fund-flow-eyebrow">CAPITAL FLOW</span>
                <strong>{isHongKongMode ? "港股通持股" : "主力资金"}</strong>
              </div>
              <div className={`fund-flow-hero-value ${heroTone}`}>
                <span>{isHongKongMode ? "最新持股市值" : "最新主力净流入"}</span>
                <strong>{formatMoney(mainValue)}</strong>
              </div>
              <div className="fund-flow-hero-meta">
                <span>{formatDate(String(findValue(latest, ["日期", "持股日期", "trade_date"]) || ""))}</span>
                <span>收盘 {displayByLabel("收盘", findValue(latest, ["收盘价", "当日收盘价", "收盘"]))}</span>
              </div>
            </article>
            <article className="fund-flow-chart-card">
              <div className="fund-flow-chart-head">
                <h4>{isHongKongMode ? "近五日市值变化" : "近五日主力净流入"}</h4>
                <span>仅展示数据源返回的净额字段</span>
              </div>
              <svg className="fund-flow-chart" viewBox="0 0 460 180" role="img" aria-label="近五日资金柱状图">
                <line className="fund-flow-zero-line" x1="24" x2="436" y1="90" y2="90" />
                {chartRows.map((row, index) => {
                  const value = chartValues[index];
                  const barHeight = Math.max(2, (Math.abs(value) / max) * 68);
                  const y = value >= 0 ? 90 - barHeight : 90;
                  const x = 48 + index * 78;
                  return (
                    <g key={`${index}-${String(findValue(row, ["日期", "持股日期"]) || "")}`}>
                      <rect className={`fund-flow-bar ${value >= 0 ? "positive" : "negative"}`} x={x} y={y} width="34" height={barHeight} rx="4" />
                      <text className="fund-flow-axis-label" x={x - 12} y="166">{formatDate(String(findValue(row, ["日期", "持股日期"]) || "")).slice(5)}</text>
                    </g>
                  );
                })}
              </svg>
            </article>
          </div>
          <div className="fund-flow-stat-grid">
            {cards.map(([label, value]) => (
              <article className={`fund-flow-stat-card ${flowTone(value)}`} key={String(label)}>
                <span>{String(label)}</span>
                <strong>{displayByLabel(String(label), value)}</strong>
              </article>
            ))}
          </div>
          <div className="fund-flow-table-wrap">
            <table className="mini-table fund-flow-table">
              <thead>
                <tr>
                  {Object.keys(latest).slice(0, 8).map((key) => <th key={key}>{key}</th>)}
                </tr>
              </thead>
              <tbody>
                {rows.slice(0, 10).map((row, index) => (
                  <tr key={index}>
                    {Object.keys(latest).slice(0, 8).map((key) => <td key={key}>{displayByLabel(key, row[key])}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <p className="empty-state">{String(data.message || "暂无资金流向数据")}</p>
      )}
    </section>
  );
}

function flowTone(value: unknown): "positive" | "negative" | "neutral" {
  const num = toNumber(value);
  if (num === null || num === 0) return "neutral";
  return num > 0 ? "positive" : "negative";
}

function F10Panel({ detail, activeTab, setActiveTab }: { detail: StockF10; activeTab: F10Tab; setActiveTab: (tab: F10Tab) => void }) {
  const summary = asRecord(detail.financial_summary);
  const periods = Array.isArray(summary.periods) ? summary.periods.map(String) : [];
  const statements = asRecord(detail.financial_statements);
  const holders = asRecord(detail.holders);
  const profile = asRecord(detail.profile);
  const profileFields = asRecord(profile.fields);
  const reports = detail.published_reports?.reports || [];
  const composition = asRecord(detail.business_composition);
  const compositionSections = asArray(composition.sections);

  return (
    <>
      <div className="detail-sub-tabs">
        {(["财务", "股东", "简况", "财报"] as const).map((item) => (
          <button type="button" key={item} className={activeTab === item ? "active" : ""} onClick={() => setActiveTab(item)}>{item}</button>
        ))}
      </div>
      {activeTab === "财务" && (
        <>
          <FinancialHighlights summary={summary} quote={detail.realtime_quote} profileFields={profileFields} />
          <div className="financial-block metrics-block">
            <div className="financial-block-heading">
              <div><span className="financial-section-mark" /><h3>财务指标</h3></div>
              <span className="financial-period">{periods[0] || "最新报告期"}</span>
            </div>
            <MetricRows rows={summary.rows} periods={periods} limit={18} />
          </div>
          {(["income_statement", "balance_sheet", "cash_flow"] as const).map((key) => {
            const block = asRecord(statements[key]);
            return (
              <div className="financial-block statement-block" key={key}>
                <div className="financial-block-heading">
                  <div><span className="financial-section-mark" /><h3>{String(block.label || block.report_name || statementName(key))}</h3></div>
                  <span className="financial-period">{formatDate(String(block.report_date || ""))}</span>
                </div>
                <MetricRows rows={block.rows} limit={12} />
              </div>
            );
          })}
        </>
      )}
      {activeTab === "股东" && (
        <>
          <div className="financial-block">
            <div className="financial-block-heading">
              <div><span className="financial-section-mark" /><h3>十大股东 / 主要持有人</h3></div>
              <span className="financial-period">{String(holders.source || "本地数据")}</span>
            </div>
            <ShareholderTable rows={holders.major} title="十大股东 / 主要持有人" />
          </div>
          <div className="financial-block">
            <div className="financial-block-heading">
              <div><span className="financial-section-mark" /><h3>流通股东 / 官方入口</h3></div>
            </div>
            {asArray(holders.official_links).length ? (
              <div className="extended-source-links">
                {asArray(holders.official_links).map((link, index) => (
                  <a className="extended-source-link" href={String(link.url || "#")} target="_blank" rel="noreferrer" key={index}>
                    {String(link.name || `官方入口 ${index + 1}`)}
                  </a>
                ))}
              </div>
            ) : (
              <ShareholderTable rows={holders.circulating} title="流通股东" />
            )}
            {holders.message ? <p className="detail-description">{String(holders.message)}</p> : null}
          </div>
        </>
      )}
      {activeTab === "简况" && (
        <>
          <div className="financial-block">
            <div className="financial-block-heading">
              <div><span className="financial-section-mark" /><h3>公司简况</h3></div>
              <span className="financial-period">{String(profile.source || "F10")}</span>
            </div>
            <div className="profile-grid">
              {[
                "公司名称",
                "英文名称",
                "所属行业",
                "董事长",
                "注册地址",
                "办公地址",
                "公司网址",
                "上市日期",
                "发行价",
                "每手股数",
                "交易所",
                "板块",
              ].map((label) => (
                <article className="info-item" key={label}>
                  <span>{label}</span>
                  <strong>{valueText(findValue(profileFields, [label]))}</strong>
                </article>
              ))}
            </div>
          </div>
          <div className="financial-block">
            <div className="financial-block-heading">
              <div><span className="financial-section-mark" /><h3>主营业务与业务回顾</h3></div>
            </div>
            <p className="detail-description">{String(findValue(profileFields, ["公司介绍", "主营业务", "经营范围", "简介"]) || composition.review || profile.message || "暂无公司简况资料")}</p>
            {compositionSections.length ? (
              <div className="composition-text-list">
                {compositionSections.slice(0, 4).map((section, index) => (
                  <article className="composition-text-item" key={index}>
                    <div className="composition-text-title">
                      <i className={`composition-dot tone-${index % 6}`} />
                      <strong>{String(section.category || `业务 ${index + 1}`)}</strong>
                    </div>
                    <p>{String(findValue(asArray(section.items)[0] || {}, ["summary", "name"]) || "")}</p>
                  </article>
                ))}
              </div>
            ) : null}
          </div>
        </>
      )}
      {activeTab === "财报" && (
        <section className="detail-list-section financial-reports-list">
          <div className="detail-list-heading">
            <h3>财报与披露</h3>
            <span>{reports.length} 份正式文件</span>
          </div>
          {reports.length ? (
            <div className="financial-report-links">
              {reports.map((report, index) => (
                <article className="financial-report-link-row" key={`published-${index}`}>
                  <div>
                    <div className="report-title">{String(report.report_name || report.title || "财报披露")}</div>
                    <div className="report-meta">{formatDate(report.report_date || report.notice_date)} · {String(report.source_name || "公告来源")}</div>
                  </div>
                  {report.url ? (
                    <a className="financial-report-link" href={String(report.url)} target="_blank" rel="noreferrer">打开原文</a>
                  ) : (
                    <span className="missing-link">暂无链接</span>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <p className="empty-state">
              暂无正式财报披露文件。财务指标已归档至“财务”页签；公告源：
              {String(detail.published_reports?.source || "未配置")}
            </p>
          )}
          {reports.length ? (
            <p className="detail-description report-source-note">
              正式财报链接来自公告披露源；财务指标、估值和同比数据请查看“财务”页签。
            </p>
          ) : null}
        </section>
      )}
    </>
  );
}

function statementName(key: string) {
  if (key === "income_statement") return "利润表";
  if (key === "balance_sheet") return "资产负债表";
  return "现金流量表";
}

function renderInlineMarkdown(text: string) {
  return text.split(/(\*\*[^*]+\*\*|__[^_]+__)/g).map((part, index) => {
    const isBold =
      (part.startsWith("**") && part.endsWith("**")) ||
      (part.startsWith("__") && part.endsWith("__"));
    return isBold ? <strong key={index}>{part.slice(2, -2)}</strong> : part;
  });
}

function renderMarkdown(text: string) {
  const nodes: ReactNode[] = [];
  let bullets: string[] = [];

  const flushBullets = () => {
    if (!bullets.length) return;
    const current = bullets;
    bullets = [];
    nodes.push(
      <ul className="research-list" key={`ul-${nodes.length}`}>
        {current.map((item, index) => (
          <li key={index}>{renderInlineMarkdown(item)}</li>
        ))}
      </ul>,
    );
  };

  text.split("\n").forEach((rawLine) => {
    const line = rawLine.trim();
    if (!line) {
      flushBullets();
      nodes.push(<div className="research-line-gap" key={`gap-${nodes.length}`} />);
      return;
    }
    if (line.startsWith("- ") || line.startsWith("* ") || /^\d+[.)]\s+/.test(line)) {
      bullets.push(line.replace(/^[-*]\s+/, "").replace(/^\d+[.)]\s+/, ""));
      return;
    }
    flushBullets();
    if (line.startsWith("### ")) {
      nodes.push(<h4 key={`h4-${nodes.length}`}>{renderInlineMarkdown(line.slice(4))}</h4>);
      return;
    }
    if (line.startsWith("## ")) {
      nodes.push(<h3 key={`h3-${nodes.length}`}>{renderInlineMarkdown(line.slice(3))}</h3>);
      return;
    }
    if (line.startsWith("# ")) {
      nodes.push(<h2 key={`h2-${nodes.length}`}>{renderInlineMarkdown(line.slice(2))}</h2>);
      return;
    }
    if (line.startsWith(">")) {
      nodes.push(<blockquote key={`quote-${nodes.length}`}>{renderInlineMarkdown(line.replace(/^>\s?/, ""))}</blockquote>);
      return;
    }
    if (/^[一二三四五六七八九十]+[、.．]\s?/.test(line)) {
      nodes.push(<h3 key={`cn-heading-${nodes.length}`}>{renderInlineMarkdown(line)}</h3>);
      return;
    }
    nodes.push(<p key={`p-${nodes.length}`}>{renderInlineMarkdown(line)}</p>);
  });
  flushBullets();
  return nodes;
}

export function StockDetailDrawer({
  stock,
  onClose,
}: {
  stock: Pick<StockSymbol, "market" | "symbol" | "name">;
  onClose: () => void;
}) {
  const requestId = useRef(0);
  const [detail, setDetail] = useState<StockF10 | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");
  const [remoteError, setRemoteError] = useState("");
  const [companyGraphOpen, setCompanyGraphOpen] = useState(false);
  const [tab, setTab] = useState<PrimaryTab>("精选");
  const [f10Tab, setF10Tab] = useState<F10Tab>("财务");
  const [newsPage, setNewsPage] = useState(1);
  const [noticePage, setNoticePage] = useState(1);
  const [newsRows, setNewsRows] = useState<StockNewsItem[] | null>(null);
  const [noticeRows, setNoticeRows] = useState<StockNotice[] | null>(null);
  const [pageLoading, setPageLoading] = useState<"news" | "notice" | null>(null);
  const [researchReport, setResearchReport] = useState("");
  const [researchRunning, setResearchRunning] = useState(false);
  const [researchStage, setResearchStage] = useState("");
  const [agentSnapshots, setAgentSnapshots] = useState<Array<ResearchFundamentalAgent | ResearchTechnicalAgent>>([]);

  const pageSize = 8;

  async function loadLocalAndRemote() {
    const current = requestId.current + 1;
    requestId.current = current;
    setLoading(true);
    setError("");
    setRemoteError("");
    setNewsRows(null);
    setNoticeRows(null);
    setNewsPage(1);
    setNoticePage(1);
    try {
      const local = await api.getStockF10(stock.market, stock.symbol, {
        localOnly: true,
        klineLimit: 5000,
        financialLimit: 20,
        noticeLimit: pageSize,
        newsLimit: pageSize,
      });
      if (requestId.current === current) setDetail(local);
    } catch (reason) {
      if (requestId.current === current) {
        setError(reason instanceof Error ? reason.message : "本地股票详情加载失败");
      }
    } finally {
      if (requestId.current === current) setLoading(false);
    }
    await refreshRemote(current);
  }

  async function refreshRemote(current = requestId.current) {
    setRefreshing(true);
    setRemoteError("");
    try {
      const remote = await api.getStockF10(stock.market, stock.symbol, {
        refresh: true,
        klineLimit: 5000,
        financialLimit: 20,
        noticeLimit: pageSize,
        newsLimit: pageSize,
      });
      if (requestId.current === current) {
        setDetail(remote);
        setNewsRows(null);
        setNoticeRows(null);
      }
    } catch (reason) {
      if (requestId.current === current) {
        setRemoteError(reason instanceof Error ? reason.message : "远程数据刷新失败，已保留本地缓存");
      }
    } finally {
      if (requestId.current === current) setRefreshing(false);
    }
  }

  useEffect(() => {
    const key = storageKey(stock.market, stock.symbol);
    setResearchReport(window.localStorage.getItem(key) || "");
    setAgentSnapshots([]);
    setResearchStage("");
    void loadLocalAndRemote();
    return () => {
      requestId.current += 1;
    };
  }, [stock.market, stock.symbol]);

  async function changeNewsPage(nextPage: number) {
    setPageLoading("news");
    try {
      const result = await api.listStockNewsPage(stock.market, stock.symbol, nextPage, pageSize);
      setNewsRows(result.items);
      setNewsPage(result.page);
    } finally {
      setPageLoading(null);
    }
  }

  async function changeNoticePage(nextPage: number) {
    setPageLoading("notice");
    try {
      const result = await api.listStockNoticesPage(stock.market, stock.symbol, nextPage, pageSize);
      setNoticeRows(result.items);
      setNoticePage(result.page);
    } finally {
      setPageLoading(null);
    }
  }

  async function generateResearch() {
    if (!detail || researchRunning) return;
    setResearchRunning(true);
    setResearchReport("");
    setAgentSnapshots([]);
    setResearchStage("正在启动研究工作流...");
    try {
      let nextReport = "";
      await api.analyzeResearch(
        { market: detail.symbol.market, symbol: detail.symbol.symbol, top_k: 5, refresh: false },
        {
          onStage: (data) => setResearchStage(data.message),
          onAgent: (data) => setAgentSnapshots((current) => [...current.filter((item) => item.agent !== data.agent), data]),
          onReport: (delta) => {
            nextReport += delta;
            setResearchReport((current) => current + delta);
          },
          onDone: () => {
            setResearchStage("AI 研报已生成");
            window.localStorage.setItem(storageKey(detail.symbol.market, detail.symbol.symbol), nextReport);
          },
          onError: (message) => setResearchStage(message),
        },
      );
    } catch (reason) {
      setResearchStage(reason instanceof Error ? reason.message : "研究分析失败");
    } finally {
      setResearchRunning(false);
    }
  }

  const quote = detail?.realtime_quote ?? null;
  const symbolInfo = detail?.symbol ?? stock;
  const profile = asRecord(detail?.profile);
  const profileFields = asRecord(profile.fields);
  const rawPayload = {
    ...asRecord((symbolInfo as StockSymbol).raw_payload),
    ...asRecord(quote?.raw_payload),
    ...profileFields,
  };
  const derived = asRecord(asRecord(quote?.raw_payload).derived);
  const financialSummary = asRecord(detail?.financial_summary);
  const summaryPeriods = Array.isArray(financialSummary.periods)
    ? financialSummary.periods.map(String)
    : [];
  const currentPrice = toNumber(quote?.current_price) ?? findNumber(rawPayload, ["最新价", "当前价", "current_price", "f43"]);
  const changePct = toNumber(quote?.change_pct) ?? findNumber(rawPayload, ["涨跌幅", "change_pct", "f170"]);
  const changeAmount = toNumber(quote?.change_amount) ?? findNumber(rawPayload, ["涨跌额", "change_amount", "f169"]);
  const totalMarketCap =
    findNumber(profileFields, ["总市值", "总市值(元)", "总市值(港元)", "港股市值", "total_market_value", "total_market_cap"]) ??
    findNumber(rawPayload, ["总市值", "f116"]) ??
    (findNumber(derived, ["total_market_cap_yi"]) !== null ? Number(findNumber(derived, ["total_market_cap_yi"])) * 100000000 : null);
  const floatMarketCap =
    findNumber(profileFields, ["流通市值", "港股市值", "流通市值(元)", "float_market_value", "float_market_cap"]) ??
    findNumber(rawPayload, ["流通市值", "f117"]) ??
    (findNumber(derived, ["float_market_cap_yi"]) !== null ? Number(findNumber(derived, ["float_market_cap_yi"])) * 100000000 : null);
  const peRatio =
    findNumber(profileFields, ["市盈率", "滚动市盈率", "PE", "pe_ttm"]) ??
    findNumber(rawPayload, ["市盈率", "PE", "pe", "f市盈率"]);
  const pbRatio =
    findNumber(profileFields, ["市净率", "PB", "pb"]) ??
    findNumber(rawPayload, ["市净率", "PB", "pb"]);
  const pushPeRatio = findNumber(rawPayload, ["f162"]);
  const pushPbRatio = findNumber(rawPayload, ["f167"]);
  const normalizedPeRatio =
    peRatio ??
    (pushPeRatio !== null ? pushPeRatio / 100 : null) ??
    findNumber(derived, ["pe_ratio", "市盈率", "PE"]) ??
    findMetricFromRows(financialSummary.rows, ["市盈率", "滚动市盈率", "PE"], summaryPeriods);
  const normalizedPbRatio =
    pbRatio ??
    (pushPbRatio !== null ? pushPbRatio / 100 : null) ??
    findNumber(derived, ["pb_ratio", "市净率", "PB"]) ??
    findMetricFromRows(financialSummary.rows, ["市净率", "PB"], summaryPeriods);
  const turnoverRate =
    toNumber(quote?.turnover_rate) ??
    findNumber(profileFields, ["换手率", "换手", "turnover_rate"]) ??
    findNumber(rawPayload, ["换手率", "换手", "f168"]);
  const name = detail?.symbol.name || stock.name || stock.symbol;
  const updatedAt = quote?.fetched_at || quote?.quote_time || (detail?.symbol as StockSymbol | undefined)?.last_synced_at;
  const newsItems = newsRows || detail?.news || [];
  const noticeItems = noticeRows || detail?.notices || [];
  const reports = detail?.published_reports?.reports || [];

  return (
    <div className="drawer-mask" onClick={onClose}>
      <aside className="stock-drawer stock-detail-drawer restored-stock-detail" onClick={(event) => event.stopPropagation()}>
        <header className="detail-market-header">
          <div>
            <span className={`market-chip ${marketClass[stock.market] || ""}`}>{marketLabel[stock.market] || stock.market}</span>
            <h2>{name}<small>({stock.symbol})</small></h2>
            <div className="detail-tags">
              <span>{(detail?.symbol as StockSymbol | undefined)?.exchange || "交易所待补充"}</span>
              <span>{(detail?.symbol as StockSymbol | undefined)?.asset_type || "股票"}</span>
              <span>{formatDate((detail?.symbol as StockSymbol | undefined)?.list_date)}</span>
              <span>{refreshing ? "同步远程数据中" : "本地优先 · 自动校验远程"}</span>
            </div>
          </div>
          <div className="detail-head-actions">
            <button type="button" onClick={() => setCompanyGraphOpen(true)}><Building2 size={15} /> 公司关联</button>
            <button type="button" onClick={() => void refreshRemote()} disabled={refreshing}>{refreshing ? "更新中" : "手动更新"}</button>
            <button type="button" onClick={onClose}>关闭</button>
          </div>
        </header>

        {companyGraphOpen && <CompanyGraphDialog stock={stock} close={() => setCompanyGraphOpen(false)} />}
        {loading && <p className="empty-state">正在读取本地数据库...</p>}
        {error && <p className="form-error">{error}</p>}
        {remoteError && <p className="form-error soft-error">{remoteError}</p>}

        {detail && (
          <>
            <section className="detail-quote-strip restored-quote-strip">
              <div className={`detail-last-price ${(changePct ?? 0) >= 0 ? "up" : "down"}`}>
                {formatNumber(currentPrice)}
                <small>{formatNumber(changeAmount)} / {formatPercent(changePct)}</small>
              </div>
              <div className="detail-quote-metrics">
                <span>今开 <b>{formatNumber(toNumber(quote?.open_price))}</b></span>
                <span>最高 <b>{formatNumber(toNumber(quote?.high_price))}</b></span>
                <span>最低 <b>{formatNumber(toNumber(quote?.low_price))}</b></span>
                <span>昨收 <b>{formatNumber(toNumber(quote?.previous_close_price))}</b></span>
                <span>成交量 <b>{formatNumber(toNumber(quote?.volume), 0)}</b></span>
                <span>成交额 <b>{formatMoney(toNumber(quote?.amount))}</b></span>
                <span>总市值 <b>{formatMoney(totalMarketCap)}</b></span>
                <span>流通市值 <b>{formatMoney(floatMarketCap)}</b></span>
                <span>市盈率 <b>{formatNumber(normalizedPeRatio)}</b></span>
                <span>市净率 <b>{formatNumber(normalizedPbRatio)}</b></span>
                <span>换手率 <b>{formatPercent(turnoverRate)}</b></span>
                <span>更新时间 <b>{formatDate(updatedAt)}</b></span>
              </div>
            </section>

            <StockKlinePanel klines={detail.recent_klines} quote={quote} profileFields={profileFields} />

            <nav className="detail-primary-tabs">
              {(["精选", "新闻", "公告", "资金", "F10", "研究"] as const).map((item) => (
                <button type="button" key={item} className={tab === item ? "active" : ""} onClick={() => setTab(item)}>{item}</button>
              ))}
            </nav>

            <section className="detail-tab-page">
              {tab === "精选" && (
                <>
                  <div className="detail-page-title">
                    <h3>个股精选</h3>
                    <span>研究中心研报、核心指标与最新披露</span>
                  </div>
                  <ResearchInsightSummary
                    report={researchReport}
                    running={researchRunning}
                    stage={researchStage}
                    agentSnapshots={agentSnapshots}
                    quote={quote}
                    summary={asRecord(detail.financial_summary)}
                    profileFields={profileFields}
                    klines={detail.recent_klines}
                  />
                  <FinancialHighlights summary={asRecord(detail.financial_summary)} quote={quote} profileFields={profileFields} />
                  <NewsList title="最新披露" items={reports.slice(0, 6) as JsonRecord[]} dateKey="report_date" page={1} pageSize={6} loading={false} />
                </>
              )}
              {tab === "新闻" && (
                <NewsList
                  title="新闻"
                  items={newsItems}
                  total={detail.news_total}
                  page={newsPage}
                  pageSize={pageSize}
                  loading={pageLoading === "news"}
                  dateKey="news_time"
                  onPageChange={(page) => void changeNewsPage(page)}
                />
              )}
              {tab === "公告" && (
                <NewsList
                  title="公告"
                  items={noticeItems}
                  total={detail.notice_total}
                  page={noticePage}
                  pageSize={pageSize}
                  loading={pageLoading === "notice"}
                  dateKey="notice_date"
                  onPageChange={(page) => void changeNoticePage(page)}
                />
              )}
              {tab === "资金" && <FundFlowPanel data={asRecord(detail.fund_flow)} />}
              {tab === "F10" && <F10Panel detail={detail} activeTab={f10Tab} setActiveTab={setF10Tab} />}
              {tab === "研究" && (
                <section className="f10-section inner-section research-detail-section">
                  <div className="f10-section-head">
                    <div>
                      <h3>AI 深度研报</h3>
                      <span>{researchStage || "使用研究中心多智能体工作流生成"}</span>
                    </div>
                    <button className="primary-button" type="button" onClick={() => void generateResearch()} disabled={researchRunning}>
                      {researchRunning ? "生成中..." : researchReport ? "重新生成" : "生成 AI 研报"}
                    </button>
                  </div>
                  {agentSnapshots.length ? (
                    <div className="agent-score-grid">
                      {agentSnapshots.map((agent) => (
                        <article className="info-item" key={agent.agent}>
                          <span>{agent.agent}</span>
                          <strong>{"score" in agent ? `${agent.score} · ${"rating" in agent ? agent.rating : agent.trend}` : "--"}</strong>
                        </article>
                      ))}
                    </div>
                  ) : null}
                  <article className="research-markdown detail-research-markdown">
                    {researchReport ? renderMarkdown(researchReport) : <p className="empty-state">点击按钮后，研报会以打字机流式效果显示在这里。</p>}
                  </article>
                </section>
              )}
            </section>
          </>
        )}
      </aside>
    </div>
  );
}
