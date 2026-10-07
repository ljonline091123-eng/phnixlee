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
  RETRY: "等待自动重试",
  RUNNING: "执行中",
  SUCCESS: "已完成",
  COMPLETED: "已完成",
  PARTIAL: "部分完成",
  EMPTY: "无有效数据",
  FAILED: "执行失败",
  SKIPPED: "未启用",
  CANCELLED: "已取消",
};

const businessTypeNames: Record<string, string> = Object.fromEntries(
  businessTypeOptions.map((item) => [item.code, item.name]),
);

const diagnosticFieldNames: Record<string, string> = {
  market: "市场",
  symbols: "股票范围",
  source_code: "数据源",
  interface_code: "接口",
  total_count: "上游返回数",
  persisted_count: "成功入库数",
  section_count: "有效分区数",
  sections: "资料分区",
  fetch_log_id: "采集日志ID",
  stock_count: "股票数",
  success_count: "完成股票数",
  partial_or_failed_count: "部分完成或失败数",
  failed_count: "失败股票数",
  documents_total: "知识文档数",
  documents_with_chunks: "已切片文档数",
  coverage_status: "切片覆盖状态",
  knowledge_base_mode: "知识库加工模式",
  graph_id: "新图谱ID",
  base_graph_id: "基础图谱ID",
  selected_graph_id: "选择的图谱ID",
  governance_status: "图谱治理状态",
  build_status: "图谱构建状态",
  dataset_statuses: "数据集发布状态",
  pipeline_run_id: "知识管道运行ID",
  lineage_batch_id: "血缘批次ID",
  audit_status: "智能体审核状态",
  scope: "审核范围",
  run_id: "运行ID",
  dataset_code: "数据集编码",
  details: "核验明细",
  verification_path: "核验接口",
};

const datasetNames: Record<string, string> = {
  stock_symbol: "股票主数据",
  stock_realtime_quote: "实时行情",
  stock_kline: "历史量价",
  stock_financial_report: "财务报告",
  stock_news: "股票新闻",
  stock_notice: "公司公告",
  stock_f10_cache: "F10资料",
  stock_context_event: "外部事件",
  research_report: "研究报告",
  knowledge_document: "知识文档",
  foundation_entity: "公司 / 主体主数据",
  foundation_security: "证券主数据",
  foundation_listing: "上市关系",
  foundation_evidence: "主数据证据",
  foundation_fact: "公司与主体事实",
  foundation_fact_evidence: "主体事实证据",
  foundation_fact_review: "主体事实审核",
  foundation_security_classification: "证券分类",
  foundation_source_identity: "来源身份映射",
  foundation_company_mapping_state: "公司映射状态",
  classification_definition: "分类标签释义",
};

function datasetName(code: string) {
  const normalized = code.replace(/^pipeline_/, "").replace(/_(?:raw|normalized|serving)$/i, "");
  return datasetNames[normalized] || code;
}

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
  if (["PARTIAL", "EMPTY"].includes(normalized)) return "partial";
  if (normalized === "RUNNING") return "running";
  return "waiting";
}

