import { FormEvent, useEffect, useState, type ReactNode } from "react";
import {
  api,
  type AgentDefinition,
  type AgentPayload,
  type DataAsset,
  type DataInterface,
  type DataSource,
  type IpoCalendarResponse,
  type KnowledgeBase,
  type KnowledgeGraph,
  type ModelCallLog,
  type ModelInstance,
  type ModelInstancePayload,
  type ModelProvider,
  type ModelProviderPayload,
  type ModelRoute,
  type ModelRoutePayload,
  type ResearchReportDetail,
  type ResearchReportSummary,
  type ModelSkill,
  type ModelSkillPayload,
  type StockSymbol,
  type SyncLog,
  type WatchlistItem,
} from "./api";
import { StockDetailDrawer } from "./StockDetailDrawer";
import { GovernedAssetTab, GovernedKnowledgeTab } from "./ResourceGovernance";

type ModuleView = "data" | "model" | "watch" | "research";
type ModelHubTab =
  "models" | "agents" | "skills" | "assets" | "knowledge" | "logs";
type DataView = "sources" | "interfaces" | "universe";
type ResearchTab = "chat" | "research";
const marketLabel: Record<string, string> = {
  ALL: "全部市场",
  CN_A: "A股",
  HK: "港股",
  NEEQ: "新三板",
  NEEQ_INNOVATION: "创新层",
};
const providerTypeLabel: Record<string, string> = {
  MOCK: "本地模拟",
  OPENAI_COMPAT: "OpenAI 兼容",
  GEMINI_REST: "Gemini REST",
  DEEPSEEK: "DeepSeek",
};
function billingLabel(config: Record<string, unknown> | undefined) {
  return String(config?.billing_label || config?.billing_type || "--");
}
const inputClass = "text-input";
function ErrorText({ error }: { error: string }) {
  return error ? <p className="form-error">{error}</p> : null;
}
function Status({ enabled }: { enabled: boolean }) {
  return (
    <span className={enabled ? "status-tag enabled" : "status-tag"}>
      {enabled ? "启用" : "停用"}
    </span>
  );
}
function formatDate(value?: string | null) {
  return value ? new Date(value).toLocaleString("zh-CN") : "--";
}

export default function App() {
  const [moduleView, setModuleView] = useState<ModuleView>("data");
  return (
    <main className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">Q</span>
          <div>
            <strong>GEMINI QUANT</strong>
            <small>Agent Control Room</small>
          </div>
        </div>
        <nav>
          {(["data", "model", "watch", "research"] as ModuleView[]).map(
            (view, index) => (
              <button
                key={view}
                className={moduleView === view ? "nav-item active" : "nav-item"}
                type="button"
                onClick={() => setModuleView(view)}
              >
                <span>{String(index + 1).padStart(2, "0")}</span>
                {view === "data"
                  ? "数据中台"
                  : view === "model"
                    ? "模型实验室"
                    : view === "watch"
                      ? "自动盯盘"
                      : "研究中心"}
              </button>
            ),
          )}
        </nav>
        <div className="sidebar-note">
          <span className="pulse" />
          <p>{moduleView === "model" ? "PHASE 02" : "PHASE 01"}</p>
          <strong>
            {moduleView === "model"
              ? "模型、智能体与知识资产"
              : "数据源接入与标准化"}
          </strong>
        </div>
      </aside>
      <section className="workspace">
        {moduleView === "data" ? (
          <DataConsolePage />
        ) : moduleView === "model" ? (
          <ModelLabPage />
        ) : moduleView === "watch" ? (
          <AutoWatchPage />
        ) : (
          <ResearchPage />
        )}
      </section>
    </main>
  );
}
function PageHeader({
  eyebrow,
  title,
  detail,
}: {
  eyebrow: string;
  title: string;
  detail?: string;
}) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
      </div>
      <div className="connection">
        <span className="connection-dot online" />
        <span>{detail || "本地服务已连接"}</span>
      </div>
    </header>
  );
}

