import { useEffect, useMemo, useState } from "react";
import {
  api,
  type StockPipelineDetailItem,
  type StockPipelineStageDetail,
  type StockPipelineStatus,
  type StockPipelineStatusDetail,
  type StockPipelineVerificationRecord,
  type StockSymbol,
} from "./api";
import "./StockPipelineStatusDialog.css";

export type StockPipelineStatusKey = "data_collection" | "knowledge_base" | "knowledge_graph";

const stageNames: Record<StockPipelineStatusKey, string> = {
  data_collection: "数据采集",
  knowledge_base: "知识库",
  knowledge_graph: "知识图谱",
};

const statusNames: Record<string, string> = {
  NOT_STARTED: "未开始",
  WAITING: "等待处理",
  PENDING: "处理中",
  PARTIAL: "部分完成",
  COMPLETED: "已完成",
  SUCCESS: "已完成",
  FAILED: "失败",
};

const businessItems = [
  ["QUOTE", "实时行情"],
  ["KLINE", "历史量价"],
  ["FINANCIAL", "财务数据"],
  ["NEWS", "新闻"],
  ["NOTICE", "公告"],
  ["F10", "F10资料"],
] as const;

const fieldNames: Record<string, string> = {
  market: "市场",
  symbol: "股票代码",
  source_table: "来源数据表",
  source_record_id: "来源记录ID",
  graph_id: "图谱ID",
  graph_name: "图谱名称",
  graph_code: "图谱编码",
  governance_status: "治理状态",
  document_id: "知识文档ID",
  document_count: "知识文档数",
  chunk_count: "切片数",
  entity_count: "股票实体数",
  relation_count: "关联关系数",
  report_period: "报告期",
  trade_date: "交易日期",
  published_at: "发布时间",
  title: "标题",
  status: "执行状态",
  source_code: "数据源",
  source_name: "数据源名称",
  interface_code: "接口",
  total_count: "上游返回数",
  persisted_count: "成功入库数",
  inserted_count: "新增数",
  updated_count: "更新数",
  error: "错误原因",
  error_message: "错误原因",
  message: "执行说明",
  started_at: "开始时间",
  completed_at: "完成时间",
  attempted_at: "执行时间",
  fetch_log_id: "采集日志ID",
};

function numberValue(value: unknown) {
  const number = Number(value ?? 0);
  return Number.isFinite(number) ? number : 0;
}

function formatDate(value?: string | null) {
  if (!value) return "--";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN");
}

function statusClass(status?: string | null) {
  const normalized = String(status || "NOT_STARTED").toUpperCase();
  if (["COMPLETED", "SUCCESS"].includes(normalized)) return "success";
  if (["PARTIAL", "PENDING", "RUNNING", "WAITING"].includes(normalized)) return "partial";
  if (normalized === "FAILED") return "failed";
  return "empty";
}

function statusLabel(status?: string | null) {
  const normalized = String(status || "NOT_STARTED").toUpperCase();
  return statusNames[normalized] || normalized;
}

function missingItem(code: string, label: string, reason: string, actionHint: string): StockPipelineDetailItem {
  return {
    code,
    label,
    status: "NOT_STARTED",
    record_count: 0,
    reason,
    action_hint: actionHint,
    records: [],
  };
}

function completedItem(code: string, label: string, count: number, lastAt?: string | null): StockPipelineDetailItem {
  return {
    code,
    label,
    status: "COMPLETED",
    record_count: count,
    last_at: lastAt,
    records: [],
  };
}

