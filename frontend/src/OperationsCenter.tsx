import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  Activity,
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  Bot,
  CheckCircle2,
  CircleDashed,
  Clock3,
  Database,
  Filter,
  GitBranch,
  RefreshCw,
  Search,
  Server,
  SlidersHorizontal,
  Wrench,
  XCircle,
} from "lucide-react";
import {
  api,
  type DataAsset,
  type DataInterface,
  type DataSource,
  type KnowledgeGraph,
  type LakehouseStatus,
  type ModelCallLog,
  type SkillOptimizationDraft,
  type SyncLog,
} from "./api";
import { batchGovernanceApi, type GovernanceRecord } from "./batchGovernanceApi";
import type { PlatformNavigationTarget } from "./PlatformOverview";
import "./OperationsCenterExtra.css";

export type OperationTaskStatus = "pending" | "running" | "success" | "warning" | "failed";

export type OperationTask = {
  id: string;
  category: "数据同步" | "数据治理" | "图谱治理" | "模型调用" | "技能审核" | "基础设施";
  title: string;
  detail: string;
  status: OperationTaskStatus;
  statusLabel: string;
  time?: string;
  target: PlatformNavigationTarget;
  metadata: Record<string, string | number | undefined>;
};

export type OperationsCenterProps = {
  className?: string;
  refreshKey?: string | number;
  onNavigate?: (target: PlatformNavigationTarget) => void;
  onOpenTask?: (task: OperationTask) => void;
  onRetrySync?: (log: SyncLog) => void;
};

type OperationsSnapshot = {
  sources: DataSource[];
  interfaces: DataInterface[];
  syncLogs: SyncLog[];
  assets: DataAsset[];
  graphs: KnowledgeGraph[];
  modelLogs: ModelCallLog[];
  skillDrafts: SkillOptimizationDraft[];
  lakehouse: LakehouseStatus | null;
  batchJobs: GovernanceRecord[];
  failedModules: string[];
  loadedAt: Date | null;
};

const emptySnapshot: OperationsSnapshot = {
  sources: [],
  interfaces: [],
  syncLogs: [],
  assets: [],
  graphs: [],
  modelLogs: [],
  skillDrafts: [],
  lakehouse: null,
  batchJobs: [],
  failedModules: [],
  loadedAt: null,
};

