import { Play, RefreshCw } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  api,
  type StockPipelineDetailItem,
  type StockPipelineStageDetail,
  type StockPipelineStatus,
  type StockPipelineStatusDetail,
  type StockPipelineVerificationRecord,
  type StockSymbol,
} from "./api";
import {
  batchGovernanceApi,
  type BatchGovernanceJob,
} from "./batchGovernanceApi";
import "./StockPipelineStatusDialog.css";

export type StockPipelineStatusKey = "data_collection" | "knowledge_base" | "knowledge_graph";

const stageNames: Record<StockPipelineStatusKey, string> = {
  data_collection: "数据采集",
  knowledge_base: "知识库",
  knowledge_graph: "知识图谱",
};

const statusNames: Record<string, string> = {
  NOT_STARTED: "未开始",
  QUEUED: "已进入队列",
  WAITING: "等待处理",
  PENDING: "处理中",
  RETRY: "等待重试",
  RUNNING: "执行中",
  STALE: "执行超时",
  PARTIAL: "部分完成",
  COMPLETED: "已完成",
  SUCCESS: "已完成",
  EMPTY: "无有效数据",
  FAILED: "失败",
  CANCELLED: "已取消",
};

const terminalJobStatuses = new Set(["SUCCESS", "COMPLETED", "PARTIAL", "FAILED", "CANCELLED"]);

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
  if (["PARTIAL", "PENDING", "QUEUED", "RETRY", "RUNNING", "WAITING"].includes(normalized)) return "partial";
  if (["FAILED", "CANCELLED", "STALE"].includes(normalized)) return "failed";
  return "empty";
}

function statusLabel(status?: string | null) {
  const normalized = String(status || "NOT_STARTED").toUpperCase();
  return statusNames[normalized] || normalized;
}

function taskStageLabel(stage?: string | null) {
  const normalized = String(stage || "").toUpperCase();
  if (normalized.includes("BUSINESS")) return "业务数据采集";
  if (normalized.includes("LAKEHOUSE")) return "湖仓数据发布";
  if (normalized.includes("DOCUMENT") || normalized.includes("CHUNK")) return "知识文档切片";
  if (normalized.includes("GRAPH")) return "知识图谱处理";
  if (["QUEUED", "PENDING", "WAITING"].includes(normalized)) return "等待调度";
  return stage || "等待调度";
}

function jobLifecycleStatus(job?: BatchGovernanceJob | null) {
  return String(job?.job_status || job?.status || "WAITING").toUpperCase();
}

function isTerminalJob(job?: BatchGovernanceJob | null) {
  return !!job && terminalJobStatuses.has(jobLifecycleStatus(job));
}

function normalizedJobStatus(job?: BatchGovernanceJob | null) {
  const lifecycle = jobLifecycleStatus(job);
  return isTerminalJob(job) && job?.result_status
    ? String(job.result_status).toUpperCase()
    : lifecycle;
}

function jobProgress(job?: BatchGovernanceJob | null) {
  if (isTerminalJob(job) && job?.progress == null) return 100;
  const raw = Number(job?.progress ?? 0);
  if (!Number.isFinite(raw)) return 0;
  return Math.max(0, Math.min(100, raw > 0 && raw <= 1 ? raw * 100 : raw));
}

function jobError(job?: BatchGovernanceJob | null) {
  const errors = [job?.error_message, ...(job?.errors || [])]
    .filter(Boolean)
    .map((value) => typeof value === "string" ? value : JSON.stringify(value));
  return [...new Set(errors)].join("；");
}

