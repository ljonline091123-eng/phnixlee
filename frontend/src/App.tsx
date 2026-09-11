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
  type ModelCallLog,
  type ModelInstance,
  type ModelInstancePayload,
  type ModelProvider,
  type ModelProviderPayload,
  type ModelRoute,
  type ModelSkill,
  type ModelSkillPayload,
  type StockSymbol,
  type SyncLog,
  type WatchlistItem,
} from "./api";
import { StockDetailDrawer } from "./StockDetailDrawer";

type ModuleView = "data" | "model" | "watch" | "research";
type ModelHubTab =
  "models" | "agents" | "skills" | "assets" | "knowledge" | "logs";
type DataView = "sources" | "interfaces" | "universe";
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
  provider_type: "DEEPSEEK",
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
type SkillForm = ModelSkillPayload & { id?: number };
type AgentForm = AgentPayload & { id?: number };
type KnowledgeForm = {
  id?: number;
  kb_code: string;
  kb_name: string;
  description: string;
  source_tables: string[];
  version: string;
  enabled: boolean;
};
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
const emptyKnowledge: KnowledgeForm = {
  kb_code: "",
  kb_name: "",
  description: "",
  source_tables: [],
  version: "1.0.0",
  enabled: true,
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
  const [logs, setLogs] = useState<ModelCallLog[]>([]);
  const [notice, setNotice] = useState("");
  const [editingProvider, setEditingProvider] =
    useState<ProviderForm>(emptyProvider);
  const [editingInstance, setEditingInstance] =
    useState<InstanceForm>(emptyInstance);
  const [editingSkill, setEditingSkill] = useState<SkillForm>(emptySkill);
  const [editingAgent, setEditingAgent] = useState<AgentForm>(emptyAgent);
  const [editingKnowledge, setEditingKnowledge] =
    useState<KnowledgeForm>(emptyKnowledge);
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
        api.listModelCallLogs(),
      ]);
      setProviders(result[0]);
      setInstances(result[1]);
      setRoutes(result[2]);
      setSkills(result[3]);
      setAgents(result[4]);
      setAssets(result[5]);
      setKnowledge(result[6]);
      setLogs(result[7]);
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
      const { id, ...payload } = editingProvider;
      if (id) await api.updateModelProvider(id, payload);
      else await api.createModelProvider(payload);
      setEditingProvider(emptyProvider);
      await load();
      message("模型供应商已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "供应商保存失败");
    }
  }
  async function saveInstance(event: FormEvent) {
    event.preventDefault();
    try {
      const { id, ...payload } = editingInstance;
      if (!payload.provider_id) throw new Error("请选择供应商");
      if (id && !payload.api_key) delete payload.api_key;
      if (id) await api.updateModelInstance(id, payload);
      else await api.createModelInstance(payload);
      setEditingInstance({
        ...emptyInstance,
        provider_id: providers[0]?.id || 0,
      });
      await load();
      message("模型实例已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "模型实例保存失败");
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
  async function saveSkill(event: FormEvent) {
    event.preventDefault();
    try {
      const { id, ...payload } = editingSkill;
      if (id) await api.updateModelSkill(id, payload);
      else await api.createModelSkill(payload);
      setEditingSkill(emptySkill);
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
      const { id, ...payload } = editingAgent;
      if (id) await api.updateAgent(id, payload);
      else await api.createAgent(payload);
      setEditingAgent(emptyAgent);
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
  async function saveKnowledge(event: FormEvent) {
    event.preventDefault();
    try {
      const { id, ...payload } = editingKnowledge;
      if (id) await api.updateKnowledgeBase(id, payload);
      else await api.createKnowledgeBase(payload);
      setEditingKnowledge(emptyKnowledge);
      await load();
      message("知识库已保存");
    } catch (e) {
      message(e instanceof Error ? e.message : "知识库保存失败");
    }
  }
  async function buildKnowledge(item: KnowledgeBase) {
    try {
      const result = await api.buildKnowledgeBase(item.id);
      await load();
      message(`知识图谱构建完成，生成 ${result.entities_created} 个实体`);
    } catch (e) {
      message(e instanceof Error ? e.message : "知识库构建失败");
    }
  }
  async function deleteKnowledge(item: KnowledgeBase) {
    if (!window.confirm(`删除知识库 ${item.kb_name}？`)) return;
    try {
      await api.deleteKnowledgeBase(item.id);
      await load();
      message("知识库已删除");
    } catch (e) {
      message(e instanceof Error ? e.message : "知识库可能正在被智能体引用");
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
          onProvider={saveProvider}
          onInstance={saveInstance}
          onEditProvider={(item) => setEditingProvider({ ...item })}
          onEditInstance={(item) =>
            setEditingInstance({ ...item, api_key: "" })
          }
          onDeleteProvider={deleteProvider}
          onDeleteInstance={deleteInstance}
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
          onSave={saveAgent}
          onEdit={(item) =>
            setEditingAgent({
              ...item,
              id: item.id,
              description: item.description || "",
            })
          }
          onDelete={deleteAgent}
        />
      )}
      {tab === "skills" && (
        <SkillTab
          skills={skills}
          value={editingSkill}
          setValue={setEditingSkill}
          onSave={saveSkill}
          onDelete={deleteSkill}
        />
      )}
      {tab === "assets" && (
        <AssetTab
          assets={assets}
          onInspect={async (item) => {
            try {
              await api.inspectDataAsset(item.id);
              await load();
              message("数据资产已检查");
            } catch (e) {
              message(e instanceof Error ? e.message : "检查失败");
            }
          }}
          onToggle={async (item) => {
            await api.updateDataAsset(item.id, { enabled: !item.enabled });
            await load();
          }}
        />
      )}
      {tab === "knowledge" && (
        <KnowledgeTab
          knowledge={knowledge}
          value={editingKnowledge}
          setValue={setEditingKnowledge}
          onSave={saveKnowledge}
          onBuild={buildKnowledge}
          onDelete={deleteKnowledge}
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

function ModelTab(props: {
  providers: ModelProvider[];
  instances: ModelInstance[];
  routes: ModelRoute[];
  provider: ProviderForm;
  setProvider: (v: ProviderForm) => void;
  instance: InstanceForm;
  setInstance: (v: InstanceForm) => void;
  onProvider: (e: FormEvent) => void;
  onInstance: (e: FormEvent) => void;
  onEditProvider: (v: ModelProvider) => void;
  onEditInstance: (v: ModelInstance) => void;
  onDeleteProvider: (v: ModelProvider) => void;
  onDeleteInstance: (v: ModelInstance) => void;
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
  } = props;
  return (
    <>
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">PROVIDERS</p>
            <h2>模型供应商</h2>
          </div>
        </div>
        <form className="field-grid" onSubmit={props.onProvider}>
          <input
            className={inputClass}
            placeholder="供应商编码"
            value={provider.provider_code}
            onChange={(e) =>
              setProvider({ ...provider, provider_code: e.target.value })
            }
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
                provider_type: e.target.value as ProviderForm["provider_type"],
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
          <button className="primary-button" type="submit">
            {provider.id ? "更新供应商" : "新增供应商"}
          </button>
        </form>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>供应商</th>
                <th>类型</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {providers.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.provider_name}</strong>
                    <code>{item.provider_code}</code>
                  </td>
                  <td>
                    {providerTypeLabel[item.provider_type] ||
                      item.provider_type}
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
              ))}
            </tbody>
          </table>
        </div>
      </section>
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">MODEL INSTANCES</p>
            <h2>模型实例</h2>
          </div>
        </div>
        <form className="field-grid" onSubmit={props.onInstance}>
          <select
            className={inputClass}
            value={instance.provider_id}
            onChange={(e) =>
              setInstance({ ...instance, provider_id: Number(e.target.value) })
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
            placeholder="实例编码"
            value={instance.instance_code}
            onChange={(e) =>
              setInstance({ ...instance, instance_code: e.target.value })
            }
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
            placeholder="API Key（可选）"
            type="password"
            value={instance.api_key || ""}
            onChange={(e) =>
              setInstance({ ...instance, api_key: e.target.value })
            }
          />
          <input
            className={inputClass}
            placeholder="用途"
            value={instance.purpose}
            onChange={(e) =>
              setInstance({ ...instance, purpose: e.target.value })
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
          <button className="primary-button" type="submit">
            {instance.id ? "更新实例" : "新增实例"}
          </button>
        </form>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>实例</th>
                <th>供应商</th>
                <th>模型</th>
                <th>Key</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {instances.map((item) => (
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
              ))}
            </tbody>
          </table>
        </div>
        <div className="table-wrap route-table-wrap">
          <h3>任务路由</h3>
          <table className="route-table">
            <thead>
              <tr>
                <th>任务</th>
                <th>首选实例</th>
                <th>降级链</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              {routes.map((route) => (
                <tr key={route.id}>
                  <td>
                    <code className="route-code">{route.task_type}</code>
                  </td>
                  <td>
                    <span className="route-pill">{route.preferred_instance_code}</span>
                  </td>
                  <td>
                    {route.fallback_chain_json.length ? (
                      <span className="route-chain">
                        {route.fallback_chain_json.map((item, index) => (
                          <span key={`${route.id}-${item}-${index}`}>
                            {index > 0 ? <em>→</em> : null}
                            <b>{item}</b>
                          </span>
                        ))}
                      </span>
                    ) : (
                      "--"
                    )}
                  </td>
                  <td>
                    <Status enabled={route.enabled} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
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
  onSave: (e: FormEvent) => void;
  onEdit: (v: AgentDefinition) => void;
  onDelete: (v: AgentDefinition) => void;
}) {
  const toggle = (
    key:
      "child_agent_ids" | "skill_ids" | "knowledge_base_ids" | "data_asset_ids",
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
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">AGENT REGISTRY</p>
          <h2>智能体管理</h2>
        </div>
      </div>
      <form onSubmit={onSave}>
        <div className="field-grid">
          <input
            className={inputClass}
            placeholder="智能体编码"
            value={value.agent_code}
            onChange={(e) => setValue({ ...value, agent_code: e.target.value })}
            required
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
            max={50}
            value={value.max_iterations}
            onChange={(e) =>
              setValue({ ...value, max_iterations: Number(e.target.value) })
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
        <div className="selection-grid">
          <label>
            子智能体
            {agents
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
              ))}
          </label>
          <label>
            Skills
            {skills.map((item) => (
              <span key={item.id}>
                <input
                  type="checkbox"
                  checked={value.skill_ids.includes(item.id)}
                  onChange={() => toggle("skill_ids", item.id)}
                />
                {item.skill_name}
              </span>
            ))}
          </label>
          <label>
            知识库
            {knowledge.map((item) => (
              <span key={item.id}>
                <input
                  type="checkbox"
                  checked={value.knowledge_base_ids.includes(item.id)}
                  onChange={() => toggle("knowledge_base_ids", item.id)}
                />
                {item.kb_name}
              </span>
            ))}
          </label>
          <label>
            数据源
            {assets.map((item) => (
              <span key={item.id}>
                <input
                  type="checkbox"
                  checked={value.data_asset_ids.includes(item.id)}
                  onChange={() => toggle("data_asset_ids", item.id)}
                />
                {item.display_name}
              </span>
            ))}
          </label>
        </div>
        <button className="primary-button" type="submit">
          {value.id ? "更新智能体" : "保存智能体"}
        </button>
      </form>
      <div className="table-wrap">
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
            {agents.map((item) => (
              <tr key={item.id}>
                <td>
                  <strong>{item.display_name}</strong>
                  <code>{item.agent_code}</code>
                </td>
                <td>{item.model_instance_code || "自动路由"}</td>
                <td>
                  {item.skill_ids.length} Skill /{" "}
                  {item.knowledge_base_ids.length} 知识库 /{" "}
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
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function SkillTab({
  skills,
  value,
  setValue,
  onSave,
  onDelete,
}: {
  skills: ModelSkill[];
  value: SkillForm;
  setValue: (v: SkillForm) => void;
  onSave: (e: FormEvent) => void;
  onDelete: (v: ModelSkill) => void;
}) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">MARKDOWN SKILLS</p>
          <h2>Skill 管理</h2>
        </div>
        <p>保存后同步到 backend/skill_docs/*.SKILL.md</p>
      </div>
      <form onSubmit={onSave}>
        <div className="field-grid">
          <input
            className={inputClass}
            placeholder="Skill 编码"
            value={value.skill_code}
            onChange={(e) => setValue({ ...value, skill_code: e.target.value })}
            required
          />
          <input
            className={inputClass}
            placeholder="Skill 名称"
            value={value.skill_name}
            onChange={(e) => setValue({ ...value, skill_name: e.target.value })}
            required
          />
          <input
            className={inputClass}
            placeholder="版本"
            value={value.version || "1.0.0"}
            onChange={(e) => setValue({ ...value, version: e.target.value })}
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
            rows={12}
            placeholder="Markdown 指令内容"
            value={value.instructions}
            onChange={(e) =>
              setValue({ ...value, instructions: e.target.value })
            }
            required
          />
        </div>
        <button className="primary-button" type="submit">
          {value.id ? "更新 Skill" : "保存 Skill"}
        </button>
      </form>
      <div className="table-wrap">
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
            {skills.map((item) => (
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
                  <button
                    type="button"
                    onClick={() =>
                      setValue({
                        ...item,
                        description: item.description || "",
                        version: item.version || "1.0.0",
                        config_json: item.config_json || {},
                      })
                    }
                  >
                    编辑
                  </button>
                  <button type="button" onClick={() => onDelete(item)}>
                    删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function AssetTab({
  assets,
  onInspect,
  onToggle,
}: {
  assets: DataAsset[];
  onInspect: (v: DataAsset) => void;
  onToggle: (v: DataAsset) => void;
}) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">GOVERNED TABLES</p>
          <h2>数据资产</h2>
        </div>
        <p>智能体只能访问登记在册的本地数据库表</p>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>资产</th>
              <th>本地表</th>
              <th>字段</th>
              <th>行数</th>
              <th>治理状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {assets.map((item) => (
              <tr key={item.id}>
                <td>
                  <strong>{item.display_name}</strong>
                  <code>{item.asset_code}</code>
                </td>
                <td>{item.table_name}</td>
                <td>{item.columns.length} 个</td>
                <td>{item.row_count.toLocaleString()}</td>
                <td>{item.governance_status}</td>
                <td className="button-row">
                  <button type="button" onClick={() => onInspect(item)}>
                    检查
                  </button>
                  <button type="button" onClick={() => void onToggle(item)}>
                    {item.enabled ? "停用" : "启用"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function KnowledgeTab({
  knowledge,
  value,
  setValue,
  onSave,
  onBuild,
  onDelete,
}: {
  knowledge: KnowledgeBase[];
  value: KnowledgeForm;
  setValue: (v: KnowledgeForm) => void;
  onSave: (e: FormEvent) => void;
  onBuild: (v: KnowledgeBase) => void;
  onDelete: (v: KnowledgeBase) => void;
}) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">KNOWLEDGE GRAPH</p>
          <h2>知识库与知识图谱</h2>
        </div>
        <p>从本地数据资产生成文档、实体和关系</p>
      </div>
      <form className="field-grid" onSubmit={onSave}>
        <input
          className={inputClass}
          placeholder="知识库编码"
          value={value.kb_code}
          onChange={(e) => setValue({ ...value, kb_code: e.target.value })}
          required
        />
        <input
          className={inputClass}
          placeholder="知识库名称"
          value={value.kb_name}
          onChange={(e) => setValue({ ...value, kb_name: e.target.value })}
          required
        />
        <input
          className={inputClass}
          placeholder="版本"
          value={value.version}
          onChange={(e) => setValue({ ...value, version: e.target.value })}
        />
        <input
          className="field-span-2"
          placeholder="来源表，逗号分隔；留空使用全部资产"
          value={value.source_tables.join(",")}
          onChange={(e) =>
            setValue({
              ...value,
              source_tables: e.target.value
                .split(",")
                .map((x) => x.trim())
                .filter(Boolean),
            })
          }
        />
        <textarea
          className="field-span-2"
          rows={2}
          placeholder="描述"
          value={value.description}
          onChange={(e) => setValue({ ...value, description: e.target.value })}
        />
        <label className="checkbox-row">
          <input
            type="checkbox"
            checked={value.enabled}
            onChange={(e) => setValue({ ...value, enabled: e.target.checked })}
          />
          启用知识库
        </label>
        <button className="primary-button" type="submit">
          {value.id ? "更新知识库" : "保存知识库"}
        </button>
      </form>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>知识库</th>
              <th>来源表</th>
              <th>实体</th>
              <th>关系</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {knowledge.map((item) => (
              <tr key={item.id}>
                <td>
                  <strong>{item.kb_name}</strong>
                  <code>
                    {item.kb_code} / v{item.version}
                  </code>
                </td>
                <td>{item.source_tables.join(", ") || "全部资产"}</td>
                <td>{item.entity_count}</td>
                <td>{item.relation_count}</td>
                <td>
                  <Status enabled={item.enabled} /> <small>{item.status}</small>
                </td>
                <td className="button-row">
                  <button
                    type="button"
                    onClick={() =>
                      setValue({
                        id: item.id,
                        kb_code: item.kb_code,
                        kb_name: item.kb_name,
                        description: item.description || "",
                        source_tables: item.source_tables,
                        version: item.version,
                        enabled: item.enabled,
                      })
                    }
                  >
                    编辑
                  </button>
                  <button type="button" onClick={() => void onBuild(item)}>
                    重建图谱
                  </button>
                  <button type="button" onClick={() => onDelete(item)}>
                    删除
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
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
  const [market, setMarket] = useState("CN_A");
  const [query, setQuery] = useState("");
  const [report, setReport] = useState("");
  const [running, setRunning] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault();
    const symbol = query.trim();
    if (!symbol) return;
    setRunning(true);
    setReport("");
    setError("");
    try {
      let nextReport = "";
      await api.analyzeResearch(
        { market, symbol, top_k: 5 },
        {
          onReport: (delta) => {
            nextReport += delta;
            setReport((current) => current + delta);
          },
          onDone: () => {
            window.localStorage.setItem(
              `gemini-quant-agent:research-report:${market}:${symbol}`,
              nextReport,
            );
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
        detail={running ? "正在生成研报..." : "DeepSeek / 多智能体分析"}
      />
      <section className="panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">DEEP RESEARCH</p>
            <h2>生成 AI 深度研报</h2>
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
        <ErrorText error={error} />
        <article className="research-markdown">
          {report ? (
            renderResearchMarkdown(report)
          ) : (
            <p className="empty-state">
              输入股票代码后，报告会以流式方式显示。
            </p>
          )}
        </article>
      </section>
    </>
  );
}
