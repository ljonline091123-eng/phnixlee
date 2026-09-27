import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  Bot,
  BrainCircuit,
  Building2,
  CheckCircle2,
  Database,
  FileStack,
  GitBranch,
  Layers3,
  Network,
  RefreshCw,
  Server,
  Sparkles,
  Wrench,
} from "lucide-react";
import {
  api,
  type AgentDefinition,
  type DataAsset,
  type DataInterface,
  type DataSource,
  type KnowledgeBase,
  type KnowledgeGraph,
  type LakeDataset,
  type LakehouseStatus,
  type ModelInstance,
  type ModelProvider,
  type ModelRoute,
  type ModelSkill,
  type SkillOptimizationDraft,
  type SyncLog,
} from "./api";

export type PlatformNavigationTarget =
  | "data-sources"
  | "master-data"
  | "business-data"
  | "lakehouse"
  | "knowledge-bases"
  | "knowledge-graphs"
  | "models"
  | "agents"
  | "skills"
  | "operations"
  | "investment-workbench";

export type PlatformOverviewProps = {
  className?: string;
  refreshKey?: string | number;
  onNavigate?: (target: PlatformNavigationTarget) => void;
  onRunDataSync?: () => void;
  onRunKnowledgePipeline?: () => void;
};

type OverviewSnapshot = {
  sources: DataSource[];
  interfaces: DataInterface[];
  syncLogs: SyncLog[];
  symbolCount: number;
  lakehouse: LakehouseStatus | null;
  datasets: LakeDataset[];
  assets: DataAsset[];
  knowledgeBases: KnowledgeBase[];
  knowledgeGraphs: KnowledgeGraph[];
  providers: ModelProvider[];
  instances: ModelInstance[];
  routes: ModelRoute[];
  agents: AgentDefinition[];
  skills: ModelSkill[];
  skillDrafts: SkillOptimizationDraft[];
  loadedAt: Date | null;
  failedModules: string[];
};

type Tone = "good" | "warning" | "danger" | "neutral";

const emptySnapshot: OverviewSnapshot = {
  sources: [],
  interfaces: [],
  syncLogs: [],
  symbolCount: 0,
  lakehouse: null,
  datasets: [],
  assets: [],
  knowledgeBases: [],
  knowledgeGraphs: [],
  providers: [],
  instances: [],
  routes: [],
  agents: [],
  skills: [],
  skillDrafts: [],
  loadedAt: null,
  failedModules: [],
};