function fallbackStage(
  kind: StockPipelineStatusKey,
  summary: StockPipelineStatus,
): StockPipelineStageDetail {
  const detail = summary.detail || {};
  const completed: StockPipelineDetailItem[] = [];
  const incomplete: StockPipelineDetailItem[] = [];

  if (kind === "data_collection") {
    businessItems.forEach(([code, label]) => {
      const count = numberValue(detail[code]);
      if (count > 0) completed.push(completedItem(code, label, count, summary.last_at));
      else incomplete.push(missingItem(
        code,
        label,
        "当前数据库没有该类股票业务记录；可能尚未采集、上游接口暂不可用，或该市场不支持此数据类型。",
        "重新执行批量采集，并在任务详情中核对该数据类型的接口返回。",
      ));
    });
  } else if (kind === "knowledge_base") {
    const documentCount = numberValue(detail.document_count);
    const chunkCount = numberValue(detail.chunk_count);
    const documentsWithChunks = numberValue(detail.documents_with_chunks);
    if (documentCount > 0) completed.push(completedItem("DOCUMENTS", "知识文档", documentCount, summary.last_at));
    else incomplete.push(missingItem(
      "DOCUMENTS",
      "知识文档",
      "当前股票尚未生成可追溯的知识文档。",
      "先把新闻、公告、财报或F10等业务记录物化为知识文档。",
    ));
    if (documentCount > 0 && documentsWithChunks >= documentCount) {
      completed.push(completedItem("CHUNKS", "文档切片", chunkCount, summary.last_at));
    } else {
      incomplete.push({
        ...missingItem(
          "CHUNKS",
          "文档切片",
          documentCount > 0
            ? `已有 ${documentCount} 份文档，其中 ${Math.max(0, documentCount - documentsWithChunks)} 份尚未形成切片。`
            : "没有知识文档，因此暂时无法生成切片。",
          "执行知识库与切片环节，并检查解析器、章节识别和切片运行结果。",
        ),
        record_count: chunkCount,
        expected_count: documentCount,
      });
    }
  } else {
    const definitions = [
      ["GRAPHS", "知识图谱", "graph_count", "先为该股票生成范围知识图谱投影。"],
      ["ENTITIES", "股票实体", "entity_count", "检查股票统一标识，并重新执行图谱构建。"],
      ["RELATIONS", "关联关系", "relation_count", "补充来源文档后重新投影，核对关系生成规则。"],
    ] as const;
    definitions.forEach(([code, label, field, hint]) => {
      const count = numberValue(detail[field]);
      if (count > 0) completed.push(completedItem(code, label, count, summary.last_at));
      else incomplete.push(missingItem(code, label, `当前数据库没有可核验的${label}记录。`, hint));
    });
  }

  return {
    stage: kind,
    label: stageNames[kind],
    summary,
    completed_items: completed,
    incomplete_items: incomplete,
    verification: { total_count: completed.reduce((sum, item) => sum + numberValue(item.record_count), 0), records: [] },
    notes: ["详情接口暂不可用时，本页会使用主数据列表返回的持久化计数作为兼容展示。"],
  };
}

function valueText(value: unknown) {
  if (value == null || value === "") return "--";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "object") return JSON.stringify(value, null, 2);
  return String(value);
}