function jobDisplayStatus(job: BatchGovernanceJob | null | undefined) {
  if (!job) return "WAITING";
  const lifecycle = normalizeStatus(job.job_status || job.status);
  return lifecycle === "COMPLETED"
    ? normalizeStatus(job.result_status || job.status)
    : lifecycle;
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

function isRetryableJob(job: BatchGovernanceJob) {
  const lifecycle = normalizeStatus(job.job_status || job.status);
  if (lifecycle === "FAILED") return true;
  if (job.job_status && lifecycle !== "COMPLETED") return false;
  return ["PARTIAL", "FAILED"].includes(normalizeStatus(job.result_status || job.status));
}

function stageLabel(stage?: string | null) {
  const normalized = String(stage || "").toUpperCase();
  return stageDefinitions.find((item) => item.aliases.some((alias) => normalized.includes(alias)))?.title || stage || "等待调度";
}

function diagnosticValue(value: unknown): string {
  if (value == null || value === "") return "--";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (Array.isArray(value)) return value.map((item) => typeof item === "object" ? JSON.stringify(item) : String(item)).join("、") || "--";
  if (typeof value === "object") {
    return Object.entries(value as Record<string, unknown>)
      .map(([key, item]) => `${diagnosticFieldNames[key] || key}：${diagnosticValue(item)}`)
      .join("；");
  }
  const text = String(value);
  const translations: Record<string, string> = {
    SUCCESS: "已完成",
    COMPLETED: "已完成",
    PUBLISHED: "已发布",
    PARTIAL: "部分完成",
    FAILED: "失败",
    ERROR: "错误",
    SKIPPED: "未执行",
    PENDING: "待治理",
    BUILT: "已构建",
    GOVERNED: "已治理",
    LOCKED: "已锁定",
    COMPLETE: "完整覆盖",
    EXISTING_DOCUMENTS_ONLY: "仅处理既有知识文档",
    SCOPED_GRAPH_DOCUMENTS_AND_CHUNKS: "范围图谱文档与切片",
  };
  return translations[text.toUpperCase()] || text;
}

const completionIssueNames: Record<string, string> = {
  IDENTITY_UNMAPPED: "存在股票身份未完成映射",
  QUALITY_GATE_NOT_PASSED: "质量门禁未通过",
  LAKEHOUSE_EXPORT_INCOMPLETE: "湖仓数据集未全部发布",
  AGENT_GOVERNANCE_NOT_PASSED: "智能体与Skill治理未通过",
  NO_KNOWLEDGE_DOCUMENTS: "当前范围没有可切片知识文档",
  CHUNK_SELECTION_TRUNCATED: "切片仅处理了限定样本",
  CHUNK_FAILURES: "部分知识文档切片失败",
  GRAPH_BUILD_FAILED: "知识图谱构建失败",
  GRAPH_PENDING_GOVERNANCE: "知识图谱已生成但尚未治理锁定",
};

function formatCompletionIssue(issue: Record<string, unknown>): string {
  const code = String(issue.code || "");
  const name = completionIssueNames[code] || code;
  const detail = issue.detail;
  if (detail == null || detail === "") return name || "存在未完成事项";
  return `${name || "未完成事项"}：${diagnosticValue(detail)}`;
}

type BatchStageViewItem = {
  key: string;
  title: string;
  status: string;
  reason: string;
  fields: Array<[string, unknown]>;
  completionGroup?: "completed" | "incomplete";
  verificationPath?: string;
};

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

function knowledgePipelineMarketRuns(job: BatchGovernanceJob) {
  const result = job.stage_results?.KNOWLEDGE_PIPELINE;
  return Array.isArray(result?.market_runs) ? result.market_runs.filter((item): item is Record<string, unknown> => !!item && typeof item === "object") : [];
}

function stageRunReason(
  stageCode: string,
  run: Record<string, unknown>,
  result: BatchGovernanceStageResult | undefined,
  job: BatchGovernanceJob,
) {
  const direct = run.error || run.reason;
  if (direct) return String(direct);
  const completionIssues = knowledgePipelineMarketRuns(job)
    .filter((item) => !run.market || item.market === run.market)
    .flatMap((item) => {
      const completion = item.completion;
      if (!completion || typeof completion !== "object") return [];
      const issues = (completion as Record<string, unknown>).issues;
      if (!Array.isArray(issues)) return [];
      return issues
        .filter((issue): issue is Record<string, unknown> => !!issue && typeof issue === "object")
        .filter((issue) => {
          const issueStage = String(issue.stage || "").toUpperCase();
          const stageAliases: Record<string, string[]> = {
            LAKEHOUSE: ["LAKEHOUSE_EXPORT", "QUALITY_GATE"],
            KNOWLEDGE_BASE: ["DOCUMENT_CHUNKS"],
            KNOWLEDGE_GRAPH: ["KNOWLEDGE_GRAPH", "AGENT_SKILL_GOVERNANCE"],
          };
          return (stageAliases[stageCode] || []).includes(issueStage);
        })
        .map(formatCompletionIssue);
    });
  const issues = completionIssues;
  if (issues.length) return [...new Set(issues)].join("；");
  const status = normalizeStatus(String(run.status || result?.status || "WAITING"));
  if (stageCode === "LAKEHOUSE" && status !== "SUCCESS") {
    const datasets = run.dataset_statuses as Record<string, unknown> | undefined;
    const unfinished = datasets && Object.entries(datasets).filter(([, value]) => String(value).toUpperCase() !== "PUBLISHED");
    if (unfinished?.length) return `以下数据集尚未发布：${unfinished.map(([name, value]) => `${datasetName(name)}（${String(value)}）`).join("、")}`;
    return "存在空数据源、质量门禁未通过或数据集未完成发布。";
  }
  if (stageCode === "KNOWLEDGE_BASE" && status !== "SUCCESS") {
    if (run.knowledge_base_mode === "EXISTING_DOCUMENTS_ONLY") return "本次只切分既有知识文档，新采集业务记录尚未全部物化为知识文档。";
    const total = Number(run.documents_total || 0);
    const completed = Number(run.documents_with_chunks || 0);
    return total ? `${Math.max(0, total - completed)} 份知识文档尚未完成切片。` : "本次范围内没有可切片的知识文档。";
  }
  if (stageCode === "KNOWLEDGE_GRAPH" && status !== "SUCCESS") {
    if (String(run.build_status || "").toUpperCase() === "BUILT") return "图谱投影已经生成，但尚未达到已治理或已锁定状态。";
    return "图谱尚未生成，或构建过程中出现错误。";
  }
  return String(result?.message || (status === "SUCCESS" ? "阶段结果已完成并记录。" : "阶段尚未完成，请结合诊断字段核对。"));
}

function batchStageViewItems(
  stageCode: string,
  result: BatchGovernanceStageResult | undefined,
  job: BatchGovernanceJob,
): BatchStageViewItem[] {
  const enrichedCompleted = Array.isArray(result?.completed_items) ? result.completed_items : [];
  const enrichedIncomplete = Array.isArray(result?.incomplete_items) ? result.incomplete_items : [];
  if (enrichedCompleted.length || enrichedIncomplete.length) {
    return [
      ...enrichedCompleted.map((value, index) => ({ value, index, fallbackStatus: "SUCCESS" })),
      ...enrichedIncomplete.map((value, index) => ({ value, index, fallbackStatus: "PARTIAL" })),
    ].filter((entry): entry is { value: Record<string, unknown>; index: number; fallbackStatus: string } => !!entry.value && typeof entry.value === "object")
      .map(({ value, index, fallbackStatus }) => {
        const code = String(value.code || value.id || `${fallbackStatus}:${index}`);
        const rawLabel = String(value.label || value.title || value.code || `阶段项目 ${index + 1}`);
        const codeParts = code.split(":");
        const datasetCode = stageCode === "LAKEHOUSE" ? codeParts.at(-1) || rawLabel : "";
        const marketPrefix = stageCode === "LAKEHOUSE" && codeParts.length > 1 ? `${codeParts[0]} · ` : "";
        const stockPrefix = stageCode === "BUSINESS_DATA" && codeParts.length >= 3 ? `${codeParts[0]}:${codeParts[1]} · ` : "";
        const completionGroup = fallbackStatus === "SUCCESS" ? "completed" : "incomplete";
        const normalizedStatus = normalizeStatus(String(value.status || fallbackStatus));
        const status = completionGroup === "incomplete" && isCompletedStageItem(normalizedStatus) ? "PARTIAL" : normalizedStatus;
        const fields = Object.entries(value).filter(([key]) => !["code", "id", "label", "title", "status", "reason", "message", "action_hint", "verification_path"].includes(key));
        if (datasetCode && !fields.some(([key]) => key === "dataset_code")) fields.unshift(["dataset_code", datasetCode]);
        return {
          key: code,
          title: stageCode === "LAKEHOUSE" ? `${marketPrefix}${datasetName(datasetCode)}` : `${stockPrefix}${rawLabel}`,
          status,
          reason: String(value.reason || value.message || value.action_hint || (fallbackStatus === "SUCCESS" ? "服务端已核验完成。" : "服务端标记为待处理。")),
          fields,
          completionGroup,
          verificationPath: typeof value.verification_path === "string" ? value.verification_path : undefined,
        };
      });
  }

  if (stageCode === "BUSINESS_DATA") {
    const rows: BatchStageViewItem[] = [];
    (job.stock_results || []).forEach((stock) => {
      const operations = stock.operations && typeof stock.operations === "object"
        ? stock.operations as Record<string, Record<string, unknown>>
        : {};
      Object.entries(operations).forEach(([dataType, operation]) => {
        const status = normalizeStatus(String(operation.status || stock.status || "WAITING"));
        rows.push({
          key: `${stock.market}:${stock.symbol}:${dataType}`,
          title: `${String(stock.name || stock.symbol)} · ${businessTypeNames[dataType] || dataType}`,
          status,
          reason: String(operation.error || operation.message || (status === "SUCCESS" ? "数据源返回及持久化结果已记录。" : "该数据类型未完整入库。")),
          fields: Object.entries(operation).filter(([key]) => !["status", "message", "error"].includes(key)),
        });
      });
      if (!Object.keys(operations).length) {
        rows.push({
          key: `${stock.market}:${stock.symbol}`,
          title: `${String(stock.name || stock.symbol)} · 业务数据`,
          status: normalizeStatus(stock.status),
          reason: stock.message || stockErrorMessage(stock.errors) || "任务未返回逐数据类型结果。",
          fields: [["market", stock.market], ["symbols", [stock.symbol]]],
        });
      }
    });
    return rows;
  }

  const rawRuns = Array.isArray(result?.market_runs)
    ? result.market_runs
    : Array.isArray(result?.runs) ? result.runs : [];
  const runs = rawRuns.filter((item): item is Record<string, unknown> => !!item && typeof item === "object");
  if (runs.length && stageCode === "LAKEHOUSE") {
    return runs.flatMap((run, runIndex) => {
      const market = String(run.market || `范围 ${runIndex + 1}`);
      const datasets = run.dataset_statuses && typeof run.dataset_statuses === "object"
        ? run.dataset_statuses as Record<string, unknown>
        : {};
      if (!Object.keys(datasets).length) return [{
        key: `${market}:lakehouse`,
        title: `${market} · 湖仓发布`,
        status: normalizeStatus(String(run.status || result?.status || "WAITING")),
        reason: stageRunReason(stageCode, run, result, job),
        fields: Object.entries(run).filter(([key]) => !["status", "message", "error", "completion"].includes(key)),
      }];
      return Object.entries(datasets).map(([dataset, rawStatus]) => {
        const normalized = normalizeStatus(String(rawStatus || "WAITING"));
        const status = normalized === "PUBLISHED" ? "SUCCESS" : ["FAILED", "ERROR"].includes(normalized) ? "FAILED" : "PARTIAL";
        return {
          key: `${market}:dataset:${dataset}`,
          title: `${market} · ${datasetName(dataset)}`,
          status,
          reason: status === "SUCCESS" ? "数据集已通过质量门禁并完成湖仓发布。" : `数据集发布状态为 ${String(rawStatus || "未返回")}；${stageRunReason(stageCode, run, result, job)}`,
          fields: [["market", market], ["dataset_code", dataset], ["dataset_statuses", { [dataset]: rawStatus }]] as Array<[string, unknown]>,
        };
      });
    });
  }

  if (runs.length && stageCode === "KNOWLEDGE_BASE") {
    return runs.flatMap((run, runIndex) => {
      const market = String(run.market || `范围 ${runIndex + 1}`);
      const total = Number(run.documents_total || 0);
      const withChunks = Number(run.documents_with_chunks || 0);
      const mode = String(run.knowledge_base_mode || result?.knowledge_base_mode || "");
      const rows: BatchStageViewItem[] = [{
        key: `${market}:documents`,
        title: `${market} · 知识文档`,
        status: total > 0 ? "SUCCESS" : "PARTIAL",
        reason: total > 0 ? `已识别 ${total} 份可加工知识文档。` : "当前范围没有可归档、可切片的知识文档。",
        fields: [["market", market], ["documents_total", total]],
      }, {
        key: `${market}:chunks`,
        title: `${market} · 文档切片覆盖`,
        status: total > 0 && withChunks >= total ? "SUCCESS" : "PARTIAL",
        reason: total > 0 && withChunks >= total
          ? `${withChunks} / ${total} 份知识文档已形成切片。`
          : stageRunReason(stageCode, run, result, job),
        fields: [["market", market], ["documents_total", total], ["documents_with_chunks", withChunks], ["coverage_status", run.coverage_status]],
      }];
      if (mode === "EXISTING_DOCUMENTS_ONLY") rows.push({
        key: `${market}:materialization`,
        title: `${market} · 新业务数据知识物化`,
        status: "PARTIAL",
        reason: "本次只处理既有知识文档；刚采集的业务记录尚未全部转换为新知识文档。",
        fields: [["knowledge_base_mode", mode]],
      });
      return rows;
    });
  }

  if (runs.length && stageCode === "KNOWLEDGE_GRAPH") {
    return runs.flatMap((run, runIndex) => {
      const market = String(run.market || `范围 ${runIndex + 1}`);
      const buildStatus = normalizeStatus(String(run.build_status || "WAITING"));
      const governanceStatus = normalizeStatus(String(run.governance_status || "PENDING"));
      return [{
        key: `${market}:projection`,
        title: `${market} · 图谱投影构建`,
        status: buildStatus === "BUILT" ? "SUCCESS" : buildStatus === "FAILED" ? "FAILED" : "PARTIAL",
        reason: buildStatus === "BUILT" ? "本批股票范围的独立图谱投影已生成。" : stageRunReason(stageCode, run, result, job),
        fields: [["market", market], ["build_status", buildStatus], ["graph_id", run.graph_id], ["base_graph_id", run.base_graph_id]],
      }, {
        key: `${market}:governance`,
        title: `${market} · 图谱治理发布`,
        status: ["GOVERNED", "LOCKED"].includes(governanceStatus) ? "SUCCESS" : "PARTIAL",
        reason: ["GOVERNED", "LOCKED"].includes(governanceStatus)
          ? "图谱已完成治理，可作为后续分析的正式知识版本。"
          : "图谱投影已保留为待治理状态，尚不能视为已验证、已锁定的正式图谱。",
        fields: [["market", market], ["governance_status", governanceStatus], ["graph_id", run.graph_id]],
      }];
    });
  }

  if (runs.length) return runs.map((run, index) => {
    const market = String(run.market || run.market_trigger || `范围 ${index + 1}`);
    const status = normalizeStatus(String(run.status || result?.status || "WAITING"));
    return {
      key: `${market}:${index}`,
      title: `${market} · ${stageDefinitions.find((item) => item.code === stageCode)?.title || stageCode}`,
      status,
      reason: stageRunReason(stageCode, run, result, job),
      fields: Object.entries(run).filter(([key]) => !["status", "message", "error", "completion"].includes(key)),
    };
  });

  if (!result) return [];
  const status = normalizeStatus(result.status);
  return [{
    key: `${stageCode}:summary`,
    title: stageDefinitions.find((item) => item.code === stageCode)?.title || stageCode,
    status,
    reason: result.message || (status === "SUCCESS" ? "阶段已完成。" : "服务端尚未返回更细的执行结果。"),
    fields: Object.entries(result).filter(([key]) => !["status", "message", "market_runs", "runs"].includes(key)),
  }];
}

function isCompletedStageItem(status: string) {
  return ["SUCCESS", "COMPLETED", "PUBLISHED", "GOVERNED", "LOCKED"].includes(normalizeStatus(status));
}

function BatchStageDetail({
  definition,
  result,
  state,
  job,
}: {
  definition: typeof stageDefinitions[number];
  result?: BatchGovernanceStageResult;
  state: string;
  job: BatchGovernanceJob;
}) {
  const items = batchStageViewItems(definition.code, result, job);
  const completed = items.filter((item) => item.completionGroup === "completed" || (!item.completionGroup && isCompletedStageItem(item.status)));
  const incomplete = items.filter((item) => item.completionGroup === "incomplete" || (!item.completionGroup && !isCompletedStageItem(item.status)));
  const renderItems = (rows: BatchStageViewItem[], emptyText: string) => rows.length
    ? <div className="batch-stage-detail-list">{rows.map((item) => {
      const verificationUrl = verificationHref(item.verificationPath);
      return <article key={item.key} className={statusClass(item.status)}>
        <header><div><strong>{item.title}</strong><small>{statusLabel(item.status)}</small></div><span className={`batch-job-status ${statusClass(item.status)}`}>{statusLabel(item.status)}</span></header>
        <p>{item.reason}</p>
        {!!item.fields.length && <dl>{item.fields.map(([key, value]) => <div key={key}><dt>{diagnosticFieldNames[key] || key}</dt><dd>{diagnosticValue(value)}</dd></div>)}</dl>}
        {verificationUrl && <a className="batch-stage-verification-link" href={verificationUrl} target="_blank" rel="noopener noreferrer">打开核验数据 ↗</a>}
      </article>;
    })}</div>
    : <p className="batch-stage-detail-empty">{emptyText}</p>;
  return <section className="batch-stage-detail" aria-label={`${definition.title}详情`}>
    <header><div><p className="eyebrow">STAGE DETAIL</p><h5>{definition.title} · {statusLabel(state)}</h5><p>{result?.message || definition.description}</p></div><span className={`batch-job-status ${statusClass(state)}`}>{statusLabel(state)}</span></header>
    <div className="batch-stage-detail-columns">
      <section><h6>已完成内容 · {completed.length}</h6>{renderItems(completed, "当前没有可核验的已完成明细。")}</section>
      <section><h6>未完成 / 待处理 · {incomplete.length}</h6>{renderItems(incomplete, normalizeStatus(state) === "SKIPPED" ? "本次未启用该阶段。" : "当前没有未完成项目。")}</section>
    </div>
  </section>;
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
  const [retryingJobId, setRetryingJobId] = useState<number | null>(null);
  const [job, setJob] = useState<BatchGovernanceJob | null>(null);
  const [recentJobs, setRecentJobs] = useState<BatchGovernanceJob[]>([]);
  const [recentLoading, setRecentLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedStageCode, setSelectedStageCode] = useState<string | null>(null);
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
    onNotify?.(`批量任务 #${job.job_id} ${statusLabel(jobDisplayStatus(job))}，已处理 ${job.stock_count} 只股票。`);
  }, [job, onCompleted, onNotify]);

  useEffect(() => {
    setSelectedStageCode(null);
  }, [job?.job_id]);

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

  async function retryJob(item: BatchGovernanceJob) {
    if (!isRetryableJob(item) || activeJob || retryingJobId !== null) return;
    setRetryingJobId(item.job_id);
    setError("");
    try {
      const created = await batchGovernanceApi.retry(item.job_id);
      completionNotified.current = null;
      setJob(created);
      setRecentJobs((current) => [created, ...current.filter((row) => row.job_id !== created.job_id)].slice(0, 8));
      onNotify?.(`已提交重试任务 #${created.job_id}。系统将按原任务请求重新执行，可能包含此前已完成的环节。`);
      try {
        const result = await batchGovernanceApi.list(8);
        const latest = Array.isArray(result) ? result : result.items;
        setRecentJobs([created, ...latest.filter((row) => row.job_id !== created.job_id)].slice(0, 8));
      } catch {
        // Keep the new task visible if refreshing the recent-job list fails.
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "重试任务提交失败");
    } finally {
      setRetryingJobId(null);
    }
  }

  if (!open) return null;

  const selectedKnowledgeBase = knowledgeBases.find((item) => item.id === knowledgeBaseId);
  const selectedGraph = graphs.find((item) => item.id === graphId);
  const currentStage = String(job?.current_stage || "").toUpperCase();
  const progress = progressValue(job);
  const effectiveOptions = job?.effective_options;
  const stageEnabled = (code: string) => code === "BUSINESS_DATA" ? effectiveOptions?.collect_business_data ?? flags.collectBusinessData
    : code === "LAKEHOUSE" ? effectiveOptions?.export_lakehouse ?? flags.exportLakehouse
      : code === "KNOWLEDGE_BASE" ? effectiveOptions?.archive_chunks ?? flags.archiveChunks
        : effectiveOptions?.run_graph ?? flags.runGraph;
  const selectedStage = stageDefinitions.find((stage) => stage.code === selectedStageCode);
  const selectedStageResult = selectedStage ? readStageResult(job, selectedStage.aliases) : undefined;
  const selectedStageIsCurrent = !!selectedStage && selectedStage.aliases.some((alias) => currentStage.includes(alias));
  const selectedStageState = !selectedStage ? "WAITING"
    : !stageEnabled(selectedStage.code) ? "SKIPPED"
      : selectedStageResult?.status || (selectedStageIsCurrent ? "RUNNING" : "WAITING");
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
            <strong className={`batch-job-status ${statusClass(jobDisplayStatus(job))}`}>{statusLabel(jobDisplayStatus(job))}</strong>
            {isRetryableJob(job) && <div className="batch-retry-control">
              <button type="button" disabled={retryingJobId !== null || activeJob} onClick={() => void retryJob(job)}>
                {retryingJobId === job.job_id ? "正在提交..." : "按原请求重试"}
              </button>
              <small>将按原任务请求重新执行，可能包含已完成环节。</small>
            </div>}
          </header>
          <div className="batch-progress-track" aria-label={`完成进度 ${Math.round(progress)}%`}><span style={{ width: `${progress}%` }} /></div>
          <div className="batch-progress-meta"><span>当前阶段：{stageLabel(job.current_stage)}</span><b>{Math.round(progress)}%</b></div>
          {job.worker_required && <div className="batch-governance-message warning">
            {job.job_status === "RETRY"
              ? <>上次执行发生可重试错误，后台 Worker 将自动重试（第 {job.attempts || 0}/{job.max_attempts || 0} 次）{job.run_after ? `，计划时间 ${new Date(job.run_after).toLocaleString("zh-CN")}` : ""}。若长时间未重试，请检查 Worker{job.worker_command ? <>：<code>{job.worker_command}</code></> : "。"}</>
              : <>任务已持久化并等待后台 Worker 执行。若长时间停留在队列，请在后端启动批量任务 Worker{job.worker_command ? <>：<code>{job.worker_command}</code></> : "。"}</>}
          </div>}
          <ol className="batch-stage-results">
            {stageDefinitions.map((stage, index) => {
              const enabled = stageEnabled(stage.code);
              const result = readStageResult(job, stage.aliases);
              const isCurrent = stage.aliases.some((alias) => currentStage.includes(alias));
              const state = !enabled ? "SKIPPED" : result?.status || (isCurrent ? "RUNNING" : "WAITING");
              const expanded = selectedStageCode === stage.code;
              return <li key={stage.code} className={`${statusClass(state)}${expanded ? " selected" : ""}`}>
                <button type="button" aria-expanded={expanded} onClick={() => setSelectedStageCode((current) => current === stage.code ? null : stage.code)}>
                  <span className="batch-stage-number">{["SUCCESS", "COMPLETED"].includes(normalizeStatus(state)) ? "✓" : index + 1}</span>
                  <div><strong>{stage.title}</strong><p>{result ? stageSummary(result) : stage.description}</p></div>
                  <small>{statusLabel(state)} · 查看详情</small>
                </button>
              </li>;
            })}
          </ol>
          {selectedStage && <BatchStageDetail definition={selectedStage} result={selectedStageResult} state={selectedStageState} job={job} />}

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
            {recentJobs.map((item) => <tr key={item.job_id}><td><strong>#{item.job_id}</strong><br /><small>{stageLabel(item.current_stage)}</small></td><td>{item.stock_count}</td><td>{Math.round(progressValue(item))}%</td><td><span className={`batch-job-status ${statusClass(jobDisplayStatus(item))}`}>{statusLabel(jobDisplayStatus(item))}</span></td><td>{item.created_at ? new Date(item.created_at).toLocaleString("zh-CN") : "--"}</td><td className="batch-recent-job-actions"><button type="button" onClick={() => void openRecentJob(item)}>查看</button>{isRetryableJob(item) && <button type="button" disabled={retryingJobId !== null || activeJob} onClick={() => void retryJob(item)}>{retryingJobId === item.job_id ? "正在提交..." : "按原请求重试"}</button>}</td></tr>)}
            {!recentLoading && !recentJobs.length && <tr><td colSpan={6} className="empty-state">暂无批量任务记录</td></tr>}
          </tbody></table></div>
        </section>
      </div>
    </section>
  </div>;
}

export default BatchStockGovernanceDialog;