const overviewCss = `
.qpo-root { color:#1c2d27; display:grid; gap:20px; min-width:0; }
.qpo-root button { font:inherit; }
.qpo-hero { position:relative; overflow:hidden; display:grid; grid-template-columns:minmax(0,1.5fr) minmax(280px,.7fr); gap:22px; padding:28px; border:1px solid #d5d8ca; border-radius:18px; background:linear-gradient(135deg,#fffdf5 0%,#f4f3e8 63%,#e8eee5 100%); box-shadow:0 16px 42px rgba(38,58,48,.08); }
.qpo-hero::after { content:""; position:absolute; width:280px; height:280px; right:-110px; top:-150px; border-radius:50%; border:52px solid rgba(207,164,75,.12); pointer-events:none; }
.qpo-eyebrow { margin:0 0 8px; color:#8a6929; font-size:12px; font-weight:700; letter-spacing:.14em; }
.qpo-hero h1 { margin:0; font-size:clamp(25px,3vw,38px); line-height:1.16; font-weight:680; }
.qpo-hero-copy { max-width:760px; margin:12px 0 0; color:#65736b; font-size:14px; line-height:1.8; }
.qpo-hero-actions { display:flex; flex-wrap:wrap; gap:10px; margin-top:20px; }
.qpo-button { display:inline-flex; align-items:center; justify-content:center; gap:8px; min-height:39px; padding:9px 14px; border:1px solid #c9d0c7; border-radius:9px; color:#274239; background:#fffdf7; font-size:13px; font-weight:650; transition:.18s ease; }
.qpo-button:hover { border-color:#607d70; transform:translateY(-1px); }
.qpo-button.primary { border-color:#24483c; color:#fffaf0; background:#24483c; }
.qpo-button.ghost { background:transparent; }
.qpo-button:disabled { cursor:not-allowed; opacity:.55; transform:none; }
.qpo-health { position:relative; z-index:1; align-self:stretch; display:flex; flex-direction:column; justify-content:space-between; min-height:176px; padding:20px; border:1px solid rgba(64,91,77,.18); border-radius:14px; background:rgba(255,255,252,.72); backdrop-filter:blur(8px); }
.qpo-health-head { display:flex; justify-content:space-between; gap:14px; align-items:flex-start; }
.qpo-health-label { margin:0; color:#69786f; font-size:12px; }
.qpo-health-score { margin:4px 0 0; font-family:Georgia,serif; font-size:38px; line-height:1; }
.qpo-health-score small { color:#87948d; font:500 13px/1 sans-serif; }
.qpo-health-icon { display:grid; place-items:center; width:42px; height:42px; border-radius:50%; }
.qpo-health-icon.good { color:#25704e; background:#e0f1e5; }
.qpo-health-icon.warning { color:#9a691b; background:#f8edcf; }
.qpo-health-icon.danger { color:#a34435; background:#f7ded8; }
.qpo-health-meta { display:flex; flex-wrap:wrap; gap:8px; margin-top:16px; }
.qpo-chip { display:inline-flex; align-items:center; gap:5px; padding:5px 8px; border-radius:999px; color:#506159; background:#edf0e9; font-size:11px; }
.qpo-chip.good { color:#226344; background:#deefe3; }
.qpo-chip.warning { color:#8b611f; background:#f6ebcd; }
.qpo-chip.danger { color:#963c30; background:#f5ddd8; }
.qpo-toolbar { display:flex; justify-content:space-between; align-items:center; gap:16px; }
.qpo-toolbar h2, .qpo-section-head h2 { margin:0; font-size:18px; }
.qpo-subtle { margin:4px 0 0; color:#78857e; font-size:12px; }
.qpo-icon-button { display:inline-flex; align-items:center; gap:7px; border:0; padding:7px 2px; color:#51675d; background:transparent; font-size:12px; }
.qpo-icon-button:hover { color:#193e31; }
.qpo-icon-button svg.spinning { animation:qpo-spin .9s linear infinite; }
@keyframes qpo-spin { to { transform:rotate(360deg); } }
.qpo-metrics { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:12px; }
.qpo-metric { min-width:0; padding:17px; border:1px solid #d7dbd1; border-radius:13px; background:#fbfaf3; }
.qpo-metric-head { display:flex; justify-content:space-between; gap:10px; color:#66746d; font-size:12px; }
.qpo-metric-icon { display:grid; place-items:center; width:30px; height:30px; border-radius:8px; color:#48675a; background:#e7ede6; }
.qpo-metric strong { display:block; margin-top:13px; font-family:Georgia,serif; font-size:26px; font-weight:650; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.qpo-metric p { margin:5px 0 0; color:#7a877f; font-size:11px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.qpo-flow { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:10px; }
.qpo-flow-card { position:relative; min-width:0; min-height:142px; padding:16px; border:1px solid #d5dbd2; border-radius:13px; color:inherit; background:#fffdf7; text-align:left; transition:.18s ease; }
.qpo-flow-card:hover { border-color:#7b9488; box-shadow:0 8px 22px rgba(34,62,50,.08); transform:translateY(-2px); }
.qpo-flow-card:not(:last-child)::after { content:"→"; position:absolute; z-index:2; top:50%; right:-16px; width:22px; color:#93a197; font-weight:700; text-align:center; transform:translateY(-50%); }
.qpo-flow-top { display:flex; justify-content:space-between; gap:8px; align-items:center; }
.qpo-flow-index { color:#a17b31; font:600 11px/1 monospace; letter-spacing:.1em; }
.qpo-status-dot { width:8px; height:8px; border-radius:50%; background:#87948d; box-shadow:0 0 0 4px rgba(126,143,134,.12); }
.qpo-status-dot.good { background:#4b9a6d; box-shadow:0 0 0 4px rgba(75,154,109,.13); }
.qpo-status-dot.warning { background:#d09b3d; box-shadow:0 0 0 4px rgba(208,155,61,.13); }
.qpo-status-dot.danger { background:#bf5948; box-shadow:0 0 0 4px rgba(191,89,72,.12); }
.qpo-flow-card h3 { margin:18px 0 6px; font-size:15px; }
.qpo-flow-card p { margin:0; color:#718078; font-size:11px; line-height:1.65; }
.qpo-flow-value { display:block; margin-top:12px; color:#365648; font-size:12px; font-weight:700; }
.qpo-main-grid { display:grid; grid-template-columns:minmax(0,1.45fr) minmax(300px,.75fr); gap:16px; }
.qpo-panel { min-width:0; border:1px solid #d7dbd1; border-radius:14px; background:#fbfaf4; overflow:hidden; }
.qpo-panel-head, .qpo-section-head { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:17px 18px; border-bottom:1px solid #e2e4dc; }
.qpo-panel-head h3 { margin:0; font-size:15px; }
.qpo-link { display:inline-flex; align-items:center; gap:5px; padding:0; border:0; color:#46665a; background:transparent; font-size:12px; }
.qpo-link:hover { color:#153f31; }
.qpo-todos { display:grid; }
.qpo-todo { display:grid; grid-template-columns:34px minmax(0,1fr) auto; gap:11px; align-items:center; padding:13px 18px; border-bottom:1px solid #e7e8e1; }
.qpo-todo:last-child { border-bottom:0; }
.qpo-todo-icon { display:grid; place-items:center; width:32px; height:32px; border-radius:9px; }
.qpo-todo-icon.good { color:#26704e; background:#e2f0e5; }
.qpo-todo-icon.warning { color:#966718; background:#f6e9c8; }
.qpo-todo-icon.danger { color:#a54234; background:#f6ded9; }
.qpo-todo-icon.neutral { color:#577067; background:#e9ede9; }
.qpo-todo h4 { margin:0; font-size:13px; }
.qpo-todo p { margin:4px 0 0; color:#77847d; font-size:11px; line-height:1.5; }
.qpo-todo button { display:inline-flex; align-items:center; gap:4px; border:0; padding:6px; color:#46665a; background:transparent; font-size:11px; }
.qpo-empty { padding:28px 18px; color:#6f7e76; text-align:center; }
.qpo-empty svg { display:block; margin:0 auto 8px; color:#5e8a73; }
.qpo-ai-list { display:grid; gap:1px; background:#e3e5de; }
.qpo-ai-row { display:flex; justify-content:space-between; align-items:center; gap:12px; width:100%; border:0; padding:14px 17px; color:inherit; background:#fbfaf4; text-align:left; }
.qpo-ai-row:hover { background:#f3f4ec; }
.qpo-ai-label { display:flex; align-items:center; gap:10px; min-width:0; }
.qpo-ai-label span { display:grid; place-items:center; flex:0 0 auto; width:30px; height:30px; border-radius:8px; color:#45685a; background:#e7eee8; }
.qpo-ai-label strong { display:block; font-size:12px; }
.qpo-ai-label small { display:block; margin-top:3px; color:#7c8882; font-size:10px; }
.qpo-ai-value { flex:0 0 auto; font:650 17px/1 Georgia,serif; }
.qpo-error { display:flex; align-items:flex-start; gap:9px; padding:12px 15px; border:1px solid #ebc9c1; border-radius:10px; color:#8f3d31; background:#fff0ec; font-size:12px; line-height:1.6; }
@media (max-width:1100px) { .qpo-metrics { grid-template-columns:repeat(3,minmax(0,1fr)); } .qpo-flow { grid-template-columns:repeat(3,minmax(0,1fr)); } .qpo-flow-card::after { display:none; } }
@media (max-width:820px) { .qpo-hero, .qpo-main-grid { grid-template-columns:1fr; } .qpo-metrics { grid-template-columns:repeat(2,minmax(0,1fr)); } .qpo-flow { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media (max-width:540px) { .qpo-hero { padding:20px; } .qpo-metrics, .qpo-flow { grid-template-columns:1fr; } .qpo-toolbar { align-items:flex-start; } .qpo-todo { grid-template-columns:34px minmax(0,1fr); } .qpo-todo > button { grid-column:2; justify-self:start; padding-left:0; } }
`;

