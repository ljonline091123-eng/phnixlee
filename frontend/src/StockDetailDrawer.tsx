import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from "react";
import { ArrowLeft, Building2, X } from "lucide-react";
import { CompanyGraphDialog } from "./CompanyGraphWorkbench";
import { KnowledgeGraphDialog } from "./KnowledgeGraphExplorer";

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

const noticeCategories = ["全部", "财务业绩", "重大事项", "风险提示", "抵押担保", "增持回购", "对外投资", "其他公告"] as const;
const researchSectionOrder = [
  { key: "industry_concepts", title: "行业概念" },
  { key: "qa", title: "问董秘" },
  { key: "earnings_forecast", title: "盈利预测" },
  { key: "institution_forecast", title: "机构预测（评级统计）" },
  { key: "latest_reports", title: "最新研报" },
  { key: "reports", title: "研报" },
] as const;

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
    const entries = Object.entries(record)
      .filter(([, item]) => item !== undefined && item !== null && item !== "")
      .map(([key, item]) => ({ label: humanFieldLabel(key), item }))
      .filter((entry) => entry.label)
      .slice(0, 6);
    if (entries.length) {
      return entries.map(({ label, item }) => `${label}: ${valueText(item)}`).join(" · ");
    }
    // Provider payloads can contain a field that is not yet in the Chinese
    // glossary.  Show its value rather than leaking an English raw key into
    // the stock page; the complete payload remains available from the source
    // record and is not discarded.
    return Object.values(record).slice(0, 6).map(valueText).join(" · ") || "--";
  }
  return String(value);
}

/** Report periods are identifiers, not amounts; keep years free of thousands separators. */
function reportPeriodText(value: unknown): string {
  if (typeof value === "number" && Number.isInteger(value) && value >= 1900 && value <= 2200) {
    return String(value);
  }
  const text = String(value ?? "").trim();
  if (/^\d{4}$/.test(text)) return text;
  return valueText(value);
}

/** Stable Chinese labels for normalized/provider fields used by F10 cards. */
const FIELD_LABELS: Record<string, string> = {
  metric: "指标",
  indicator: "指标",
  name: "名称",
  label: "项目",
  title: "标题",
  value: "数值",
  data: "数据",
  amount: "金额",
  content: "内容",
  summary: "摘要",
  source: "来源",
  source_name: "来源名称",
  source_url: "来源链接",
  source_id: "来源编号",
  report_name: "报告名称",
  report_type: "报告类型",
  report_date: "报告日期",
  notice_date: "公告日期",
  published_at: "发布日期",
  institution: "机构",
  rating: "评级",
  report_count: "研报数量",
  rating_count: "评级数量",
  forecast_year: "预测年度",
  eps: "每股收益",
  pe: "市盈率",
  pb: "市净率",
  net_profit: "净利润",
  url: "原文链接",
  code: "代码",
  symbol: "股票代码",
  stock_code: "股票代码",
  market: "市场",
  ratio: "比例",
  percent: "比例",
  shares: "持股数量",
  rank: "序号",
  date: "日期",
  change: "变动",
  status: "状态",
  definition: "释义",
  criteria: "判定口径",
  agent: "研究智能体",
  source_count: "来源数量",
  duplicate_count: "重复数量",
  change_pct: "涨跌幅",
  yoy: "同比",
  qoq: "环比",
  delta: "变动",
  delta_pct: "变动比例",
  MA5: "5日均线",
  MA20: "20日均线",
  MA60: "60日均线",
  VOL: "成交量",
  agent_name: "研究智能体",
  score: "评分",
  trend: "趋势",
  capital_intent: "资金倾向",
  positive_factors: "积极因素",
  negative_factors: "风险因素",
  data_gaps: "数据缺口",
  report_period: "报告期",
  as_of: "截至日期",
  fetched_at: "采集时间",
  updated_at: "更新时间",
  forecast: "预测值",
  target_price: "目标价",
  current_price: "当前价",
  previous_close_price: "昨收",
  open_price: "今开",
  high_price: "最高",
  low_price: "最低",
  volume: "成交量",
  turnover_rate: "换手率",
};

function humanFieldLabel(key: string): string {
  const text = String(key || "").trim();
  if (!text) return "";
  // Chinese provider labels are already user-facing and should be preserved.
  if (/[\u3400-\u9fff]/.test(text)) return text;
  return FIELD_LABELS[text] || FIELD_LABELS[text.toLowerCase()] || "";
}

function rowValueText(row: JsonRecord): string {
  const direct = pickValue(row, ["value", "值", "data", "数据", "数值", "content", "内容", "summary", "摘要"]);
  if (direct !== undefined) return valueText(direct);
  const labelKeys = new Set(["label", "name", "metric", "indicator", "title", "项目", "名称", "指标"]);
  const entries = Object.entries(row)
    .filter(([key, item]) => !labelKeys.has(key) && item !== undefined && item !== null && item !== "")
    .map(([key, item]) => ({ label: humanFieldLabel(key), item }))
    .filter((entry) => entry.label)
    .slice(0, 8);
  if (entries.length) return entries.map(({ label, item }) => `${label}: ${valueText(item)}`).join(" · ");
  return "--";
}