const operationsCss = `
.qoc-root { color:#1d2d27; display:grid; gap:18px; min-width:0; }
.qoc-root button,.qoc-root input,.qoc-root select { font:inherit; }
.qoc-head { display:flex; justify-content:space-between; align-items:flex-start; gap:20px; padding:24px 26px; border:1px solid #d5dbd1; border-radius:16px; background:linear-gradient(135deg,#fffdf6,#eff3eb); box-shadow:0 13px 32px rgba(36,60,49,.07); }
.qoc-eyebrow { margin:0 0 7px; color:#8d6c2c; font-size:11px; font-weight:700; letter-spacing:.14em; }
.qoc-head h1 { margin:0; font-size:clamp(24px,3vw,34px); }
.qoc-head p:last-child { max-width:720px; margin:9px 0 0; color:#68766f; font-size:13px; line-height:1.7; }
.qoc-head-actions { display:flex; gap:9px; flex-wrap:wrap; justify-content:flex-end; }
.qoc-button { display:inline-flex; align-items:center; justify-content:center; gap:7px; min-height:38px; padding:8px 13px; border:1px solid #c8d0c6; border-radius:9px; color:#2b493d; background:#fffdf7; font-size:12px; font-weight:650; transition:.18s ease; }
.qoc-button:hover { border-color:#6b8679; transform:translateY(-1px); }
.qoc-button.primary { border-color:#294c3f; color:#fffaf0; background:#294c3f; }
.qoc-button:disabled { opacity:.55; cursor:not-allowed; transform:none; }
.qoc-button svg.spin { animation:qoc-spin .9s linear infinite; }
@keyframes qoc-spin { to { transform:rotate(360deg); } }
.qoc-summary { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:11px; }
.qoc-summary-card { min-width:0; padding:16px; border:1px solid #d7dcd2; border-radius:13px; background:#fbfaf4; }
.qoc-summary-card > span { display:flex; align-items:center; justify-content:space-between; gap:8px; color:#6a7870; font-size:11px; }
.qoc-summary-card i { display:grid; place-items:center; width:28px; height:28px; border-radius:8px; font-style:normal; }
.qoc-summary-card strong { display:block; margin-top:10px; font:650 27px/1 Georgia,serif; }
.qoc-summary-card p { margin:6px 0 0; color:#7b8881; font-size:10px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.qoc-summary-card.total i { color:#3f6255; background:#e4ece6; }
.qoc-summary-card.pending i,.qoc-summary-card.warning i { color:#98691c; background:#f7ebca; }
.qoc-summary-card.running i { color:#35668b; background:#dfebf3; }
.qoc-summary-card.failed i { color:#a54234; background:#f5ddd8; }
.qoc-summary-card.success i { color:#27704e; background:#dfefe3; }
.qoc-error { display:flex; align-items:flex-start; gap:9px; padding:11px 14px; border:1px solid #eac8c0; border-radius:10px; color:#8f3c31; background:#fff0ec; font-size:12px; line-height:1.6; }
.qoc-layout { display:grid; grid-template-columns:minmax(0,1.55fr) minmax(290px,.65fr); gap:15px; align-items:start; }
.qoc-panel { min-width:0; border:1px solid #d7dbd1; border-radius:14px; background:#fbfaf4; overflow:hidden; }
.qoc-panel-head { display:flex; align-items:center; justify-content:space-between; gap:14px; padding:16px 17px; border-bottom:1px solid #e1e4dc; }
.qoc-panel-head h2,.qoc-panel-head h3 { margin:0; font-size:15px; }
.qoc-subtle { margin:4px 0 0; color:#7a8780; font-size:10px; }
.qoc-filters { display:grid; grid-template-columns:minmax(170px,1fr) 145px 135px; gap:9px; padding:12px 14px; border-bottom:1px solid #e3e5de; background:#f4f4ec; }
.qoc-field { position:relative; min-width:0; }
.qoc-field > svg { position:absolute; z-index:1; top:50%; left:10px; color:#798980; transform:translateY(-50%); }
.qoc-field input,.qoc-field select { width:100%; height:36px; border:1px solid #cfd5cd; border-radius:8px; padding:7px 10px; color:#263b33; background:#fffdf8; font-size:11px; outline:none; }
.qoc-field input { padding-left:32px; }
.qoc-field input:focus,.qoc-field select:focus { border-color:#668578; box-shadow:0 0 0 3px rgba(73,111,94,.09); }
.qoc-table-wrap { max-width:100%; overflow:auto; }
.qoc-table { width:100%; min-width:760px; border-collapse:collapse; }
.qoc-table th { padding:10px 13px; color:#75827b; background:#f7f6f0; font-size:10px; font-weight:650; text-align:left; white-space:nowrap; }
.qoc-table td { padding:12px 13px; border-top:1px solid #e5e7e0; color:#405148; font-size:11px; vertical-align:middle; }
.qoc-task-name { display:flex; align-items:center; gap:10px; min-width:235px; }
.qoc-task-icon { display:grid; place-items:center; flex:0 0 auto; width:31px; height:31px; border-radius:8px; color:#4d6d60; background:#e8eee8; }
.qoc-task-name strong { display:block; max-width:370px; color:#1f342c; font-size:12px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.qoc-task-name small { display:block; max-width:400px; margin-top:3px; color:#849088; font-size:9px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.qoc-badge { display:inline-flex; align-items:center; gap:5px; padding:5px 8px; border-radius:999px; background:#e9ede9; white-space:nowrap; }
.qoc-badge.pending,.qoc-badge.warning { color:#8e6119; background:#f7ebcb; }
.qoc-badge.running { color:#32617f; background:#dfebf3; }
.qoc-badge.failed { color:#9c3e32; background:#f5ddd8; }
.qoc-badge.success { color:#246846; background:#dfefe3; }
.qoc-table-action { display:inline-flex; align-items:center; gap:4px; border:0; padding:5px; color:#426559; background:transparent; font-size:10px; }
.qoc-table-action:hover { color:#153f31; }
.qoc-empty { padding:40px 20px; color:#738078; text-align:center; }
.qoc-empty svg { display:block; margin:0 auto 9px; color:#688b79; }
.qoc-side { display:grid; gap:15px; }
.qoc-status-list { display:grid; }
.qoc-status-row { display:grid; grid-template-columns:33px minmax(0,1fr) auto; gap:10px; align-items:center; padding:12px 15px; border-bottom:1px solid #e5e7e0; }
.qoc-status-row:last-child { border-bottom:0; }
.qoc-status-icon { display:grid; place-items:center; width:31px; height:31px; border-radius:8px; color:#49695c; background:#e6ede7; }
.qoc-status-row strong { display:block; font-size:11px; }
.qoc-status-row small { display:block; margin-top:3px; color:#7d8982; font-size:9px; line-height:1.45; }
.qoc-status-value { color:#355748; font:650 16px/1 Georgia,serif; }
.qoc-queue { display:grid; }
.qoc-queue-item { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:10px; padding:12px 15px; border-bottom:1px solid #e5e7e0; }
.qoc-queue-item:last-child { border-bottom:0; }
.qoc-queue-item strong { display:block; font-size:11px; }
.qoc-queue-item p { margin:4px 0 0; color:#7c8982; font-size:9px; line-height:1.5; }
.qoc-queue-item button { align-self:center; display:inline-flex; align-items:center; gap:4px; border:0; color:#46675b; background:transparent; font-size:10px; }
.qoc-footer-note { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:10px 14px; border-top:1px solid #e3e5de; color:#7a8780; font-size:9px; }
@media (max-width:1050px) { .qoc-summary { grid-template-columns:repeat(3,minmax(0,1fr)); } .qoc-layout { grid-template-columns:1fr; } .qoc-side { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media (max-width:720px) { .qoc-head { flex-direction:column; padding:20px; } .qoc-head-actions { justify-content:flex-start; } .qoc-summary { grid-template-columns:repeat(2,minmax(0,1fr)); } .qoc-filters { grid-template-columns:1fr; } .qoc-side { grid-template-columns:1fr; } }
@media (max-width:470px) { .qoc-summary { grid-template-columns:1fr; } }
`;