function formatCount(value: number) {
  return Number.isFinite(value) ? value.toLocaleString("zh-CN") : "0";
}

function isSuccess(status?: string) {
  return ["SUCCESS", "SUCCEEDED", "COMPLETED", "GOVERNED", "LOCKED", "READY", "PASS"].includes(
    String(status || "").toUpperCase(),
  );
}

function isFailure(status?: string) {
  return ["FAILED", "FAILURE", "ERROR", "UNAVAILABLE", "MISSING"].includes(String(status || "").toUpperCase());
}

function toneForRatio(done: number, total: number): Tone {
  if (!total) return "neutral";
  if (done === total) return "good";
  if (done / total >= 0.6) return "warning";
  return "danger";
}

function settledValue<T>(result: PromiseSettledResult<T>, fallback: T, moduleName: string, failures: string[]) {
  if (result.status === "fulfilled") return result.value;
  failures.push(moduleName);
  return fallback;
}

function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return (
    <article className="qpo-metric">
      <div className="qpo-metric-head">
        <span>{label}</span>
        <span className="qpo-metric-icon">{icon}</span>
      </div>
      <strong>{value}</strong>
      <p title={detail}>{detail}</p>
    </article>
  );
}

export function PlatformOverview({
  className = "",
  refreshKey,
  onNavigate,
  onRunDataSync,
  onRunKnowledgePipeline,
}: PlatformOverviewProps) {
  const [snapshot, setSnapshot] = useState<OverviewSnapshot>(emptySnapshot);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    const results = await Promise.allSettled([
      api.listSources(),
      api.listInterfaces(),
      api.listSyncLogs(),
      api.listSymbols(new URLSearchParams({ market: "ALL", keyword: "", page: "1", page_size: "1" })),
      api.lakehouseStatus(),
      api.lakehouseDatasets(),
      api.listDataAssets(),
      api.listKnowledgeBases(),
      api.listKnowledgeGraphs(),
      api.listModelProviders(),
      api.listModelInstances(),
      api.listModelRoutes(),
      api.listAgents(),
      api.listModelSkills(),
      api.listSkillDrafts(),
    ] as const);
    const failures: string[] = [];
    const sources = settledValue(results[0], [], "数据源", failures);
    const interfaces = settledValue(results[1], [], "接口目录", failures);
    const syncLogs = settledValue(results[2], [], "同步日志", failures);
    const symbols = settledValue(results[3], { items: [], total: 0, page: 1, page_size: 1 }, "股票主数据", failures);
    const lakehouse = settledValue(results[4], null, "湖仓状态", failures);
    const datasets = settledValue(results[5], [], "湖仓数据集", failures);
    const assets = settledValue(results[6], [], "数据资产", failures);
    const knowledgeBases = settledValue(results[7], [], "知识库", failures);
    const knowledgeGraphs = settledValue(results[8], [], "知识图谱", failures);
    const providers = settledValue(results[9], [], "模型供应商", failures);
    const instances = settledValue(results[10], [], "模型实例", failures);
    const routes = settledValue(results[11], [], "模型路由", failures);
    const agents = settledValue(results[12], [], "智能体", failures);
    const skills = settledValue(results[13], [], "技能", failures);
    const skillDrafts = settledValue(results[14], [], "技能优化草案", failures);
    setSnapshot({
      sources,
      interfaces,
      syncLogs,
      symbolCount: symbols.total,
      lakehouse,
      datasets,
      assets,
      knowledgeBases,
      knowledgeGraphs,
      providers,
      instances,
      routes,
      agents,
      skills,
      skillDrafts,
      loadedAt: new Date(),
      failedModules: failures,
    });
    setLoading(false);
  }, []);

  useEffect(() => {
    void load();
  }, [load, refreshKey]);

  const computed = useMemo(() => {
    const enabledSources = snapshot.sources.filter((item) => item.enabled).length;
    const enabledInterfaces = snapshot.interfaces.filter((item) => item.enabled).length;
    const governedAssets = snapshot.assets.filter((item) => ["GOVERNED", "LOCKED", "READY"].includes(item.governance_status)).length;
    const governedGraphs = snapshot.knowledgeGraphs.filter((item) => ["GOVERNED", "LOCKED"].includes(item.governance_status)).length;
    const failedSyncs = snapshot.syncLogs.filter((item) => isFailure(item.status)).length;
    const pendingAssets = snapshot.assets.filter((item) => !["GOVERNED", "LOCKED", "READY"].includes(item.governance_status)).length;
    const pendingGraphs = snapshot.knowledgeGraphs.filter((item) => !["GOVERNED", "LOCKED"].includes(item.governance_status)).length;
    const enabledInstances = snapshot.instances.filter((item) => item.enabled).length;
    const enabledAgents = snapshot.agents.filter((item) => item.enabled).length;
    const enabledSkills = snapshot.skills.filter((item) => item.enabled).length;
    const enabledRoutes = snapshot.routes.filter((item) => item.enabled).length;
    const sourceScore = snapshot.sources.length ? enabledSources / snapshot.sources.length : 0;
    const interfaceScore = snapshot.interfaces.length ? enabledInterfaces / snapshot.interfaces.length : 0;
    const assetScore = snapshot.assets.length ? governedAssets / snapshot.assets.length : 0;
    const graphScore = snapshot.knowledgeGraphs.length ? governedGraphs / snapshot.knowledgeGraphs.length : 0;
    const lakeScore = snapshot.lakehouse?.storage_health.healthy ? 1 : 0;
    const aiScore = snapshot.instances.length && snapshot.agents.length && snapshot.skills.length ? 1 : 0.4;
    const score = Math.round((sourceScore + interfaceScore + assetScore + graphScore + lakeScore + aiScore) / 6 * 100);
    const healthTone: Tone = score >= 85 && failedSyncs === 0 ? "good" : score >= 60 ? "warning" : "danger";
    return {
      enabledSources,
      enabledInterfaces,
      governedAssets,
      governedGraphs,
      failedSyncs,
      pendingAssets,
      pendingGraphs,
      enabledInstances,
      enabledAgents,
      enabledSkills,
      enabledRoutes,
      score,
      healthTone,
    };
  }, [snapshot]);

  const todos = useMemo(() => {
    const items: Array<{ title: string; detail: string; tone: Tone; target: PlatformNavigationTarget }> = [];
    if (snapshot.lakehouse && !snapshot.lakehouse.storage_health.healthy) {
      items.push({ title: "湖仓存储不可用", detail: snapshot.lakehouse.storage_health.detail || "请检查对象存储和本地服务。", tone: "danger", target: "lakehouse" });
    }
    if (computed.failedSyncs) {
      items.push({ title: `${computed.failedSyncs} 次数据同步失败`, detail: "查看失败来源、错误信息并重新执行同步。", tone: "danger", target: "operations" });
    }
    if (computed.pendingAssets) {
      items.push({ title: `${computed.pendingAssets} 项数据资产待治理`, detail: "完成质量检查、字段规范和来源登记后再发布。", tone: "warning", target: "lakehouse" });
    }
    if (computed.pendingGraphs) {
      items.push({ title: `${computed.pendingGraphs} 张知识图谱待治理`, detail: "检查来源覆盖、证据链和候选事实审核状态。", tone: "warning", target: "knowledge-graphs" });
    }
    if (snapshot.skillDrafts.length) {
      items.push({ title: `${snapshot.skillDrafts.length} 份技能优化草案待审核`, detail: "评估回归结果后决定批准或驳回。", tone: "warning", target: "skills" });
    }
    if (!computed.enabledRoutes && snapshot.instances.length) {
      items.push({ title: "尚未启用模型路由", detail: "智能体任务无法按任务类型稳定选择模型。", tone: "warning", target: "models" });
    }
    return items.slice(0, 6);
  }, [computed, snapshot.instances.length, snapshot.lakehouse, snapshot.skillDrafts.length]);

  const flow = [
    {
      index: "01",
      title: "数据接入",
      detail: "数据源、接口与增量同步",
      value: `${computed.enabledSources}/${snapshot.sources.length} 数据源 · ${computed.enabledInterfaces}/${snapshot.interfaces.length} 接口`,
      tone: toneForRatio(computed.enabledSources + computed.enabledInterfaces, snapshot.sources.length + snapshot.interfaces.length),
      target: "data-sources" as const,
    },
    {
      index: "02",
      title: "主数据与业务数据",
      detail: "股票、公司身份以及动态业务事实",
      value: `${formatCount(snapshot.symbolCount)} 只证券 · ${formatCount(snapshot.assets.reduce((sum, item) => sum + item.row_count, 0))} 条资产记录`,
      tone: snapshot.symbolCount > 0 ? "good" as const : "warning" as const,
      target: "master-data" as const,
    },
    {
      index: "03",
      title: "湖仓与知识库",
      detail: "分层存储、文档切片、向量与血缘",
      value: `${snapshot.lakehouse?.dataset_count ?? snapshot.datasets.length} 数据集 · ${snapshot.knowledgeBases.length} 知识库`,
      tone: snapshot.lakehouse?.storage_health.healthy ? "good" as const : "warning" as const,
      target: "lakehouse" as const,
    },
    {
      index: "04",
      title: "知识图谱",
      detail: "股票公司实体、事实、关系与证据链",
      value: `${computed.governedGraphs}/${snapshot.knowledgeGraphs.length} 张已治理`,
      tone: toneForRatio(computed.governedGraphs, snapshot.knowledgeGraphs.length),
      target: "knowledge-graphs" as const,
    },
    {
      index: "05",
      title: "AI 能力与投研",
      detail: "模型、技能、智能体和选股复盘",
      value: `${computed.enabledInstances} 模型 · ${computed.enabledSkills} 技能 · ${computed.enabledAgents} 智能体`,
      tone: computed.enabledInstances && computed.enabledSkills && computed.enabledAgents ? "good" as const : "warning" as const,
      target: "agents" as const,
    },
  ];

  const healthIcon = computed.healthTone === "good" ? <CheckCircle2 size={22} /> : <AlertTriangle size={22} />;

  return (
    <section className={`qpo-root ${className}`.trim()} aria-busy={loading}>
      <style>{overviewCss}</style>
      <header className="qpo-hero">
        <div>
          <p className="qpo-eyebrow">PLATFORM OVERVIEW · 平台总览</p>
          <h1>从数据接入到智能投研，一张图掌握全链路</h1>
          <p className="qpo-hero-copy">
            汇总数据源、股票与公司主数据、湖仓知识资产、知识图谱和 AI 能力状态；异常与待审核事项集中进入运营治理。
          </p>
          <div className="qpo-hero-actions">
            <button className="qpo-button primary" type="button" onClick={onRunKnowledgePipeline ?? (() => onNavigate?.("data-sources"))}>
              <Sparkles size={16} />同步并更新知识
            </button>
            <button className="qpo-button" type="button" onClick={onRunDataSync ?? (() => onNavigate?.("master-data"))}>
              <Database size={16} />同步主数据
            </button>
            <button className="qpo-button ghost" type="button" onClick={() => onNavigate?.("operations")}>
              <Activity size={16} />查看任务中心
            </button>
          </div>
        </div>
        <aside className="qpo-health">
          <div className="qpo-health-head">
            <div>
              <p className="qpo-health-label">平台链路健康度</p>
              <p className="qpo-health-score">{loading ? "--" : computed.score}<small> / 100</small></p>
            </div>
            <span className={`qpo-health-icon ${computed.healthTone}`}>{healthIcon}</span>
          </div>
          <div className="qpo-health-meta">
            <span className={`qpo-chip ${snapshot.lakehouse?.storage_health.healthy ? "good" : "warning"}`}>湖仓{snapshot.lakehouse?.storage_health.healthy ? "正常" : "待检查"}</span>
            <span className={`qpo-chip ${computed.failedSyncs ? "danger" : "good"}`}>同步失败 {computed.failedSyncs}</span>
            <span className={`qpo-chip ${todos.length ? "warning" : "good"}`}>待办 {todos.length}</span>
          </div>
        </aside>
      </header>

      {snapshot.failedModules.length > 0 && (
        <div className="qpo-error" role="status">
          <AlertTriangle size={17} />
          <span>以下模块暂未读取成功：{snapshot.failedModules.join("、")}。其他已成功加载的数据仍可正常查看。</span>
        </div>
      )}

      <div className="qpo-toolbar">
        <div>
          <h2>运行概况</h2>
          <p className="qpo-subtle">{snapshot.loadedAt ? `最近刷新：${snapshot.loadedAt.toLocaleString("zh-CN")}` : "正在连接各业务模块"}</p>
        </div>
        <button className="qpo-icon-button" type="button" disabled={loading} onClick={() => void load()}>
          <RefreshCw size={14} className={loading ? "spinning" : ""} />{loading ? "刷新中" : "刷新数据"}
        </button>
      </div>

      <div className="qpo-metrics">
        <MetricCard icon={<Server size={16} />} label="数据接入" value={`${computed.enabledSources}/${snapshot.sources.length}`} detail={`${computed.enabledInterfaces} 个接口已启用`} />
        <MetricCard icon={<Building2 size={16} />} label="证券主数据" value={formatCount(snapshot.symbolCount)} detail={`${snapshot.assets.length} 项数据资产`} />
        <MetricCard icon={<Layers3 size={16} />} label="湖仓资产" value={formatCount(snapshot.lakehouse?.dataset_count ?? snapshot.datasets.length)} detail={`${formatCount(snapshot.lakehouse?.object_count ?? 0)} 个对象 · ${formatCount(snapshot.lakehouse?.chunk_count ?? 0)} 个切片`} />
        <MetricCard icon={<Network size={16} />} label="知识产品" value={formatCount(snapshot.knowledgeBases.length + snapshot.knowledgeGraphs.length)} detail={`${snapshot.knowledgeBases.length} 知识库 · ${computed.governedGraphs} 张已治理图谱`} />
        <MetricCard icon={<BrainCircuit size={16} />} label="AI 能力" value={formatCount(computed.enabledInstances + computed.enabledAgents + computed.enabledSkills)} detail={`${computed.enabledInstances} 模型 · ${computed.enabledAgents} 智能体 · ${computed.enabledSkills} 技能`} />
      </div>

      <section>
        <div className="qpo-section-head" style={{ border: 0, paddingInline: 0 }}>
          <div>
            <h2>数据到智能应用链路</h2>
            <p className="qpo-subtle">点击任一环节进入对应中心，定位覆盖与治理问题。</p>
          </div>
        </div>
        <div className="qpo-flow">
          {flow.map((item) => (
            <button className="qpo-flow-card" type="button" key={item.index} onClick={() => onNavigate?.(item.target)}>
              <span className="qpo-flow-top"><span className="qpo-flow-index">STEP {item.index}</span><span className={`qpo-status-dot ${item.tone}`} /></span>
              <h3>{item.title}</h3>
              <p>{item.detail}</p>
              <span className="qpo-flow-value">{item.value}</span>
            </button>
          ))}
        </div>
      </section>

      <div className="qpo-main-grid">
        <section className="qpo-panel">
          <header className="qpo-panel-head">
            <div><h3>重点待办</h3><p className="qpo-subtle">按风险与治理状态自动汇总</p></div>
            <button className="qpo-link" type="button" onClick={() => onNavigate?.("operations")}>全部任务 <ArrowRight size={13} /></button>
          </header>
          {todos.length ? (
            <div className="qpo-todos">
              {todos.map((item) => (
                <article className="qpo-todo" key={`${item.target}-${item.title}`}>
                  <span className={`qpo-todo-icon ${item.tone}`}>{item.tone === "danger" ? <AlertTriangle size={16} /> : <Activity size={16} />}</span>
                  <div><h4>{item.title}</h4><p>{item.detail}</p></div>
                  <button type="button" onClick={() => onNavigate?.(item.target)}>处理 <ArrowRight size={12} /></button>
                </article>
              ))}
            </div>
          ) : (
            <div className="qpo-empty"><CheckCircle2 size={24} />当前没有需要优先处理的事项</div>
          )}
        </section>

        <section className="qpo-panel">
          <header className="qpo-panel-head">
            <div><h3>AI 能力装配</h3><p className="qpo-subtle">模型、技能、知识与智能体组合状态</p></div>
          </header>
          <div className="qpo-ai-list">
            <button className="qpo-ai-row" type="button" onClick={() => onNavigate?.("models")}>
              <span className="qpo-ai-label"><span><BrainCircuit size={16} /></span><span><strong>模型与路由</strong><small>{snapshot.providers.filter((item) => item.enabled).length} 个供应商 · {computed.enabledRoutes} 条路由</small></span></span><b className="qpo-ai-value">{computed.enabledInstances}</b>
            </button>
            <button className="qpo-ai-row" type="button" onClick={() => onNavigate?.("skills")}>
              <span className="qpo-ai-label"><span><Wrench size={16} /></span><span><strong>技能库</strong><small>{snapshot.skillDrafts.length} 份优化草案待审核</small></span></span><b className="qpo-ai-value">{computed.enabledSkills}</b>
            </button>
            <button className="qpo-ai-row" type="button" onClick={() => onNavigate?.("agents")}>
              <span className="qpo-ai-label"><span><Bot size={16} /></span><span><strong>智能体</strong><small>绑定模型、技能与知识资产</small></span></span><b className="qpo-ai-value">{computed.enabledAgents}</b>
            </button>
            <button className="qpo-ai-row" type="button" onClick={() => onNavigate?.("knowledge-bases")}>
              <span className="qpo-ai-label"><span><FileStack size={16} /></span><span><strong>知识库</strong><small>文档、切片、检索与版本治理</small></span></span><b className="qpo-ai-value">{snapshot.knowledgeBases.length}</b>
            </button>
            <button className="qpo-ai-row" type="button" onClick={() => onNavigate?.("knowledge-graphs")}>
              <span className="qpo-ai-label"><span><GitBranch size={16} /></span><span><strong>知识图谱</strong><small>{computed.pendingGraphs} 张待治理</small></span></span><b className="qpo-ai-value">{snapshot.knowledgeGraphs.length}</b>
            </button>
          </div>
        </section>
      </div>
    </section>
  );
}

export default PlatformOverview;