function sourceUrls(section: JsonRecord): Array<{ url: string; label: string }> {
  const values: unknown[] = [];
  ["source_url", "url"].forEach((key) => {
    if (section[key]) values.push(section[key]);
  });
  if (Array.isArray(section.source_urls)) values.push(...section.source_urls);
  const links: Array<{ url: string; label: string }> = [];
  values.forEach((value) => {
    if (value && typeof value === "object") {
      const record = asRecord(value);
      const url = String(record.url || record.source_url || "").trim();
      if (url) links.push({ url, label: String(record.title || record.name || "查看原文") });
      return;
    }
    const url = String(value || "").trim();
    if (/^https?:\/\//i.test(url)) links.push({ url, label: "查看原文" });
  });
  return [...new Map(links.map((item) => [item.url, item])).values()];
}

function SourceLinks({ section }: { section: JsonRecord }) {
  const links = [...sourceUrls(section), ...sectionRows(section).flatMap((row) => sourceUrls(row))];
  if (!links.length) return null;
  return (
    <span className="f10-source-links">
      {links.slice(0, 3).map((link) => (
        <a key={link.url} href={link.url} target="_blank" rel="noreferrer">{link.label}</a>
      ))}
    </span>
  );
}

function trendFromRow(row: JsonRecord): { tone: "positive" | "negative" | "neutral"; text: string } | null {
  const raw = pickValue(row, ["同比", "同比增长", "环比", "变动比例", "涨跌幅", "yoy", "qoq", "change_pct", "delta_pct"]);
  if (raw === undefined || raw === null || raw === "") return null;
  const numeric = toNumber(raw);
  if (numeric !== null) {
    return { tone: numeric > 0 ? "positive" : numeric < 0 ? "negative" : "neutral", text: `${numeric > 0 ? "↑" : numeric < 0 ? "↓" : "—"} ${formatPercent(numeric)}` };
  }
  const text = String(raw);
  const positive = /增持|增加|上升|增长|上涨|利好|↑|正/.test(text);
  const negative = /减持|减少|下降|下跌|利空|↓|负/.test(text);
  return { tone: positive ? "positive" : negative ? "negative" : "neutral", text: `${positive ? "↑" : negative ? "↓" : "—"} ${text}` };
}

function agentDisplayName(agent: unknown): string {
  const key = String(agent || "").toLowerCase();
  const labels: Record<string, string> = {
    fundamental: "基本面研究",
    fundamental_agent: "基本面研究",
    technical: "技术面研究",
    technical_agent: "技术面研究",
    sentiment: "情绪研究",
    valuation: "估值研究",
    risk: "风险研究",
    research: "研究智能体",
  };
  return labels[key] || (key ? "研究智能体" : "研究智能体");
}

type RatingBucket = { period: string; buy: number; add: number; neutral: number; reduce: number; sell: number; total: number };
type RatingStatisticsProjection = { buckets?: unknown; reference_date?: unknown; reference_basis?: unknown };

function ratingText(value: unknown): string {
  const text = String(value || "").trim();
  if (!text) return "未披露评级";
  if (/强烈?买入|买入|推荐/.test(text)) return "买入";
  if (/强烈?增持|增持/.test(text)) return "增持";
  if (/中性|持有/.test(text)) return "中性";
  if (/减持/.test(text)) return "减持";
  if (/卖出|回避/.test(text)) return "卖出";
  return text;
}

function ratingDate(row: JsonRecord): Date | null {
  const raw = pickValue(row, ["最新报告日期", "日期", "报告日期", "report_date"]);
  if (!raw) return null;
  const text = String(raw).trim();
  const normalized = /^\d{8}$/.test(text)
    ? `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6, 8)}`
    : text.replace(/[年/.]/g, "-").replace(/月/g, "-").replace(/日/g, "");
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}

function ratingStatistics(rows: JsonRecord[]): RatingBucket[] {
  const dates = rows.map(ratingDate).filter((value): value is Date => value !== null);
  // Cached research feeds can lag the calendar by months or years.  Anchor
  // the windows to the newest observed report instead of making stale but
  // valid ratings look like zero coverage.
  const now = dates.length ? new Date(Math.max(...dates.map((value) => value.getTime()))) : new Date();
  const ranges = [
    { period: "1个月内", days: 31 },
    { period: "2个月内", days: 62 },
    { period: "3个月内", days: 93 },
    { period: "6个月内", days: 186 },
    { period: "1年内", days: 366 },
  ];
  return ranges.map(({ period, days }) => {
    const cutoff = now.getTime() - days * 86400000;
    const counts = { buy: 0, add: 0, neutral: 0, reduce: 0, sell: 0 };
    rows.forEach((row) => {
      const date = ratingDate(row);
      if (!date || date.getTime() < cutoff || date.getTime() > now.getTime() + 86400000) return;
      const rating = ratingText(pickValue(row, ["rating", "评级", "东财评级"]));
      if (rating === "买入") counts.buy += 1;
      else if (rating === "增持") counts.add += 1;
      else if (rating === "中性") counts.neutral += 1;
      else if (rating === "减持") counts.reduce += 1;
      else if (rating === "卖出") counts.sell += 1;
    });
    return { period, ...counts, total: counts.buy + counts.add + counts.neutral + counts.reduce + counts.sell };
  });
}

function projectedRatingStatistics(section: JsonRecord, rows: JsonRecord[]): { buckets: RatingBucket[]; referenceDate: string; basis: string } {
  const rawProjection = section.rating_statistics;
  const projection = Array.isArray(rawProjection)
    ? { buckets: rawProjection }
    : asRecord(rawProjection as RatingStatisticsProjection);
  const buckets = asArray(projection.buckets).map((row) => ({
    period: String(row.period || ""),
    buy: toNumber(row.buy) ?? 0,
    add: toNumber(row.add) ?? 0,
    neutral: toNumber(row.neutral) ?? 0,
    reduce: toNumber(row.reduce) ?? 0,
    sell: toNumber(row.sell) ?? 0,
    total: toNumber(row.total) ?? 0,
  })).filter((row) => row.period);
  const observedDates = rows.map(ratingDate).filter((value): value is Date => value !== null);
  const fallbackReferenceDate = observedDates.length
    ? new Date(Math.max(...observedDates.map((value) => value.getTime()))).toISOString().slice(0, 10)
    : "";
  const referenceDate = String(projection.reference_date || section.rating_statistics_reference_date || fallbackReferenceDate);
  const basis = String(projection.reference_basis || section.rating_statistics_basis || "最新报告日期");
  return { buckets: buckets.length ? buckets : ratingStatistics(rows), referenceDate, basis };
}

function ResearchForecastPanel({ section }: { section: JsonRecord }) {
  const rows = sectionRows(section);
  const [mode, setMode] = useState<"forecast" | "rating">("forecast");
  const ratingProjection = useMemo(() => projectedRatingStatistics(section, rows), [section, rows]);
  const statistics = ratingProjection.buckets;
  return (
    <section className="research-forecast-panel">
      <div className="research-forecast-heading">
        <div><span className="financial-section-mark" /><h3>机构预测</h3></div>
        <div className="research-forecast-tabs" role="tablist" aria-label="机构预测视图">
          <button type="button" role="tab" aria-selected={mode === "forecast"} className={mode === "forecast" ? "active" : ""} onClick={() => setMode("forecast")}>机构预测</button>
          <button type="button" role="tab" aria-selected={mode === "rating"} className={mode === "rating" ? "active" : ""} onClick={() => setMode("rating")}>评级统计</button>
        </div>
        <span className="f10-data-status available" aria-live="polite">{rows.length ? "已获取" : "暂无数据"}</span>
      </div>
      {mode === "forecast" ? (
        rows.length ? <div className="research-forecast-table-wrap" role="tabpanel" aria-label="机构预测">
          <table className="research-forecast-table">
            <thead><tr><th scope="col">报告期</th><th scope="col">机构</th><th scope="col">评级</th><th scope="col">评级数量</th><th scope="col">最新报告日期</th></tr></thead>
            <tbody>{rows.slice(0, 100).map((row, index) => {
              const institution = String(pickValue(row, ["机构", "institution"]) || "未披露机构");
              const rating = ratingText(pickValue(row, ["评级", "东财评级", "rating"]));
              const reportDate = String(pickValue(row, ["最新报告日期", "日期", "report_date"]) || "");
              const reportPeriod = pickValue(row, ["报告期", "预测年度", "年度", "report_period", "forecast_year"])
                || (ratingDate(row)?.getFullYear() ?? "--");
              const ratingTone = rating === "买入" || rating === "增持" ? "positive" : rating === "减持" || rating === "卖出" ? "negative" : "neutral";
              return <tr key={`${institution}-${rating}-${index}`}>
                <td>{reportPeriodText(reportPeriod)}</td>
                <td className="research-forecast-institution">{institution}</td>
                <td><span className={`research-rating ${ratingTone}`}>{rating}</span></td>
                <td>{valueText(pickValue(row, ["评级数量", "评级数", "report_count"]))}</td>
                <td>{formatDate(reportDate)}</td>
              </tr>;
            })}</tbody>
          </table>
        </div> : <p className="empty-state" role="tabpanel">{String(section.message || "当前数据源未返回机构预测")}</p>
      ) : (
        <div className="rating-stat-table-wrap" role="tabpanel" aria-label="评级统计"><table className="rating-stat-table"><thead><tr><th scope="col">时间段</th><th scope="col">买入</th><th scope="col">增持</th><th scope="col">中性</th><th scope="col">减持</th><th scope="col">卖出</th><th scope="col">总家数</th></tr></thead><tbody>{statistics.map((row) => <tr key={row.period}><th scope="row">{row.period}</th><td className="rating-buy">{row.buy}</td><td>{row.add}</td><td>{row.neutral}</td><td>{row.reduce}</td><td>{row.sell}</td><td>{row.total}</td></tr>)}</tbody></table></div>
      )}
      <div className="f10-section-meta research-forecast-meta"><span>来源：{String(section.source || "暂无")}</span>{section.as_of ? <span>截至：{formatDate(String(section.as_of))}</span> : null}{mode === "rating" && ratingProjection.referenceDate ? <span>统计基准：{formatDate(ratingProjection.referenceDate)}（{ratingProjection.basis}）</span> : null}</div>
    </section>
  );
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
  if (/^(?:nat|nan|null|none|undefined|-{1,2})$/i.test(text.trim())) return "--";
  if (/^\d{4}-\d{2}-\d{2}/.test(text)) return text.slice(0, 10);
  if (/^\d{8}$/.test(text)) return `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6, 8)}`;
  return text;
}

function friendlyRefreshError(reason: unknown): string {
  const raw = reason instanceof Error ? reason.message : String(reason || "");
  if (/abort|cancel/i.test(raw)) return "";
  if (!raw || /failed to fetch|networkerror|load failed|timeout|timed out/i.test(raw)) {
    return "远程数据暂时不可用，已继续显示本地缓存";
  }
  // Keep useful server-side Chinese diagnostics, but never expose a raw
  // browser/transport error such as "Failed to fetch" in the user interface.
  return /[\u3400-\u9fff]/.test(raw)
    ? `远程更新未完成，已继续显示本地缓存：${raw}`
    : "远程更新未完成，已继续显示本地缓存";
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

/**
 * Resolve a provider field only when its key is an exact alias.
 *
 * F10 shareholder payloads commonly contain both `持股数量` and
 * `平均持股数`/`股东总数`.  The generic fuzzy resolver is useful for loosely
 * shaped provider records, but it can silently substitute one of those
 * similarly named fields when the requested value is null.  Tables that
 * compare reporting periods must treat a null source value as unknown.
 */
function findExactValue(record: JsonRecord, aliases: string[]): unknown {
  const direct = pickValue(record, aliases);
  if (direct !== undefined) return direct;
  const normalizedAliases = aliases.map((key) => key.toLowerCase().replace(/[\s_()（）-]/g, ""));
  for (const [key, value] of Object.entries(record)) {
    const normalized = key.toLowerCase().replace(/[\s_()（）-]/g, "");
    if (normalizedAliases.includes(normalized) && value !== undefined && value !== null && value !== "") return value;
  }
  return undefined;
}

function findNumber(record: JsonRecord, aliases: string[]): number | null {
  return toNumber(findValue(record, aliases));
}

function displayByLabel(label: string, value: unknown): string {
  const numeric = toNumber(value);
  if (numeric === null) return valueText(value);
  const text = String(label || "").replace(/[\s（）()]/g, "");
  const hasYiUnit = /亿(?:元|股)?/.test(text);
  const hasWanUnit = /万(?:元|股)?/.test(text);

  // These are ratios or per-share observations even when their names also
  // contain “资产/收益/权益”; evaluate them before monetary fields.
  if (/(市盈率|市净率|\bPE\b|\bPB\b)/i.test(text)) {
    return formatNumber(numeric);
  }
  if (/每股|每份|EPS|每股收益|每股净资产|每股公积金|每股未分配|每股股息/i.test(text)) {
    return formatNumber(numeric, 2);
  }
  if (/权益乘数/.test(text)) {
    return formatNumber(numeric, 2);
  }
  if (/(率|幅|占比|占流通|占总股本|持股比|股本比|比例|百分比|同比|环比|ROE|ROA|毛利率|净利率|收益率|增长率|pe|pb|市盈|市净)/i.test(text)) {
    return formatPercent(numeric);
  }
  if (/融资融券余额|融资融券总余额|两融余额/.test(text)) {
    return hasYiUnit ? `${formatNumber(numeric, 2)}亿` : hasWanUnit ? `${formatNumber(numeric / 10000, 2)}亿` : `${formatNumber(numeric / 100000000, 2)}亿`;
  }
  if (/融资余额/.test(text)) {
    return hasYiUnit ? `${formatNumber(numeric, 2)}亿` : hasWanUnit ? `${formatNumber(numeric / 10000, 2)}亿` : `${formatNumber(numeric / 100000000, 2)}亿`;
  }
  if (/融券余额|融券市值/.test(text)) {
    return hasWanUnit ? `${formatNumber(numeric, 2)}万元` : hasYiUnit ? `${formatNumber(numeric * 10000, 2)}万元` : `${formatNumber(numeric / 10000, 2)}万元`;
  }
  if (/(持股数量|持股数|持仓数量|股份数量|股数|流通A股|有效流通A股|限售A股|流通股本|限售股本|解禁数量|解禁股数|上市流通数量|发行数量|发行股数|配售数量)/.test(text)) {
    return hasYiUnit ? `${formatNumber(numeric, 2)}亿` : hasWanUnit ? `${formatNumber(numeric / 10000, 2)}亿` : `${formatNumber(numeric / 100000000, 2)}亿`;
  }
  if (/(市值|金额|成交额|资产|负债|权益|收入|利润|现金|资本|持股市值|净流入|净额|发行量|股本)/.test(text) && !/权益乘数/.test(text)) {
    return hasYiUnit ? `${formatNumber(numeric, 2)}亿` : hasWanUnit ? `${formatNumber(numeric / 10000, 2)}亿` : `${formatNumber(numeric / 100000000, 2)}亿`;
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
      return { label, value: getRowValue(record, periods), source: record };
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

/** 将公司主数据中的行业、主题板块、指数和类型分开显示。
 * 旧缓存只有“所属行业/入选指数”字段，新缓存可能提供 sectors/themes/tags；
 * 这里做兼容读取，但不把指数误标为概念板块。
 */
 type SectorGroupValue = { text: string; definition?: string; criteria?: string; source?: string; status?: string; code?: string; related_stocks?: JsonRecord[]; member_count?: number | null; trend_pct?: number | null; dimension?: string };

function sectorGroups(profile: JsonRecord, fields: JsonRecord): Array<{ label: string; values: SectorGroupValue[] }> {
  const result: Array<{ label: string; values: SectorGroupValue[] }> = [];
  const toValues = (value: unknown): SectorGroupValue[] => {
    if (value === undefined || value === null || value === "") return [];
    if (Array.isArray(value)) return value.flatMap((item) => toValues(item));
    if (value && typeof value === "object") {
      const row = asRecord(value);
      const text = pickValue(row, ["name", "label", "title", "名称", "板块", "主题"]);
      if (text === undefined || text === null || text === "") return [];
      return String(text).split(/[，,;；|]/).map((part) => part.trim()).filter(Boolean).map((textPart) => ({
        text: textPart,
        definition: String(pickValue(row, ["definition", "释义", "定义"]) || "") || undefined,
        criteria: String(pickValue(row, ["criteria", "判定口径", "标准"]) || "") || undefined,
        source: String(pickValue(row, ["source", "source_name", "来源", "来源名称"]) || "") || undefined,
        status: String(pickValue(row, ["status", "状态"]) || "") || undefined,
        code: String(pickValue(row, ["code", "分类编码"]) || "") || undefined,
        related_stocks: asArray(row.related_stocks),
        member_count: toNumber(row.member_count),
        trend_pct: toNumber(row.trend_pct),
        dimension: String(pickValue(row, ["dimension", "classification_dimension"]) || "") || undefined,
      }));
    }
    return String(value).split(/[，,;；|]/).map((item) => item.trim()).filter(Boolean).map((text) => ({ text }));
  };
  const add = (label: string, value: unknown) => {
    const values = [...new Map(toValues(value).map((item) => [item.text, item])).values()].slice(0, 12);
    if (values.length) result.push({ label, values });
  };
  const typedGroups = asArray(profile.classification_groups);
  if (typedGroups.length) {
    typedGroups.forEach((group) => {
      const title = String(pickValue(group, ["title", "label", "名称"]) || "分类");
      const values = asArray(group.items).flatMap((item) => toValues(item));
      const unique = [...new Map(values.map((item) => [item.text, item])).values()].slice(0, 12);
      if (unique.length) result.push({ label: title, values: unique });
    });
    if (result.length) return result;
  }
  add("行业", pickValue(profile, ["industry", "industry_name", "行业", "所属行业"]) ?? pickValue(fields, ["三级行业", "所属行业", "行业"]));
  add("板块/主题", pickValue(profile, ["sectors", "sector", "themes", "theme", "板块", "主题", "所属板块"]));
  add("指数", pickValue(profile, ["indices", "index_memberships", "index", "入选指数"] ) ?? pickValue(fields, ["入选指数", "指数"]));
  add("类型", pickValue(profile, ["classifications", "classification_tags", "tags", "类型", "股票类型"]));
  // Some providers put the actual theme list under fields as a JSON-like array.
  if (!result.some((group) => group.label === "板块/主题")) add("板块/主题", pickValue(fields, ["板块", "主题", "所属板块", "所属概念", "概念"]));
  return result;
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
  return findExactValue(row, aliases);
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
          <span>5日均线：{formatNumber(ma(closes, 5))}</span>
          <span>20日均线：{formatNumber(ma(closes, 20))}</span>
          <span>60日均线：{formatNumber(ma(closes, 60))}</span>
          <span>成交量：{formatNumber(volumes[volumes.length - 1], 0)}</span>
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
            <strong>
              {displayByLabel(row.label, row.value)}
              {(() => {
                const trend = trendFromRow(asRecord(row.source));
                return trend ? <em className={`metric-trend ${trend.tone}`}>{trend.text}</em> : null;
              })()}
            </strong>
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
  profile,
  profileFields,
  klines,
  onClassificationSelect,
}: {
  report: string;
  running: boolean;
  stage: string;
  agentSnapshots: Array<ResearchFundamentalAgent | ResearchTechnicalAgent>;
  quote: StockF10["realtime_quote"];
  summary: JsonRecord;
  profile: JsonRecord;
  profileFields: JsonRecord;
  klines: StockKline[];
  onClassificationSelect?: (group: { label: string; values: SectorGroupValue[] }, item: SectorGroupValue) => void;
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
      <div className="stock-sector-strip">
        <span>股票分类</span>
        <div className="stock-sector-groups">
          {sectorGroups(profile, profileFields).length ? sectorGroups(profile, profileFields).map((group) => (
            <span className="stock-sector-group" key={group.label}>
              <b>{group.label}</b>{group.values.map((item) => onClassificationSelect ? <button type="button" key={`${group.label}-${item.text}`} title={[item.definition, item.criteria, item.source].filter(Boolean).join("\n")} onClick={() => onClassificationSelect(group, item)}>{item.text}</button> : <em key={`${group.label}-${item.text}`} title={[item.definition, item.criteria, item.source].filter(Boolean).join("\n")}>{item.text}</em>)}
            </span>
          )) : <small>暂无行业、板块或类型数据</small>}
        </div>
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
          const noticeCategory = String(record.category || "");
          return (
            <article className="detail-news-row" key={`${String(date)}-${index}`}>
              <div>
                <strong>{noticeCategory ? <em className="notice-category-badge">{noticeCategory}</em> : null}{record.is_latest ? <em className="notice-latest-badge">最新</em> : null}{String(headline)}</strong>
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
    { key: "change", label: "变动", aliases: ["变动", "持股变动", "增减", "change", "变化"] },
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
                  <td>{(() => { const value = shareholderField(row, columns[6].aliases); const number = toNumber(value); const text = String(value || ""); const positive = number !== null ? number > 0 : /增持|新进|增加|上升/.test(text); const negative = number !== null ? number < 0 : /减持|退出|减少|下降/.test(text); return <span className={`holding-change ${positive ? "positive" : negative ? "negative" : "neutral"}`}>{positive ? "↑" : negative ? "↓" : "—"} {valueText(value)}</span>; })()}</td>
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
  const [flowMode, setFlowMode] = useState<"main" | "institution" | "hot">("main");
  const [fundSection, setFundSection] = useState<"flow" | "dragon" | "block" | "analysis">("flow");
  const isHongKongMode = String(data.mode || data.source || "").includes("港股") || findValue(latest, ["持股市值"]) !== undefined;
  const netKeyAliases = isHongKongMode
    ? ["持股市值变化-1日", "持股市值变动-1日", "市值变化-1日"]
    : ["主力净流入-净额", "主力净流入", "主力净额", "主力资金净流入"];
  const mainValue = isHongKongMode ? findNumber(latest, ["持股市值", "持股市值(元)"]) : findNumber(latest, netKeyAliases);
  const chartRows = rows.slice(0, 5).reverse();
  const modeAliases: Record<string, string[]> = {
    main: netKeyAliases,
    institution: ["超大单净流入-净额", "超大单净流入", "大单净流入-净额", "大单净流入"],
    hot: ["中单净流入-净额", "中单净流入", "小单净流入-净额", "小单净流入"],
  };
  const selectedAliases = modeAliases[flowMode] || netKeyAliases;
  const chartValues = chartRows.map((row) => {
    if (flowMode === "institution") return (findNumber(row, ["超大单净流入-净额", "超大单净流入"]) || 0) + (findNumber(row, ["大单净流入-净额", "大单净流入"]) || 0);
    if (flowMode === "hot") return (findNumber(row, ["中单净流入-净额", "中单净流入"]) || 0) + (findNumber(row, ["小单净流入-净额", "小单净流入"]) || 0);
    return findNumber(row, selectedAliases) ?? 0;
  });
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
        ["主力流入", findValue(latest, ["主力净流入-净额", "主力净流入"])],
        ["主力流出", (() => { const value = findNumber(latest, ["主力净流入-净额", "主力净流入"]); return value !== null && value < 0 ? Math.abs(value) : 0; })()],
        ["净流入", findValue(latest, ["主力净流入-净额", "主力净流入"])],
      ];
  const orderSegments = [
    ["特大买单", findNumber(latest, ["超大单净流入-净额", "超大单净流入"]) || 0],
    ["大买单", findNumber(latest, ["大单净流入-净额", "大单净流入"]) || 0],
    ["中买单", findNumber(latest, ["中单净流入-净额", "中单净流入"]) || 0],
    ["小买单", findNumber(latest, ["小单净流入-净额", "小单净流入"]) || 0],
    ["小卖单", -(findNumber(latest, ["小单净流入-净额", "小单净流入"]) || 0)],
    ["中卖单", -(findNumber(latest, ["中单净流入-净额", "中单净流入"]) || 0)],
    ["大卖单", -(findNumber(latest, ["大单净流入-净额", "大单净流入"]) || 0)],
    ["特大卖单", -(findNumber(latest, ["超大单净流入-净额", "超大单净流入"]) || 0)],
  ] as Array<[string, number]>;
  const orderTotal = orderSegments.reduce((sum, [, value]) => sum + Math.abs(value), 0) || 1;
  let orderCursor = 0;
  const orderStops = orderSegments.map(([label, value], index) => {
    const portion = Math.abs(value) / orderTotal * 100;
    const start = orderCursor;
    orderCursor += portion;
    const color = index < 4 ? ["#c53c45", "#df6c58", "#e29850", "#efbd75"][index] : ["#83b99b", "#5ca27f", "#3e8969", "#2b7058"][index - 4];
    return { label, value, color, stop: `${color} ${start}% ${orderCursor}%` };
  });
  const donutStyle = { background: `conic-gradient(${orderStops.map((item) => item.stop).join(", ")})` };

  const formatFundAmount = (value: unknown) => {
    const number = toNumber(value);
    return number === null ? "--" : `${formatNumber(number / 10000, 2)}万元`;
  };
  const fundSectionMeta: Record<typeof fundSection, { label: string; empty: string; matcher: RegExp }> = {
    flow: { label: "资金流向", empty: "暂无资金流向数据", matcher: /./ },
    dragon: { label: "龙虎榜", empty: "当前数据源未接入该股票龙虎榜明细", matcher: /龙虎榜|上榜|营业部|买入金额|卖出金额/ },
    block: { label: "大宗交易", empty: "当前数据源未接入该股票大宗交易明细", matcher: /大宗|成交价|成交量|折溢价|买方营业部|卖方营业部/ },
    analysis: { label: "解盘", empty: "当前数据源未返回解盘分析", matcher: /解盘|研判|分析|观点/ },
  };
  const auxiliaryRows = fundSection === "flow" ? [] : rows.filter((row) => fundSectionMeta[fundSection].matcher.test(Object.keys(row).join(" ")));

  return (
    <section className="f10-section inner-section">
      <nav className="fund-flow-nav" aria-label="资金数据分类">
        {([["flow", "资金流向"], ["dragon", "龙虎榜"], ["block", "大宗交易"], ["analysis", "解盘"]] as const).map(([key, item]) => <button type="button" className={fundSection === key ? "active" : ""} aria-pressed={fundSection === key} key={key} onClick={() => setFundSection(key)}>{item}</button>)}
      </nav>
      <div className="f10-section-head">
        <div>
          <h3>{fundSection === "flow" && isHongKongMode ? "港股通持股 / 资金参考" : fundSectionMeta[fundSection].label}</h3>
          <span>{String(data.source || "本地资金数据")}</span>
        </div>
      </div>
      {fundSection === "flow" && rows.length ? (
        <>
          <div className="fund-flow-overview">
            <article className="fund-flow-hero">
              <div className="fund-flow-hero-head">
                <span className="fund-flow-eyebrow">资金流向</span>
                <strong>{isHongKongMode ? "港股通持股" : "主力资金"}</strong>
              </div>
              <div className={`fund-flow-hero-value ${heroTone}`}>
                <span>{isHongKongMode ? "最新持股市值" : "最新主力净流入"}</span>
                <strong>{isHongKongMode ? formatMoney(mainValue) : formatFundAmount(mainValue)}</strong>
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
          {!isHongKongMode ? <div className="fund-flow-order-panel">
            <div className="fund-flow-order-head"><div><h4>资金流向</h4><span>单位：万元 · 净额按买卖方向展示</span></div><div className="fund-flow-donut" style={donutStyle} aria-label="买卖单资金结构"><i /></div></div>
            <div className="fund-flow-order-legend">{orderStops.map((item) => <span key={item.label}><i style={{ background: item.color }} /><b>{item.label}</b><em>{formatFundAmount(Math.abs(item.value))}</em></span>)}</div>
          </div> : null}
          <div className="fund-flow-stat-grid">
            {cards.map(([label, value]) => (
              <article className={`fund-flow-stat-card ${flowTone(value)}`} key={String(label)}>
                <span>{String(label)}</span>
                <strong>{isHongKongMode ? displayByLabel(String(label), value) : String(label).includes("占比") ? formatPercent(toNumber(value)) : formatFundAmount(value)}</strong>
              </article>
            ))}
          </div>
          {!isHongKongMode ? <div className="fund-flow-period-tabs" role="tablist" aria-label="资金五日趋势">
            {([["main", "五日主力增减"], ["institution", "五日机构增减"], ["hot", "五日游资增减"]] as const).map(([key, label]) => <button type="button" role="tab" aria-selected={flowMode === key} className={flowMode === key ? "active" : ""} key={key} onClick={() => setFlowMode(key)}>{label}</button>)}
          </div> : null}
          <div className="fund-flow-table-wrap">
            <table className="mini-table fund-flow-table">
              <thead>
                <tr>
                  {Object.keys(latest).slice(0, 8).map((key, index) => <th key={key}>{humanFieldLabel(key) || `字段${index + 1}`}</th>)}
                </tr>
              </thead>
              <tbody>
                {rows.slice(0, 10).map((row, index) => (
                  <tr key={index}>
                    {Object.keys(latest).slice(0, 8).map((key) => <td key={key}>{!isHongKongMode && /(净额|金额)/.test(key) ? formatFundAmount(row[key]) : displayByLabel(key, row[key])}</td>)}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : fundSection !== "flow" ? (
        auxiliaryRows.length ? <div className="fund-flow-auxiliary-panel">
          <p className="fund-flow-auxiliary-note">{fundSectionMeta[fundSection].label}数据</p>
          <div className="fund-flow-table-wrap"><table className="mini-table fund-flow-table"><thead><tr>{Object.keys(auxiliaryRows[0]).slice(0, 8).map((key, index) => <th key={key}>{humanFieldLabel(key) || `字段${index + 1}`}</th>)}</tr></thead><tbody>{auxiliaryRows.slice(0, 30).map((row, index) => <tr key={index}>{Object.keys(auxiliaryRows[0]).slice(0, 8).map((key) => <td key={key}>{displayByLabel(key, row[key])}</td>)}</tr>)}</tbody></table></div>
        </div> : <p className="empty-state">{fundSectionMeta[fundSection].empty}</p>
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

function sectionRows(section: JsonRecord): JsonRecord[] {
  const rows = section.rows;
  if (Array.isArray(rows)) return rows.map((row) => asRecord(row));
  if (rows && typeof rows === "object") {
    return Object.entries(asRecord(rows)).map(([label, value]) => ({ label, value }));
  }
  return [];
}

function F10SectionBlock({
  section,
  onOpen,
  holderRows = false,
  footer,
}: {
  section: JsonRecord;
  onOpen?: (section: JsonRecord) => void;
  holderRows?: boolean;
  footer?: ReactNode;
}) {
  const [expanded, setExpanded] = useState(false);
  const rows = sectionRows(section);
  const rowPending = rows.some((row) => ["PENDING", "待核验", "待审核"].includes(String(pickValue(row, ["status", "事实状态", "verification_status"]) || "").toUpperCase()));
  const status = rowPending ? "PENDING" : String(section.status || (rows.length ? "AVAILABLE" : "UNAVAILABLE"));
  const statusLabel = status === "AVAILABLE" ? "已获取" : status === "PARTIAL" ? "部分获取" : status === "PENDING" ? (/未接入|需授权/.test(String(section.message || "")) ? "未接入" : "待核验") : "暂无数据";
  const actionLabel = String(
    section.action_label || (section.key === "anomaly" ? "融资融券近一个月" : "查看详细数据"),
  );
  return (
    <section className={`financial-block f10-section-card ${status.toLowerCase()}`}>
      <div className="financial-block-heading">
        <div><span className="financial-section-mark" /><h3>{String(section.title || "资料分区")}</h3></div>
        <span className={`f10-data-status ${status.toLowerCase()}`}>{statusLabel}</span>
      </div>
      {rows.length ? (
        holderRows ? <ShareholderTable rows={rows} title={String(section.title || "股东数据")} /> : (
          <div className="f10-section-row-list">
            {rows.slice(0, expanded ? 100 : 8).map((row, index) => {
              const label = String(pickValue(row, ["label", "name", "项目", "指标", "metric", "股东名称"]) || `项目 ${index + 1}`);
              const trend = trendFromRow(row);
              return <div className="financial-value-row" key={`${label}-${index}`}><span>{label}</span><strong>{rowValueText(row)}{trend ? <em className={`metric-trend ${trend.tone}`}>{trend.text}</em> : null}</strong></div>;
            })}
          </div>
        )
      ) : <p className="empty-state compact-empty">{String(section.message || "当前数据源未返回该分区数据")}</p>}
      {footer}
      <div className="f10-section-meta"><span>来源：{String(section.source || "暂无")}</span><SourceLinks section={section} />{section.as_of ? <span>截至：{formatDate(String(section.as_of))}</span> : null}</div>
      {(onOpen || rows.length > 8) ? <button type="button" className="f10-detail-link" onClick={() => onOpen ? onOpen({ ...section, detail_title: section.detail_title || actionLabel }) : setExpanded((value) => !value)}>{onOpen ? actionLabel : (expanded ? "收起详细数据" : "查看详细数据")}</button> : null}
    </section>
  );
}

const f10RowLabelKeys = ["label", "name", "metric", "indicator", "title", "项目", "名称", "指标"];
const f10RowValueKeys = ["value", "值", "data", "amount", "content", "summary", "数值", "金额", "数据"];

function f10Period(row: JsonRecord): string {
  const raw = String(pickValue(row, ["报告期", "截止日期", "截至日期", "report_period", "report_date", "date", "日期"]) || "");
  if (/^(?:nat|nan|null|none|undefined|-{1,2})$/i.test(raw.trim())) return "";
  const match = raw.match(/(20\d{2})[-/]?(\d{2})[-/]?(\d{2})/);
  if (!match) return raw;
  const [, year, month, day] = match;
  if (month === "06" && day === "30") return `${year}中报`;
  if (month === "03" && day === "31") return `${year}一季报`;
  if (month === "09" && day === "30") return `${year}三季报`;
  if (month === "12" && day === "31") return `${year}年报`;
  return `${year}-${month}-${day}`;
}

function F10SectionHeading({ section, onOpen, pin = false }: { section: JsonRecord; onOpen?: (section: JsonRecord) => void; pin?: boolean }) {
  const rows = sectionRows(section);
  return (
    <header className="f10-mobile-heading">
      <div className="f10-mobile-heading-title"><span aria-hidden="true" /><h3>{String(section.title || "资料分区")}</h3></div>
      <div className="f10-mobile-heading-actions">
        {pin ? <span className="f10-pin-label">置顶</span> : null}
        {onOpen && rows.length ? <button type="button" onClick={() => onOpen(section)}>更多 <span aria-hidden="true">›</span></button> : null}
      </div>
    </header>
  );
}

function latestPeriodValue(value: unknown): unknown {
  if (!value || typeof value !== "object" || Array.isArray(value)) return value;
  const record = asRecord(value);
  const entries = Object.entries(record).filter(([, item]) => item !== undefined && item !== null && item !== "");
  const periodEntries = entries.filter(([key]) => /^(?:20\d{2}(?:$|[-/]\d{2}(?:[-/]\d{2})?|Q[1-4]|年|中报|一季报|三季报|年报))/.test(key.trim()));
  if (!periodEntries.length) return value;
  periodEntries.sort(([left], [right]) => right.localeCompare(left));
  return periodEntries[0][1];
}

function F10FactRows({ rows, onOpenRow, emptyText }: { rows: JsonRecord[]; onOpenRow?: (row: JsonRecord) => void; emptyText?: string }) {
  const facts = rows.flatMap((row) => {
    const label = pickValue(row, f10RowLabelKeys);
    const directValue = pickValue(row, f10RowValueKeys);
    if (label !== undefined || directValue !== undefined) {
      return [{ label: String(label || "资料"), value: latestPeriodValue(directValue ?? row), row }];
    }
    return Object.entries(row)
      .filter(([key, value]) => !["id", "source", "source_name", "source_id", "url", "source_url", "status", "verification_status", "report_period", "date"].includes(key) && value !== null && value !== undefined && value !== "")
      .map(([key, value]) => ({ label: humanFieldLabel(key) || key, value: latestPeriodValue(value), row }));
  });
  return facts.length ? (
    <div className="f10-mobile-facts">
      {facts.map((fact, index) => (
        <button type="button" className={`f10-mobile-fact ${onOpenRow ? "is-clickable" : ""}`} key={`${fact.label}-${index}`} onClick={onOpenRow ? () => onOpenRow(fact.row) : undefined}>
          <span>{fact.label}</span><strong>{displayByLabel(fact.label, fact.value)}</strong>
        </button>
      ))}
    </div>
  ) : <p className="f10-mobile-empty">{emptyText || "暂无该分区数据"}</p>;
}

function F10SectionSource({ section }: { section: JsonRecord }) {
  const source = String(section.source || "");
  const asOf = section.as_of ? formatDate(String(section.as_of)) : "";
  return source || asOf || sourceUrls(section).length ? <footer className="f10-mobile-source">{source ? <span>来源：{source}</span> : null}{asOf ? <span>截至：{asOf}</span> : null}<SourceLinks section={section} /></footer> : null;
}

function ResearchIndustryConcepts({ section, onOpen }: { section: JsonRecord; onOpen: (section: JsonRecord) => void }) {
  const rows = sectionRows(section);
  if (!rows.length) {
    return <F10SectionBlock section={section} onOpen={onOpen} />;
  }
  return <section className="research-industry-panel" aria-labelledby="research-industry-title">
    <header className="research-industry-heading">
      <div><span className="financial-section-mark" aria-hidden="true" /><h3 id="research-industry-title">行业概念</h3></div>
      <span>{rows.length} 个分类</span>
    </header>
    <div className="research-industry-cards">
      {rows.slice(0, 24).map((row, index) => {
        const name = String(pickValue(row, ["name", "label", "概念名称", "概念", "行业", "板块"]) || `分类${index + 1}`);
        const dimension = String(pickValue(row, ["dimension", "classification_dimension"]) || "").toUpperCase();
        const dimensionLabel = dimension === "INDUSTRY" ? "行业" : dimension === "THEME" ? "主题板块" : dimension === "BOARD" ? "上市板块" : "来源标签";
        const definition = pickValue(row, ["definition", "释义", "概念解析", "概念说明", "解释", "description"]);
        const criteria = pickValue(row, ["criteria", "判定口径", "标准", "master_criteria"]);
        const version = pickValue(row, ["definition_version", "master_definition_version"]);
        const source = pickValue(row, ["source_name", "source", "来源名称", "来源"]);
        const unresolved = String(pickValue(row, ["definition_status"]) || "") === "UNRESOLVED";
        const members = asArray(row.related_stocks);
        const memberCount = toNumber(row.member_count);
        const trend = toNumber(row.trend_pct);
        return <button type="button" className="research-industry-card" key={`${name}-${index}`} onClick={() => onOpen({ ...section, rows: [row], detail_title: `${name} · 概念释义` })}>
          <span className="research-industry-card-head"><strong>{name}</strong><em>{dimensionLabel}</em></span>
          <span className={`research-industry-definition${unresolved ? " unresolved" : ""}`}>{String(definition || "暂无对应的版本化主数据释义")}</span>
          {criteria ? <span className="research-industry-criteria">口径：{String(criteria)}</span> : null}
          <span className="research-industry-members">
            <span>{memberCount !== null ? `${formatNumber(memberCount, 0)} 只相关股票` : "相关股票待补充"}</span>
            {trend !== null ? <strong className={trend > 0 ? "positive" : trend < 0 ? "negative" : "neutral"}>{trend > 0 ? "↑" : trend < 0 ? "↓" : "—"} {formatPercent(Math.abs(trend))}</strong> : null}
          </span>
          {members.length ? <span className="research-industry-member-list">{members.slice(0, 5).map((member, memberIndex) => <span key={`${String(member.symbol || member.name)}-${memberIndex}`}>{String(member.name || member.symbol || "相关股票")}{member.change_pct !== null && member.change_pct !== undefined ? ` ${formatPercent(toNumber(member.change_pct))}` : ""}</span>)}</span> : null}
          <span className="research-industry-meta">{source ? `来源：${String(source)}` : "来源待补充"}{version ? ` · ${String(version)}` : ""}</span>
        </button>;
      })}
    </div>
    {rows.length > 24 ? <p className="research-industry-more">已展示前 24 个分类，点击“更多”查看完整列表。</p> : null}
    <F10SectionSource section={section} />
  </section>;
}

function ClassificationDetailDialog({ group, item, onClose }: { group: string; item: SectorGroupValue; onClose: () => void }) {
  const members = item.related_stocks || [];
  const trend = item.trend_pct ?? null;
  const values = members.map((member) => toNumber(member.change_pct)).filter((value): value is number => value !== null).slice(0, 20);
  const max = Math.max(...values.map((value) => Math.abs(value)), 1);
  return <div className="classification-detail-overlay" role="dialog" aria-modal="true" onClick={onClose}>
    <section className="classification-detail-dialog" onClick={(event) => event.stopPropagation()}>
      <header><div><span>{group}</span><h3>{item.text}</h3></div><button type="button" onClick={onClose}>关闭</button></header>
      <div className="classification-detail-body">
        <p className="classification-detail-definition">{item.definition || "该分类来自证券主数据，具体口径请结合来源证据核验。"}</p>
        {item.criteria ? <p className="classification-detail-criteria">判定口径：{item.criteria}</p> : null}
        <div className="classification-detail-summary"><span>相关股票 <strong>{item.member_count ?? members.length}</strong> 只</span><span>整体走势 <strong className={trend !== null && trend < 0 ? "negative" : "positive"}>{trend === null ? "待补充" : `${trend >= 0 ? "↑" : "↓"} ${formatPercent(Math.abs(trend))}`}</strong></span></div>
        {values.length ? <div className="classification-detail-chart" aria-label="分类股票涨跌走势">{values.map((value, index) => <i key={index} className={value >= 0 ? "positive" : "negative"} style={{ height: `${Math.max(8, Math.abs(value) / max * 100)}%` }} title={`${formatPercent(value)}`} />)}</div> : <p className="classification-detail-empty">暂无成员股票的最新涨跌数据。</p>}
        <div className="classification-detail-stocks">{members.length ? members.map((member, index) => {
          const change = toNumber(member.change_pct);
          const market = String(member.market || "CN_A");
          const symbol = String(member.symbol || "");
          const href = member.source_url || (market === "HK" ? `https://quote.eastmoney.com/hk/${symbol}.html` : `https://quote.eastmoney.com/${symbol}.html`);
          return <a href={String(href)} target="_blank" rel="noreferrer" key={`${market}-${symbol}-${index}`}><span>{String(member.name || symbol)}</span><small>{symbol}</small>{change !== null ? <em className={change >= 0 ? "positive" : "negative"}>{change >= 0 ? "↑" : "↓"}{formatPercent(Math.abs(change))}</em> : null}</a>;
        }) : <p className="classification-detail-empty">该分类暂未返回成分股清单。</p>}</div>
      </div>
    </section>
  </div>;
}

type CompactColumn = { key: string; label: string; aliases: string[]; format?: "date" | "number" | "ratio"; exact?: boolean };

function shareholderName(row: JsonRecord): string {
  return String(findExactValue(row, ["股东名称", "股东", "名称", "name", "股东全称"]) || "").trim();
}

function controlRelationText(value: unknown): string {
  const text = String(value ?? "").trim();
  if (!text) return "";
  if (/[\u3400-\u9fff]/.test(text)) return text;
  const labels: Record<string, string> = {
    CONTROLLING_HOLDER: "控股股东",
    ULTIMATE_CONTROLLER: "实际控制人",
    CONTROL_HOLDER: "控股股东",
    ACTUAL_CONTROLLER: "实际控制人",
    CONTROLLER: "控制人",
  };
  return labels[text.toUpperCase()] || "";
}

function holdingChangeText(value: unknown): string {
  const text = String(value ?? "").trim();
  if (!text || text === "--") return "--";
  if (/新进|新建|首次/.test(text)) return "新进";
  if (/不变|持平|无变化/.test(text)) return "不变";
  const number = toNumber(value);
  if (number !== null) return number > 0 ? "↑ 增持" : number < 0 ? "↓ 减持" : "不变";
  if (/增持|增加|上升|买入/.test(text)) return "↑ 增持";
  if (/减持|减少|下降|卖出/.test(text)) return "↓ 减持";
  return text;
}

function shareholderChangeRows(rows: JsonRecord[], selectedPeriod: string, periods: string[]): JsonRecord[] {
  if (!selectedPeriod || periods.length < 2) {
    return rows.map((row) => ({ ...row, holding_change: holdingChangeText(findExactValue(row, ["变动", "持股变动", "增减", "change", "变化"])) }));
  }
  const index = periods.indexOf(selectedPeriod);
  const previousPeriod = index >= 0 ? periods[index + 1] : undefined;
  const previousRows = previousPeriod ? rows.filter((row) => f10Period(row) === previousPeriod) : [];
  const previousByName = new Map(previousRows.map((row) => [shareholderName(row), row]));
  return rows.filter((row) => f10Period(row) === selectedPeriod).map((row) => {
    const existing = findExactValue(row, ["变动", "持股变动", "增减", "change", "变化"]);
    const previous = previousByName.get(shareholderName(row));
    // A missing comparison period is unknown, not evidence of a new holder.
    // Only label “新进” when a real prior-period table exists and this name
    // is absent from that table.
    if (!previousPeriod || !shareholderName(row)) {
      return { ...row, holding_change: existing ? holdingChangeText(existing) : "--" };
    }
    if (!previous) return { ...row, holding_change: existing ? holdingChangeText(existing) : "新进" };
    const currentShares = toNumber(findExactValue(row, ["持股数量", "持股数", "股份数", "数量", "shares"]));
    const previousShares = toNumber(findExactValue(previous, ["持股数量", "持股数", "股份数", "数量", "shares"]));
    if (currentShares !== null && previousShares !== null) {
      const delta = currentShares - previousShares;
      return { ...row, holding_change: Math.abs(delta) < 1e-9 ? "不变" : delta > 0 ? "↑ 增持" : "↓ 减持" };
    }
    return { ...row, holding_change: existing ? holdingChangeText(existing) : "--" };
  });
}

function F10CompactTable({ rows, columns, title, emptyText }: { rows: JsonRecord[]; columns: CompactColumn[]; title: string; emptyText?: string }) {
  return rows.length ? (
    <div className="f10-mobile-table-wrap">
      <table className="f10-mobile-table" aria-label={title}>
        <thead><tr>{columns.map((column) => <th key={column.key}>{column.label}</th>)}</tr></thead>
        <tbody>{rows.map((row, index) => <tr key={`${title}-${index}`}>
          {columns.map((column) => {
            const value = column.exact ? findExactValue(row, column.aliases) : findValue(row, column.aliases);
            const rendered = column.format === "date" ? formatDate(String(value || "")) : ["year", "plan", "name", "relation", "subject"].includes(column.key) ? valueText(value) : displayByLabel(column.label, value);
            const change = column.key === "change" ? holdingChangeText(value) : rendered;
            const tone = column.key === "change" ? (change.startsWith("↑") || change === "新进" ? "positive" : change.startsWith("↓") ? "negative" : "neutral") : "";
            return <td key={column.key}>{column.key === "change" ? <span className={`holding-change ${tone}`}>{change}</span> : rendered}</td>;
          })}
        </tr>)}</tbody>
      </table>
    </div>
  ) : <p className="f10-mobile-empty">{emptyText || `暂无${title}数据`}</p>;
}

function F10ReportPeriodTable({ section, columns, onOpen, compareShareholders = false }: { section: JsonRecord; columns: CompactColumn[]; onOpen?: (section: JsonRecord) => void; compareShareholders?: boolean }) {
  const rows = sectionRows(section);
  const periods = [...new Set(rows.map(f10Period).filter(Boolean))];
  const [selectedPeriod, setSelectedPeriod] = useState(periods[0] || "");
  const selected = periods.includes(selectedPeriod) ? selectedPeriod : periods[0];
  const filtered = selected ? rows.filter((row) => f10Period(row) === selected) : rows;
  const displayRows = compareShareholders ? shareholderChangeRows(rows, selected, periods) : filtered;
  return (
    <>
      <div className="f10-mobile-heading-row"><F10SectionHeading section={section} onOpen={onOpen} pin /><span className="f10-period-asof">{selected || ""}</span></div>
      {periods.length > 1 ? <div className="f10-period-tabs" role="tablist" aria-label={`${String(section.title)}报告期`}>
        {periods.slice(0, 6).map((period) => <button type="button" role="tab" aria-selected={period === selected} className={period === selected ? "active" : ""} key={period} onClick={() => setSelectedPeriod(period)}>{period}</button>)}
      </div> : null}
      <F10CompactTable rows={displayRows} columns={columns} title={String(section.title || "股东数据")} />
      <F10SectionSource section={section} />
    </>
  );
}

function F10ShareholderSection({ section, onOpen, currentShareFields = {} }: { section: JsonRecord; onOpen: (section: JsonRecord) => void; currentShareFields?: JsonRecord }) {
  const key = String(section.key || "");
  const rows = sectionRows(section);
  if (["top_ten", "top_ten_circulating"].includes(key)) {
    const ratioLabel = key === "top_ten_circulating" ? "占流通比" : "占总股本比";
    return <section className={`f10-mobile-section f10-holder-${key}`}>
      <F10ReportPeriodTable section={section} onOpen={onOpen} compareShareholders columns={[
        { key: "name", label: "股东名称", aliases: ["股东名称", "股东", "名称", "name", "股东全称"], exact: true },
        { key: "ratio", label: ratioLabel, aliases: ["持股比例", "持股比", "占流通股比例", "占流通比", "占总股本比例", "占总股本比", "比例", "ratio"], format: "ratio", exact: true },
        { key: "shares", label: "持股数量", aliases: ["持股数量", "持股数", "股份数", "数量", "shares"], format: "number", exact: true },
        { key: "change", label: "变动", aliases: ["holding_change", "变动", "持股变动", "增减", "变化", "change"], exact: true },
      ]} />
    </section>;
  }
  const tableColumns: Record<string, CompactColumn[]> = {
    restricted_release: [
      { key: "date", label: "解禁时间", aliases: ["解禁时间", "解禁日期", "上市日期", "日期", "解除限售日期", "date"], format: "date", exact: true },
      { key: "shares", label: "解禁数量", aliases: ["解禁数量", "解禁股数", "上市流通数量", "数量", "shares"], format: "number", exact: true },
      { key: "ratio", label: "占总股本比", aliases: ["占总股本比", "占总股本比例", "总股本比例", "比例", "ratio"], format: "ratio", exact: true },
    ],
    institutional: [
      { key: "period", label: "报告期", aliases: ["报告期", "截止日期", "截至日期", "report_period", "date"], format: "date", exact: true },
      { key: "shares", label: "持股数量", aliases: ["持股数量", "持股数", "机构持股数量", "shares"], format: "number", exact: true },
      { key: "ratio", label: "占流通股", aliases: ["占流通股", "占流通股比例", "持股比例", "比例", "ratio"], format: "ratio", exact: true },
      { key: "institutions", label: "机构家数", aliases: ["机构家数", "机构数量", "机构数"], exact: true },
      { key: "funds", label: "基金家数", aliases: ["基金家数", "基金数量", "基金数"], exact: true },
    ],
    holder_count: [
      { key: "date", label: "截止日期", aliases: ["股东户数统计截止日", "截止日期", "截至日期", "报告期", "date"], format: "date", exact: true },
      { key: "count", label: "股东户数(户)", aliases: ["股东户数-本次", "股东户数", "股东户数(户)", "股东人数", "户数", "count"], format: "number", exact: true },
      { key: "average", label: "户均持股(股)", aliases: ["户均持股数量", "户均持股", "户均持股数", "每户持股", "average"], format: "number", exact: true },
    ],
  };
  if (key === "capital_structure") {
    const shareFacts = [
      ["总股本", ["A股总股本", "总股本", "总股本数量"]],
      ["流通A股", ["A股流通股本", "流通A股", "已流通股份"]],
      ["有效流通A股", ["有效流通A股", "有效流通股本"]],
      ["限售A股", ["A股限售股本", "限售A股", "流通受限股份"]],
    ].map(([label, aliases]) => ({ label, value: findValue(currentShareFields, aliases as string[]) }))
      .filter((fact) => fact.value !== undefined && fact.value !== null && fact.value !== "");
    return <section className={`f10-mobile-section f10-holder-${key}`}>
      <F10SectionHeading section={section} onOpen={onOpen} />
      <F10FactRows rows={shareFacts} emptyText={String(section.message || "暂无股本结构数据")} />
      <F10SectionSource section={section} />
    </section>;
  }
  if (key === "institutional") {
    const groups = new Map<string, { period: string; shares: number; ratio: number; funds: Set<string> }>();
    rows.forEach((row) => {
      const date = String(pickValue(row, ["截止日期", "报告期", "date"]) || "");
      const period = f10Period({ date });
      if (!period) return;
      const group = groups.get(period) || { period, shares: 0, ratio: 0, funds: new Set<string>() };
      group.shares += toNumber(pickValue(row, ["持仓数量", "持股数量", "持股数", "shares"])) || 0;
      group.ratio += toNumber(pickValue(row, ["占流通股比例", "占流通股", "持股比例", "ratio"])) || 0;
      const fundId = String(pickValue(row, ["基金代码", "基金名称", "机构名称", "name"]) || "");
      if (fundId) group.funds.add(fundId);
      groups.set(period, group);
    });
    const aggregateRows = [...groups.values()].map((group) => ({
      report_period: group.period,
      total_shares: group.shares,
      float_ratio: group.ratio,
      institution_count: undefined,
      fund_count: group.funds.size,
    }));
    return <section className={`f10-mobile-section f10-holder-${key}`}>
      <F10ReportPeriodTable section={{ ...section, rows: aggregateRows }} onOpen={() => onOpen(section)} columns={[
        { key: "period", label: "报告期", aliases: ["report_period"] },
        { key: "shares", label: "持股数量", aliases: ["total_shares"], format: "number" },
        { key: "ratio", label: "占流通股", aliases: ["float_ratio"], format: "ratio" },
        { key: "institutions", label: "机构家数", aliases: ["institution_count"] },
        { key: "funds", label: "基金家数", aliases: ["fund_count"] },
      ]} />
    </section>;
  }
  if (key === "control") {
    const controlRows = rows.flatMap((row) => {
      const relation = controlRelationText(pickValue(row, ["关系", "控股关系", "relation", "control_relation"]) ?? pickValue(row, ["control_role"]));
      const subject = pickValue(row, ["主体名称", "主体", "subject_name", "subject", "实际控制人", "实际控制人名称", "控股股东", "控股股东名称", "ultimate_controller", "controlling_shareholder"]);
      const ratio = pickValue(row, ["持股比例", "持股比", "持股比例(%)", "shareholding_ratio"]);
      if (!relation && !subject) return [];
      const displayRelation = relation || "关联主体";
      const displaySubject = subject || "--";
      return [{ label: displayRelation, value: displaySubject, relation: displayRelation, subject: displaySubject, ratio }];
    });
    const hasRatio = controlRows.some((row) => row.ratio !== undefined && row.ratio !== null && row.ratio !== "");
    const openControlDetail = controlRows.length ? () => onOpen({ ...section, rows: controlRows, detail_title: "控股股东与实际控制人" }) : undefined;
    return <section className={`f10-mobile-section f10-holder-${key}`}>
      <F10SectionHeading section={section} onOpen={openControlDetail} />
      {controlRows.length ? <F10CompactTable rows={controlRows} columns={[{ key: "relation", label: "关系", aliases: ["relation"], exact: true }, { key: "subject", label: "主体", aliases: ["subject"], exact: true }, ...(hasRatio ? [{ key: "ratio", label: "持股比例", aliases: ["ratio"], format: "ratio" as const, exact: true }] : [])]} title="控股股东与实际控制人" /> : <p className="f10-mobile-empty">{String(section.message || "暂无可核验的控股股东或实际控制人信息")}</p>}
      <F10SectionSource section={section} />
    </section>;
  }
  return <section className={`f10-mobile-section f10-holder-${key}`}>
    <F10SectionHeading section={section} onOpen={onOpen} pin />
    {tableColumns[key] ? <F10CompactTable rows={rows} columns={tableColumns[key]} title={String(section.title || "股东数据")} /> : <F10FactRows rows={rows} emptyText={String(section.message || "暂无该分区数据")} onOpenRow={(row) => onOpen({ ...section, rows: [row], detail_title: String(pickValue(row, f10RowLabelKeys) || section.title) })} />}
    <F10SectionSource section={section} />
  </section>;
}

function F10OverviewSection({ section, concepts, onOpen }: { section: JsonRecord; concepts: JsonRecord[]; onOpen: (section: JsonRecord) => void }) {
  const key = String(section.key || "");
  const rows = sectionRows(section);
  const isBasic = key === "basic";
  const isAnomaly = key === "anomaly";
  if (key === "dividend") return <F10DividendSection section={section} onOpen={onOpen} />;
  return <section className={`f10-mobile-section f10-overview-${key}`}>
    <F10SectionHeading section={section} onOpen={!isAnomaly && rows.length ? onOpen : undefined} />
    {isBasic && concepts.length ? <div className="f10-concept-tags">{concepts.map((concept, index) => <span key={`${String(concept.name || concept.label)}-${index}`}>{String(concept.name || concept.label || "概念")}</span>)}</div> : null}
    {isAnomaly ? <button type="button" className="f10-margin-shortcut" onClick={() => onOpen({ ...section, detail_title: section.detail_title || "融资融券近一个月" })}>融资融券近一个月 <span aria-hidden="true">›</span></button> : <F10FactRows rows={rows} emptyText={String(section.message || "暂无该分区数据")} />}
    {isBasic && concepts.length ? <button type="button" className="f10-concept-detail-link" onClick={() => onOpen({ key: "concepts", title: "概念详细解析", rows: concepts, source: section.source, detail_title: "概念详细解析" })}>概念详细解析 <span aria-hidden="true">››</span></button> : null}
    <F10SectionSource section={section} />
  </section>;
}

function dividendDate(row: JsonRecord): string {
  return String(findValue(row, ["公告日期", "分红公告日期", "报告期", "日期", "date", "report_date"]) || "");
}

function dividendTableRows(rows: JsonRecord[]): JsonRecord[] {
  return rows.map((row) => {
    const planParts = [
      ["送股", findValue(row, ["送股", "送股比例"])],
      ["转增", findValue(row, ["转增", "转增比例"])],
      ["派息", findValue(row, ["派息", "现金分红", "分红"] )],
    ].filter(([, value]) => value !== undefined && value !== null && value !== "")
      .map(([label, value]) => `${label}${valueText(value)}`);
    return {
      year: dividendDate(row).slice(0, 4) || "--",
      plan: planParts.length ? planParts.join("；") : String(findValue(row, ["方案", "分配方案", "进度"]) || "--"),
      ex_date: findValue(row, ["除权除息日", "除权日", "除息日", "ex_date"]),
      record_date: findValue(row, ["股权登记日", "登记日", "record_date"]),
      raw: row,
    };
  });
}

function F10DividendSection({ section, onOpen }: { section: JsonRecord; onOpen: (section: JsonRecord) => void }) {
  const rows = sectionRows(section);
  const byYear = new Map<string, JsonRecord>();
  rows.forEach((row) => {
    const year = dividendDate(row).slice(0, 4);
    if (year && !byYear.has(year)) byYear.set(year, row);
  });
  const recent = [...byYear.entries()]
    .sort(([left], [right]) => right.localeCompare(left))
    .slice(0, 3)
    .map(([, row]) => row);
  const tableRows = dividendTableRows(recent.length ? recent : rows.slice(0, 3));
  return <section className="f10-mobile-section f10-overview-dividend">
    <F10SectionHeading section={section} onOpen={rows.length ? () => onOpen({ ...section, key: "dividend_detail", detail_title: "分红配送全部" }) : undefined} />
    <F10CompactTable rows={tableRows} title="分红配送" emptyText={String(section.message || "暂无分红配送数据")} columns={[
      { key: "year", label: "年度", aliases: ["year"] },
      { key: "plan", label: "方案", aliases: ["plan"] },
      { key: "ex_date", label: "除权日", aliases: ["ex_date"], format: "date" },
    ]} />
    {rows.length > recent.length ? <button type="button" className="f10-concept-detail-link f10-dividend-all-link" onClick={() => onOpen({ ...section, key: "dividend_detail", detail_title: "分红配送全部" })}>查看全部 <span aria-hidden="true">›</span></button> : null}
    <F10SectionSource section={section} />
  </section>;
}

function F10CompositionSection({ section, sourceRows, onOpen }: { section: JsonRecord; sourceRows: JsonRecord[]; onOpen: (section: JsonRecord) => void }) {
  const rows = sourceRows.length ? sourceRows : sectionRows(section);
  const amounts = rows.map((row) => toNumber(pickValue(row, ["revenue_ratio", "收入占比", "占比", "ratio", "amount", "value", "value_amount", "金额"])));
  const total = amounts.reduce<number>((sum, value) => sum + (value && value > 0 ? value : 0), 0);
  let cursor = 0;
  const palette = ["#d63f48", "#e8a33a", "#3e8f77", "#5279a7", "#9a6cab", "#8c9d50"];
  const stops = amounts.map((value, index) => {
    const portion = total ? Math.max(0, value || 0) / total * 100 : 0;
    const start = cursor;
    cursor += portion;
    return `${palette[index % palette.length]} ${start}% ${cursor}%`;
  }).filter((stop) => !stop.endsWith(" 0%"));
  const style = stops.length ? { background: `conic-gradient(${stops.join(", ")})` } : undefined;
  return <section className="f10-mobile-section f10-financial-composition">
    <F10SectionHeading section={section} onOpen={onOpen} />
    <div className="f10-composition-layout">
      <div className="f10-composition-donut" style={style} aria-label="主营业务收入构成" />
      <div className="f10-composition-legend">{rows.slice(0, 6).map((row, index) => <div className="f10-composition-row" key={index}>
        <i style={{ background: palette[index % palette.length] }} />
        <span>{String(pickValue(row, f10RowLabelKeys) || `业务${index + 1}`)}</span>
        <strong>{displayByLabel("营业收入", pickValue(row, ["revenue", "营业收入", "收入", ...f10RowValueKeys]))}</strong>
      </div>)}</div>
    </div>
    {!rows.length ? <p className="f10-mobile-empty">{String(section.message || "暂无主营构成数据")}</p> : null}
    <F10SectionSource section={section} />
  </section>;
}

function F10IndicatorRows({ section, onOpen }: { section: JsonRecord; onOpen: (section: JsonRecord) => void }) {
  const rows = sectionRows(section);
  return rows.length ? <div className="f10-indicator-grid">{rows.map((row, index) => {
    const label = String(pickValue(row, f10RowLabelKeys) || `指标${index + 1}`);
    const raw = pickValue(row, ["数据", "value", "值", "data", "amount"]);
    const series = asRecord(raw);
    const dates = Object.keys(series).filter((date) => series[date] !== null && series[date] !== undefined && series[date] !== "").sort((left, right) => right.localeCompare(left));
    const currentDate = dates[0];
    const value = currentDate ? series[currentDate] : raw;
    return <button className="f10-indicator-item" type="button" key={`${label}-${index}`} onClick={() => onOpen({ ...section, rows: dates.length ? dates.map((date) => ({ label: formatDate(date), value: series[date] })) : [row], detail_title: label })}>
      <span>{label}</span><strong>{displayByLabel(label, value)}</strong>{currentDate ? <small>{formatDate(currentDate)}</small> : null}
    </button>;
  })}</div> : <p className="f10-mobile-empty">{String(section.message || "暂无主要指标数据")}</p>;
}

function F10FinancialSection({ section, onOpen, compositionRows }: { section: JsonRecord; onOpen: (section: JsonRecord) => void; compositionRows: JsonRecord[] }) {
  const key = String(section.key || "");
  if (key === "composition") return <F10CompositionSection section={section} sourceRows={compositionRows} onOpen={onOpen} />;
  if (key === "indicators") return <section className="f10-mobile-section f10-financial-indicators"><F10SectionHeading section={section} onOpen={onOpen} /><F10IndicatorRows section={section} onOpen={onOpen} /><F10SectionSource section={section} /></section>;
  const rows = sectionRows(section);
  const isStatement = ["income", "balance", "cash_flow", "income_statement", "balance_sheet"].includes(key);
  return <section className={`f10-mobile-section f10-financial-${key}`}>
    <F10SectionHeading section={section} onOpen={onOpen} />
    {isStatement && rows.length ? <div className="f10-statement-rows">{rows.map((row, index) => {
      const label = String(pickValue(row, f10RowLabelKeys) || `项目${index + 1}`);
      const trend = trendFromRow(row);
      return <button className="f10-statement-row" type="button" key={`${label}-${index}`} onClick={() => onOpen({ ...section, rows: [row], detail_title: label })}>
        <span>{label}</span><strong>{displayByLabel(label, pickValue(row, f10RowValueKeys) ?? rowValueText(row))}{trend ? <em className={`metric-trend ${trend.tone}`}>{trend.text}</em> : null}</strong><b aria-hidden="true">›</b>
      </button>;
    })}</div> : <F10FactRows rows={rows} emptyText={String(section.message || "暂无该分区数据")} onOpenRow={(row) => onOpen({ ...section, rows: [row], detail_title: String(pickValue(row, f10RowLabelKeys) || section.title) })} />}
    <F10SectionSource section={section} />
  </section>;
}

function F10ConceptDetail({ section }: { section: JsonRecord }) {
  const concepts = sectionRows(section);
  return concepts.length ? <div className="f10-concept-detail-list">{concepts.map((concept, index) => {
    const name = String(pickValue(concept, ["name", "label", "概念名称", "概念"]) || "未命名概念");
    const definition = pickValue(concept, ["definition", "释义", "概念解析", "概念说明", "解释", "description"]);
    const importance = pickValue(concept, ["relevance_label", "相关度标签"]);
    const dimension = String(pickValue(concept, ["dimension", "classification_dimension"]) || "").toUpperCase();
    const relatedStocks = asArray(concept.related_stocks);
    const trend = toNumber(concept.trend_pct);
    const memberCount = toNumber(concept.member_count);
    return <article className="f10-concept-detail-item" key={`${name}-${index}`}>
      <header><div><h4>{name}</h4><small>{dimension === "INDUSTRY" ? "行业" : dimension === "BOARD" ? "上市板块" : "主题板块"}{memberCount !== null ? ` · ${memberCount} 只相关证券` : ""}</small></div><span>{String(importance || "最相关")}</span></header>
      <p>{valueText(definition || "暂无可核验的概念释义")}</p>
      <div className="f10-concept-related-head"><strong>相关股票</strong>{trend !== null ? <em className={trend >= 0 ? "positive" : "negative"}>整体 {trend >= 0 ? "↑" : "↓"} {formatPercent(Math.abs(trend))}</em> : <small>暂无统一行情</small>}</div>
      {relatedStocks.length ? <div className="f10-concept-related-list">{relatedStocks.slice(0, 30).map((stock, stockIndex) => {
        const market = String(stock.market || "CN_A");
        const symbol = String(stock.symbol || "");
        const change = toNumber(stock.change_pct);
        const href = stock.source_url || (market === "HK" ? `https://quote.eastmoney.com/hk/${symbol}.html` : `https://quote.eastmoney.com/${symbol}.html`);
        return <a key={`${market}-${symbol}-${stockIndex}`} href={String(href)} target="_blank" rel="noreferrer" title="打开来源行情页面"><span>{String(stock.name || symbol)}</span><small>{symbol}</small>{change !== null ? <em className={change >= 0 ? "positive" : "negative"}>{change >= 0 ? "↑" : "↓"}{formatPercent(Math.abs(change))}</em> : null}</a>;
      })}</div> : <p className="f10-concept-related-empty">当前主数据未返回该分类的成分股清单。</p>}
    </article>;
  })}</div> : <p className="f10-mobile-empty">{String(section.message || "暂无概念详情")}</p>;
}

function F10MarginHistory({ section }: { section: JsonRecord }) {
  const rows = sectionRows(section);
  return <F10CompactTable rows={rows} title="融资融券" emptyText={String(section.message || "暂无融资融券数据")} columns={[
    { key: "date", label: "交易日期", aliases: ["交易日期", "日期", "date", "trade_date"], format: "date" },
    { key: "financing", label: "融资余额", aliases: ["融资余额", "financing_balance"] },
    { key: "securities", label: "融券余额", aliases: ["融券余额", "融券市值", "securities_balance"] },
    { key: "combined", label: "融资融券余额", aliases: ["融资融券余额", "融资融券总余额", "两融余额", "total_balance"] },
  ]} />;
}

function F10DividendDetail({ section }: { section: JsonRecord }) {
  const rows = dividendTableRows(sectionRows(section));
  return <F10CompactTable rows={rows} title="分红配送全部" emptyText={String(section.message || "暂无分红配送数据")} columns={[
    { key: "year", label: "年度", aliases: ["year"] },
    { key: "plan", label: "方案", aliases: ["plan"] },
    { key: "ex_date", label: "除权日", aliases: ["ex_date"], format: "date" },
    { key: "record_date", label: "股权登记日", aliases: ["record_date"], format: "date" },
  ]} />;
}

function F10Panel({ detail, activeTab, setActiveTab }: { detail: StockF10; activeTab: F10Tab; setActiveTab: (tab: F10Tab) => void }) {
  const summary = asRecord(detail.financial_summary);
  const periods = Array.isArray(summary.periods) ? summary.periods.map(String) : [];
  const statements = asRecord(detail.financial_statements);
  const holders = asRecord(detail.holders);
  const profile = asRecord(detail.profile);
  const profileFields = asRecord(profile.fields);
  const currentShareFields = asRecord(asRecord(detail.symbol).ext_json);
  const composition = asRecord(detail.business_composition);
  const compositionRows = asArray(composition.sections).flatMap((item) => {
    const category = String(pickValue(item, ["category", "name", "分类", "类别"]) || "主营业务");
    const items = asArray(item.items);
    if (!items.length) return [{ ...item, label: category, value: pickValue(item, ["summary", "摘要", "value", "数据"]) || item }];
    return items.map((row) => ({
      ...row,
      label: String(pickValue(row, ["name", "项目", "label", "主营构成"]) || category),
      value: pickValue(row, ["revenue", "营业收入", "收入", "value", "amount", "data", "summary", "数值", "金额"]) ?? row,
    }));
  });
  const reports = detail.published_reports?.reports || [];
  const [detailSection, setDetailSection] = useState<JsonRecord | null>(null);
  const holderSections = asArray(holders.sections);
  const overviewSections = asArray(profile.overview_sections);
  const financialSections = asArray(summary.financial_sections);

  const openDetail = (section: JsonRecord) => setDetailSection(section);
  const closeDetail = () => setDetailSection(null);

  return (
    <>
      <div className="detail-sub-tabs">
        {(["财务", "股东", "简况", "财报"] as const).map((item) => (
          <button type="button" key={item} className={activeTab === item ? "active" : ""} onClick={() => setActiveTab(item)}>{item}</button>
        ))}
      </div>
      {activeTab === "财务" && (
        <div className="f10-mobile-page f10-financial-page">
          {(financialSections.length ? financialSections : [
            { key: "composition", title: "主营构成", rows: compositionRows, source: composition.source, status: compositionRows.length ? "AVAILABLE" : "UNAVAILABLE", message: composition.message },
            { key: "indicators", title: "主要指标", rows: summary.rows, source: summary.source, status: Array.isArray(summary.rows) && summary.rows.length ? "AVAILABLE" : "UNAVAILABLE" },
            ...(["income_statement", "balance_sheet", "cash_flow"] as const).map((key) => { const block = asRecord(statements[key]); return { key, title: String(block.label || block.report_name || statementName(key)), rows: block.rows, source: block.source || statements.source, status: Array.isArray(block.rows) && block.rows.length ? "AVAILABLE" : "UNAVAILABLE", as_of: block.report_date }; }),
          ]).map((section) => <F10FinancialSection key={String(section.key)} section={section} onOpen={openDetail} compositionRows={compositionRows} />)}
        </div>
      )}
      {activeTab === "股东" && (
        <div className="f10-mobile-page f10-shareholder-page">
          {holderSections.length ? holderSections.map((section) => <F10ShareholderSection key={String(section.key)} section={section} onOpen={openDetail} currentShareFields={currentShareFields} />) : (
            <><F10ShareholderSection section={{ key: "top_ten", title: "十大股东", rows: holders.major, source: holders.source }} onOpen={openDetail} currentShareFields={currentShareFields} /><F10ShareholderSection section={{ key: "top_ten_circulating", title: "十大流通股东", rows: holders.circulating, source: holders.source }} onOpen={openDetail} currentShareFields={currentShareFields} /></>
          )}
          {asArray(holders.official_links).length ? <div className="extended-source-links">{asArray(holders.official_links).map((link, index) => <a className="extended-source-link" href={String(link.url || "#")} target="_blank" rel="noreferrer" key={index}>{String(link.name || `官方入口 ${index + 1}`)}</a>)}</div> : null}
        </div>
      )}
      {activeTab === "简况" && (
        <div className="f10-mobile-page f10-overview-page">
          {(overviewSections.length ? overviewSections : [{ key: "basic", title: "基本情况", rows: Object.entries(profileFields).map(([label, value]) => ({ label, value })), source: profile.source }]).map((section) => {
            const concepts = asArray(profile.concepts);
            return <F10OverviewSection key={String(section.key)} section={section} concepts={concepts} onOpen={openDetail} />;
          })}
        </div>
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
      {detailSection ? <div className="f10-detail-overlay" role="dialog" aria-modal="true"><div className="f10-detail-dialog f10-mobile-fullpage"><header><button type="button" aria-label="返回" onClick={closeDetail}><ArrowLeft size={18} /><span>返回</span></button><h3>{String(detailSection.detail_title || detailSection.title || "详细数据")}</h3></header><div className="f10-detail-body">{detailSection.key === "concepts" ? <F10ConceptDetail section={detailSection} /> : detailSection.key === "anomaly" ? <F10MarginHistory section={detailSection} /> : detailSection.key === "dividend_detail" ? <F10DividendDetail section={detailSection} /> : sectionRows(detailSection).length ? sectionRows(detailSection).map((row, index) => <div className="f10-detail-row" key={index}><strong>{String(pickValue(row, ["label", "name", "项目", "指标"]) || `项目 ${index + 1}`)}</strong><span>{displayByLabel(String(pickValue(row, ["label", "name", "项目", "指标"]) || "数据"), pickValue(row, f10RowValueKeys) ?? rowValueText(row))}</span></div>) : <p className="empty-state">{String(detailSection.message || "当前数据源未返回详细数据")}</p>}<div className="f10-detail-source"><span>来源：{String(detailSection.source || "暂无")}</span><SourceLinks section={detailSection} /></div></div></div></div> : null}
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
  const drawerRef = useRef<HTMLElement | null>(null);
  const detailController = useRef<AbortController | null>(null);
  const [detail, setDetail] = useState<StockF10 | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [refreshProgress, setRefreshProgress] = useState(0);
  const [refreshStage, setRefreshStage] = useState("");
  const [error, setError] = useState("");
  const [remoteError, setRemoteError] = useState("");
  const [companyGraphOpen, setCompanyGraphOpen] = useState(false);
  const [knowledgeGraphOpen, setKnowledgeGraphOpen] = useState(false);
  const [tab, setTab] = useState<PrimaryTab>("精选");
  const [f10Tab, setF10Tab] = useState<F10Tab>("财务");
  const [newsPage, setNewsPage] = useState(1);
  const [noticePage, setNoticePage] = useState(1);
  const [noticeCategory, setNoticeCategory] = useState<(typeof noticeCategories)[number]>("全部");
  const [noticeTotal, setNoticeTotal] = useState<number | undefined>(undefined);
  const [newsRows, setNewsRows] = useState<StockNewsItem[] | null>(null);
  const [noticeRows, setNoticeRows] = useState<StockNotice[] | null>(null);
  const [pageLoading, setPageLoading] = useState<"news" | "notice" | null>(null);
  const [researchReport, setResearchReport] = useState("");
  const [researchRunning, setResearchRunning] = useState(false);
  const [researchStage, setResearchStage] = useState("");
  const [researchDetail, setResearchDetail] = useState<JsonRecord | null>(null);
  const [classificationDetail, setClassificationDetail] = useState<{ group: string; item: SectorGroupValue } | null>(null);
  const [agentSnapshots, setAgentSnapshots] = useState<Array<ResearchFundamentalAgent | ResearchTechnicalAgent>>([]);

  const pageSize = 8;

  useEffect(() => {
    if (!remoteError) return;
    const timer = window.setTimeout(() => setRemoteError(""), 6000);
    return () => window.clearTimeout(timer);
  }, [remoteError]);

  async function loadLocalAndRemote() {
    const current = requestId.current + 1;
    requestId.current = current;
    detailController.current?.abort();
    const controller = new AbortController();
    detailController.current = controller;
    setLoading(true);
    setRefreshing(false);
    setRefreshProgress(0);
    setRefreshStage("");
    setDetail(null);
    setError("");
    setRemoteError("");
    setNewsRows(null);
    setNoticeRows(null);
    setNewsPage(1);
    setNoticePage(1);
    setNoticeCategory("全部");
    setNoticeTotal(undefined);
    try {
      const local = await api.getStockF10(stock.market, stock.symbol, {
        localOnly: true,
        klineLimit: 5000,
        financialLimit: 20,
        noticeLimit: pageSize,
        noticeCategory: "全部",
        newsLimit: pageSize,
        signal: controller.signal,
      });
      if (requestId.current === current && !controller.signal.aborted) setDetail(local);
    } catch (reason) {
      if (requestId.current === current && !controller.signal.aborted) {
        setError(reason instanceof Error ? reason.message : "本地股票详情加载失败");
      }
    } finally {
      if (requestId.current === current && !controller.signal.aborted) setLoading(false);
    }
    if (requestId.current === current && !controller.signal.aborted) await refreshRemote(current, controller.signal, "全部");
  }

  async function refreshRemote(current = requestId.current, signal = detailController.current?.signal, category = noticeCategory) {
    if (requestId.current !== current || signal?.aborted) return;
    setRefreshing(true);
    setRefreshProgress(12);
    setRefreshStage("正在连接数据源");
    setRemoteError("");
    let progressTimer: number | undefined;
    progressTimer = window.setInterval(() => {
      setRefreshProgress((value) => Math.min(88, value + (value < 40 ? 8 : 3)));
      setRefreshStage((value) => value || "正在同步行情、财务与披露");
    }, 420);
    try {
      const remote = await api.getStockF10(stock.market, stock.symbol, {
        refresh: true,
        klineLimit: 5000,
        financialLimit: 20,
        noticeLimit: pageSize,
        noticeCategory: category,
        newsLimit: pageSize,
        signal,
      });
      if (requestId.current === current && !signal?.aborted) {
        setDetail(remote);
        setRefreshProgress(96);
        setRefreshStage("正在整理 F10 与研究数据");
        setNewsRows(null);
        setNoticeRows(null);
        setNoticeTotal(undefined);
      }
    } catch (reason) {
      if (requestId.current === current && !signal?.aborted) {
        setRemoteError(friendlyRefreshError(reason));
        setRefreshStage("更新失败，继续使用本地缓存");
      }
    } finally {
      if (progressTimer !== undefined) window.clearInterval(progressTimer);
      if (requestId.current === current && !signal?.aborted) {
        setRefreshProgress(100);
        setRefreshStage((value) => value || "数据更新完成");
        setRefreshing(false);
        window.setTimeout(() => {
          if (requestId.current === current) {
            setRefreshProgress(0);
            setRefreshStage("");
          }
        }, 1200);
      }
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
      detailController.current?.abort();
    };
  }, [stock.market, stock.symbol]);

  useEffect(() => {
    const drawer = drawerRef.current;
    const header = drawer?.querySelector<HTMLElement>(".detail-market-header");
    if (!drawer || !header) return;
    const syncHeight = () => drawer.style.setProperty("--stock-detail-sticky-header-height", `${header.getBoundingClientRect().height}px`);
    syncHeight();
    const observer = new ResizeObserver(syncHeight);
    observer.observe(header);
    return () => observer.disconnect();
  }, [stock.market, stock.symbol, detail, refreshing]);

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

  async function changeNoticePage(nextPage: number, category = noticeCategory) {
    setPageLoading("notice");
    try {
      const result = await api.listStockNoticesPage(stock.market, stock.symbol, nextPage, pageSize, category);
      setNoticeRows(result.items);
      setNoticePage(result.page);
      setNoticeTotal(result.total);
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
  const researchSections = asArray(asRecord(detail?.research_sections).sections);
  const researchSectionsByKey = new Map(researchSections.map((section) => [String(section.key || ""), section]));
  const orderedResearchSections = [
    ...researchSectionOrder.map(({ key, title }) => researchSectionsByKey.get(key) || {
      key,
      title,
      status: "UNAVAILABLE",
      rows: [],
      message: key === "qa" ? "当前未接入问董秘公开接口" : "暂无该分区数据",
    }),
    ...researchSections.filter((section) => !researchSectionOrder.some(({ key }) => key === String(section.key || ""))),
  ];
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
  const chooseNoticeCategory = (category: (typeof noticeCategories)[number]) => {
    setNoticeCategory(category);
    setNoticePage(1);
    void changeNoticePage(1, category);
  };

  return (
    <div className="drawer-mask" onClick={onClose}>
      <aside ref={drawerRef} className="stock-drawer stock-detail-drawer restored-stock-detail" onClick={(event) => event.stopPropagation()}>
        <header className="detail-market-header">
          <div>
            <span className={`market-chip ${marketClass[stock.market] || ""}`}>{marketLabel[stock.market] || stock.market}</span>
            <h2>{name}<small>({stock.symbol})</small></h2>
            <div className="detail-tags">
              <span>{(detail?.symbol as StockSymbol | undefined)?.exchange || "交易所待补充"}</span>
              <span>{(detail?.symbol as StockSymbol | undefined)?.asset_type || "股票"}</span>
              <span>{formatDate((detail?.symbol as StockSymbol | undefined)?.list_date)}</span>
              <span>{refreshing ? "同步远程数据中" : refreshProgress === 100 ? "数据更新完成" : "本地优先 · 自动校验远程"}</span>
            </div>
          </div>
          <div className="detail-head-actions">
            <button type="button" onClick={() => setCompanyGraphOpen(true)}><Building2 size={15} /> 公司关联</button>
            <button type="button" onClick={() => setKnowledgeGraphOpen(true)}>知识图谱</button>
            <button type="button" onClick={() => void refreshRemote()} disabled={refreshing}>{refreshing ? "更新中" : "手动更新"}</button>
            <button type="button" onClick={onClose}>关闭</button>
          </div>
        </header>

        {(refreshing || refreshProgress > 0) ? <div className={`stock-refresh-progress ${refreshProgress === 100 ? "complete" : ""}`} role="status" aria-live="polite">
          <div className="stock-refresh-progress-head"><span>{refreshStage || "正在更新数据"}</span><strong>{Math.round(refreshProgress)}%</strong></div>
          <div className="stock-refresh-progress-track" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(refreshProgress)}><i style={{ width: `${refreshProgress}%` }} /></div>
        </div> : null}

        {companyGraphOpen && <CompanyGraphDialog stock={stock} close={() => setCompanyGraphOpen(false)} />}
        {knowledgeGraphOpen && <KnowledgeGraphDialog initialStock={stock} close={() => setKnowledgeGraphOpen(false)} />}
        {loading && <p className="empty-state">正在读取本地数据库...</p>}
        {error && <p className="form-error">{error}</p>}
        {remoteError && <div className="form-error soft-error" role="status" aria-live="polite"><span>{remoteError}</span><button type="button" aria-label="关闭远程更新提示" title="关闭提示" onClick={() => setRemoteError("")}><X size={14} aria-hidden="true" /></button></div>}

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
                    profile={profile}
                    profileFields={profileFields}
                    klines={detail.recent_klines}
                    onClassificationSelect={(group, item) => setClassificationDetail({ group: group.label, item })}
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
                <>
                  <nav className="notice-category-tabs" aria-label="公告分类">
                    {noticeCategories.map((category) => <button type="button" key={category} className={noticeCategory === category ? "active" : ""} onClick={() => chooseNoticeCategory(category)}>{category}</button>)}
                  </nav>
                  <NewsList
                    title={noticeCategory === "全部" ? "公告" : noticeCategory}
                    items={noticeItems}
                    total={noticeTotal ?? detail.notice_total}
                    page={noticePage}
                    pageSize={pageSize}
                    loading={pageLoading === "notice"}
                    dateKey="notice_date"
                    onPageChange={(page) => void changeNoticePage(page)}
                  />
                </>
              )}
              {tab === "资金" && <FundFlowPanel data={asRecord(detail.fund_flow)} />}
              {tab === "F10" && <F10Panel detail={detail} activeTab={f10Tab} setActiveTab={setF10Tab} />}
              {tab === "研究" && (
                  <>
                    <section className="research-section-grid">
                      {orderedResearchSections.map((section) => String(section.key) === "institution_forecast"
                        ? <ResearchForecastPanel key={String(section.key)} section={section} />
                        : String(section.key) === "industry_concepts"
                          ? <ResearchIndustryConcepts key={String(section.key)} section={section} onOpen={(item) => setResearchDetail(item)} />
                        : <F10SectionBlock key={String(section.key)} section={section} onOpen={(item) => setResearchDetail(item)} />)}
                    </section>
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
                            <span>{agentDisplayName(agent.agent)}</span>
                            <strong>{"score" in agent ? `${agent.score} · ${"rating" in agent ? agent.rating : agent.trend}` : "--"}</strong>
                          </article>
                        ))}
                      </div>
                    ) : null}
                    <article className="research-markdown detail-research-markdown">
                      {researchReport ? renderMarkdown(researchReport) : <p className="empty-state">点击按钮后，研报会以打字机流式效果显示在这里。</p>}
                    </article>
                    </section>
                    {researchDetail ? <div className="f10-detail-overlay" role="dialog" aria-modal="true"><div className="f10-detail-dialog"><header><h3>{String(researchDetail.title || "研究详情")}</h3><button type="button" onClick={() => setResearchDetail(null)}>关闭</button></header><div className="f10-detail-body">{sectionRows(researchDetail).length ? sectionRows(researchDetail).map((row, index) => <div className="f10-detail-row" key={index}><strong>{String(pickValue(row, ["label", "name", "项目", "指标"]) || `项目 ${index + 1}`)}</strong><span>{rowValueText(row)}</span></div>) : <p className="empty-state">{String(researchDetail.message || "当前数据源未返回详细数据")}</p>}<div className="f10-detail-source"><span>来源：{String(researchDetail.source || "暂无")}</span><SourceLinks section={researchDetail} /></div></div></div></div> : null}
                  </>
              )}
            </section>
          </>
        )}
        {classificationDetail ? <ClassificationDetailDialog group={classificationDetail.group} item={classificationDetail.item} onClose={() => setClassificationDetail(null)} /> : null}
      </aside>
    </div>
  );
}