function verificationHref(path?: string | null) {
  if (!path) return "";
  try {
    if (/^https?:\/\//i.test(path)) return path;
    const configured = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000/api/v1";
    const base = new URL(configured, window.location.origin);
    if (path.startsWith("/")) return new URL(path, base.origin).toString();
    return new URL(path.replace(/^\/+/, ""), `${base.toString().replace(/\/?$/, "/")}`).toString();
  } catch {
    return path;
  }
}

function safeExternalUrl(value?: string | null) {
  return value && /^https?:\/\//i.test(value) ? value : "";
}

function VerificationRecord({ record, index }: { record: StockPipelineVerificationRecord; index: number }) {
  const fields = Object.entries(record.fields || {});
  const sourceUrl = safeExternalUrl(record.source_url);
  const apiUrl = verificationHref(record.verification_path);
  return <article className="pipeline-verification-record">
    <header>
      <div><strong>{record.title || `核验记录 ${index + 1}`}</strong><small>{record.source_name || "本地持久化数据"}</small></div>
      {record.observed_at && <time>{formatDate(record.observed_at)}</time>}
    </header>
    {!!fields.length && <dl>{fields.map(([key, value]) => <div key={key}><dt>{fieldNames[key] || key}</dt><dd>{valueText(value)}</dd></div>)}</dl>}
    <footer>
      {sourceUrl && <a href={sourceUrl} target="_blank" rel="noopener noreferrer">查看来源原文 ↗</a>}
      {apiUrl && <a href={apiUrl} target="_blank" rel="noopener noreferrer">查看接口数据 ↗</a>}
      {record.record_id != null && <code>记录 #{String(record.record_id)}</code>}
    </footer>
  </article>;
}

function LatestAttempt({ value }: { value: string | Record<string, unknown> }) {
  if (typeof value === "string") return <p className="pipeline-latest-attempt-text">{value}</p>;
  const priority = [
    "status", "source_name", "source_code", "interface_code", "total_count", "persisted_count",
    "inserted_count", "updated_count", "error", "error_message", "message", "started_at", "completed_at",
    "attempted_at", "fetch_log_id",
  ];
  const entries = Object.entries(value).sort(([left], [right]) => {
    const leftRank = priority.indexOf(left);
    const rightRank = priority.indexOf(right);
    return (leftRank < 0 ? priority.length : leftRank) - (rightRank < 0 ? priority.length : rightRank);
  });
  return <dl className="pipeline-attempt-fields">{entries.map(([key, item]) => <div key={key}><dt>{fieldNames[key] || key}</dt><dd>{valueText(item)}</dd></div>)}</dl>;
}

function DetailItem({ item, incomplete = false }: { item: StockPipelineDetailItem; incomplete?: boolean }) {
  const records = item.records || [];
  const apiUrl = verificationHref(item.verification_path);
  const latestAttempt = item.latest_attempt;
  return <details className={`pipeline-detail-item${incomplete ? " incomplete" : " completed"}`} open={incomplete || undefined}>
    <summary>
      <span className={`pipeline-detail-state ${statusClass(item.status || (incomplete ? "PARTIAL" : "COMPLETED"))}`} aria-hidden="true" />
      <span><strong>{item.label}</strong><small>{item.record_count != null ? `${item.record_count.toLocaleString()} 条记录` : statusLabel(item.status)}</small></span>
      <span className="pipeline-detail-disclosure">查看详情</span>
    </summary>
    <div className="pipeline-detail-item-body">
      {item.last_at && <p><b>最近更新：</b>{formatDate(item.last_at)}</p>}
      {item.reason && <p className="pipeline-detail-reason"><b>原因：</b>{item.reason}</p>}
      {item.action_hint && <p className="pipeline-detail-action"><b>建议：</b>{item.action_hint}</p>}
      {latestAttempt && <details className="pipeline-latest-attempt"><summary>查看最近一次执行信息</summary><LatestAttempt value={latestAttempt} /></details>}
      {!!records.length && <div className="pipeline-verification-list">{records.map((record, index) => <VerificationRecord key={`${record.record_id ?? index}:${index}`} record={record} index={index} />)}</div>}
      {!records.length && !item.reason && <p className="pipeline-detail-muted">状态已由数据库记录核验；当前接口未返回单条样本，可通过下方核验入口或对应业务页面继续查看。</p>}
      {apiUrl && <a className="pipeline-api-link" href={apiUrl} target="_blank" rel="noopener noreferrer">打开核验接口 ↗</a>}
    </div>
  </details>;
}

export function StockPipelineStatusDialog({
  stock,
  kind,
  summary,
  onClose,
}: {
  stock: StockSymbol;
  kind: StockPipelineStatusKey;
  summary: StockPipelineStatus;
  onClose: () => void;
}) {
  const [detail, setDetail] = useState<StockPipelineStatusDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    api.getStockPipelineStatusDetail(stock.market, stock.symbol, 5, controller.signal)
      .then(setDetail)
      .catch((reason) => {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : "状态详情读取失败");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [stock.market, stock.symbol]);

  useEffect(() => {
    const close = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [onClose]);

  const stage = useMemo(
    () => detail?.stages?.[kind] || fallbackStage(kind, summary),
    [detail, kind, summary],
  );
  const stageSummary = stage.summary || summary;
  const completed = stage.completed_items || [];
  const incomplete = stage.incomplete_items || [];
  const verificationRecords = stage.verification?.records || [];
  const itemRecordsAvailable = [...completed, ...incomplete].some((item) => (item.records || []).length > 0);
  const completedCount = stageSummary.completed_count ?? completed.length;
  const expectedCount = stageSummary.expected_count ?? completed.length + incomplete.length;

  return <div className="resource-dialog-backdrop pipeline-status-backdrop" role="presentation" onMouseDown={(event) => {
    if (event.target === event.currentTarget) onClose();
  }}>
    <section className="resource-dialog pipeline-status-dialog" role="dialog" aria-modal="true" aria-label={`${stock.name}${stageNames[kind]}详情`}>
      <header className="resource-dialog-header">
        <div><p className="eyebrow">PIPELINE STATUS DETAIL</p><h3>{stock.name} · {stageNames[kind]}</h3><p>{stock.market}:{stock.symbol} · 基于数据库实际记录核验</p></div>
        <button className="quiet-button" type="button" onClick={onClose}>关闭</button>
      </header>
      <div className="resource-dialog-body pipeline-status-body">
        <section className={`pipeline-status-summary ${statusClass(stageSummary.status)}`}>
          <div><span className="pipeline-summary-dot" /><strong>{stageSummary.label || statusLabel(stageSummary.status)}</strong><small>{completedCount} / {expectedCount} 项</small></div>
          <p>{stage.label || stageNames[kind]}状态由持久化业务记录、知识文档与切片、图谱实体和关系计算，不依赖任务页面的历史提示。</p>
          {stageSummary.last_at && <time>最近更新：{formatDate(stageSummary.last_at)}</time>}
        </section>

        {loading && <div className="pipeline-detail-message" role="status">正在读取完成项、缺失项和核验样本…</div>}
        {error && <div className="pipeline-detail-message warning" role="status">详情接口暂时无法读取：{error}。以下先展示主数据列表中的持久化计数。</div>}

        <section className="pipeline-detail-section">
          <header><div><h4>已完成内容</h4><p>点击具体项目可查看记录数量、更新时间、样本数据和核验入口。</p></div><span>{completed.length} 项</span></header>
          <div className="pipeline-detail-list">
            {completed.map((item) => <DetailItem key={item.code} item={item} />)}
            {!completed.length && <p className="pipeline-detail-empty">该阶段尚无已完成项目。</p>}
          </div>
        </section>

        <section className="pipeline-detail-section incomplete-section">
          <header><div><h4>未完成 / 待处理</h4><p>展示缺失内容、判断原因及建议处理方式。</p></div><span>{incomplete.length} 项</span></header>
          <div className="pipeline-detail-list">
            {incomplete.map((item) => <DetailItem key={item.code} item={item} incomplete />)}
            {!incomplete.length && <p className="pipeline-detail-empty success">当前口径下没有未完成项目。</p>}
          </div>
        </section>

        {!!verificationRecords.length && !itemRecordsAvailable && <section className="pipeline-detail-section">
          <header><div><h4>阶段核验样本</h4><p>接口返回 {stage.verification?.total_count?.toLocaleString() || verificationRecords.length} 条可核验记录，当前展示最多 {verificationRecords.length} 条。</p></div></header>
          <div className="pipeline-verification-list">{verificationRecords.map((record, index) => <VerificationRecord key={`${record.record_id ?? index}:${index}`} record={record} index={index} />)}</div>
        </section>}

        {!!stage.notes?.length && <section className="pipeline-detail-notes"><strong>口径说明</strong><ul>{stage.notes.map((note, index) => <li key={index}>{note}</li>)}</ul></section>}
      </div>
    </section>
  </div>;
}

export default StockPipelineStatusDialog;