function DataConsolePage() {
  const [tab, setTab] = useState<DataView>("sources");
  const [sources, setSources] = useState<DataSource[]>([]);
  const [interfaces, setInterfaces] = useState<DataInterface[]>([]);
  const [symbols, setSymbols] = useState<StockSymbol[]>([]);
  const [logs, setLogs] = useState<SyncLog[]>([]);
  const [market, setMarket] = useState("ALL");
  const [keyword, setKeyword] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(false);
  const [detailStock, setDetailStock] = useState<StockSymbol | null>(null);
  async function load() {
    setLoading(true);
    try {
      const [s, i, l] = await Promise.all([
        api.listSources(),
        api.listInterfaces(),
        api.listSyncLogs(),
      ]);
      setSources(s);
      setInterfaces(i);
      setLogs(l);
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "数据中台连接失败");
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    void load();
  }, []);
  async function loadSymbols(event?: FormEvent) {
    event?.preventDefault();
    try {
      const result = await api.listSymbols(
        new URLSearchParams({ market, keyword, page: "1", page_size: "30" }),
      );
      setSymbols(result.items);
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "主数据查询失败");
    }
  }
  useEffect(() => {
    // Open the universe with the first page of all markets already visible.
    void loadSymbols();
  }, []);
  async function sync() {
    try {
      const source = sources.find((item) => item.enabled);
      if (!source) throw new Error("没有启用的数据源");
      await api.synchronize(market, source.source_code);
      setNotice(
        `${market === "ALL" ? "全部市场" : marketLabel[market] || market} 主数据同步已提交`,
      );
      await loadSymbols();
      await load();
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "同步失败");
    }
  }
  return (
    <>
      <PageHeader
        eyebrow="DATA PLATFORM / PHASE 01"
        title="数据中台"
        detail={
          loading ? "正在连接数据中台..." : notice || "本地数据服务已连接"
        }
      />
      <div className="resource-tabs">
        {(["sources", "interfaces", "universe"] as DataView[]).map((item) => (
          <button
            type="button"
            key={item}
            className={tab === item ? "active" : ""}
            onClick={() => setTab(item)}
          >
            {item === "sources"
              ? "数据源"
              : item === "interfaces"
                ? "接口目录"
                : "股票主数据"}
          </button>
        ))}
      </div>
      {tab === "sources" && (
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">SOURCE REGISTRY</p>
              <h2>数据源管理</h2>
            </div>
            <button type="button" onClick={() => void load()}>
              刷新
            </button>
          </div>
          <div className="table-wrap">
              <table className="watch-table">
              <thead>
                <tr>
                  <th>编码</th>
                  <th>名称</th>
                  <th>类型</th>
                  <th>优先级</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {sources.map((source) => (
                  <tr key={source.id}>
                    <td>
                      <code>{source.source_code}</code>
                    </td>
                    <td>{source.source_name}</td>
                    <td>{source.adapter_type}</td>
                    <td>{source.priority}</td>
                    <td>
                      <Status enabled={source.enabled} />
                    </td>
                    <td>
                      <button
                        type="button"
                        onClick={async () => {
                          try {
                            const result = await api.testSource(source.id);
                            setNotice(result.message);
                          } catch (e) {
                            setNotice(
                              e instanceof Error ? e.message : "数据源测试失败",
                            );
                          }
                        }}
                      >
                        测试连接
                      </button>
                      <button
                        type="button"
                        onClick={async () => {
                          try {
                            await api.updateSource(source.id, {
                              enabled: !source.enabled,
                            });
                            setNotice(
                              `${source.source_code} 已${source.enabled ? "停用" : "启用"}`,
                            );
                            await load();
                          } catch (e) {
                            setNotice(
                              e instanceof Error ? e.message : "数据源状态更新失败",
                            );
                          }
                        }}
                      >
                        {source.enabled ? "停用" : "启用"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {tab === "interfaces" && (
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">INTERFACE CATALOG</p>
              <h2>数据接口目录</h2>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>接口</th>
                  <th>分类</th>
                  <th>请求模式</th>
                  <th>市场</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {interfaces.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.interface_name}</strong>
                      <code>{item.interface_code}</code>
                    </td>
                    <td>{item.data_category}</td>
                    <td>{item.request_mode}</td>
                    <td>{item.supported_markets.join("、")}</td>
                    <td>
                      <Status enabled={item.enabled} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {tab === "universe" && (
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">SECURITY UNIVERSE</p>
              <h2>股票主数据</h2>
            </div>
            <button
              className="primary-button"
              type="button"
              onClick={() => void sync()}
            >
              同步主数据
            </button>
          </div>
          <form className="inline-form" onSubmit={loadSymbols}>
            <select
              className={inputClass}
              value={market}
              onChange={(e) => setMarket(e.target.value)}
            >
              <option value="ALL">全部</option>
              <option value="CN_A">A股</option>
              <option value="HK">港股</option>
              <option value="NEEQ">新三板</option>
              <option value="NEEQ_INNOVATION">创新层</option>
            </select>
<input
              className={inputClass}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="按代码或名称模糊搜索"
            />
            <button type="submit">查询</button>
          </form>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>市场</th>
                  <th>代码</th>
                  <th>名称</th>
                  <th>交易所</th>
                  <th>上市日期</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {symbols.map((stock) => (
                  <tr key={`${stock.market}-${stock.symbol}`}>
                    <td>{marketLabel[stock.market] || stock.market}</td>
                    <td>
                      <button
                        type="button"
                        className="symbol-link"
                        onClick={() => setDetailStock(stock)}
                        title="查看最新 F10 资料"
                      >
                        <code>{stock.symbol}</code>
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="text-button"
                        onClick={() => setDetailStock(stock)}
                      >
                        {stock.name}
                      </button>
                    </td>
                    <td>{stock.exchange}</td>
                    <td>{stock.list_date || "--"}</td>
                    <td>{stock.status}</td>
                  </tr>
                ))}
                {!symbols.length && (
                  <tr>
                    <td colSpan={6} className="empty-state">
                      请输入条件查询股票主数据
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="log-list">
            {logs.slice(0, 5).map((log) => (
              <div className="log-row" key={log.id}>
                <span
                  className={
                    log.status === "SUCCESS"
                      ? "log-dot success"
                      : "log-dot failed"
                  }
                />
                <span>
                  {log.market} / {log.interface_code}
                </span>
                <span>{formatDate(log.completed_at || log.started_at)}</span>
              </div>
            ))}
          </div>
        </section>
      )}
      {detailStock && (
        <StockDetailDrawer
          stock={detailStock}
          onClose={() => setDetailStock(null)}
        />
      )}
    </>
  );
}

type ProviderForm = ModelProviderPayload & { id?: number };
const emptyProvider: ProviderForm = {
  provider_code: "",
  provider_name: "",
  provider_type: "OPENAI_COMPAT",
  enabled: true,
  description: "",
  config_json: {},
};
type InstanceForm = ModelInstancePayload & { id?: number };
const emptyInstance: InstanceForm = {
  provider_id: 0,
  instance_code: "",
  model_code: "",
  model_name: "",
  purpose: "chat",
  api_key: "",
  api_base_url: "",
  api_path: "/chat/completions",
  max_tokens: 4096,
  temperature: 0.2,
  top_p: 0.9,
  enabled: true,
  config_json: {},
  description: "",
};
type RouteForm = Omit<ModelRoutePayload, "fallback_chain_json"> & {
  id?: number;
  fallback_chain_text: string;
};
const emptyRoute: RouteForm = {
  task_type: "",
  preferred_instance_code: "",
  fallback_chain_text: "",
  route_policy: "PREFERRED_THEN_FALLBACK",
  enabled: true,
  description: "",
};
type SkillForm = ModelSkillPayload & { id?: number };
type AgentForm = AgentPayload & { id?: number };
const emptySkill: SkillForm = {
  skill_code: "",
  skill_name: "",
  description: "",
  instructions: "",
  enabled: true,
  config_json: {},
  version: "1.0.0",
};
const emptyAgent: AgentForm = {
  agent_code: "",
  display_name: "",
  system_prompt: "",
  model_instance_code: null,
  max_iterations: 8,
  enabled: true,
  description: "",
  version: "1.0.0",
  child_agent_ids: [],
  skill_ids: [],
  knowledge_base_ids: [],
  data_asset_ids: [],
};

function ModelLabPage() {
  const [tab, setTab] = useState<ModelHubTab>("models");
  const [providers, setProviders] = useState<ModelProvider[]>([]);
  const [instances, setInstances] = useState<ModelInstance[]>([]);
  const [routes, setRoutes] = useState<ModelRoute[]>([]);
  const [skills, setSkills] = useState<ModelSkill[]>([]);
  const [agents, setAgents] = useState<AgentDefinition[]>([]);
  const [assets, setAssets] = useState<DataAsset[]>([]);
  const [knowledge, setKnowledge] = useState<KnowledgeBase[]>([]);
  const [graphs, setGraphs] = useState<KnowledgeGraph[]>([]);
  const [logs, setLogs] = useState<ModelCallLog[]>([]);
  const [notice, setNotice] = useState("");
  const [editingProvider, setEditingProvider] =
    useState<ProviderForm>(emptyProvider);
  const [editingInstance, setEditingInstance] =
    useState<InstanceForm>(emptyInstance);
  const [editingRoute, setEditingRoute] = useState<RouteForm>(emptyRoute);
  const [editingSkill, setEditingSkill] = useState<SkillForm>(emptySkill);
  const [editingAgent, setEditingAgent] = useState<AgentForm>(emptyAgent);
  const [modelDialog, setModelDialog] = useState<
    "provider" | "instance" | "route" | null
  >(null);
  const [agentDialogOpen, setAgentDialogOpen] = useState(false);
  const [skillDialogOpen, setSkillDialogOpen] = useState(false);
  const [chat, setChat] = useState({
    instance_code: "",
    message: "请用一句话测试当前模型。",
  });
  const [chatResult, setChatResult] = useState("");
  async function load() {
    try {
      const result = await Promise.all([
        api.listModelProviders(),
        api.listModelInstances(),
        api.listModelRoutes(),
        api.listModelSkills(),
        api.listAgents(),
        api.listDataAssets(),
        api.listKnowledgeBases(),
        api.listKnowledgeGraphs(),
        api.listModelCallLogs(),
      ]);
      setProviders(result[0]);
      setInstances(result[1]);
      setRoutes(result[2]);
      setSkills(result[3]);
      setAgents(result[4]);
      setAssets(result[5]);
      setKnowledge(result[6]);
      setGraphs(result[7]);
      setLogs(result[8]);
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "资源中心加载失败");
    }
  }
  useEffect(() => {
    void load();
  }, []);
function message(text: string) {
    setNotice(text);
    window.setTimeout(() => setNotice(""), 3500);
  }
  async function saveProvider(event: FormEvent) {
    event.preventDefault();
    try {
      const { id } = editingProvider;
      const payload: ModelProviderPayload = {
        provider_code: editingProvider.provider_code,
        provider_name: editingProvider.provider_name,
        provider_type: editingProvider.provider_type,
        enabled: editingProvider.enabled,
        description: editingProvider.description || "",
        config_json: editingProvider.config_json || {},
      };
      if (id) await api.updateModelProvider(id, payload);
      else await api.createModelProvider(payload);
      setEditingProvider(emptyProvider);
      setModelDialog(null);
      await load();
      message("模型供应商已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "供应商保存失败");
    }
  }
  async function saveInstance(event: FormEvent) {
    event.preventDefault();
    try {
      const { id } = editingInstance;
      const payload: ModelInstancePayload = {
        provider_id: editingInstance.provider_id || providers[0]?.id || 0,
        instance_code: editingInstance.instance_code,
        model_code: editingInstance.model_code,
        model_name: editingInstance.model_name,
        purpose: editingInstance.purpose,
        api_key: editingInstance.api_key,
        api_base_url: editingInstance.api_base_url,
        api_path: editingInstance.api_path,
        max_tokens: editingInstance.max_tokens,
        temperature: editingInstance.temperature,
        top_p: editingInstance.top_p,
        enabled: editingInstance.enabled,
        fallback_instance_code: editingInstance.fallback_instance_code,
        config_json: editingInstance.config_json || {},
        description: editingInstance.description || "",
      };
      if (!payload.provider_id) throw new Error("请选择供应商");
      if (!payload.api_key) delete payload.api_key;
      if (id) await api.updateModelInstance(id, payload);
      else await api.createModelInstance(payload);
      setEditingInstance({
        ...emptyInstance,
        provider_id: providers[0]?.id || 0,
      });
      setModelDialog(null);
      await load();
      message("模型实例已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "模型实例保存失败");
    }
  }
  async function saveRoute(event: FormEvent) {
    event.preventDefault();
    try {
      const fallback_chain_json = editingRoute.fallback_chain_text
        .split(/[\n,，]/)
        .map((item) => item.trim())
        .filter(Boolean);
      const payload: ModelRoutePayload = {
        task_type: editingRoute.task_type.trim(),
        preferred_instance_code:
          editingRoute.preferred_instance_code || instances[0]?.instance_code || "",
        fallback_chain_json,
        route_policy: editingRoute.route_policy || "PREFERRED_THEN_FALLBACK",
        enabled: editingRoute.enabled,
        description: editingRoute.description || "",
      };
      if (!payload.task_type) throw new Error("请输入任务类型");
      if (!payload.preferred_instance_code) throw new Error("请选择首选模型实例");
      if (editingRoute.id) {
        const updatePayload: Partial<ModelRoutePayload> = { ...payload };
        delete updatePayload.task_type;
        await api.updateModelRoute(editingRoute.id, updatePayload);
      } else {
        await api.createModelRoute(payload);
      }
      setEditingRoute(emptyRoute);
      setModelDialog(null);
      await load();
      message("任务路由已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "任务路由保存失败");
    }
  }
  async function testProvider(item: ModelProvider) {
    try {
      const result = await api.testModelProvider(item.id);
      message(
        `${item.provider_name}：${result.message}${result.response_text ? ` ${result.response_text}` : ""}`,
      );
      await load();
    } catch (e) {
      message(e instanceof Error ? e.message : "供应商测试失败");
    }
  }
  async function testInstance(item: ModelInstance) {
    try {
      const result = await api.testModelInstance(item.id);
      message(
        `${item.instance_code}：${result.message}${result.response_text ? ` ${result.response_text}` : ""}`,
      );
      await load();
    } catch (e) {
      message(e instanceof Error ? e.message : "模型实例测试失败");
    }
  }
  async function deleteProvider(item: ModelProvider) {
    if (!window.confirm(`删除供应商 ${item.provider_name}？`)) return;
    try {
      await api.deleteModelProvider(item.id);
      await load();
      message("供应商已删除");
    } catch (e) {
      message(
        e instanceof Error ? e.message : "删除失败，可能仍有模型实例引用",
      );
    }
  }
  async function deleteInstance(item: ModelInstance) {
    if (!window.confirm(`删除模型实例 ${item.instance_code}？`)) return;
    try {
      await api.deleteModelInstance(item.id);
      await load();
      message("模型实例已删除");
    } catch (e) {
      message(e instanceof Error ? e.message : "删除失败，可能仍被路由引用");
    }
  }
  async function deleteRoute(item: ModelRoute) {
    if (!window.confirm(`删除任务路由 ${item.task_type}？`)) return;
    try {
      await api.deleteModelRoute(item.id);
      await load();
      message("任务路由已删除");
    } catch (e) {
      message(e instanceof Error ? e.message : "任务路由删除失败");
    }
  }
  async function saveSkill(event: FormEvent) {
    event.preventDefault();
    try {
      const { id } = editingSkill;
      const skillCode = (editingSkill.skill_code || "").trim();
      const payload: ModelSkillPayload = {
        skill_code: skillCode || undefined,
        skill_name: editingSkill.skill_name,
        description: editingSkill.description || "",
        instructions: editingSkill.instructions,
        enabled: editingSkill.enabled,
        config_json: editingSkill.config_json || {},
        version: editingSkill.version || "1.0.0",
      };
      if (id) await api.updateModelSkill(id, payload);
      else await api.createModelSkill(payload);
      setEditingSkill(emptySkill);
      setSkillDialogOpen(false);
      await load();
      message("Skill 已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "Skill 保存失败");
    }
  }
  async function deleteSkill(item: ModelSkill) {
    if (!window.confirm(`删除 Skill ${item.skill_name}？`)) return;
    try {
      await api.deleteModelSkill(item.id);
      await load();
      message("Skill 已删除");
    } catch (e) {
      message(e instanceof Error ? e.message : "Skill 可能正在被智能体引用");
    }
  }
  async function saveAgent(event: FormEvent) {
    event.preventDefault();
    try {
      const { id } = editingAgent;
      const agentCode = (editingAgent.agent_code || "").trim();
      const payload: AgentPayload = {
        agent_code: agentCode || undefined,
        display_name: editingAgent.display_name,
        system_prompt: editingAgent.system_prompt,
        model_instance_code: editingAgent.model_instance_code,
        max_iterations: editingAgent.max_iterations,
        enabled: editingAgent.enabled,
        description: editingAgent.description || "",
        version: editingAgent.version || "1.0.0",
        child_agent_ids: editingAgent.child_agent_ids,
        skill_ids: editingAgent.skill_ids,
        knowledge_base_ids: editingAgent.knowledge_base_ids,
        data_asset_ids: editingAgent.data_asset_ids,
      };
      if (id) await api.updateAgent(id, payload);
      else await api.createAgent(payload);
      setEditingAgent(emptyAgent);
      setAgentDialogOpen(false);
      await load();
      message("智能体已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "智能体保存失败");
    }
  }
  async function deleteAgent(item: AgentDefinition) {
    if (!window.confirm(`删除智能体 ${item.display_name}？`)) return;
    try {
      await api.deleteAgent(item.id);
      await load();
      message("智能体已删除");
    } catch (e) {
      message(e instanceof Error ? e.message : "智能体删除失败");
    }
  }
  async function sendChat(event: FormEvent) {
    event.preventDefault();
    try {
      const result = await api.chatWithModel({
        task_type: "model_lab_chat",
        instance_code: chat.instance_code || undefined,
        messages: [{ role: "user", content: chat.message }],
      });
      setChatResult(result.response_text);
      await load();
    } catch (e) {
      setChatResult(e instanceof Error ? e.message : "模型调用失败");
    }
  }
  return (
    <>
      <PageHeader
        eyebrow="MODEL LAB / PHASE 02"
        title="模型实验室"
        detail={notice || "模型、智能体与知识资产"}
      />
      <div className="resource-tabs">
        {(
          [
            "models",
            "agents",
            "skills",
            "assets",
            "knowledge",
            "logs",
          ] as ModelHubTab[]
        ).map((item) => (
          <button
            type="button"
            key={item}
            className={tab === item ? "active" : ""}
            onClick={() => setTab(item)}
          >
            {item === "models"
              ? "模型与路由"
              : item === "agents"
                ? "智能体"
                : item === "skills"
                  ? "Skills"
                  : item === "assets"
                    ? "数据资产"
                    : item === "knowledge"
                      ? "知识库"
                      : "调试与日志"}
          </button>
        ))}
      </div>
      {tab === "models" && (
        <ModelTab
          providers={providers}
          instances={instances}
          routes={routes}
          provider={editingProvider}
          setProvider={setEditingProvider}
          instance={{
            ...editingInstance,
            provider_id: editingInstance.provider_id || providers[0]?.id || 0,
          }}
          setInstance={setEditingInstance}
          route={{
            ...editingRoute,
            preferred_instance_code:
              editingRoute.preferred_instance_code ||
              instances[0]?.instance_code ||
              "",
          }}
          setRoute={setEditingRoute}
          dialog={modelDialog}
          onOpenProvider={() => {
            setEditingProvider(emptyProvider);
            setModelDialog("provider");
          }}
          onOpenInstance={() => {
            setEditingInstance({
              ...emptyInstance,
              provider_id: providers[0]?.id || 0,
            });
            setModelDialog("instance");
          }}
          onOpenRoute={() => {
            setEditingRoute({
              ...emptyRoute,
              preferred_instance_code: instances[0]?.instance_code || "",
            });
            setModelDialog("route");
          }}
          onCloseDialog={() => setModelDialog(null)}
          onProvider={saveProvider}
          onInstance={saveInstance}
          onRoute={saveRoute}
          onEditProvider={(item) => {
            setEditingProvider({
              id: item.id,
              provider_code: item.provider_code,
              provider_name: item.provider_name,
              provider_type: item.provider_type,
              enabled: item.enabled,
              description: item.description || "",
              config_json: item.config_json || {},
            });
            setModelDialog("provider");
          }}
          onEditInstance={(item) => {
            setEditingInstance({
              id: item.id,
              provider_id: item.provider_id,
              instance_code: item.instance_code,
              model_code: item.model_code,
              model_name: item.model_name,
              purpose: item.purpose,
              api_key: "",
              api_base_url: item.api_base_url || "",
              api_path: item.api_path || "/chat/completions",
              max_tokens: item.max_tokens,
              temperature: item.temperature,
              top_p: item.top_p,
              enabled: item.enabled,
              fallback_instance_code: item.fallback_instance_code || "",
              config_json: item.config_json || {},
              description: item.description || "",
            });
            setModelDialog("instance");
          }}
          onEditRoute={(item) => {
            setEditingRoute({
              id: item.id,
              task_type: item.task_type,
              preferred_instance_code: item.preferred_instance_code,
              fallback_chain_text: (item.fallback_chain_json || []).join("\n"),
              route_policy: item.route_policy,
              enabled: item.enabled,
              description: item.description || "",
            });
            setModelDialog("route");
          }}
          onDeleteProvider={deleteProvider}
          onDeleteInstance={deleteInstance}
          onDeleteRoute={deleteRoute}
          onTestProvider={testProvider}
          onTestInstance={testInstance}
        />
      )}
      {tab === "agents" && (
        <AgentTab
          agents={agents}
          instances={instances}
          skills={skills}
          knowledge={knowledge}
          assets={assets}
          value={editingAgent}
          setValue={setEditingAgent}
          dialogOpen={agentDialogOpen}
          onOpen={() => {
            setEditingAgent(emptyAgent);
            setAgentDialogOpen(true);
          }}
          onClose={() => {
            setAgentDialogOpen(false);
            setEditingAgent(emptyAgent);
          }}
          onSave={saveAgent}
          onEdit={(item) => {
            setEditingAgent({
              ...item,
              id: item.id,
              description: item.description || "",
            });
            setAgentDialogOpen(true);
          }}
          onDelete={deleteAgent}
        />
      )}
      {tab === "skills" && (
        <SkillTab
          skills={skills}
          value={editingSkill}
          setValue={setEditingSkill}
          dialogOpen={skillDialogOpen}
          onOpen={() => {
            setEditingSkill(emptySkill);
            setSkillDialogOpen(true);
          }}
          onClose={() => {
            setSkillDialogOpen(false);
            setEditingSkill(emptySkill);
          }}
          onSave={saveSkill}
          onEdit={(item) => {
            setEditingSkill({
              id: item.id,
              skill_code: item.skill_code,
              skill_name: item.skill_name,
              description: item.description || "",
              instructions: item.instructions,
              enabled: item.enabled,
              config_json: item.config_json || {},
              version: item.version || "1.0.0",
            });
            setSkillDialogOpen(true);
          }}
          onDelete={deleteSkill}
        />
      )}
      {tab === "assets" && (
        <GovernedAssetTab
          assets={assets}
          agents={agents}
          reload={load}
          notify={message}
        />
      )}
      {tab === "knowledge" && (
        <GovernedKnowledgeTab
          knowledge={knowledge}
          graphs={graphs}
          assets={assets}
          agents={agents}
          reload={load}
          notify={message}
        />
      )}
      {tab === "logs" && (
        <LogsTab
          logs={logs}
          chat={chat}
          setChat={setChat}
          result={chatResult}
          onSend={sendChat}
        />
      )}
    </>
  );
}

function ResourceDialog({
  eyebrow,
  title,
  children,
  onClose,
}: {
  eyebrow: string;
  title: string;
  children: ReactNode;
  onClose: () => void;
}) {
  return (
    <div
      className="resource-dialog-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section className="resource-dialog" role="dialog" aria-modal="true">
        <div className="resource-dialog-header">
          <div>
            <p className="eyebrow">{eyebrow}</p>
            <h3>{title}</h3>
          </div>
          <button className="quiet-button" type="button" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="resource-dialog-body">{children}</div>
      </section>
    </div>
  );
}

function ModelTab(props: {
  providers: ModelProvider[];
  instances: ModelInstance[];
  routes: ModelRoute[];
  provider: ProviderForm;
  setProvider: (v: ProviderForm) => void;
  instance: InstanceForm;
  setInstance: (v: InstanceForm) => void;
  route: RouteForm;
  setRoute: (v: RouteForm) => void;
  dialog: "provider" | "instance" | "route" | null;
  onOpenProvider: () => void;
  onOpenInstance: () => void;
  onOpenRoute: () => void;
  onCloseDialog: () => void;
  onProvider: (e: FormEvent) => void;
  onInstance: (e: FormEvent) => void;
  onRoute: (e: FormEvent) => void;
  onEditProvider: (v: ModelProvider) => void;
  onEditInstance: (v: ModelInstance) => void;
  onEditRoute: (v: ModelRoute) => void;
  onDeleteProvider: (v: ModelProvider) => void;
  onDeleteInstance: (v: ModelInstance) => void;
  onDeleteRoute: (v: ModelRoute) => void;
  onTestProvider: (v: ModelProvider) => void;
  onTestInstance: (v: ModelInstance) => void;
}) {
  const {
    providers,
    instances,
    routes,
    provider,
    setProvider,
    instance,
    setInstance,
    route,
    setRoute,
    dialog,
  } = props;
  return (
    <>
      <div className="resource-stack">
        <section className="panel resource-management-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">PROVIDERS</p>
              <h2>模型供应商清单</h2>
              <p>维护 DeepSeek、本地模拟或兼容 OpenAI 的模型供应商。</p>
            </div>
            <div className="panel-actions">
              <button
                className="primary-button"
                type="button"
                onClick={props.onOpenProvider}
              >
                新增供应商
              </button>
            </div>
          </div>
          <div className="resource-list-header">
            <h3>供应商列表</h3>
            <span>{providers.length} 个供应商</span>
          </div>
          <div className="table-wrap resource-list-wrap">
            <table>
              <thead>
                <tr>
                  <th>供应商</th>
                  <th>类型</th>
                  <th>费用</th>
                  <th>API Base</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {providers.length ? (
                  providers.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.provider_name}</strong>
                        <code>{item.provider_code}</code>
                      </td>
                      <td>
                        {providerTypeLabel[item.provider_type] ||
                          item.provider_type}
                      </td>
                      <td>{billingLabel(item.config_json)}</td>
                      <td className="muted-cell">
                        {String(
                          item.config_json.api_base_url ||
                            item.config_json.base_url ||
                            "--",
                        )}
                      </td>
                      <td>
                        <Status enabled={item.enabled} />
                      </td>
                      <td className="button-row">
                        <button
                          type="button"
                          onClick={() => props.onEditProvider(item)}
                        >
                          编辑
                        </button>
                        <button
                          type="button"
                          onClick={() => props.onTestProvider(item)}
                        >
                          测试
                        </button>
                        <button
                          type="button"
                          onClick={() => props.onDeleteProvider(item)}
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-table-cell" colSpan={6}>
                      暂无模型供应商，点击“新增供应商”创建。
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel resource-management-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">MODEL INSTANCES</p>
              <h2>模型实例清单</h2>
              <p>维护具体模型编码、用途、访问路径与调用参数。</p>
            </div>
            <div className="panel-actions">
              <button
                className="primary-button"
                type="button"
                onClick={props.onOpenInstance}
              >
                新增模型
              </button>
            </div>
          </div>
          <div className="field-help-grid">
            <div>
              <strong>最大 Token</strong>
              <span>单次回答允许输出的最大长度，值越大越适合长研报，但成本和耗时更高。</span>
            </div>
            <div>
              <strong>Temperature</strong>
              <span>控制随机性，0 更稳定保守，数值越高回答越发散。</span>
            </div>
            <div>
              <strong>Top P</strong>
              <span>控制候选词采样范围，越低越保守，越高越开放。</span>
            </div>
            <div>
              <strong>API/SK</strong>
              <span>API Base URL 是服务地址，API Path 是调用路径，SK/API Key 是密钥；未配置会自动走降级链。</span>
            </div>
          </div>
          <div className="resource-list-header">
            <h3>模型列表</h3>
            <span>{instances.length} 个模型实例</span>
          </div>
          <div className="table-wrap resource-list-wrap">
            <table>
              <thead>
                <tr>
                  <th>实例</th>
                  <th>供应商</th>
                  <th>模型</th>
                  <th>用途</th>
                  <th>费用</th>
                  <th>参数</th>
                  <th>Key</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {instances.length ? (
                  instances.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.instance_code}</strong>
                        <code>{item.model_name}</code>
                      </td>
                      <td>
                        {providers.find((p) => p.id === item.provider_id)
                          ?.provider_name || item.provider_id}
                      </td>
                      <td>{item.model_code}</td>
                      <td>{item.purpose || "--"}</td>
                      <td>{billingLabel(item.config_json)}</td>
                      <td className="muted-cell">
                        Token {item.max_tokens} / T {item.temperature} / P {item.top_p}
                      </td>
                      <td>{item.api_key_configured ? "已配置" : "未配置"}</td>
                      <td>
                        <Status enabled={item.enabled} />
                      </td>
                      <td className="button-row">
                        <button
                          type="button"
                          onClick={() => props.onEditInstance(item)}
                        >
                          编辑
                        </button>
                        <button
                          type="button"
                          onClick={() => props.onTestInstance(item)}
                        >
                          测试
                        </button>
                        <button
                          type="button"
                          onClick={() => props.onDeleteInstance(item)}
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-table-cell" colSpan={9}>
                      暂无模型实例，点击“新增模型”创建。
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>

        <section className="panel resource-management-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">TASK ROUTES</p>
              <h2>任务路由清单</h2>
              <p>维护任务类型到首选模型、降级链和路由策略的映射。</p>
            </div>
            <div className="panel-actions">
              <button
                className="primary-button"
                type="button"
                onClick={props.onOpenRoute}
              >
                新增路由
              </button>
            </div>
          </div>
          <div className="resource-list-header">
            <h3>路由列表</h3>
            <span>{routes.length} 条路由</span>
          </div>
          <div className="table-wrap resource-list-wrap route-table-wrap">
            <table className="route-table">
              <thead>
                <tr>
                  <th>任务</th>
                  <th>首选实例</th>
                  <th>降级链</th>
                  <th>策略</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {routes.length ? (
                  routes.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <code className="route-code">{item.task_type}</code>
                      </td>
                      <td>
                        <span className="route-pill">
                          {item.preferred_instance_code}
                        </span>
                      </td>
                      <td>
                        {item.fallback_chain_json.length ? (
                          <span className="route-chain">
                            {item.fallback_chain_json.map((chainItem, index) => (
                              <span key={`${item.id}-${chainItem}-${index}`}>
                                {index > 0 ? <em>→</em> : null}
                                <b>{chainItem}</b>
                              </span>
                            ))}
                          </span>
                        ) : (
                          "--"
                        )}
                      </td>
                      <td>{item.route_policy || "--"}</td>
                      <td>
                        <Status enabled={item.enabled} />
                      </td>
                      <td className="button-row">
                        <button
                          type="button"
                          onClick={() => props.onEditRoute(item)}
                        >
                          编辑
                        </button>
                        <button
                          type="button"
                          onClick={() => props.onDeleteRoute(item)}
                        >
                          删除
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td className="empty-table-cell" colSpan={6}>
                      暂无任务路由，点击“新增路由”创建。
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {dialog === "provider" && (
        <ResourceDialog
          eyebrow="PROVIDER FORM"
          title={provider.id ? "编辑模型供应商" : "新增模型供应商"}
          onClose={props.onCloseDialog}
        >
          <form className="resource-editor-form" onSubmit={props.onProvider}>
            <div className="resource-form-section">
              <div className="resource-section-title">基础信息</div>
              <div className="field-grid">
                <input
                  className={inputClass}
                  placeholder="供应商编码，如 DEEPSEEK"
                  value={provider.provider_code}
                  onChange={(e) =>
                    setProvider({ ...provider, provider_code: e.target.value })
                  }
                  disabled={Boolean(provider.id)}
                  required
                />
                <input
                  className={inputClass}
                  placeholder="显示名称"
                  value={provider.provider_name}
                  onChange={(e) =>
                    setProvider({ ...provider, provider_name: e.target.value })
                  }
                  required
                />
                <select
                  className={inputClass}
                  value={provider.provider_type}
                  onChange={(e) =>
                    setProvider({
                      ...provider,
                      provider_type: e.target
                        .value as ProviderForm["provider_type"],
                    })
                  }
                >
                  {Object.entries(providerTypeLabel).map(([key, label]) => (
                    <option key={key} value={key}>
                      {label}
                    </option>
                  ))}
                </select>
                <input
                  className={inputClass}
                  placeholder="API Base URL（可选）"
                  value={String(
                    provider.config_json.api_base_url ||
                      provider.config_json.base_url ||
                      "",
                  )}
                  onChange={(e) =>
                    setProvider({
                      ...provider,
                      config_json: {
                        ...provider.config_json,
                        api_base_url: e.target.value,
                      },
                    })
                  }
                />
                <select
                  className={inputClass}
                  value={String(provider.config_json.billing_label || "")}
                  onChange={(e) =>
                    setProvider({
                      ...provider,
                      config_json: {
                        ...provider.config_json,
                        billing_label: e.target.value,
                        billing_type:
                          e.target.value === "免费"
                            ? "FREE"
                            : e.target.value === "付费"
                              ? "PAID"
                              : "FREE_TIER_OR_PAID",
                      },
                    })
                  }
                >
                  <option value="">费用类型</option>
                  <option value="免费">免费</option>
                  <option value="付费">付费</option>
                  <option value="免费额度/付费">免费额度/付费</option>
                </select>
                <textarea
                  className="field-span-2"
                  rows={3}
                  placeholder="说明"
                  value={provider.description || ""}
                  onChange={(e) =>
                    setProvider({ ...provider, description: e.target.value })
                  }
                />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={provider.enabled}
                    onChange={(e) =>
                      setProvider({ ...provider, enabled: e.target.checked })
                    }
                  />
                  启用供应商
                </label>
              </div>
            </div>
            <div className="form-actions">
              <button
                className="quiet-button"
                type="button"
                onClick={props.onCloseDialog}
              >
                取消
              </button>
              <button className="primary-button" type="submit">
                {provider.id ? "更新供应商" : "创建供应商"}
              </button>
            </div>
          </form>
        </ResourceDialog>
      )}

      {dialog === "instance" && (
        <ResourceDialog
          eyebrow="MODEL FORM"
          title={instance.id ? "编辑模型实例" : "新增模型实例"}
          onClose={props.onCloseDialog}
        >
          <form className="resource-editor-form" onSubmit={props.onInstance}>
            <div className="resource-form-section">
              <div className="resource-section-title">模型配置</div>
              <div className="field-grid">
                <select
                  className={inputClass}
                  value={instance.provider_id}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      provider_id: Number(e.target.value),
                    })
                  }
                  required
                >
                  <option value={0}>选择供应商</option>
                  {providers.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.provider_name}
                    </option>
                  ))}
                </select>
                <input
                  className={inputClass}
                  placeholder="实例编码，如 DEEPSEEK_CHAT"
                  value={instance.instance_code}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      instance_code: e.target.value,
                    })
                  }
                  disabled={Boolean(instance.id)}
                  required
                />
                <input
                  className={inputClass}
                  placeholder="模型编码，如 deepseek-chat"
                  value={instance.model_code}
                  onChange={(e) =>
                    setInstance({ ...instance, model_code: e.target.value })
                  }
                  required
                />
                <input
                  className={inputClass}
                  placeholder="显示名称"
                  value={instance.model_name}
                  onChange={(e) =>
                    setInstance({ ...instance, model_name: e.target.value })
                  }
                  required
                />
                <input
                  className={inputClass}
                  placeholder="用途，如 chat / research"
                  value={instance.purpose}
                  onChange={(e) =>
                    setInstance({ ...instance, purpose: e.target.value })
                  }
                />
                <input
                  className={inputClass}
                  placeholder="API Key（留空则不更新）"
                  type="password"
                  value={instance.api_key || ""}
                  onChange={(e) =>
                    setInstance({ ...instance, api_key: e.target.value })
                  }
                />
                <input
                  className={inputClass}
                  placeholder="API Base URL（可选）"
                  value={instance.api_base_url || ""}
                  onChange={(e) =>
                    setInstance({ ...instance, api_base_url: e.target.value })
                  }
                />
                <input
                  className={inputClass}
                  placeholder="API Path（可选）"
                  value={instance.api_path || ""}
                  onChange={(e) =>
                    setInstance({ ...instance, api_path: e.target.value })
                  }
                />
                <select
                  className={inputClass}
                  value={String(instance.config_json.billing_label || "")}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      config_json: {
                        ...instance.config_json,
                        billing_label: e.target.value,
                        billing_type:
                          e.target.value === "免费"
                            ? "FREE"
                            : e.target.value === "付费"
                              ? "PAID"
                              : "FREE_TIER_OR_PAID",
                      },
                    })
                  }
                >
                  <option value="">费用类型</option>
                  <option value="免费">免费</option>
                  <option value="付费">付费</option>
                  <option value="免费额度/付费">免费额度/付费</option>
                </select>
                <input
                  className={inputClass}
                  type="number"
                  min={1}
                  max={200000}
                  placeholder="最大 Token"
                  value={instance.max_tokens}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      max_tokens: Number(e.target.value),
                    })
                  }
                />
                <input
                  className={inputClass}
                  type="number"
                  min={0}
                  max={2}
                  step={0.1}
                  placeholder="Temperature"
                  value={instance.temperature}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      temperature: Number(e.target.value),
                    })
                  }
                />
                <input
                  className={inputClass}
                  type="number"
                  min={0}
                  max={1}
                  step={0.05}
                  placeholder="Top P"
                  value={instance.top_p}
                  onChange={(e) =>
                    setInstance({ ...instance, top_p: Number(e.target.value) })
                  }
                />
                <select
                  className={inputClass}
                  value={instance.fallback_instance_code || ""}
                  onChange={(e) =>
                    setInstance({
                      ...instance,
                      fallback_instance_code: e.target.value || undefined,
                    })
                  }
                >
                  <option value="">不设置实例级降级</option>
                  {instances
                    .filter((item) => item.instance_code !== instance.instance_code)
                    .map((item) => (
                      <option key={item.id} value={item.instance_code}>
                        {item.instance_code}
                      </option>
                    ))}
                </select>
                <textarea
                  className="field-span-2"
                  rows={3}
                  placeholder="说明"
                  value={instance.description || ""}
                  onChange={(e) =>
                    setInstance({ ...instance, description: e.target.value })
                  }
                />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={instance.enabled}
                    onChange={(e) =>
                      setInstance({ ...instance, enabled: e.target.checked })
                    }
                  />
                  启用实例
                </label>
              </div>
            </div>
            <div className="form-actions">
              <button
                className="quiet-button"
                type="button"
                onClick={props.onCloseDialog}
              >
                取消
              </button>
              <button className="primary-button" type="submit">
                {instance.id ? "更新模型" : "创建模型"}
              </button>
            </div>
          </form>
        </ResourceDialog>
      )}

      {dialog === "route" && (
        <ResourceDialog
          eyebrow="ROUTE FORM"
          title={route.id ? "编辑任务路由" : "新增任务路由"}
          onClose={props.onCloseDialog}
        >
          <form className="resource-editor-form" onSubmit={props.onRoute}>
            <div className="resource-form-section">
              <div className="resource-section-title">路由规则</div>
              <div className="field-grid">
                <input
                  className={inputClass}
                  placeholder="任务类型，如 model_lab_chat"
                  value={route.task_type}
                  onChange={(e) =>
                    setRoute({ ...route, task_type: e.target.value })
                  }
                  disabled={Boolean(route.id)}
                  required
                />
                <select
                  className={inputClass}
                  value={route.preferred_instance_code}
                  onChange={(e) =>
                    setRoute({
                      ...route,
                      preferred_instance_code: e.target.value,
                    })
                  }
                  required
                >
                  <option value="">选择首选模型实例</option>
                  {instances.map((item) => (
                    <option key={item.id} value={item.instance_code}>
                      {item.instance_code}
                    </option>
                  ))}
                </select>
                <input
                  className={inputClass}
                  placeholder="路由策略"
                  value={route.route_policy}
                  onChange={(e) =>
                    setRoute({ ...route, route_policy: e.target.value })
                  }
                />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={route.enabled}
                    onChange={(e) =>
                      setRoute({ ...route, enabled: e.target.checked })
                    }
                  />
                  启用路由
                </label>
                <textarea
                  className="field-span-2"
                  rows={4}
                  placeholder="降级实例编码，每行一个，或用逗号分隔"
                  value={route.fallback_chain_text}
                  onChange={(e) =>
                    setRoute({
                      ...route,
                      fallback_chain_text: e.target.value,
                    })
                  }
                />
                <textarea
                  className="field-span-2"
                  rows={3}
                  placeholder="说明"
                  value={route.description || ""}
                  onChange={(e) =>
                    setRoute({ ...route, description: e.target.value })
                  }
                />
              </div>
            </div>
            <div className="form-actions">
              <button
                className="quiet-button"
                type="button"
                onClick={props.onCloseDialog}
              >
                取消
              </button>
              <button className="primary-button" type="submit">
                {route.id ? "更新路由" : "创建路由"}
              </button>
            </div>
          </form>
        </ResourceDialog>
      )}
    </>
  );
}
function AgentTab({
  agents,
  instances,
  skills,
  knowledge,
  assets,
  value,
  setValue,
  dialogOpen,
  onOpen,
  onClose,
  onSave,
  onEdit,
  onDelete,
}: {
  agents: AgentDefinition[];
  instances: ModelInstance[];
  skills: ModelSkill[];
  knowledge: KnowledgeBase[];
  assets: DataAsset[];
  value: AgentForm;
  setValue: (v: AgentForm) => void;
  dialogOpen: boolean;
  onOpen: () => void;
  onClose: () => void;
  onSave: (e: FormEvent) => void;
  onEdit: (v: AgentDefinition) => void;
  onDelete: (v: AgentDefinition) => void;
}) {
  const toggle = (
    key:
      | "child_agent_ids"
      | "skill_ids"
      | "knowledge_base_ids"
      | "data_asset_ids",
    id: number,
  ) => {
    const selected = value[key].includes(id);
    setValue({
      ...value,
      [key]: selected
        ? value[key].filter((item) => item !== id)
        : [...value[key], id],
    });
  };
  return (
    <>
      <section className="panel resource-management-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">AGENT REGISTRY</p>
            <h2>智能体清单</h2>
            <div className="auto-code-note">
              编码可留空由系统自动生成，格式为 AGENT_0001；已保存的智能体可在列表中编辑或删除。
            </div>
          </div>
          <div className="panel-actions">
            <button className="primary-button" type="button" onClick={onOpen}>
              新增智能体
            </button>
          </div>
        </div>
        <div className="resource-list-header">
          <h3>智能体列表</h3>
          <span>{agents.length} 个智能体</span>
        </div>
        <div className="table-wrap resource-list-wrap">
          <table>
            <thead>
              <tr>
                <th>智能体</th>
                <th>模型</th>
                <th>绑定资源</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {agents.length ? (
                agents.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.display_name}</strong>
                      <code>{item.agent_code}</code>
                    </td>
                    <td>{item.model_instance_code || "自动路由"}</td>
                    <td>
                      {item.skill_ids.length} Skill / {" "}
                      {item.knowledge_base_ids.length} 知识库 / {" "}
                      {item.data_asset_ids.length} 数据资产
                    </td>
                    <td>
                      <Status enabled={item.enabled} />
                    </td>
                    <td className="button-row">
                      <button type="button" onClick={() => onEdit(item)}>
                        编辑
                      </button>
                      <button type="button" onClick={() => onDelete(item)}>
                        删除
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="empty-table-cell" colSpan={5}>
                    暂无智能体，点击“新增智能体”创建。
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {dialogOpen && (
        <ResourceDialog
          eyebrow="AGENT FORM"
          title={value.id ? "编辑智能体" : "新增智能体"}
          onClose={onClose}
        >
          <form className="resource-editor-form" onSubmit={onSave}>
            <div className="resource-form-section">
              <div className="resource-section-title">基础信息</div>
              <div className="field-grid">
                <input
                  className={inputClass}
                  placeholder="智能体编码（可留空自动生成）"
                  value={value.agent_code || ""}
                  onChange={(e) =>
                    setValue({ ...value, agent_code: e.target.value })
                  }
                />
                <input
                  className={inputClass}
                  placeholder="显示名称"
                  value={value.display_name}
                  onChange={(e) =>
                    setValue({ ...value, display_name: e.target.value })
                  }
                  required
                />
                <select
                  className={inputClass}
                  value={value.model_instance_code || ""}
                  onChange={(e) =>
                    setValue({
                      ...value,
                      model_instance_code: e.target.value || null,
                    })
                  }
                >
                  <option value="">自动路由</option>
                  {instances.map((item) => (
                    <option key={item.id} value={item.instance_code}>
                      {item.instance_code}
                    </option>
                  ))}
                </select>
                <input
                  className={inputClass}
                  type="number"
                  min={1}
                  max={100}
                  value={value.max_iterations}
                  onChange={(e) =>
                    setValue({
                      ...value,
                      max_iterations: Number(e.target.value),
                    })
                  }
                  placeholder="最大迭代次数"
                />
                <textarea
                  className="field-span-2"
                  rows={4}
                  placeholder="系统提示词"
                  value={value.system_prompt}
                  onChange={(e) =>
                    setValue({ ...value, system_prompt: e.target.value })
                  }
                  required
                />
                <textarea
                  className="field-span-2"
                  rows={2}
                  placeholder="描述"
                  value={value.description || ""}
                  onChange={(e) =>
                    setValue({ ...value, description: e.target.value })
                  }
                />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={value.enabled}
                    onChange={(e) =>
                      setValue({ ...value, enabled: e.target.checked })
                    }
                  />
                  启用智能体
                </label>
              </div>
            </div>

            <div className="resource-form-section">
              <div className="resource-section-title">资源绑定</div>
              <div className="selection-grid">
                <label>
                  子智能体
                  {agents.filter((item) => item.id !== value.id).length ? (
                    agents
                      .filter((item) => item.id !== value.id)
                      .map((item) => (
                        <span key={item.id}>
                          <input
                            type="checkbox"
                            checked={value.child_agent_ids.includes(item.id)}
                            onChange={() => toggle("child_agent_ids", item.id)}
                          />
                          {item.display_name}
                        </span>
                      ))
                  ) : (
                    <span>暂无可选子智能体</span>
                  )}
                </label>
                <label>
                  Skills
                  {skills.length ? (
                    skills.map((item) => (
                      <span key={item.id}>
                        <input
                          type="checkbox"
                          checked={value.skill_ids.includes(item.id)}
                          onChange={() => toggle("skill_ids", item.id)}
                        />
                        {item.skill_name}
                      </span>
                    ))
                  ) : (
                    <span>暂无 Skill</span>
                  )}
                </label>
                <label>
                  知识库
                  {knowledge.length ? (
                    knowledge.map((item) => (
                      <span key={item.id}>
                        <input
                          type="checkbox"
                          checked={value.knowledge_base_ids.includes(item.id)}
                          onChange={() =>
                            toggle("knowledge_base_ids", item.id)
                          }
                        />
                        {item.kb_name}
                      </span>
                    ))
                  ) : (
                    <span>暂无知识库</span>
                  )}
                </label>
                <label>
                  数据源
                  {assets.length ? (
                    assets.map((item) => (
                      <span key={item.id}>
                        <input
                          type="checkbox"
                          checked={value.data_asset_ids.includes(item.id)}
                          onChange={() => toggle("data_asset_ids", item.id)}
                        />
                        {item.display_name}
                      </span>
                    ))
                  ) : (
                    <span>暂无数据源</span>
                  )}
                </label>
              </div>
            </div>

            <div className="form-actions">
              <button className="quiet-button" type="button" onClick={onClose}>
                取消
              </button>
              <button className="primary-button" type="submit">
                {value.id ? "更新智能体" : "创建智能体"}
              </button>
            </div>
          </form>
        </ResourceDialog>
      )}
    </>
  );
}

function SkillTab({
  skills,
  value,
  setValue,
  dialogOpen,
  onOpen,
  onClose,
  onSave,
  onEdit,
  onDelete,
}: {
  skills: ModelSkill[];
  value: SkillForm;
  setValue: (v: SkillForm) => void;
  dialogOpen: boolean;
  onOpen: () => void;
  onClose: () => void;
  onSave: (e: FormEvent) => void;
  onEdit: (v: ModelSkill) => void;
  onDelete: (v: ModelSkill) => void;
}) {
  return (
    <>
      <section className="panel resource-management-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">MARKDOWN SKILLS</p>
            <h2>Skill 清单</h2>
            <div className="auto-code-note">
              编码可留空由系统自动生成，格式为 SKILL_0001；保存后同步到 backend/skill_docs/*.SKILL.md。
            </div>
          </div>
          <div className="panel-actions">
            <button className="primary-button" type="button" onClick={onOpen}>
              新增 Skill
            </button>
          </div>
        </div>
        <div className="resource-list-header">
          <h3>Skill 列表</h3>
          <span>{skills.length} 个 Skill</span>
        </div>
        <div className="table-wrap resource-list-wrap">
          <table>
            <thead>
              <tr>
                <th>Skill</th>
                <th>版本</th>
                <th>格式</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {skills.length ? (
                skills.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.skill_name}</strong>
                      <code>{item.skill_code}</code>
                    </td>
                    <td>{item.version || "--"}</td>
                    <td>{item.format || "Markdown"}</td>
                    <td>
                      <Status enabled={item.enabled} />
                    </td>
                    <td className="button-row">
                      <button type="button" onClick={() => onEdit(item)}>
                        编辑
                      </button>
                      <button type="button" onClick={() => onDelete(item)}>
                        删除
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td className="empty-table-cell" colSpan={5}>
                    暂无 Skill，点击“新增 Skill”创建。
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      {dialogOpen && (
        <ResourceDialog
          eyebrow="SKILL FORM"
          title={value.id ? "编辑 Skill" : "新增 Skill"}
          onClose={onClose}
        >
          <form className="resource-editor-form" onSubmit={onSave}>
            <div className="resource-form-section">
              <div className="resource-section-title">Skill 内容</div>
              <div className="field-grid">
                <input
                  className={inputClass}
                  placeholder="Skill 编码（可留空自动生成）"
                  value={value.skill_code || ""}
                  onChange={(e) =>
                    setValue({ ...value, skill_code: e.target.value })
                  }
                />
                <input
                  className={inputClass}
                  placeholder="Skill 名称"
                  value={value.skill_name}
                  onChange={(e) =>
                    setValue({ ...value, skill_name: e.target.value })
                  }
                  required
                />
                <input
                  className={inputClass}
                  placeholder="版本"
                  value={value.version || "1.0.0"}
                  onChange={(e) =>
                    setValue({ ...value, version: e.target.value })
                  }
                />
                <label className="checkbox-row">
                  <input
                    type="checkbox"
                    checked={value.enabled}
                    onChange={(e) =>
                      setValue({ ...value, enabled: e.target.checked })
                    }
                  />
                  启用 Skill
                </label>
                <textarea
                  className="field-span-2"
                  rows={3}
                  placeholder="描述"
                  value={value.description || ""}
                  onChange={(e) =>
                    setValue({ ...value, description: e.target.value })
                  }
                />
                <textarea
                  className="field-span-2 skill-instructions-input"
                  rows={14}
                  placeholder="Markdown 指令内容"
                  value={value.instructions}
                  onChange={(e) =>
                    setValue({ ...value, instructions: e.target.value })
                  }
                  required
                />
              </div>
            </div>
            <div className="form-actions">
              <button className="quiet-button" type="button" onClick={onClose}>
                取消
              </button>
              <button className="primary-button" type="submit">
                {value.id ? "更新 Skill" : "创建 Skill"}
              </button>
            </div>
          </form>
        </ResourceDialog>
      )}
    </>
  );
}
function LogsTab({
  logs,
  chat,
  setChat,
  result,
  onSend,
}: {
  logs: ModelCallLog[];
  chat: { instance_code: string; message: string };
  setChat: (v: { instance_code: string; message: string }) => void;
  result: string;
  onSend: (e: FormEvent) => void;
}) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">MODEL DEBUGGER</p>
          <h2>模型测试与调用日志</h2>
        </div>
      </div>
      <form className="mini-form" onSubmit={onSend}>
        <div className="field-grid">
          <input
            className={inputClass}
            placeholder="实例编码，留空自动路由"
            value={chat.instance_code}
            onChange={(e) =>
              setChat({ ...chat, instance_code: e.target.value })
            }
          />
          <textarea
            className="field-span-2"
            rows={3}
            value={chat.message}
            onChange={(e) => setChat({ ...chat, message: e.target.value })}
          />
          <button className="primary-button" type="submit">
            发送测试
          </button>
        </div>
        <pre className="chat-result">{result || "等待模型返回"}</pre>
      </form>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>时间</th>
              <th>任务</th>
              <th>实例</th>
              <th>状态</th>
              <th>响应</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{formatDate(log.started_at)}</td>
                <td>{log.task_type}</td>
                <td>{log.instance_code || "--"}</td>
                <td>{log.status}</td>
                <td>{log.response_text || log.error_message || "--"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function trendClass(value?: number | string | null) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric === 0) return "trend-flat";
  return numeric > 0 ? "trend-up" : "trend-down";
}

function formatSignedPercent(value?: number | string | null) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return "--";
  return `${numeric > 0 ? "+" : ""}${numeric.toFixed(2)}%`;
}

function AutoWatchPage() {
  const [tab, setTab] = useState<"watch" | "ipo">("watch");
  const [items, setItems] = useState<WatchlistItem[]>([]);
  const [ipo, setIpo] = useState<IpoCalendarResponse | null>(null);
  const [market, setMarket] = useState("ALL");
  const [query, setQuery] = useState("");
  const [candidates, setCandidates] = useState<StockSymbol[]>([]);
  const [notice, setNotice] = useState("");
  const [detailStock, setDetailStock] = useState<Pick<
    StockSymbol,
    "market" | "symbol" | "name"
  > | null>(null);
  useEffect(() => {
    void api
      .listWatchlist()
      .then(setItems)
      .catch(() => setNotice("自选列表加载失败"));
  }, []);
  useEffect(() => {
    const timer = window.setTimeout(() => {
      if (query.trim())
        void api
          .searchStocks(market, query, 8)
          .then(setCandidates)
          .catch(() => setCandidates([]));
      else setCandidates([]);
    }, 250);
    return () => window.clearTimeout(timer);
  }, [market, query]);
  async function add(stock: StockSymbol) {
    try {
      await api.addWatchlist({ market: stock.market, symbol: stock.symbol });
      setItems(await api.listWatchlist());
      setQuery("");
      setCandidates([]);
      setNotice("已加入自选");
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "加入自选失败");
    }
  }
  async function loadIpo() {
    try {
      setIpo(await api.listIpoCalendar());
    } catch (e) {
      setNotice(e instanceof Error ? e.message : "打新数据加载失败");
    }
  }
  function openDetail(
    marketCode: string,
    symbol: string,
    name?: string | null,
  ) {
    setDetailStock({ market: marketCode, symbol, name: name || symbol });
  }
  return (
    <>
      <PageHeader
        eyebrow="AUTO WATCH"
        title="自动盯盘"
        detail={notice || "自选与打新"}
      />
      <div className="resource-tabs">
        <button
          type="button"
          className={tab === "watch" ? "active" : ""}
          onClick={() => setTab("watch")}
        >
          自选
        </button>
        <button
          type="button"
          className={tab === "ipo" ? "active" : ""}
          onClick={() => {
            setTab("ipo");
            void loadIpo();
          }}
        >
          打新
        </button>
      </div>
      {tab === "watch" ? (
        <section className="panel">
          <div className="inline-form">
            <select
              className={inputClass}
              value={market}
              onChange={(e) => setMarket(e.target.value)}
            >
              <option value="ALL">全部</option>
              <option value="CN_A">A股</option>
              <option value="HK">港股</option>
              <option value="NEEQ">新三板</option>
              <option value="NEEQ_INNOVATION">创新层</option>
            </select>
            <div className="autocomplete">
              <input
                className={inputClass}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="输入代码或名称模糊搜索"
              />
              {candidates.length > 0 && (
                <div className="suggestions">
                  {candidates.map((stock) => (
                    <button
                      type="button"
                      key={`${stock.market}-${stock.symbol}`}
                      onClick={() => void add(stock)}
                    >
                      <strong>{stock.symbol}</strong> {stock.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>市场</th>
                  <th>代码</th>
                  <th>名称</th>
                  <th>现价</th>
                  <th>涨跌幅</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                  {items.map((item) => (
                    <tr key={`${item.market}-${item.symbol}`} className={`watch-row ${trendClass(item.change_pct)}`}>
                    <td>{marketLabel[item.market] || item.market}</td>
                    <td>
                      <button
                        type="button"
                        className="symbol-link"
                        onClick={() =>
                          openDetail(item.market, item.symbol, item.name)
                        }
                        title="查看最新 F10 资料"
                      >
                        <code>{item.symbol}</code>
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="text-button"
                        onClick={() =>
                          openDetail(item.market, item.symbol, item.name)
                        }
                      >
                        {item.name || "--"}
                      </button>
                    </td>
                    <td className={`watch-price ${trendClass(item.change_pct)}`}>{item.current_price ?? "--"}</td>
                    <td className={`watch-change ${trendClass(item.change_pct)}`}>{formatSignedPercent(item.change_pct)}</td>
                    <td>
                      <button
                        type="button"
                        onClick={async () => {
                          await api.removeWatchlist(item.market, item.symbol);
                          setItems(await api.listWatchlist());
                        }}
                      >
                        移除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : (
        <section className="panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">IPO CALENDAR</p>
              <h2>未来一周可申购</h2>
            </div>
            <button type="button" onClick={() => void loadIpo()}>
              刷新
            </button>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>申购日</th>
                  <th>市场</th>
                  <th>代码</th>
                  <th>名称</th>
                  <th>发行价</th>
                  <th>资料</th>
                </tr>
              </thead>
              <tbody>
                {(ipo?.items || []).map((item) => (
                  <tr key={`${item.market}-${item.symbol}-${item.apply_date}`}>
                    <td>{item.apply_date}</td>
                    <td>{marketLabel[item.market] || item.market}</td>
                    <td>
                      <button
                        type="button"
                        className="symbol-link"
                        onClick={() =>
                          openDetail(item.market, item.symbol, item.name)
                        }
                        title="查看最新 F10 资料"
                      >
                        <code>{item.symbol}</code>
                      </button>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="text-button"
                        onClick={() =>
                          openDetail(item.market, item.symbol, item.name)
                        }
                      >
                        {item.name}
                      </button>
                    </td>
                    <td>{item.price || "--"}</td>
                    <td>
                      {item.prospectus_url ? (
                        <a
                          href={item.prospectus_url}
                          target="_blank"
                          rel="noreferrer"
                        >
                          招股书
                        </a>
                      ) : (
                        "--"
                      )}
                    </td>
                  </tr>
                ))}
                {ipo && !ipo.items.length && (
                  <tr>
                    <td colSpan={6} className="empty-state">
                      当前窗口暂无可申购股票
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}
      {detailStock && (
        <StockDetailDrawer
          stock={detailStock}
          onClose={() => setDetailStock(null)}
        />
      )}
    </>
  );
}

function renderResearchInlineMarkdown(text: string) {
  return text.split(/(\*\*[^*]+\*\*|__[^_]+__)/g).map((part, index) => {
    const isBold =
      (part.startsWith("**") && part.endsWith("**")) ||
      (part.startsWith("__") && part.endsWith("__"));
    return isBold ? <strong key={index}>{part.slice(2, -2)}</strong> : part;
  });
}

function renderResearchMarkdown(text: string) {
  const nodes: ReactNode[] = [];
  let bullets: string[] = [];

  const flushBullets = () => {
    if (!bullets.length) return;
    const current = bullets;
    bullets = [];
    nodes.push(
      <ul className="research-list" key={`ul-${nodes.length}`}>
        {current.map((item, index) => (
          <li key={index}>{renderResearchInlineMarkdown(item)}</li>
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
      nodes.push(<h4 key={`h4-${nodes.length}`}>{renderResearchInlineMarkdown(line.slice(4))}</h4>);
      return;
    }
    if (line.startsWith("## ")) {
      nodes.push(<h3 key={`h3-${nodes.length}`}>{renderResearchInlineMarkdown(line.slice(3))}</h3>);
      return;
    }
    if (line.startsWith("# ")) {
      nodes.push(<h2 key={`h2-${nodes.length}`}>{renderResearchInlineMarkdown(line.slice(2))}</h2>);
      return;
    }
    if (line.startsWith(">")) {
      nodes.push(<blockquote key={`quote-${nodes.length}`}>{renderResearchInlineMarkdown(line.replace(/^>\s?/, ""))}</blockquote>);
      return;
    }
    if (/^[一二三四五六七八九十]+[、.．]\s?/.test(line)) {
      nodes.push(<h3 key={`cn-heading-${nodes.length}`}>{renderResearchInlineMarkdown(line)}</h3>);
      return;
    }
    nodes.push(<p key={`p-${nodes.length}`}>{renderResearchInlineMarkdown(line)}</p>);
  });
  flushBullets();
  return nodes;
}

function ResearchPage() {
  const [tab, setTab] = useState<ResearchTab>("chat");
  const [market, setMarket] = useState("CN_A");
  const [query, setQuery] = useState("");
  const [report, setReport] = useState("");
  const [running, setRunning] = useState(false);
  const [stage, setStage] = useState("");
  const [error, setError] = useState("");
  const [chatQuestion, setChatQuestion] = useState("请帮我检查当前股票数据治理、知识图谱和选股分析能力还缺什么。");
  const [chatAnswer, setChatAnswer] = useState("");
  const [chatRunning, setChatRunning] = useState(false);
  const [assets, setAssets] = useState<DataAsset[]>([]);
  const [knowledge, setKnowledge] = useState<KnowledgeBase[]>([]);
  const [selectedAssets, setSelectedAssets] = useState<string[]>([]);
  const [selectedKnowledge, setSelectedKnowledge] = useState<number[]>([]);
  const [reports, setReports] = useState<ResearchReportSummary[]>([]);
  const [selectedReport, setSelectedReport] = useState<ResearchReportDetail | null>(null);

  async function loadReports() {
    const result = await api.listResearchReports();
    setReports(result);
  }

  useEffect(() => {
    void Promise.all([api.listDataAssets(), api.listKnowledgeBases(), api.listResearchReports()])
      .then(([assetResult, knowledgeResult, reportResult]) => {
        setAssets(assetResult);
        setKnowledge(knowledgeResult);
        setReports(reportResult);
        setSelectedAssets((current) =>
          current.length ? current : assetResult.filter((item) => item.enabled).map((item) => item.asset_code),
        );
        setSelectedKnowledge((current) => {
          if (current.length) return current;
          const fullKg = knowledgeResult.find((item) => item.kb_code === "STOCK_FULL_KG");
          return fullKg ? [fullKg.id] : knowledgeResult.filter((item) => item.enabled).slice(0, 1).map((item) => item.id);
        });
      })
      .catch((e) => setError(e instanceof Error ? e.message : "研究中心资源加载失败"));
  }, []);

  function toggleAsset(code: string) {
    setSelectedAssets((current) =>
      current.includes(code) ? current.filter((item) => item !== code) : [...current, code],
    );
  }

  function toggleKnowledge(id: number) {
    setSelectedKnowledge((current) =>
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id],
    );
  }

  async function openReport(id: number) {
    try {
      const detail = await api.getResearchReport(id);
      setSelectedReport(detail);
      setReport(detail.report_markdown);
      setError("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "研报加载失败");
    }
  }

  async function sendChat(event: FormEvent) {
    event.preventDefault();
    const question = chatQuestion.trim();
    if (!question) return;
    setChatRunning(true);
    setChatAnswer("");
    setError("");
    try {
      const result = await api.chatWithModel({
        task_type: "qa_query",
        messages: [
          {
            role: "system",
            content:
              "你是研究中心对话页签的问答问数智能体，围绕数据治理、知识图谱、股票分析、问数和问答执行操作。回答要区分事实、推断和缺失数据。",
          },
          { role: "user", content: question },
        ],
        temperature: 0.2,
        max_tokens: 2200,
        metadata_json: {
          skill_code: "STOCK_QA_QUERY",
          agent: "QA_QUERY_AGENT",
          data_source_codes: selectedAssets,
          knowledge_base_ids: selectedKnowledge,
        },
      });
      setChatAnswer(result.response_text);
      void loadReports();
    } catch (e) {
      setChatAnswer(e instanceof Error ? e.message : "智能体调用失败");
    } finally {
      setChatRunning(false);
    }
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    const symbol = query.trim();
    if (!symbol) return;
    setRunning(true);
    setReport("");
    setSelectedReport(null);
    setStage("");
    setError("");
    try {
      let nextReport = "";
      await api.analyzeResearch(
        {
          market,
          symbol,
          top_k: 5,
          data_source_codes: selectedAssets,
          knowledge_base_ids: selectedKnowledge,
        },
        {
          onStage: (data) => setStage(data.message),
          onReport: (delta) => {
            nextReport += delta;
            setReport((current) => current + delta);
          },
          onDone: (data) => {
            setStage(`研报已保存${data.report_id ? `：#${data.report_id}` : ""}`);
            void loadReports().then(() => {
              if (data.report_id) void openReport(data.report_id);
            });
          },
          onError: setError,
        },
      );
    } catch (e) {
      setError(e instanceof Error ? e.message : "研究分析失败");
    } finally {
      setRunning(false);
    }
  }
  return (
    <>
      <PageHeader
        eyebrow="AI RESEARCH"
        title="研究中心"
        detail={
          running
            ? stage || "正在生成研报..."
            : chatRunning
              ? "智能体正在回答..."
              : "默认对话页签；研究页签用于生成、保存和复盘研报"
        }
      />
      <div className="resource-tabs">
        <button type="button" className={tab === "chat" ? "active" : ""} onClick={() => setTab("chat")}>
          对话
        </button>
        <button type="button" className={tab === "research" ? "active" : ""} onClick={() => setTab("research")}>
          研究
        </button>
      </div>
      {tab === "chat" && (
        <section className="panel research-chat-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">AGENT CHAT</p>
              <h2>智能体对话</h2>
              <p>调用问答问数智能体，处理数据治理、分析、问数、问答和预警解释。</p>
            </div>
          </div>
          <form className="research-chat-form" onSubmit={sendChat}>
            <textarea
              className="text-input research-question-input"
              rows={5}
              value={chatQuestion}
              onChange={(e) => setChatQuestion(e.target.value)}
              placeholder="请输入数据治理、问数、问答、分析或预警问题"
            />
            <button className="primary-button" type="submit" disabled={chatRunning}>
              {chatRunning ? "调用中..." : "发送给智能体"}
            </button>
          </form>
          <ErrorText error={error} />
          <article className="research-markdown">
            {chatAnswer ? renderResearchMarkdown(chatAnswer) : <p className="empty-state">这里会显示智能体回答。</p>}
          </article>
        </section>
      )}
      {tab === "research" && (
        <div className="research-layout">
          <section className="panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">DEEP RESEARCH</p>
                <h2>生成 AI 深度研报</h2>
                <p>指定股票、数据源和知识库后，由研报智能体生成报告并保存数据库。</p>
              </div>
            </div>
            <form className="inline-form" onSubmit={submit}>
              <select
                className={inputClass}
                value={market}
                onChange={(e) => setMarket(e.target.value)}
              >
                <option value="CN_A">A股</option>
                <option value="HK">港股</option>
                <option value="NEEQ">新三板</option>
              </select>
              <input
                className={inputClass}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="输入股票代码或名称"
              />
              <button className="primary-button" type="submit" disabled={running}>
                {running ? "生成中..." : "生成研报"}
              </button>
            </form>
            <div className="selection-grid research-selection-grid">
              <label>
                数据源 / DW 数据层
                {assets.length ? (
                  assets.map((item) => (
                    <span key={item.id}>
                      <input
                        type="checkbox"
                        checked={selectedAssets.includes(item.asset_code)}
                        onChange={() => toggleAsset(item.asset_code)}
                      />
                      {item.display_name}
                    </span>
                  ))
                ) : (
                  <span>暂无数据资产</span>
                )}
              </label>
              <label>
                知识库
                {knowledge.length ? (
                  knowledge.map((item) => (
                    <span key={item.id}>
                      <input
                        type="checkbox"
                        checked={selectedKnowledge.includes(item.id)}
                        onChange={() => toggleKnowledge(item.id)}
                      />
                      {item.kb_name}（{item.entity_count} 实体 / {item.relation_count} 关系）
                    </span>
                  ))
                ) : (
                  <span>暂无知识库</span>
                )}
              </label>
            </div>
            <ErrorText error={error} />
            <article className="research-markdown">
              {report ? (
                renderResearchMarkdown(report)
              ) : (
                <p className="empty-state">
                  输入股票代码后，报告会以流式方式显示，并自动保存到数据库。
                </p>
              )}
            </article>
          </section>

          <section className="panel report-history-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">REPORT HISTORY</p>
                <h2>历史研报</h2>
                <p>所有生成过研报的股票都会保存，点击可查看对应研报。</p>
              </div>
            </div>
            <div className="report-history-list">
              {reports.length ? (
                reports.map((item) => (
                  <button
                    type="button"
                    key={item.id}
                    className={selectedReport?.id === item.id ? "report-history-item active" : "report-history-item"}
                    onClick={() => void openReport(item.id)}
                  >
                    <strong>{item.symbol} {item.name}</strong>
                    <span>{marketLabel[item.market] || item.market} / {item.rating || "--"} / {item.score ?? "--"}分</span>
                    <small>{formatDate(item.created_at)}</small>
                  </button>
                ))
              ) : (
                <p className="empty-state">暂无历史研报。</p>
              )}
            </div>
          </section>
        </div>
      )}
    </>
  );
}
/* Resource identifiers are generated server-side; forms intentionally omit code inputs. */