function continuationIdempotencyKey() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `stock-pipeline-continue-${crypto.randomUUID()}`;
  }
  return `stock-pipeline-continue-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
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
      "KNOWLEDGE_DOCUMENTS",
      "知识文档",
      "当前股票尚未生成可追溯的知识文档。",
      "先把新闻、公告、财报或F10等业务记录物化为知识文档。",
    ));
    if (documentCount > 0 && documentsWithChunks >= documentCount) {
      completed.push(completedItem("DOCUMENT_CHUNKS", "文档切片", chunkCount, summary.last_at));
    } else {
      incomplete.push({
        ...missingItem(
          "DOCUMENT_CHUNKS",
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

function DetailItem({
  item,
  incomplete = false,
  continuing = false,
  continueDisabled = false,
  canContinue = true,
  blockedReason,
  onContinue,
}: {
  item: StockPipelineDetailItem;
  incomplete?: boolean;
  continuing?: boolean;
  continueDisabled?: boolean;
  canContinue?: boolean;
  blockedReason?: string | null;
  onContinue?: () => void;
}) {
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
      {incomplete && !canContinue && blockedReason && <p className="pipeline-detail-blocked"><b>暂不可续作：</b>{blockedReason}</p>}
      {latestAttempt && <details className="pipeline-latest-attempt"><summary>查看最近一次执行信息</summary><LatestAttempt value={latestAttempt} /></details>}
      {!!records.length && <div className="pipeline-verification-list">{records.map((record, index) => <VerificationRecord key={`${record.record_id ?? index}:${index}`} record={record} index={index} />)}</div>}
      {!records.length && !item.reason && <p className="pipeline-detail-muted">状态已由数据库记录核验；当前接口未返回单条样本，可通过下方核验入口或对应业务页面继续查看。</p>}
      <div className="pipeline-detail-item-actions">
        {apiUrl && <a className="pipeline-api-link" href={apiUrl} target="_blank" rel="noopener noreferrer">打开核验接口 ↗</a>}
        {incomplete && canContinue && <button
          className="pipeline-continue-button"
          type="button"
          disabled={continueDisabled || !canContinue}
          onClick={onContinue}
          title={!canContinue ? blockedReason || "当前项目暂不支持自动续作" : undefined}
        >
          {continuing ? <RefreshCw size={14} aria-hidden="true" className="spin" /> : <Play size={14} aria-hidden="true" />}
          {continuing ? "正在提交…" : "继续完成此项"}
        </button>}
      </div>
    </div>
  </details>;
}

export function StockPipelineStatusDialog({
  stock,
  kind,
  summary,
  onClose,
  onCompleted,
}: {
  stock: StockSymbol;
  kind: StockPipelineStatusKey;
  summary: StockPipelineStatus;
  onClose: () => void;
  onCompleted?: (job: BatchGovernanceJob) => void;
}) {
  const [detail, setDetail] = useState<StockPipelineStatusDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [continuingKey, setContinuingKey] = useState("");
  const [continuationJob, setContinuationJob] = useState<BatchGovernanceJob | null>(null);
  const [continuationError, setContinuationError] = useState("");
  const [refreshingAfterJob, setRefreshingAfterJob] = useState(false);
  const continuationLock = useRef(false);
  const continuationKeys = useRef<Record<string, string>>({});
  const completedJob = useRef<number | null>(null);
  const onCompletedRef = useRef(onCompleted);

  useEffect(() => {
    onCompletedRef.current = onCompleted;
  }, [onCompleted]);

  const loadDetail = useCallback(async (signal?: AbortSignal) => {
    const next = await api.getStockPipelineStatusDetail(stock.market, stock.symbol, 5, signal);
    setDetail(next);
    return next;
  }, [stock.market, stock.symbol]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    loadDetail(controller.signal)
      .catch((reason) => {
        if (!controller.signal.aborted) setError(reason instanceof Error ? reason.message : "状态详情读取失败");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [loadDetail]);

  useEffect(() => {
    continuationLock.current = false;
    continuationKeys.current = {};
    completedJob.current = null;
    setContinuingKey("");
    setContinuationJob(null);
    setContinuationError("");
    setRefreshingAfterJob(false);
  }, [kind, stock.market, stock.symbol]);

  const continuationActive = !!continuationJob && !isTerminalJob(continuationJob);

  useEffect(() => {
    if (!continuationJob?.job_id || !continuationActive) return;
    let disposed = false;
    let timer: number | undefined;
    const controller = new AbortController();
    const poll = async () => {
      try {
        const next = await batchGovernanceApi.get(continuationJob.job_id, controller.signal);
        if (disposed) return;
        setContinuationJob(next);
        setContinuationError("");
        if (!isTerminalJob(next)) timer = window.setTimeout(poll, 1400);
      } catch (reason) {
        if (disposed || controller.signal.aborted) return;
        setContinuationError(`任务进度读取失败：${reason instanceof Error ? reason.message : "未知错误"}`);
        timer = window.setTimeout(poll, 3000);
      }
    };
    timer = window.setTimeout(poll, 900);
    return () => {
      disposed = true;
      controller.abort();
      if (timer) window.clearTimeout(timer);
    };
  }, [continuationActive, continuationJob?.job_id]);

  useEffect(() => {
    if (!continuationJob?.job_id || !isTerminalJob(continuationJob)) return;
    if (completedJob.current === continuationJob.job_id) return;
    completedJob.current = continuationJob.job_id;
    const controller = new AbortController();
    setRefreshingAfterJob(true);
    loadDetail(controller.signal)
      .then(() => setError(""))
      .catch((reason) => {
        if (!controller.signal.aborted) {
          setContinuationError(`任务已结束，但状态重新核验失败：${reason instanceof Error ? reason.message : "未知错误"}`);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setRefreshingAfterJob(false);
          onCompletedRef.current?.(continuationJob);
        }
      });
    return () => controller.abort();
  }, [continuationJob, loadDetail]);

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
  const actionableIncomplete = detail
    ? incomplete.filter((item) => item.can_continue === true && !!item.continuation_action)
    : [];
  const continuationActionCount = new Set(
    actionableIncomplete.map((item) => item.continuation_action),
  ).size;
  const canContinueAll = actionableIncomplete.length > 0 && continuationActionCount === 1;
  const verificationRecords = stage.verification?.records || [];
  const itemRecordsAvailable = [...completed, ...incomplete].some((item) => (item.records || []).length > 0);
  const completedCount = stageSummary.completed_count ?? completed.length;
  const expectedCount = stageSummary.expected_count ?? completed.length + incomplete.length;
  const taskBusy = !!continuingKey || continuationActive || refreshingAfterJob;
  const progress = jobProgress(continuationJob);
  const taskError = jobError(continuationJob);

  async function continueIncomplete(itemCodes: string[], actionKey: string) {
    if (!itemCodes.length || continuationLock.current || continuationActive) return;
    continuationLock.current = true;
    setContinuingKey(actionKey);
    setContinuationError("");
    const fingerprint = `${stock.market}:${stock.symbol}:${kind}:${[...itemCodes].sort().join(",")}`;
    const idempotencyKey = continuationKeys.current[fingerprint] || continuationIdempotencyKey();
    continuationKeys.current[fingerprint] = idempotencyKey;
    try {
      const created = await batchGovernanceApi.continueStockPipeline(stock.market, stock.symbol, {
        stage: kind,
        item_codes: itemCodes,
        idempotency_key: idempotencyKey,
      });
      delete continuationKeys.current[fingerprint];
      completedJob.current = null;
      setContinuationJob(created);
    } catch (reason) {
      setContinuationError(reason instanceof Error ? reason.message : "续作任务提交失败");
    } finally {
      continuationLock.current = false;
      setContinuingKey("");
    }
  }

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
        {continuationJob && <section className={`pipeline-continuation-progress ${statusClass(normalizedJobStatus(continuationJob))}`} aria-live="polite">
          <header>
            <div><strong>续作任务 #{continuationJob.job_id}</strong><small>当前阶段：{taskStageLabel(continuationJob.current_stage)}</small></div>
            <span>{statusLabel(normalizedJobStatus(continuationJob))}</span>
          </header>
          <div
            className="pipeline-continuation-track"
            role="progressbar"
            aria-label="续作任务进度"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(progress)}
          ><span style={{ width: `${progress}%` }} /></div>
          <footer><span>{refreshingAfterJob ? "任务已结束，正在重新核验数据库状态…" : continuationActive ? "任务正在后台执行，本页会自动更新。" : "任务已结束，已按数据库实际记录重新核验。"}</span><b>{Math.round(progress)}%</b></footer>
          {taskError && <p className="pipeline-continuation-error">{taskError}</p>}
        </section>}
        {continuationError && <div className="pipeline-detail-message warning" role="alert">{continuationError}</div>}

        <section className="pipeline-detail-section">
          <header><div><h4>已完成内容</h4><p>点击具体项目可查看记录数量、更新时间、样本数据和核验入口。</p></div><span>{completed.length} 项</span></header>
          <div className="pipeline-detail-list">
            {completed.map((item) => <DetailItem key={item.code} item={item} />)}
            {!completed.length && <p className="pipeline-detail-empty">该阶段尚无已完成项目。</p>}
          </div>
        </section>

        <section className="pipeline-detail-section incomplete-section">
          <header>
            <div><h4>未完成 / 待处理</h4><p>展示缺失内容、判断原因及建议处理方式。</p></div>
            <div className="pipeline-section-actions">
              <span>{incomplete.length} 项</span>
              {!!incomplete.length && <button
                className="pipeline-continue-button"
                type="button"
                disabled={taskBusy || loading || !canContinueAll}
                title={!actionableIncomplete.length
                  ? "当前未完成项需要先处理其前置条件，暂不能自动续作"
                  : continuationActionCount > 1
                    ? "未完成项需要不同的续作动作，请先逐项处理"
                    : undefined}
                onClick={() => void continueIncomplete(actionableIncomplete.map((item) => item.code), "all")}
              >
                {continuingKey === "all" ? <RefreshCw size={14} aria-hidden="true" className="spin" /> : <Play size={14} aria-hidden="true" />}
                {continuingKey === "all" ? "正在提交…" : "继续完成全部"}
              </button>}
            </div>
          </header>
          <div className="pipeline-detail-list">
            {incomplete.map((item) => <DetailItem
              key={item.code}
              item={item}
              incomplete
              continuing={continuingKey === item.code}
              continueDisabled={taskBusy || loading}
              canContinue={!!detail && item.can_continue === true && !!item.continuation_action}
              blockedReason={!detail
                ? "状态详情未成功读取，无法安全判断续作范围。"
                : item.can_continue === true && !item.continuation_action
                  ? "服务端未返回安全的续作动作，请刷新后重试。"
                  : item.blocked_reason}
              onContinue={detail && item.can_continue === true && item.continuation_action
                ? () => void continueIncomplete([item.code], item.code)
                : undefined}
            />)}
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
