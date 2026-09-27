import { useEffect, useMemo, useState, type FormEvent } from "react";
import type { KnowledgeGraph } from "./api";
import {
  knowledgePipelineApi,
  type KnowledgePipelineCatalog,
  type KnowledgePipelineRequest,
  type KnowledgePipelineResult,
  type PipelineRunDetail,
} from "./knowledgePipelineApi";
import "./KnowledgePipelinePanel.css";

const marketNames: Record<string, string> = {
  CN_A: "A 股",
  HK: "港股",
  NEEQ: "新三板基础层",
  NEEQ_INNOVATION: "新三板创新层",
};

const statusNames: Record<string, string> = {
  WAITING: "待执行",
  RUNNING: "执行中",
  SUCCESS: "已完成",
  COMPLETED: "已完成",
  PASSED: "已通过",
  PARTIAL: "部分完成",
  PENDING: "待治理",
  PENDING_REVIEW: "待人工审核",
  FAILED: "执行失败",
  SKIPPED: "未启用",
  NOT_REQUESTED: "未启用",
  BUILT: "已构建",
  GOVERNED: "已治理",
  LOCKED: "已锁定",
  PUBLISHED: "已发布",
};

const stageNames: Record<string, string> = {
  IDENTITY_RESOLUTION: "主体身份解析",
  QUALITY_GATE: "数据质量检查",
  AGENT_SKILL_GOVERNANCE: "智能体与技能治理",
  LAKEHOUSE_EXPORT: "湖仓分层发布",
  DOCUMENT_CHUNKS: "文档切片与归档",
  KNOWLEDGE_GRAPH: "知识图谱投影",
  LINEAGE: "统一血缘登记",
};

const issueNames: Record<string, string> = {
  IDENTITY_UNMAPPED: "部分股票尚未完成股票—公司主体映射",
  QUALITY_GATE_NOT_PASSED: "部分来源未通过数据质量检查",
  LAKEHOUSE_EXPORT_INCOMPLETE: "部分数据集未完成湖仓发布",
  AGENT_GOVERNANCE_NOT_PASSED: "智能体与技能治理尚未通过",
  NO_KNOWLEDGE_DOCUMENTS: "当前范围没有可用于切片的知识文档",
  CHUNK_SELECTION_TRUNCATED: "文档数量超过本次切片上限，仅处理了部分文档",
  CHUNK_FAILURES: "部分文档切片失败",
  GRAPH_BUILD_FAILED: "知识图谱投影构建失败",
  GRAPH_PENDING_GOVERNANCE: "投影图已生成，尚需执行图谱治理",
};

type StepState = "WAITING" | "RUNNING" | "SUCCESS" | "PARTIAL" | "FAILED" | "SKIPPED";

type PipelineStep = {
  code: string;
  title: string;
  description: string;
  enabled: boolean;
  state: StepState;
};

export type KnowledgePipelinePanelProps = {
  className?: string;
  onNotify?: (message: string) => void;
  onProjectionReady?: (graphId: number, result: KnowledgePipelineResult) => void;
  onRequestProjectionGovernance?: (graphId: number) => void;
};

function isProjectionGraph(graph: KnowledgeGraph) {
  return /_PIPE_\d+$/i.test(graph.graph_code);
}

function normalizeStatus(value?: string): StepState {
  const status = String(value || "").toUpperCase();
  if (["SUCCESS", "COMPLETED", "PASSED", "BUILT", "PUBLISHED"].includes(status)) return "SUCCESS";
  if (["PARTIAL", "PENDING", "PENDING_REVIEW", "REVIEW"].includes(status)) return "PARTIAL";
  if (["FAILED", "ERROR", "UNAVAILABLE"].includes(status)) return "FAILED";
  if (["SKIPPED", "NOT_REQUESTED", "NOT_RUN"].includes(status)) return "SKIPPED";
  if (status === "RUNNING") return "RUNNING";
  return "WAITING";
}

function statusLabel(value?: string) {
  return statusNames[String(value || "").toUpperCase()] || value || "未知状态";
}