const statusLabel: Record<OperationTaskStatus, string> = {
  pending: "待处理",
  running: "执行中",
  success: "已完成",
  warning: "需关注",
  failed: "执行失败",
};

function normalizeStatus(status?: string): OperationTaskStatus {
  const value = String(status || "").toUpperCase();
  if (["FAILED", "FAILURE", "ERROR", "UNAVAILABLE", "MISSING"].includes(value)) return "failed";
  if (["RUNNING", "PROCESSING", "STARTED"].includes(value)) return "running";
  if (["SUCCESS", "SUCCEEDED", "COMPLETED", "GOVERNED", "LOCKED", "READY", "PASS"].includes(value)) return "success";
  if (["PENDING_REVIEW", "PARTIAL", "WARNING", "SCHEMA_CHANGED"].includes(value)) return "warning";
  return "pending";
}

function formatDate(value?: string) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("zh-CN", { hour12: false });
}

function settledValue<T>(result: PromiseSettledResult<T>, fallback: T, moduleName: string, failures: string[]) {
  if (result.status === "fulfilled") return result.value;
  failures.push(moduleName);
  return fallback;
}

function taskIcon(category: OperationTask["category"]): ReactNode {
  if (category === "数据同步") return <Database size={15} />;
  if (category === "数据治理") return <SlidersHorizontal size={15} />;
  if (category === "图谱治理") return <GitBranch size={15} />;
  if (category === "模型调用") return <Bot size={15} />;
  if (category === "技能审核") return <Wrench size={15} />;
  return <Server size={15} />;
}

