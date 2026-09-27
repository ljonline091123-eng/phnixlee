import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import type { KnowledgeBase, KnowledgeGraph, StockSymbol } from "./api";
import {
  batchGovernanceApi,
  type BatchGovernanceBusinessType,
  type BatchGovernanceJob,
  type BatchGovernanceRequest,
  type BatchGovernanceStageResult,
  type BatchGovernanceStockResult,
} from "./batchGovernanceApi";
import { knowledgePipelineApi } from "./knowledgePipelineApi";
import "./BatchStockGovernanceDialog.css";

const businessTypeOptions: Array<{
  code: BatchGovernanceBusinessType;
  name: string;
  description: string;
}> = [
  { code: "QUOTE", name: "实时行情", description: "最新价格、成交量与换手率" },
  { code: "KLINE", name: "历史量价", description: "日线、成交量与区间行情" },
  { code: "FINANCIAL", name: "财务数据", description: "财务报告与标准指标" },
  { code: "NEWS", name: "新闻", description: "股票相关新闻与原文" },
  { code: "NOTICE", name: "公告", description: "上市公司公告与披露" },
  { code: "F10", name: "F10资料", description: "公司资料、股东与业务构成" },
];

const allBusinessTypes = businessTypeOptions.map((item) => item.code);
const terminalStatuses = new Set(["SUCCESS", "COMPLETED", "PARTIAL", "FAILED", "CANCELLED"]);

const statusNames: Record<string, string> = {
  PENDING: "等待执行",
  QUEUED: "已进入队列",
  WAITING: "等待执行",
  RUNNING: "执行中",
  SUCCESS: "已完成",
  COMPLETED: "已完成",
  PARTIAL: "部分完成",
  FAILED: "执行失败",
  SKIPPED: "未启用",
  CANCELLED: "已取消",
};

const stageDefinitions = [
  {
    code: "BUSINESS_DATA",
    aliases: ["BUSINESS_DATA", "BUSINESS_DATA_COLLECTION", "COLLECT_BUSINESS_DATA"],
    title: "业务数据采集",
    description: "按股票获取行情、财务、新闻、公告和F10资料。",
  },
  {
    code: "LAKEHOUSE",
    aliases: ["LAKEHOUSE", "LAKEHOUSE_EXPORT", "KNOWLEDGE_PIPELINE"],
    title: "湖仓数据发布",
    description: "完成质量检查并发布分层数据集、对象和血缘。",
  },
  {
    code: "KNOWLEDGE_BASE",
    aliases: ["KNOWLEDGE_BASE", "DOCUMENT_CHUNKS", "ARCHIVE_CHUNKS", "KNOWLEDGE_PIPELINE"],
    title: "知识库与切片",
    description: "归档证据文档，识别章节并形成可追溯切片。",
  },
  {
    code: "KNOWLEDGE_GRAPH",
    aliases: ["KNOWLEDGE_GRAPH", "GRAPH", "GRAPH_PROJECTION", "KNOWLEDGE_PIPELINE"],
    title: "知识图谱投影",
    description: "构建本次股票范围的独立图谱投影，等待后续治理。",
  },
] as const;

type StageFlags = {
  collectBusinessData: boolean;
  exportLakehouse: boolean;
  archiveChunks: boolean;
  runGraph: boolean;
};

export type BatchStockGovernanceDialogProps = {
  open: boolean;
  stocks: StockSymbol[];
  onClose: () => void;
  onNotify?: (message: string) => void;
  onCompleted?: (job: BatchGovernanceJob) => void;
};

function normalizeStatus(status?: string) {
  return String(status || "WAITING").toUpperCase();
}

function statusLabel(status?: string) {
  const normalized = normalizeStatus(status);
  return statusNames[normalized] || normalized;
}

function statusClass(status?: string) {
  const normalized = normalizeStatus(status);
  if (["SUCCESS", "COMPLETED"].includes(normalized)) return "success";
  if (["FAILED", "CANCELLED"].includes(normalized)) return "failed";
  if (normalized === "PARTIAL") return "partial";
  if (normalized === "RUNNING") return "running";
  return "waiting";
}

function isProjectionGraph(graph: KnowledgeGraph) {
  return /_PIPE_\d+$/i.test(graph.graph_code);
}