function parseSymbols(value: string, market: string) {
  const prefix = `${market.toUpperCase()}:`;
  const seen = new Set<string>();
  return value
    .split(/[\s,，;；]+/)
    .map(item => item.trim().toUpperCase())
    .map(item => item.startsWith(prefix) ? item.slice(prefix.length) : item)
    .filter(Boolean)
    .filter(item => {
      if (seen.has(item)) return false;
      seen.add(item);
      return true;
    });
}

function newIdempotencyKey() {
  const suffix = typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : Math.random().toString(36).slice(2);
  return `knowledge-ui-${Date.now()}-${suffix}`;
}

function percentage(value?: number | null) {
  return value == null ? "—" : `${Math.round(value * 1000) / 10}%`;
}

export function KnowledgePipelinePanel({
  className = "",
  onNotify,
  onProjectionReady,
  onRequestProjectionGovernance,
}: KnowledgePipelinePanelProps) {
  const [catalog, setCatalog] = useState<KnowledgePipelineCatalog | null>(null);
  const [catalogRevision, setCatalogRevision] = useState(0);
  const [catalogLoading, setCatalogLoading] = useState(true);
  const [catalogError, setCatalogError] = useState("");
  const [market, setMarket] = useState("CN_A");
  const [symbolsText, setSymbolsText] = useState("");
  const [knowledgeBaseId, setKnowledgeBaseId] = useState<number | undefined>();
  const [graphId, setGraphId] = useState<number | undefined>();
  const [exportLakehouse, setExportLakehouse] = useState(true);
  const [archiveChunks, setArchiveChunks] = useState(true);
  const [runGraph, setRunGraph] = useState(true);
  const [runAgentGovernance, setRunAgentGovernance] = useState(true);
  const [includeCompanyTables, setIncludeCompanyTables] = useState(true);
  const [datasetLimit, setDatasetLimit] = useState(10000);
  const [governanceRecordLimit, setGovernanceRecordLimit] = useState(50);
  const [graphDocumentsPerStock, setGraphDocumentsPerStock] = useState(50);
  const [chunkDocumentLimit, setChunkDocumentLimit] = useState(10000);
  const [chunkSize, setChunkSize] = useState(1800);
  const [overlap, setOverlap] = useState(180);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [detailWarning, setDetailWarning] = useState("");
  const [result, setResult] = useState<KnowledgePipelineResult | null>(null);
  const [runDetail, setRunDetail] = useState<PipelineRunDetail | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    setCatalogLoading(true);
    void knowledgePipelineApi.catalog(controller.signal)
      .then(data => {
        if (controller.signal.aborted) return;
        setCatalog(data);
        setCatalogError("");
      })
      .catch(reason => {
        if (!controller.signal.aborted) {
          setCatalogError(reason instanceof Error ? reason.message : "读取知识库与图谱清单失败");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setCatalogLoading(false);
      });
    return () => controller.abort();
  }, [catalogRevision]);

  const enabledKnowledgeBases = useMemo(
    () => (catalog?.knowledgeBases || []).filter(item => item.enabled),
    [catalog],
  );

  useEffect(() => {
    if (!enabledKnowledgeBases.length) return;
    if (knowledgeBaseId && enabledKnowledgeBases.some(item => item.id === knowledgeBaseId)) return;
    const preferred = enabledKnowledgeBases.find(item => item.kb_code === "STOCK_FULL_KG") || enabledKnowledgeBases[0];
    setKnowledgeBaseId(preferred.id);
  }, [enabledKnowledgeBases, knowledgeBaseId]);

  const graphOptions = useMemo(() => {
    const matched = (catalog?.graphs || []).filter(item => item.enabled && item.knowledge_base_id === knowledgeBaseId);
    const bases = matched.filter(item => !isProjectionGraph(item));
    return bases.length ? bases : matched;
  }, [catalog, knowledgeBaseId]);

  useEffect(() => {
    if (!knowledgeBaseId || !graphOptions.length) {
      setGraphId(undefined);
      return;
    }
    if (graphId && graphOptions.some(item => item.id === graphId)) return;
    const preferred = graphOptions.find(item => ["GOVERNED", "LOCKED"].includes(item.governance_status)) || graphOptions[0];
    setGraphId(preferred.id);
  }, [graphId, graphOptions, knowledgeBaseId]);

  const symbols = useMemo(() => parseSymbols(symbolsText, market), [market, symbolsText]);
  const selectedKnowledgeBase = enabledKnowledgeBases.find(item => item.id === knowledgeBaseId);
  const selectedGraph = graphOptions.find(item => item.id === graphId);
  const projectionGraphId = result?.graph.projection
    ? result.graph.graph_id || result.graph_id || undefined
    : undefined;
  const projectionGraph = projectionGraphId
    ? catalog?.graphs.find(item => item.id === projectionGraphId)
    : undefined;

  const stageMap = useMemo(
    () => new Map((runDetail?.stages || []).map(stage => [stage.stage_code, stage])),
    [runDetail],
  );

  function stageState(code: string, enabled: boolean, fallback: StepState): StepState {
    if (!enabled) return "SKIPPED";
    if (busy) return "RUNNING";
    const persisted = stageMap.get(code);
    if (persisted) return normalizeStatus(persisted.status);
    if (!result) return "WAITING";
    return fallback;
  }

  const qualityRows = Object.values(result?.quality || {});
  const qualityPassed = qualityRows.filter(item => item.passed).length;
  const exportRows = Object.values(result?.exports || {});
  const exportsPublished = exportRows.filter(item => String(item.status).toUpperCase() === "PUBLISHED").length;
  const graphCounts = result?.graph.counts || {};
  const displayedAgentGovernance = result
    ? result.completion.requested_stages.agent_skill_governance
    : runAgentGovernance;
  const displayedLakehouse = result
    ? result.completion.requested_stages.lakehouse_export
    : exportLakehouse;
  const displayedChunks = result
    ? result.completion.requested_stages.document_chunks
    : archiveChunks;
  const displayedGraph = result
    ? result.completion.requested_stages.knowledge_graph
    : runGraph;

  const steps: PipelineStep[] = [
    {
      code: "IDENTITY_RESOLUTION",
      title: stageNames.IDENTITY_RESOLUTION,
      description: result
        ? `已映射 ${result.identity.mapped} / ${result.identity.requested} 只，未映射 ${result.identity.unmapped} 只。`
        : "统一市场、证券、公司主体及股票—公司映射。",
      enabled: true,
      state: stageState("IDENTITY_RESOLUTION", true, result?.identity.unmapped ? "PARTIAL" : "SUCCESS"),
    },
    {
      code: "QUALITY_GATE",
      title: stageNames.QUALITY_GATE,
      description: result
        ? `${qualityPassed} / ${qualityRows.length} 个来源通过质量门禁。`
        : "检查来源可用性、必填字段、重复记录和业务键。",
      enabled: true,
      state: stageState("QUALITY_GATE", true, qualityRows.length > 0 && qualityPassed === qualityRows.length ? "SUCCESS" : "PARTIAL"),
    },
    {
      code: "AGENT_SKILL_GOVERNANCE",
      title: stageNames.AGENT_SKILL_GOVERNANCE,
      description: displayedAgentGovernance
        ? (result ? `治理结果：${statusLabel(result.agent_governance.status)}。` : "调用知识来源治理智能体与技能，生成审核记录。")
        : "本次不调用治理智能体。",
      enabled: displayedAgentGovernance,
      state: stageState("AGENT_SKILL_GOVERNANCE", displayedAgentGovernance, normalizeStatus(result?.agent_governance.status)),
    },
    {
      code: "LAKEHOUSE_EXPORT",
      title: stageNames.LAKEHOUSE_EXPORT,
      description: displayedLakehouse
        ? (result ? `${exportsPublished} / ${exportRows.length} 个数据集完成发布。` : "将范围内数据发布到湖仓 Normalized 层并保留版本。")
        : "本次不发布湖仓数据集。",
      enabled: displayedLakehouse,
      state: stageState("LAKEHOUSE_EXPORT", displayedLakehouse, exportRows.length > 0 && exportsPublished === exportRows.length ? "SUCCESS" : "PARTIAL"),
    },
    {
      code: "DOCUMENT_CHUNKS",
      title: stageNames.DOCUMENT_CHUNKS,
      description: displayedChunks
        ? (result
          ? `已处理 ${result.chunks.documents_with_chunks ?? result.chunks.documents} / ${result.chunks.documents_total ?? 0} 份文档，新增 ${result.chunks.created_chunk_count ?? 0} 个切片。`
          : "按章节与结构切片，归档原文并登记切片版本。")
        : "本次不执行文档切片。",
      enabled: displayedChunks,
      state: stageState(
        "DOCUMENT_CHUNKS",
        displayedChunks,
        (result?.chunks.documents_total || 0) > 0 && !result?.chunks.truncated && !result?.chunks.failed_documents ? "SUCCESS" : "PARTIAL",
      ),
    },
    {
      code: "KNOWLEDGE_GRAPH",
      title: stageNames.KNOWLEDGE_GRAPH,
      description: displayedGraph
        ? (result?.graph.projection
          ? `已从基础图谱 #${result.base_graph_id} 生成独立投影图 #${projectionGraphId}。`
          : result ? `图谱构建结果：${statusLabel(result.graph.status)}。` : "基于所选股票生成独立投影，不覆盖现有图谱。")
        : "本次不生成知识图谱投影。",
      enabled: displayedGraph,
      state: stageState("KNOWLEDGE_GRAPH", displayedGraph, normalizeStatus(result?.graph.status)),
    },
    {
      code: "LINEAGE",
      title: stageNames.LINEAGE,
      description: result?.lineage_batch_id
        ? `血缘批次：${result.lineage_batch_id}`
        : "记录本次管道与下游知识图谱之间的统一血缘。",
      enabled: true,
      state: busy ? "RUNNING" : result ? (result.lineage_batch_id ? "SUCCESS" : "PARTIAL") : "WAITING",
    },
  ];

  function validate() {
    if (!symbols.length) return "请至少输入一只股票代码。";
    if (symbols.length > 200) return `单次最多处理 200 只股票，当前共 ${symbols.length} 只。`;
    if (!knowledgeBaseId) return "请选择目标知识库。";
    if (runGraph && !graphId) return "已勾选生成图谱投影，请先选择基础图谱。";
    if (archiveChunks && overlap >= chunkSize) return "切片重叠长度必须小于单片长度。";
    return "";
  }

  async function runPipeline(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    const payload: KnowledgePipelineRequest = {
      market,
      symbols,
      knowledge_base_id: knowledgeBaseId,
      graph_id: graphId,
      export_lakehouse: exportLakehouse,
      archive_chunks: archiveChunks,
      run_graph: runGraph,
      run_agent_governance: runAgentGovernance,
      governance_record_limit: governanceRecordLimit,
      governance_run_key: runAgentGovernance ? `ui-${Date.now().toString(36)}` : undefined,
      include_company_tables: includeCompanyTables,
      dataset_limit: datasetLimit,
      graph_documents_per_stock: runGraph ? graphDocumentsPerStock : null,
      chunk_size: chunkSize,
      overlap,
      chunk_document_limit: archiveChunks ? chunkDocumentLimit : null,
      idempotency_key: newIdempotencyKey(),
    };
    setBusy(true);
    setError("");
    setDetailWarning("");
    setResult(null);
    setRunDetail(null);
    try {
      const next = await knowledgePipelineApi.run(payload);
      setResult(next);
      const nextProjectionId = next.graph.projection ? next.graph.graph_id || next.graph_id : undefined;
      if (nextProjectionId) onProjectionReady?.(nextProjectionId, next);
      onNotify?.(`知识生产管道 #${next.pipeline_run_id} 执行完成：${statusLabel(next.status)}`);
      try {
        setRunDetail(await knowledgePipelineApi.getRun(next.pipeline_run_id));
      } catch (reason) {
        setDetailWarning(`结果已生成，但读取分阶段运行明细失败：${reason instanceof Error ? reason.message : "未知错误"}`);
      }
      setCatalogRevision(old => old + 1);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "知识生产管道执行失败，请稍后重试。" );
    } finally {
      setBusy(false);
    }
  }

  return <section className={`panel knowledge-pipeline-panel ${className}`.trim()} aria-label="同步并更新知识">
    <header className="knowledge-pipeline-heading">
      <div>
        <p className="eyebrow">知识生产</p>
        <h2>同步并更新知识</h2>
        <p>股票业务数据同步完成后，在这里统一执行主体解析、质量检查、湖仓发布、文档切片和知识图谱投影。</p>
      </div>
      <div className="knowledge-pipeline-heading-note">
        <strong>增量安全</strong>
        <span>生成范围投影，不覆盖已有完整图谱</span>
      </div>
    </header>

    {catalogError && <div className="knowledge-pipeline-message error" role="alert">
      <span>读取知识资源失败：{catalogError}</span>
      <button type="button" onClick={() => setCatalogRevision(old => old + 1)}>重新加载</button>
    </div>}
    {error && <div className="knowledge-pipeline-message error" role="alert">{error}</div>}
    {detailWarning && <div className="knowledge-pipeline-message warning" role="status">{detailWarning}</div>}

    <form className="knowledge-pipeline-form" onSubmit={runPipeline}>
      <div className="knowledge-pipeline-scope-grid">
        <label>
          <span>证券市场</span>
          <select value={market} disabled={busy} onChange={event => setMarket(event.target.value)}>
            {Object.entries(marketNames).map(([code, name]) => <option key={code} value={code}>{name}（{code}）</option>)}
          </select>
        </label>
        <label className="knowledge-pipeline-symbols">
          <span>股票代码 <small>支持换行、逗号或空格分隔，最多 200 只</small></span>
          <textarea
            value={symbolsText}
            disabled={busy}
            rows={4}
            placeholder={market === "HK" ? "例如：00700, 00941" : "例如：000001, 000002, 600519"}
            onChange={event => setSymbolsText(event.target.value)}
          />
          <small className={symbols.length > 200 ? "over-limit" : ""}>已识别 {symbols.length} 只，重复代码会自动去除</small>
        </label>
        <label>
          <span>目标知识库</span>
          <select
            value={knowledgeBaseId || ""}
            disabled={busy || catalogLoading || !enabledKnowledgeBases.length}
            onChange={event => {
              setKnowledgeBaseId(event.target.value ? Number(event.target.value) : undefined);
              setGraphId(undefined);
            }}
          >
            {!enabledKnowledgeBases.length && <option value="">暂无可用知识库</option>}
            {enabledKnowledgeBases.map(item => <option key={item.id} value={item.id}>{item.kb_name}（{item.kb_code}）</option>)}
          </select>
          {selectedKnowledgeBase && <small>版本 {selectedKnowledgeBase.version} · {selectedKnowledgeBase.source_tables.length} 个来源</small>}
        </label>
        <label>
          <span>基础图谱</span>
          <select
            value={graphId || ""}
            disabled={busy || catalogLoading || !graphOptions.length}
            onChange={event => setGraphId(event.target.value ? Number(event.target.value) : undefined)}
          >
            {!graphOptions.length && <option value="">该知识库暂无基础图谱</option>}
            {graphOptions.map(item => <option key={item.id} value={item.id}>
              {item.graph_name}（#{item.id} · {statusLabel(item.governance_status)}）
            </option>)}
          </select>
          {selectedGraph && <small>{selectedGraph.entity_count.toLocaleString()} 个实体 · {selectedGraph.relation_count.toLocaleString()} 条关系</small>}
        </label>
      </div>

      <fieldset className="knowledge-pipeline-options">
        <legend>本次加工范围</legend>
        <label className={exportLakehouse ? "selected" : ""}>
          <input type="checkbox" checked={exportLakehouse} disabled={busy} onChange={event => setExportLakehouse(event.target.checked)} />
          <span><strong>发布湖仓数据</strong><small>质量检查后写入 Normalized 数据集并登记版本</small></span>
        </label>
        <label className={archiveChunks ? "selected" : ""}>
          <input type="checkbox" checked={archiveChunks} disabled={busy} onChange={event => setArchiveChunks(event.target.checked)} />
          <span><strong>文档切片归档</strong><small>处理新闻、公告、财报等非结构化证据</small></span>
        </label>
        <label className={`${runGraph ? "selected " : ""}graph-option`.trim()}>
          <input type="checkbox" checked={runGraph} disabled={busy} onChange={event => setRunGraph(event.target.checked)} />
          <span><strong>生成知识图谱投影</strong><small><b>run_graph</b>：不勾选就不会更新物化图谱</small></span>
        </label>
        <label className={runAgentGovernance ? "selected" : ""}>
          <input type="checkbox" checked={runAgentGovernance} disabled={busy} onChange={event => setRunAgentGovernance(event.target.checked)} />
          <span><strong>智能体 + 技能治理</strong><small>审核多来源覆盖与知识生产质量</small></span>
        </label>
        <label className={includeCompanyTables ? "selected" : ""}>
          <input type="checkbox" checked={includeCompanyTables} disabled={busy} onChange={event => setIncludeCompanyTables(event.target.checked)} />
          <span><strong>包含公司层数据</strong><small>将公司、映射、分类、事实与证据纳入质量和湖仓处理</small></span>
        </label>
      </fieldset>

      {!runGraph && <div className="knowledge-pipeline-message warning" role="status">
        当前未勾选“生成知识图谱投影”。本次仍可更新湖仓和切片，但不会生成新的物化知识图谱。
      </div>}

      <details className="knowledge-pipeline-advanced" open={advancedOpen} onToggle={event => setAdvancedOpen(event.currentTarget.open)}>
        <summary>高级参数</summary>
        <div>
          <label>单来源数据上限<input type="number" min={1} max={100000} value={datasetLimit} disabled={busy} onChange={event => setDatasetLimit(Number(event.target.value))} /></label>
          <label>智能体审核上限<input type="number" min={1} max={500} value={governanceRecordLimit} disabled={busy} onChange={event => setGovernanceRecordLimit(Number(event.target.value))} /></label>
          <label>单股图谱文档上限<input type="number" min={1} max={5000} value={graphDocumentsPerStock} disabled={busy || !runGraph} onChange={event => setGraphDocumentsPerStock(Number(event.target.value))} /></label>
          <label>切片文档总上限<input type="number" min={1} max={100000} value={chunkDocumentLimit} disabled={busy || !archiveChunks} onChange={event => setChunkDocumentLimit(Number(event.target.value))} /></label>
          <label>单片长度<input type="number" min={300} max={10000} value={chunkSize} disabled={busy || !archiveChunks} onChange={event => setChunkSize(Number(event.target.value))} /></label>
          <label>重叠长度<input type="number" min={0} max={2000} value={overlap} disabled={busy || !archiveChunks} onChange={event => setOverlap(Number(event.target.value))} /></label>
        </div>
      </details>

      <div className="knowledge-pipeline-submit-row">
        <div>
          <strong>{marketNames[market]} · {symbols.length} 只股票</strong>
          <span>{runGraph ? "将生成独立图谱投影并保留原图" : "本次不生成图谱投影"}</span>
        </div>
        <button className="primary-button" type="submit" disabled={busy || catalogLoading || !catalog || !!catalogError}>
          {busy ? "正在同步并更新知识…" : "开始同步并更新知识"}
        </button>
      </div>
    </form>

    <section className="knowledge-pipeline-progress" aria-live="polite">
      <div className="knowledge-pipeline-section-heading">
        <div><h3>执行步骤</h3><p>步骤状态以服务端持久化运行记录为准。</p></div>
        {result && <span className={`knowledge-pipeline-run-status state-${normalizeStatus(result.status).toLowerCase()}`}>
          运行 #{result.pipeline_run_id} · {statusLabel(result.status)}
        </span>}
      </div>
      <ol>
        {steps.map((step, index) => <li key={step.code} className={`state-${step.state.toLowerCase()}`}>
          <span className="knowledge-pipeline-step-number">{step.state === "SUCCESS" ? "✓" : index + 1}</span>
          <div><strong>{step.title}</strong><p>{step.description}</p></div>
          <span className="knowledge-pipeline-step-state">{statusLabel(step.state)}</span>
        </li>)}
      </ol>
    </section>

    {result && <section className="knowledge-pipeline-result">
      <div className="knowledge-pipeline-section-heading">
        <div><h3>本次响应摘要</h3><p>{marketNames[result.market] || result.market} · {result.symbols.slice(0, 20).join("、")}{result.symbols.length > 20 ? ` 等 ${result.symbols.length} 只` : ""}</p></div>
        <span className={result.completion.model_selection_ready ? "ready" : "not-ready"}>
          {result.completion.model_selection_ready ? "可进入模型选股" : "尚未达到选股就绪"}
        </span>
      </div>
      <div className="knowledge-pipeline-metrics">
        <div><small>股票—公司映射</small><strong>{result.identity.mapped} / {result.identity.requested}</strong><span>未映射 {result.identity.unmapped}</span></div>
        <div><small>质量门禁</small><strong>{qualityPassed} / {qualityRows.length}</strong><span>通过的数据来源</span></div>
        <div><small>湖仓发布</small><strong>{exportsPublished} / {exportRows.length}</strong><span>已发布数据集</span></div>
        <div><small>文档切片</small><strong>{result.chunks.documents_with_chunks ?? result.chunks.documents} / {result.chunks.documents_total ?? 0}</strong><span>文档选择覆盖 {percentage(result.chunks.document_selection_ratio)}</span></div>
        <div><small>图谱实体 / 关系</small><strong>{graphCounts.entities_created ?? 0} / {graphCounts.relations_created ?? 0}</strong><span>证据文档 {graphCounts.documents_created ?? 0}</span></div>
      </div>

      {!!result.completion.issues.length && <div className="knowledge-pipeline-issues">
        <h4>仍需处理的事项</h4>
        <ul>{result.completion.issues.map((issue, index) => <li key={`${issue.code}-${index}`}>
          <strong>{issueNames[issue.code] || issue.code}</strong>
          <span>{stageNames[issue.stage] || issue.stage}</span>
        </li>)}</ul>
      </div>}

      <div className={`knowledge-pipeline-next-action ${projectionGraphId ? "has-projection" : ""}`}>
        <div>
          <small>下一步</small>
          {projectionGraphId ? <>
            <h4>治理投影图 #{projectionGraphId}</h4>
            <p>本次从基础图谱 #{result.base_graph_id} 生成了独立投影。投影默认处于“待治理”，请先在知识图谱清单核对范围和来源，再执行治理；达到“已治理”或“已锁定”后，才能作为真实模型选股的正式知识来源。</p>
            <div className="knowledge-pipeline-graph-counts">
              <span>投影 graph_id：<code>{projectionGraphId}</code></span>
              <span>当前状态：{statusLabel(projectionGraph?.governance_status || result.completion.graph_governance_status || "PENDING")}</span>
            </div>
          </> : <>
            <h4>{displayedGraph ? "检查图谱构建结果" : "需要图谱时重新运行"}</h4>
            <p>{displayedGraph
              ? "本次没有生成独立投影图，请查看知识图谱步骤和响应明细中的失败原因。"
              : "本次只更新了已勾选的数据和知识加工环节。如需更新物化图谱，请勾选“生成知识图谱投影（run_graph）”后重新运行。"}</p>
          </>}
        </div>
        {projectionGraphId && onRequestProjectionGovernance && <button type="button" className="primary-button" onClick={() => onRequestProjectionGovernance(projectionGraphId)}>
          前往图谱清单治理
        </button>}
      </div>

      <details className="knowledge-pipeline-raw-result">
        <summary>查看完整响应与诊断信息</summary>
        <pre>{JSON.stringify({ result, stages: runDetail?.stages || [] }, null, 2)}</pre>
      </details>
    </section>}
  </section>;
}

export default KnowledgePipelinePanel;