function statusIcon(status: OperationTaskStatus) {
  if (status === "success") return <CheckCircle2 size={12} />;
  if (status === "failed") return <XCircle size={12} />;
  if (status === "running") return <Activity size={12} />;
  if (status === "warning") return <AlertTriangle size={12} />;
  return <Clock3 size={12} />;
}

function SummaryCard({ icon, label, value, detail, tone }: { icon: ReactNode; label: string; value: number; detail: string; tone: string }) {
  return (
    <article className={`qoc-summary-card ${tone}`}>
      <span>{label}<i>{icon}</i></span>
      <strong>{value.toLocaleString("zh-CN")}</strong>
      <p title={detail}>{detail}</p>
    </article>
  );
}

export function OperationsCenter({
  className = "",
  refreshKey,
  onNavigate,
  onOpenTask,
  onRetrySync,
}: OperationsCenterProps) {
  const [snapshot, setSnapshot] = useState<OperationsSnapshot>(emptySnapshot);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState<"all" | OperationTaskStatus>("all");
  const [categoryFilter, setCategoryFilter] = useState<"all" | OperationTask["category"]>("all");
  const [selectedTask, setSelectedTask] = useState<OperationTask | null>(null);
  const [taskPage, setTaskPage] = useState(0);
  const taskPageSize = 20;

  const load = useCallback(async () => {
    setLoading(true);
    const results = await Promise.allSettled([
      api.listSources(),
      api.listInterfaces(),
      api.listSyncLogs(),
      api.listDataAssets(),
      api.listKnowledgeGraphs(),
      api.listModelCallLogs(40),
      api.listSkillDrafts(),
      api.lakehouseStatus(),
      batchGovernanceApi.listRecords({ limit: 100, offset: 0 }),
    ] as const);
    const failures: string[] = [];
    setSnapshot({
      sources: settledValue(results[0], [], "数据源", failures),
      interfaces: settledValue(results[1], [], "接口目录", failures),
      syncLogs: settledValue(results[2], [], "同步日志", failures),
      assets: settledValue(results[3], [], "数据资产治理", failures),
      graphs: settledValue(results[4], [], "知识图谱治理", failures),
      modelLogs: settledValue(results[5], [], "模型调用日志", failures),
      skillDrafts: settledValue(results[6], [], "技能审核", failures),
      lakehouse: settledValue(results[7], null, "湖仓状态", failures),
      batchJobs: results[8].status === "fulfilled" ? results[8].value.items || [] : (failures.push("股票批量治理任务"), []),
      failedModules: failures,
      loadedAt: new Date(),
    });
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load, refreshKey]);

  const interfaceNames = useMemo(() => new Map(snapshot.interfaces.map((item) => [item.interface_code, item.interface_name])), [snapshot.interfaces]);
  const sourceNames = useMemo(() => new Map(snapshot.sources.map((item) => [item.id, item.source_name])), [snapshot.sources]);

  const tasks = useMemo<OperationTask[]>(() => {
    const syncTasks: OperationTask[] = snapshot.syncLogs.map((log) => {
      const status = normalizeStatus(log.status);
      const source = sourceNames.get(log.source_id) || `数据源 #${log.source_id}`;
      return {
        id: `sync-${log.id}`,
        category: "数据同步",
        title: `${interfaceNames.get(log.interface_code) || "数据同步"} · ${log.market}`,
        detail: log.error_message || `${source}：共 ${log.total_count} 条，新增 ${log.inserted_count} 条，更新 ${log.updated_count} 条`,
        status,
        statusLabel: statusLabel[status],
        time: log.completed_at || log.started_at,
        target: "data-sources",
        metadata: { logId: log.id, interfaceCode: log.interface_code, market: log.market, sourceId: log.source_id },
      };
    });
    const assetTasks: OperationTask[] = snapshot.assets.map((asset) => {
      const status = normalizeStatus(asset.governance_status);
      return {
        id: `asset-${asset.id}`,
        category: "数据治理",
        title: asset.display_name,
        detail: `${asset.table_name} · ${asset.row_count.toLocaleString("zh-CN")} 条记录 · 来源${asset.source_health === "HEALTHY" ? "正常" : "待检查"}`,
        status,
        statusLabel: statusLabel[status],
        time: asset.last_governed_at || asset.updated_at,
        target: "lakehouse",
        metadata: { assetId: asset.id, tableName: asset.table_name, governanceStatus: asset.governance_status },
      };
    });
    const graphTasks: OperationTask[] = snapshot.graphs.map((graph) => {
      const status = normalizeStatus(graph.governance_status);
      return {
        id: `graph-${graph.id}`,
        category: "图谱治理",
        title: graph.graph_name,
        detail: `${graph.entity_count.toLocaleString("zh-CN")} 个实体 · ${graph.relation_count.toLocaleString("zh-CN")} 条关系 · 版本 ${graph.version}`,
        status,
        statusLabel: statusLabel[status],
        time: graph.last_governed_at || undefined,
        target: "knowledge-graphs",
        metadata: { graphId: graph.id, graphCode: graph.graph_code, governanceStatus: graph.governance_status },
      };
    });
    const modelTasks: OperationTask[] = snapshot.modelLogs.map((log) => {
      const status = normalizeStatus(log.status);
      const modelName = log.instance_code || log.model_code || "默认模型路由";
      return {
        id: `model-${log.id}`,
        category: "模型调用",
        title: `模型任务：${log.task_type}`,
        detail: log.error_message || `${log.provider_code || "模型服务"} / ${modelName}${log.latency_ms ? ` · ${log.latency_ms}ms` : ""}`,
        status,
        statusLabel: statusLabel[status],
        time: log.completed_at || log.started_at,
        target: "models",
        metadata: { callLogId: log.id, taskType: log.task_type, instanceCode: log.instance_code },
      };
    });
    const skillTasks: OperationTask[] = snapshot.skillDrafts.map((draft) => ({
      id: `skill-${draft.id}`,
      category: "技能审核",
      title: `技能 #${draft.skill_id} 优化草案`,
      detail: draft.rationale || `基于版本 ${draft.base_skill_version} 生成，等待人工审核。`,
      status: normalizeStatus(draft.status),
      statusLabel: statusLabel[normalizeStatus(draft.status)],
      time: draft.created_at,
      target: "skills",
      metadata: { draftId: draft.id, skillId: draft.skill_id, baseVersion: draft.base_skill_version },
    }));
    const batchTasks: OperationTask[] = snapshot.batchJobs.map((job) => {
      const status = normalizeStatus(job.result_status || job.status);
      const stockNames = job.stock_names?.filter(Boolean).slice(0, 3).join("、") || `${job.stock_count} 只股票`;
      return {
        id: `batch-governance-${job.job_id}`,
        category: "数据治理",
        title: `股票批量治理任务 #${job.job_id}`,
        detail: `${stockNames} · ${job.current_stage || "待执行"} · ${job.governance_mode === "AI_AGENT_SKILL_GOVERNANCE" ? "AI + Agent + Skill" : "系统治理"}`,
        status,
        statusLabel: statusLabel[status],
        time: job.completed_at || job.started_at || job.created_at,
        target: "governance-records",
        metadata: { jobId: job.job_id, pipelineRunId: job.pipeline_run_id || undefined, stage: job.current_stage || undefined, progress: job.progress ?? 0 },
      };
    });
    const infrastructureTasks: OperationTask[] = snapshot.lakehouse ? [{
      id: "lakehouse-health",
      category: "基础设施",
      title: "湖仓对象存储",
      detail: `${snapshot.lakehouse.storage_backend} · ${snapshot.lakehouse.storage_health.detail}`,
      status: snapshot.lakehouse.storage_health.healthy ? "success" : "failed",
      statusLabel: snapshot.lakehouse.storage_health.healthy ? "运行正常" : "服务异常",
      target: "lakehouse",
      metadata: { backend: snapshot.lakehouse.storage_backend },
    }] : [];
    return [...syncTasks, ...assetTasks, ...graphTasks, ...modelTasks, ...skillTasks, ...batchTasks, ...infrastructureTasks].sort((a, b) => {
      const left = a.time ? new Date(a.time).getTime() : 0;
      const right = b.time ? new Date(b.time).getTime() : 0;
      return right - left;
    });
  }, [interfaceNames, snapshot.assets, snapshot.batchJobs, snapshot.graphs, snapshot.lakehouse, snapshot.modelLogs, snapshot.skillDrafts, snapshot.syncLogs, sourceNames]);

  const filteredTasks = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLocaleLowerCase("zh-CN");
    return tasks.filter((task) => {
      if (statusFilter !== "all" && task.status !== statusFilter) return false;
      if (categoryFilter !== "all" && task.category !== categoryFilter) return false;
      if (!normalizedKeyword) return true;
      return `${task.title} ${task.detail} ${task.category} ${task.statusLabel}`.toLocaleLowerCase("zh-CN").includes(normalizedKeyword);
    });
  }, [categoryFilter, keyword, statusFilter, tasks]);

  useEffect(() => { setTaskPage(0); }, [categoryFilter, keyword, statusFilter]);

  const counts = useMemo(() => ({
    pending: tasks.filter((item) => item.status === "pending").length,
    running: tasks.filter((item) => item.status === "running").length,
    warning: tasks.filter((item) => item.status === "warning").length,
    failed: tasks.filter((item) => item.status === "failed").length,
    success: tasks.filter((item) => item.status === "success").length,
  }), [tasks]);

  const governanceQueue = useMemo(() => tasks.filter((item) => ["pending", "warning", "failed"].includes(item.status) && item.category !== "模型调用").slice(0, 7), [tasks]);
  const recentSuccessRate = tasks.length ? Math.round(counts.success / tasks.length * 100) : 0;
  const totalTaskPages = Math.max(1, Math.ceil(filteredTasks.length / taskPageSize));
  const visibleTasks = filteredTasks.slice(taskPage * taskPageSize, (taskPage + 1) * taskPageSize);

  function openTask(task: OperationTask) {
    if (onOpenTask) { onOpenTask(task); return; }
    // Governance and infrastructure rows need context before navigation;
    // show the durable task details instead of sending every action to lakehouse.
    if (["数据治理", "图谱治理", "技能审核", "基础设施"].includes(task.category)) setSelectedTask(task);
    else onNavigate?.(task.target);
  }

  return (
    <section className={`qoc-root ${className}`.trim()} aria-busy={loading}>
      <style>{operationsCss}</style>
      <header className="qoc-head">
        <div>
          <p className="qoc-eyebrow">OPERATIONS & GOVERNANCE · 运营治理</p>
          <h1>任务、质量与审核集中处理</h1>
          <p>统一查看数据同步、资产治理、图谱治理、模型调用和技能优化任务；从异常直接回到对应业务模块处理。</p>
        </div>
        <div className="qoc-head-actions">
          <button className="qoc-button primary" type="button" onClick={() => onNavigate?.("master-data")}><Database size={15} />发起数据同步</button>
          <button className="qoc-button" type="button" disabled={loading} onClick={() => void load()}><RefreshCw size={14} className={loading ? "spin" : ""} />{loading ? "刷新中" : "刷新任务"}</button>
        </div>
      </header>

      {snapshot.failedModules.length > 0 && (
        <div className="qoc-error" role="status"><AlertCircle size={16} /><span>部分任务源读取失败：{snapshot.failedModules.join("、")}。当前列表保留其他可用模块的数据。</span></div>
      )}

      <div className="qoc-summary">
        <SummaryCard icon={<Activity size={15} />} label="任务总数" value={tasks.length} detail="当前聚合的运行与治理记录" tone="total" />
        <SummaryCard icon={<Clock3 size={15} />} label="待处理/需关注" value={counts.pending + counts.warning} detail="待治理、待审核或质量告警" tone="pending" />
        <SummaryCard icon={<CircleDashed size={15} />} label="执行中" value={counts.running} detail="正在同步或运行中的任务" tone="running" />
        <SummaryCard icon={<XCircle size={15} />} label="失败" value={counts.failed} detail="需要查看错误并重新执行" tone="failed" />
        <SummaryCard icon={<CheckCircle2 size={15} />} label="已完成" value={counts.success} detail={`聚合任务完成率 ${recentSuccessRate}%`} tone="success" />
      </div>

      <div className="qoc-layout">
        <section className="qoc-panel">
          <header className="qoc-panel-head">
            <div><h2>统一任务流水</h2><p className="qoc-subtle">同步、治理、模型调用与人工审核记录</p></div>
            <span className="qoc-subtle">显示 {visibleTasks.length} / {filteredTasks.length}</span>
          </header>
          <div className="qoc-filters">
            <label className="qoc-field"><Search size={14} /><input value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索任务、数据表或错误信息" /></label>
            <label className="qoc-field"><select aria-label="按状态筛选" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as typeof statusFilter)}><option value="all">全部状态</option><option value="pending">待处理</option><option value="warning">需关注</option><option value="running">执行中</option><option value="failed">执行失败</option><option value="success">已完成</option></select></label>
            <label className="qoc-field"><select aria-label="按类型筛选" value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value as typeof categoryFilter)}><option value="all">全部类型</option><option value="数据同步">数据同步</option><option value="数据治理">数据治理</option><option value="图谱治理">图谱治理</option><option value="模型调用">模型调用</option><option value="技能审核">技能审核</option><option value="基础设施">基础设施</option></select></label>
          </div>
          {filteredTasks.length ? (
            <div className="qoc-table-wrap">
              <table className="qoc-table">
                <thead><tr><th>任务</th><th>类型</th><th>状态</th><th>时间</th><th>操作</th></tr></thead>
                <tbody>
                  {visibleTasks.map((task) => (
                    <tr key={task.id}>
                      <td><div className="qoc-task-name"><span className="qoc-task-icon">{taskIcon(task.category)}</span><span><strong title={task.title}>{task.title}</strong><small title={task.detail}>{task.detail}</small></span></div></td>
                      <td>{task.category}</td>
                      <td><span className={`qoc-badge ${task.status}`}>{statusIcon(task.status)}{task.statusLabel}</span></td>
                      <td>{formatDate(task.time)}</td>
                      <td>
                        {task.category === "数据同步" && task.status === "failed" && onRetrySync ? (
                          <button className="qoc-table-action" type="button" onClick={() => { const log = snapshot.syncLogs.find((item) => `sync-${item.id}` === task.id); if (log) onRetrySync(log); }}>重试 <RefreshCw size={11} /></button>
                        ) : (
                          <button className="qoc-table-action" type="button" onClick={() => openTask(task)}>查看 <ArrowRight size={11} /></button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="qoc-empty"><Filter size={23} />没有符合当前筛选条件的任务</div>
          )}
          <footer className="qoc-footer-note"><span>{snapshot.loadedAt ? `最近刷新：${snapshot.loadedAt.toLocaleString("zh-CN")}` : "正在读取任务"}</span><span className="qoc-pagination"><button type="button" disabled={taskPage === 0} onClick={() => setTaskPage((value) => Math.max(0, value - 1))}>上一页</button><span>第 {taskPage + 1} / {totalTaskPages} 页</span><button type="button" disabled={taskPage + 1 >= totalTaskPages} onClick={() => setTaskPage((value) => value + 1)}>下一页</button></span></footer>
        </section>

        <aside className="qoc-side">
          <section className="qoc-panel">
            <header className="qoc-panel-head"><div><h3>平台运行状态</h3><p className="qoc-subtle">关键服务与治理覆盖</p></div></header>
            <div className="qoc-status-list">
              <div className="qoc-status-row"><span className="qoc-status-icon"><Server size={15} /></span><span><strong>湖仓对象存储</strong><small>{snapshot.lakehouse?.storage_health.detail || "状态暂未读取"}</small></span><b className="qoc-status-value">{snapshot.lakehouse?.storage_health.healthy ? "正常" : "检查"}</b></div>
              <div className="qoc-status-row"><span className="qoc-status-icon"><Database size={15} /></span><span><strong>数据源与接口</strong><small>{snapshot.sources.filter((item) => item.enabled).length} 个数据源、{snapshot.interfaces.filter((item) => item.enabled).length} 个接口启用</small></span><b className="qoc-status-value">{snapshot.sources.filter((item) => item.enabled).length}/{snapshot.sources.length}</b></div>
              <div className="qoc-status-row"><span className="qoc-status-icon"><SlidersHorizontal size={15} /></span><span><strong>数据资产治理</strong><small>已治理或锁定的数据资产</small></span><b className="qoc-status-value">{snapshot.assets.filter((item) => ["GOVERNED", "LOCKED", "READY"].includes(item.governance_status)).length}/{snapshot.assets.length}</b></div>
              <div className="qoc-status-row"><span className="qoc-status-icon"><GitBranch size={15} /></span><span><strong>知识图谱治理</strong><small>已治理或锁定的物化图谱</small></span><b className="qoc-status-value">{snapshot.graphs.filter((item) => ["GOVERNED", "LOCKED"].includes(item.governance_status)).length}/{snapshot.graphs.length}</b></div>
            </div>
          </section>

          <section className="qoc-panel">
            <header className="qoc-panel-head"><div><h3>治理队列</h3><p className="qoc-subtle">优先展示失败、待审核与待治理项目</p></div></header>
            {governanceQueue.length ? (
              <div className="qoc-queue">
                {governanceQueue.map((task) => (
                  <article className="qoc-queue-item" key={`queue-${task.id}`}>
                    <div><strong>{task.title}</strong><p>{task.statusLabel} · {task.category}</p></div>
                    <button type="button" onClick={() => openTask(task)}>处理 <ArrowRight size={11} /></button>
                  </article>
                ))}
              </div>
            ) : (
              <div className="qoc-empty"><CheckCircle2 size={23} />当前治理队列为空</div>
            )}
          </section>
        </aside>
      </div>
      {selectedTask && <div className="qoc-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelectedTask(null); }}><section className="qoc-modal" role="dialog" aria-modal="true"><header><div><h2>{selectedTask.title}</h2><p>{selectedTask.category} · {selectedTask.statusLabel}</p></div><button type="button" onClick={() => setSelectedTask(null)}>关闭</button></header><div className="qoc-modal-body"><div className={`qoc-badge ${selectedTask.status}`}>{statusIcon(selectedTask.status)}{selectedTask.statusLabel}</div><p>{selectedTask.detail}</p><dl>{Object.entries(selectedTask.metadata).map(([key, value]) => <div key={key}><dt>{key}</dt><dd>{value == null ? "--" : String(value)}</dd></div>)}</dl><div className="qoc-modal-actions"><button type="button" className="qoc-button primary" onClick={() => { setSelectedTask(null); onNavigate?.(selectedTask.target); }}>进入对应模块</button>{["数据治理", "图谱治理"].includes(selectedTask.category) && <button type="button" className="qoc-button" onClick={() => { setSelectedTask(null); onNavigate?.("governance-records"); }}>查看治理记录</button>}</div></div></section></div>}
    </section>
  );
}

export default OperationsCenter;