function createIdempotencyKey() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return `master-batch-${crypto.randomUUID()}`;
  return `master-batch-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function readStageResult(job: BatchGovernanceJob | null, aliases: readonly string[]) {
  if (!job?.stage_results) return undefined;
  const normalizedAliases = aliases.map((item) => item.replace(/[^A-Z0-9]/gi, "").toUpperCase());
  return Object.entries(job.stage_results).find(([key]) => {
    const normalizedKey = key.replace(/[^A-Z0-9]/gi, "").toUpperCase();
    return normalizedAliases.some((alias) => normalizedKey === alias || normalizedKey.includes(alias));
  })?.[1];
}

function countValue(result: BatchGovernanceStageResult | undefined, key: "processed" | "succeeded" | "failed") {
  const value = result?.[key];
  return typeof value === "number" ? value : undefined;
}

function stageSummary(result?: BatchGovernanceStageResult) {
  if (!result) return "等待服务端返回阶段结果";
  if (result.message) return result.message;
  const segments = [
    countValue(result, "processed") != null ? `处理 ${countValue(result, "processed")}` : "",
    countValue(result, "succeeded") != null ? `成功 ${countValue(result, "succeeded")}`
      : typeof result.success_count === "number" ? `成功 ${result.success_count}` : "",
    countValue(result, "failed") != null ? `失败 ${countValue(result, "failed")}` : "",
  ].filter(Boolean);
  return segments.join(" · ") || "阶段结果已记录";
}

function errorMessage(error: string | Record<string, unknown>) {
  if (typeof error === "string") return error;
  const scope = [error.market, error.symbol, error.data_type].filter(Boolean).join(" · ");
  const message = String(error.message || error.error || JSON.stringify(error));
  return scope ? `${scope}：${message}` : message;
}

function stockErrorMessage(errors?: BatchGovernanceStockResult["errors"]) {
  return (errors || []).map((item) => typeof item === "string" ? item : item.message || JSON.stringify(item)).join("；");
}

function progressValue(job: BatchGovernanceJob | null) {
  if (job && terminalStatuses.has(normalizeStatus(job.status)) && job.progress == null) return 100;
  const raw = Number(job?.progress ?? 0);
  if (!Number.isFinite(raw)) return 0;
  return Math.max(0, Math.min(100, raw > 0 && raw <= 1 ? raw * 100 : raw));
}

function stageLabel(stage?: string | null) {
  const normalized = String(stage || "").toUpperCase();
  return stageDefinitions.find((item) => item.aliases.some((alias) => normalized.includes(alias)))?.title || stage || "等待调度";
}

export function BatchStockGovernanceDialog({
  open,
  stocks,
  onClose,
  onNotify,
  onCompleted,
}: BatchStockGovernanceDialogProps) {
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [graphs, setGraphs] = useState<KnowledgeGraph[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState<number | null>(null);
  const [graphId, setGraphId] = useState<number | null>(null);
  const [flags, setFlags] = useState<StageFlags>({
    collectBusinessData: true,
    exportLakehouse: true,
    archiveChunks: true,
    runGraph: true,
  });
  const [businessTypes, setBusinessTypes] = useState<BatchGovernanceBusinessType[]>(allBusinessTypes);
  const [runAgentGovernance, setRunAgentGovernance] = useState(false);
  const [klineDays, setKlineDays] = useState(365);
  const [disclosureDays, setDisclosureDays] = useState(730);
  const [submitting, setSubmitting] = useState(false);
  const [job, setJob] = useState<BatchGovernanceJob | null>(null);
  const [recentJobs, setRecentJobs] = useState<BatchGovernanceJob[]>([]);
  const [recentLoading, setRecentLoading] = useState(false);
  const [error, setError] = useState("");
  const completionNotified = useRef<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setCatalogLoading(true);
    setCatalogError("");
    knowledgePipelineApi.catalog(controller.signal)
      .then((catalog) => {
        const bases = catalog.knowledgeBases.filter((item) => item.enabled);
        setKnowledgeBases(bases);
        setGraphs(catalog.graphs.filter((item) => item.enabled && !isProjectionGraph(item)));
        setKnowledgeBaseId((current) => current ?? bases[0]?.id ?? null);
      })
      .catch((reason) => {
        if (!controller.signal.aborted) setCatalogError(reason instanceof Error ? reason.message : "知识资源读取失败");
      })
      .finally(() => {
        if (!controller.signal.aborted) setCatalogLoading(false);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    setRecentLoading(true);
    batchGovernanceApi.list(8, controller.signal)
      .then((result) => setRecentJobs(Array.isArray(result) ? result : result.items))
      .catch((reason) => {
        if (!controller.signal.aborted) setError(`最近任务读取失败：${reason instanceof Error ? reason.message : "未知错误"}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setRecentLoading(false);
      });
    return () => controller.abort();
  }, [open]);

  const graphOptions = useMemo(
    () => graphs.filter((item) => !knowledgeBaseId || item.knowledge_base_id === knowledgeBaseId),
    [graphs, knowledgeBaseId],
  );

  useEffect(() => {
    setGraphId((current) => graphOptions.some((item) => item.id === current) ? current : graphOptions[0]?.id ?? null);
  }, [graphOptions]);

  const activeJob = !!job && !terminalStatuses.has(normalizeStatus(job.status));

  useEffect(() => {
    if (!job?.job_id || !activeJob) return;
    let disposed = false;
    let timer: number | undefined;
    const controller = new AbortController();
    const poll = async () => {
      try {
        const next = await batchGovernanceApi.get(job.job_id, controller.signal);
        if (disposed) return;
        setJob(next);
        setError("");
        if (!terminalStatuses.has(normalizeStatus(next.status))) {
          timer = window.setTimeout(poll, 1400);
        }
      } catch (reason) {
        if (disposed || controller.signal.aborted) return;
        setError(`任务进度读取失败：${reason instanceof Error ? reason.message : "未知错误"}`);
        timer = window.setTimeout(poll, 3000);
      }
    };
    timer = window.setTimeout(poll, 900);
    return () => {
      disposed = true;
      controller.abort();
      if (timer) window.clearTimeout(timer);
    };
  }, [activeJob, job?.job_id]);

  useEffect(() => {
    if (!job?.job_id || !terminalStatuses.has(normalizeStatus(job.status))) return;
    if (completionNotified.current === job.job_id) return;
    completionNotified.current = job.job_id;
    setRecentJobs((current) => [job, ...current.filter((item) => item.job_id !== job.job_id)].slice(0, 8));
    onCompleted?.(job);
    onNotify?.(`批量任务 #${job.job_id} ${statusLabel(job.result_status || job.status)}，已处理 ${job.stock_count} 只股票。`);
  }, [job, onCompleted, onNotify]);

  const enabledStageCount = [
    flags.collectBusinessData,
    flags.exportLakehouse,
    flags.archiveChunks,
    flags.runGraph,
  ].filter(Boolean).length;

  function updateFlag(name: keyof StageFlags, checked: boolean) {
    setFlags((current) => {
      if (name === "runGraph" && checked) {
        return { ...current, runGraph: true, exportLakehouse: true, archiveChunks: true };
      }
      return { ...current, [name]: checked };
    });
  }

  function toggleBusinessType(code: BatchGovernanceBusinessType) {
    setBusinessTypes((current) => current.includes(code)
      ? current.filter((item) => item !== code)
      : [...current, code]);
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (!stocks.length) {
      setError("请先在股票主数据列表中勾选股票。");
      return;
    }
    if (stocks.length > 30) {
      setError("单次最多处理30只股票，请缩小选择范围。");
      return;
    }
    if (!enabledStageCount) {
      setError("请至少选择一个采集或治理环节。");
      return;
    }
    if (flags.collectBusinessData && !businessTypes.length) {
      setError("已启用业务数据采集，请至少选择一种业务数据。");
      return;
    }
    const payload: BatchGovernanceRequest = {
      stocks: stocks.map((stock) => ({ market: stock.market, symbol: stock.symbol })),
      collect_business_data: flags.collectBusinessData,
      export_lakehouse: flags.exportLakehouse,
      archive_chunks: flags.archiveChunks,
      run_graph: flags.runGraph,
      run_agent_governance: runAgentGovernance,
      business_types: businessTypes,
      knowledge_base_id: knowledgeBaseId,
      graph_id: flags.runGraph ? graphId : null,
      kline_days: Math.max(30, Math.min(3650, klineDays || 365)),
      disclosure_days: Math.max(30, Math.min(3650, disclosureDays || 730)),
      idempotency_key: createIdempotencyKey(),
    };
    setSubmitting(true);
    setError("");
    try {
      const created = await batchGovernanceApi.create(payload);
      completionNotified.current = null;
      setJob(created);
      setRecentJobs((current) => [created, ...current.filter((item) => item.job_id !== created.job_id)].slice(0, 8));
      onNotify?.(`已提交批量采集与治理任务 #${created.job_id}。`);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "批量任务提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  async function openRecentJob(item: BatchGovernanceJob) {
    setError("");
    try {
      const detail = await batchGovernanceApi.get(item.job_id);
      completionNotified.current = terminalStatuses.has(normalizeStatus(detail.status)) ? detail.job_id : null;
      setJob(detail);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "任务详情读取失败");
    }
  }

  if (!open) return null;

  const selectedKnowledgeBase = knowledgeBases.find((item) => item.id === knowledgeBaseId);
  const selectedGraph = graphs.find((item) => item.id === graphId);
  const currentStage = String(job?.current_stage || "").toUpperCase();
  const progress = progressValue(job);
  const effectiveOptions = job?.effective_options;
  const activeJobError = job?.job_status === "FAILED" || job?.job_status === "RETRY"
    ? job.error_message
    : null;
  const jobErrors = [activeJobError, ...(job?.errors || [])].filter((item): item is string | Record<string, unknown> => !!item);

  return <div className="resource-dialog-backdrop batch-governance-backdrop" role="presentation" onMouseDown={(event) => {
    if (event.target === event.currentTarget) onClose();
  }}>
    <section className="resource-dialog batch-governance-dialog" role="dialog" aria-modal="true" aria-label="批量采集与治理">
      <header className="resource-dialog-header">
        <div>
          <p className="eyebrow">BATCH DATA GOVERNANCE</p>
          <h3>批量采集与治理</h3>
          <p>已选择 {stocks.length} 只股票，一次完成业务数据采集、湖仓发布、知识库加工与图谱投影。</p>
        </div>
        <button className="quiet-button" type="button" onClick={onClose}>关闭</button>
      </header>

      <div className="resource-dialog-body batch-governance-body">
        <section className="batch-governance-scope" aria-label="已选择股票">
          <div><strong>处理范围</strong><span>{stocks.length} / 30 只</span></div>
          <div className="batch-governance-stock-list">
            {stocks.slice(0, 18).map((stock) => <span key={`${stock.market}:${stock.symbol}`}>
              <b>{stock.name}</b><code>{stock.market}:{stock.symbol}</code>
            </span>)}
            {stocks.length > 18 && <span className="more">其余 {stocks.length - 18} 只</span>}
          </div>
        </section>

        {catalogError && <div className="batch-governance-message warning" role="status">知识资源目录暂时无法读取：{catalogError}。仍可只采集业务数据或发布湖仓。</div>}
        {error && <div className="batch-governance-message error" role="alert">{error}</div>}

        <form className="batch-governance-form" onSubmit={submit}>
          <fieldset className="batch-governance-stages" disabled={submitting || activeJob}>
            <legend>选择加工环节</legend>
            <label className={flags.collectBusinessData ? "selected" : ""}>
              <input type="checkbox" checked={flags.collectBusinessData} onChange={(event) => updateFlag("collectBusinessData", event.target.checked)} />
              <span><b>1</b><strong>业务数据</strong><small>从已配置数据源按股票增量采集</small></span>
            </label>
            <label className={flags.exportLakehouse ? "selected" : ""}>
              <input type="checkbox" checked={flags.exportLakehouse} disabled={flags.runGraph} onChange={(event) => updateFlag("exportLakehouse", event.target.checked)} />
              <span><b>2</b><strong>湖仓数据</strong><small>质量门禁、分层数据集与统一血缘</small></span>
            </label>
            <label className={flags.archiveChunks ? "selected" : ""}>
              <input type="checkbox" checked={flags.archiveChunks} disabled={flags.runGraph} onChange={(event) => updateFlag("archiveChunks", event.target.checked)} />
              <span><b>3</b><strong>知识库</strong><small>归档当前已有证据文档并执行章节切片</small></span>
            </label>
            <label className={flags.runGraph ? "selected" : ""}>
              <input type="checkbox" checked={flags.runGraph} onChange={(event) => updateFlag("runGraph", event.target.checked)} />
              <span><b>4</b><strong>知识图谱</strong><small>生成独立范围投影，不覆盖历史图谱</small></span>
            </label>
          </fieldset>

          <section className={`batch-governance-business-types${flags.collectBusinessData ? "" : " disabled"}`}>
            <div className="batch-governance-section-heading">
              <div><strong>业务数据范围</strong><small>不同市场将自动使用对应的已启用接口；无法获取的数据会记录到逐股结果中。</small></div>
              <button type="button" disabled={!flags.collectBusinessData || submitting || activeJob} onClick={() => setBusinessTypes(businessTypes.length === allBusinessTypes.length ? [] : allBusinessTypes)}>
                {businessTypes.length === allBusinessTypes.length ? "取消全选" : "全部选择"}
              </button>
            </div>
            <div className="batch-governance-type-grid">
              {businessTypeOptions.map((item) => <label key={item.code} className={businessTypes.includes(item.code) ? "selected" : ""}>
                <input
                  type="checkbox"
                  checked={businessTypes.includes(item.code)}
                  disabled={!flags.collectBusinessData || submitting || activeJob}
                  onChange={() => toggleBusinessType(item.code)}
                />
                <span><strong>{item.name}</strong><small>{item.description}</small></span>
              </label>)}
            </div>
          </section>

          <div className="batch-governance-settings">
            <label>目标知识库
              <select value={knowledgeBaseId ?? ""} disabled={catalogLoading || submitting || activeJob} onChange={(event) => setKnowledgeBaseId(event.target.value ? Number(event.target.value) : null)}>
                <option value="">由系统自动选择</option>
                {knowledgeBases.map((item) => <option key={item.id} value={item.id}>{item.kb_name}（{item.kb_code}）</option>)}
              </select>
              <small>{selectedKnowledgeBase ? `版本 ${selectedKnowledgeBase.version}` : "使用后端默认知识库"}</small>
            </label>
            <label>基础知识图谱
              <select value={graphId ?? ""} disabled={!flags.runGraph || catalogLoading || submitting || activeJob} onChange={(event) => setGraphId(event.target.value ? Number(event.target.value) : null)}>
                <option value="">由系统自动选择</option>
                {graphOptions.map((item) => <option key={item.id} value={item.id}>{item.graph_name}（#{item.id}）</option>)}
              </select>
              <small>{selectedGraph ? `${selectedGraph.entity_count} 个实体 · ${selectedGraph.relation_count} 条关系` : "使用后端默认基础图谱"}</small>
            </label>
            <label>历史量价区间（天）
              <input type="number" min={30} max={3650} value={klineDays} disabled={submitting || activeJob} onChange={(event) => setKlineDays(Number(event.target.value))} />
              <small>默认回看365天</small>
            </label>
            <label>披露资料区间（天）
              <input type="number" min={30} max={3650} value={disclosureDays} disabled={submitting || activeJob} onChange={(event) => setDisclosureDays(Number(event.target.value))} />
              <small>新闻、公告默认回看730天</small>
            </label>
          </div>

          <label className="batch-governance-agent-option">
            <input type="checkbox" checked={runAgentGovernance} disabled={submitting || activeJob} onChange={(event) => setRunAgentGovernance(event.target.checked)} />
            <span><strong>执行智能体 + Skill治理（可选）</strong><small>当前能力是全局有界来源审查，不等同于只审核本次所选股票；模型抽取结果仍保持候选状态。</small></span>
          </label>

          {flags.archiveChunks && !flags.runGraph && <div className="batch-governance-message warning">
            本次未选择知识图谱。知识库环节只会切分当前已经存在的 KnowledgeDocument，不会自动把刚采集的全部业务记录转换成新知识文档。
          </div>}

          {flags.runGraph && <div className="batch-governance-message warning">
            知识图谱会自动包含湖仓发布和知识库切片两个前置环节。新图谱是独立范围投影，初始状态为“待治理（PENDING）”，不会覆盖或自动发布为历史已验证图谱。
          </div>}

          <div className="batch-governance-submit">
            <div><strong>{stocks.length} 只股票 · {enabledStageCount} 个加工环节</strong><span>任务在后台执行，关闭窗口不会中断；本次会话再次打开可继续查看。</span></div>
            <button className="primary-button" type="submit" disabled={submitting || activeJob || !stocks.length}>
              {submitting ? "正在提交…" : activeJob ? "任务执行中…" : job ? "再次发起新任务" : "开始批量采集与治理"}
            </button>
          </div>
        </form>

        {job && <section className="batch-governance-progress" aria-live="polite">
          <header>
            <div><p className="eyebrow">TASK PROGRESS</p><h4>任务 #{job.job_id}</h4><span>{job.stock_count} 只股票 · {job.pipeline_run_ids?.length || (job.pipeline_run_id ? 1 : 0)} 个知识管道运行</span></div>
            <strong className={`batch-job-status ${statusClass(job.result_status || job.status)}`}>{statusLabel(job.result_status || job.status)}</strong>
          </header>
          <div className="batch-progress-track" aria-label={`完成进度 ${Math.round(progress)}%`}><span style={{ width: `${progress}%` }} /></div>
          <div className="batch-progress-meta"><span>当前阶段：{stageLabel(job.current_stage)}</span><b>{Math.round(progress)}%</b></div>
          {job.worker_required && <div className="batch-governance-message warning">
            任务已持久化并等待后台 Worker 执行。若长时间停留在队列，请在后端启动批量任务 Worker{job.worker_command ? <>：<code>{job.worker_command}</code></> : "。"}
          </div>}
          <ol className="batch-stage-results">
            {stageDefinitions.map((stage, index) => {
              const enabled = stage.code === "BUSINESS_DATA" ? effectiveOptions?.collect_business_data ?? flags.collectBusinessData
                : stage.code === "LAKEHOUSE" ? effectiveOptions?.export_lakehouse ?? flags.exportLakehouse
                  : stage.code === "KNOWLEDGE_BASE" ? effectiveOptions?.archive_chunks ?? flags.archiveChunks
                    : effectiveOptions?.run_graph ?? flags.runGraph;
              const result = readStageResult(job, stage.aliases);
              const isCurrent = stage.aliases.some((alias) => currentStage.includes(alias));
              const state = !enabled ? "SKIPPED" : result?.status || (isCurrent ? "RUNNING" : "WAITING");
              return <li key={stage.code} className={statusClass(state)}>
                <span className="batch-stage-number">{["SUCCESS", "COMPLETED"].includes(normalizeStatus(state)) ? "✓" : index + 1}</span>
                <div><strong>{stage.title}</strong><p>{result ? stageSummary(result) : stage.description}</p></div>
                <small>{statusLabel(state)}</small>
              </li>;
            })}
          </ol>

          {!!job.graph_ids?.length && <div className="batch-governance-message success">
            已生成图谱投影：{job.graph_ids.map((id) => `#${id}`).join("、")}。请到“知识图谱”核对范围、来源和治理状态。
          </div>}
          {!!jobErrors.length && <div className="batch-governance-errors"><strong>任务错误</strong><ul>{jobErrors.map((item, index) => <li key={index}>{errorMessage(item)}</li>)}</ul></div>}
          {!!job.stock_results?.length && <details className="batch-stock-results" open={terminalStatuses.has(normalizeStatus(job.status))}>
            <summary>查看逐股处理结果（{job.stock_results.length}）</summary>
            <div className="table-wrap"><table><thead><tr><th>市场</th><th>股票</th><th>状态</th><th>说明</th></tr></thead><tbody>
              {job.stock_results.map((item) => <tr key={`${item.market}:${item.symbol}`}><td>{item.market}</td><td><code>{item.symbol}</code></td><td>{statusLabel(item.status)}</td><td>{item.message || stockErrorMessage(item.errors) || "已记录"}</td></tr>)}
            </tbody></table></div>
          </details>}
          <details className="batch-raw-result"><summary>查看完整任务诊断信息</summary><pre>{JSON.stringify(job, null, 2)}</pre></details>
        </section>}

        <section className="batch-recent-jobs" aria-label="最近批量任务">
          <header><div><h4>最近批量任务</h4><p>页面刷新或关闭弹窗后，可从这里重新打开任务并继续查看进度。</p></div><span>{recentLoading ? "正在读取…" : `${recentJobs.length} 条`}</span></header>
          <div className="table-wrap"><table><thead><tr><th>任务</th><th>股票数</th><th>进度</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead><tbody>
            {recentJobs.map((item) => <tr key={item.job_id}><td><strong>#{item.job_id}</strong><br /><small>{stageLabel(item.current_stage)}</small></td><td>{item.stock_count}</td><td>{Math.round(progressValue(item))}%</td><td><span className={`batch-job-status ${statusClass(item.result_status || item.status)}`}>{statusLabel(item.result_status || item.status)}</span></td><td>{item.created_at ? new Date(item.created_at).toLocaleString("zh-CN") : "--"}</td><td><button type="button" onClick={() => void openRecentJob(item)}>查看</button></td></tr>)}
            {!recentLoading && !recentJobs.length && <tr><td colSpan={6} className="empty-state">暂无批量任务记录</td></tr>}
          </tbody></table></div>
        </section>
      </div>
    </section>
  </div>;
}

export default BatchStockGovernanceDialog;
