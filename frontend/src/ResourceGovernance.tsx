import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, type AgentDefinition, type BatchGovernanceResult, type DataAsset,
  type DataAssetPayload, type DataGovernanceSummary, type DataPreview, type GovernanceRun,
  type GraphExplore, type KnowledgeBase, type KnowledgeGraph, type KnowledgeGraphPayload } from "./api";

type CommonProps = { assets: DataAsset[]; agents: AgentDefinition[]; reload: () => Promise<void>; notify: (text: string) => void };
const stateLabel: Record<string, string> = { PENDING: "待治理", GOVERNED: "已治理", LOCKED: "已锁定", READY: "可用", MISSING: "来源缺失", SCHEMA_CHANGED: "字段变化", COMPLETED: "治理完成", PENDING_REVIEW: "待人工审核", SKIPPED: "已跳过", FAILED: "执行失败", RUNNING: "执行中" };
const label = (value: string) => stateLabel[value] || value;
const asText = (value: unknown) => value == null ? "" : typeof value === "object" ? JSON.stringify(value) : String(value);
const errText = (error: unknown) => error instanceof Error ? error.message : "操作失败";
function Dialog({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  return <div className="resource-dialog-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) close(); }}>
    <section className="resource-dialog" role="dialog" aria-modal="true">
      <div className="resource-dialog-header"><h3>{title}</h3><button type="button" onClick={close}>关闭</button></div>
      <div className="resource-dialog-body">{children}</div>
    </section>
  </div>;
}
function SourceChoices({ assets, selected, change }: { assets: DataAsset[]; selected: number[]; change: (ids: number[]) => void }) {
  return <div className="governance-source-grid">{assets.map(item =>
    <label key={item.id}><input type="checkbox" checked={selected.includes(item.id)} onChange={() => change(selected.includes(item.id) ? selected.filter(id => id !== item.id) : [...selected, item.id])} />{item.display_name} <code>{item.table_name}</code></label>
  )}</div>;
}
function AgentChoice({ agents, value, change, code }: { agents: AgentDefinition[]; value: number | undefined; change: (id?: number) => void; code: string }) {
  return <label>治理智能体 <select value={value || ""} onChange={event => change(event.target.value ? Number(event.target.value) : undefined)}>
    <option value="">默认智能体（{code}）</option>
    {agents.filter(item => item.enabled).map(item => <option key={item.id} value={item.id}>{item.display_name}</option>)}
  </select></label>;
}

export function GovernedAssetTab({ assets, agents, reload, notify }: CommonProps) {
  const [tables, setTables] = useState<string[]>([]);
  const [dialog, setDialog] = useState<"create" | "edit" | "preview" | "govern" | "result" | "batch-result" | null>(null);
  const [active, setActive] = useState<DataAsset | null>(null);
  const [form, setForm] = useState<DataAssetPayload>({ asset_code: "", table_name: "", display_name: "", description: "", enabled: true });
  const [preview, setPreview] = useState<DataPreview | null>(null);
  const [runs, setRuns] = useState<GovernanceRun[]>([]);
  const [governanceResult, setGovernanceResult] = useState<GovernanceRun | null>(null);
  const [batchResult, setBatchResult] = useState<BatchGovernanceResult[]>([]);
  const [sources, setSources] = useState<number[]>([]);
  const [agentId, setAgentId] = useState<number>();
  const [selected, setSelected] = useState<number[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => { void api.listAssetSourceTables().then(setTables).catch(error => setError(errText(error))); }, []);
  const run = async (action: () => Promise<unknown>, success: string) => {
    setBusy(true); setError("");
    try { await action(); await reload(); notify(success); return true; }
    catch (error) { setError(errText(error)); return false; }
    finally { setBusy(false); }
  };
  const openPreview = async (item: DataAsset) => {
    setActive(item); setPreview(null); setRuns([]); setError(""); setDialog("preview");
    try { const [data, history] = await Promise.all([api.previewDataAsset(item.id), api.listGovernanceRuns("ASSET", item.id)]); setPreview(data); setRuns(history); }
    catch (error) { setError(errText(error)); }
  };
  const save = async (event: FormEvent) => {
    event.preventDefault();
    const ok = await run(() => dialog === "edit" && active
      ? api.updateDataAsset(active.id, { display_name: form.display_name, description: form.description,
          allowed_columns: form.allowed_columns, enabled: form.enabled })
      : api.createDataAsset(form), "数据资产已保存");
    if (ok) setDialog(null);
  };
  const govern = async (event: FormEvent) => {
    event.preventDefault(); if (!active) return;
    setBusy(true); setError("");
    try {
      const result = await api.governDataAsset(active.id, sources, agentId);
      setGovernanceResult(result); await reload(); setDialog("result");
      notify(result.status === "COMPLETED" ? "数据资产治理完成" : "治理已完成预审，存在待审核事项");
    } catch (error) { setError(errText(error)); }
    finally { setBusy(false); }
  };
  const batch = async () => {
    if (!selected.length) return;
    setBusy(true); setError("");
    try {
      const result = await api.batchGovernAssets(selected);
      const failed = result.results.filter(item => item.status === "FAILED" || item.status === "SKIPPED");
      setBatchResult(result.results);
      setSelected([]); await reload();
      notify(`批量治理完成：完成 ${result.results.length - failed.length} 项，未执行 ${failed.length} 项`);
      if (failed.length) setError(failed.map(item => `${item.target_id}: ${item.message || item.status}`).join("；"));
      setDialog("batch-result");
    } catch (error) { setError(errText(error)); } finally { setBusy(false); }
  };
  const summary = governanceResult?.summary_json as Partial<DataGovernanceSummary> | undefined;
  const qualityIssues = summary?.quality_issues || [];
  const pendingReview = summary?.pending_review || [];
  const targetTables = summary?.target_tables || [];
  return <section className="panel resource-management-panel">
    <div className="panel-heading"><div><p className="eyebrow">GOVERNED TABLES</p><h2>数据资产清单</h2><p>预览来源、选择智能体治理，并保留每次运行记录。批量治理使用各资产当前来源。</p></div>
      <div className="panel-actions"><button type="button" onClick={() => { setForm({ asset_code: "", table_name: "", display_name: "", description: "", enabled: true }); setActive(null); setError(""); setDialog("create"); }}>新增资产</button>
        <button type="button" disabled={!selected.length || busy} onClick={() => void batch()}>批量治理（{selected.length}）</button></div></div>
    {error && <p className="form-error governance-inline-error">{error}</p>}
    <div className="table-wrap resource-list-wrap"><table><thead><tr><th>选择</th><th>资产 / 来源</th><th>数据</th><th>治理状态</th><th>使用状态</th><th>操作</th></tr></thead><tbody>
      {assets.map(item => <tr key={item.id}><td><input aria-label={`选择${item.display_name}`} type="checkbox" disabled={item.governance_status === "LOCKED"} checked={selected.includes(item.id)} onChange={() => setSelected(selected.includes(item.id) ? selected.filter(id => id !== item.id) : [...selected, item.id])} /></td>
        <td><strong>{item.display_name}</strong><code>{item.asset_code} · {item.table_name}</code></td><td>{item.row_count.toLocaleString()} 行 / {item.columns.length} 字段<br /><small>{label(item.source_health)}</small></td>
        <td>{label(item.governance_status)}<br /><small>{item.last_governed_at ? new Date(item.last_governed_at).toLocaleString("zh-CN") : "尚未治理"}</small>{item.governance_report_json?.provider_code === "MOCK" && <><br /><small>本地模拟分析</small></>}</td>
        <td>{item.enabled ? "启用" : "停用"}</td><td className="button-row">
          <button type="button" onClick={() => void openPreview(item)}>预览</button>
          <button type="button" onClick={() => { setActive(item); setForm({ asset_code: item.asset_code, table_name: item.table_name, display_name: item.display_name, description: item.description || "", allowed_columns: item.allowed_columns, enabled: item.enabled }); setDialog("edit"); }}>编辑</button>
          <button type="button" disabled={item.governance_status === "LOCKED" || busy} onClick={() => { setActive(item); setSources([]); setAgentId(undefined); setGovernanceResult(null); setError(""); setDialog("govern"); }}>治理</button>
          <button type="button" onClick={() => void run(() => api.setAssetGovernanceState(item.id, item.governance_status === "LOCKED" ? "PENDING" : "LOCKED"), item.governance_status === "LOCKED" ? "已解锁" : "已锁定")}>{item.governance_status === "LOCKED" ? "解锁" : "锁定"}</button>
          <button type="button" onClick={() => void run(() => api.updateDataAsset(item.id, { enabled: !item.enabled }), "使用状态已更新")}>{item.enabled ? "停用" : "启用"}</button>
        </td></tr>)}</tbody></table></div>
    {(dialog === "create" || dialog === "edit") && <Dialog title={dialog === "create" ? "新增数据资产" : "编辑数据资产"} close={() => setDialog(null)}><form className="governance-form" onSubmit={save}>
      {error && <p className="form-error">{error}</p>}
      <label>资产编码<input required disabled={dialog === "edit"} value={form.asset_code} onChange={event => setForm({ ...form, asset_code: event.target.value.toUpperCase() })} /></label>
      <label>数据源表<select required disabled={dialog === "edit"} value={form.table_name} onChange={event => setForm({ ...form, table_name: event.target.value })}><option value="">选择已存在的数据表</option>{tables.filter(table => dialog === "edit" || !assets.some(item => item.table_name === table)).map(table => <option key={table} value={table}>{table}</option>)}</select></label>
      {dialog === "create" && tables.every(table => assets.some(item => item.table_name === table)) && <p>当前本地业务表均已登记。数据中台接入新表后，可在这里新增对应资产。</p>}
      <label>显示名称<input required value={form.display_name} onChange={event => setForm({ ...form, display_name: event.target.value })} /></label>
      <label>说明<textarea value={form.description || ""} onChange={event => setForm({ ...form, description: event.target.value })} /></label>
      {dialog === "edit" && active && <fieldset><legend>可读取字段</legend><div className="governance-source-grid">{active.columns.map(column => <label key={column}><input type="checkbox" checked={form.allowed_columns?.includes(column) || false} onChange={() => setForm({ ...form, allowed_columns: form.allowed_columns?.includes(column) ? form.allowed_columns.filter(name => name !== column) : [...(form.allowed_columns || []), column] })} />{column}</label>)}</div></fieldset>}
      <label><input type="checkbox" checked={form.enabled} onChange={event => setForm({ ...form, enabled: event.target.checked })} />启用</label><button className="primary-button" type="submit" disabled={busy}>保存</button>
    </form></Dialog>}
    {dialog === "govern" && active && <Dialog title={`治理数据资产：${active.display_name}`} close={() => setDialog(null)}><form className="governance-form" onSubmit={govern}><p>目标资产始终作为数据源。可以补充关联来源，治理结果会记录在历史中。</p>
      {error && <p className="form-error">{error}</p>}<SourceChoices assets={assets.filter(item => item.id !== active.id)} selected={sources} change={setSources} />
      <AgentChoice agents={agents} value={agentId} change={setAgentId} code="数据治理智能体" /><button className="primary-button" type="submit" disabled={busy}>{busy ? "治理中…" : "开始治理"}</button></form></Dialog>}
    {dialog === "result" && active && governanceResult && summary && <Dialog title={`治理结果：${active.display_name}`} close={() => setDialog(null)}><div className="governance-result">
      <div className="governance-result-summary">
        <div><small>执行结果</small><strong>{label(governanceResult.status)}</strong></div>
        <div><small>置信度</small><strong>{typeof summary.confidence === "number" ? `${(summary.confidence * 100).toFixed(0)}%` : "—"}</strong></div>
        <div><small>质量问题</small><strong>{qualityIssues.length}</strong></div>
        <div><small>待审核记录</small><strong>{pendingReview.length}</strong></div>
      </div>
      <section><h4>分类统计</h4><div className="governance-category-list">{Object.entries(summary.category_counts || {}).map(([name, count]) => <span key={name}><strong>{name}</strong>{count} 条</span>)}</div></section>
      <section><h4>目标表映射</h4>{targetTables.length ? <div className="table-wrap"><table><thead><tr><th>类别</th><th>目标表</th><th>业务键</th></tr></thead><tbody>{targetTables.map(item => <tr key={`${item.category}-${item.table_name}`}><td>{item.category}</td><td><code>{item.table_name}</code></td><td>{item.business_key.join("、")}</td></tr>)}</tbody></table></div> : <p className="governance-empty">暂无可确认的目标表映射。</p>}</section>
      <section><h4>质量问题</h4>{qualityIssues.length ? <div className="table-wrap"><table><thead><tr><th>记录</th><th>字段</th><th>问题</th><th>等级</th><th>证据</th></tr></thead><tbody>{qualityIssues.map((item, index) => <tr key={`${item.record_id}-${item.field_name}-${index}`}><td>{item.record_id}</td><td>{item.field_name}</td><td>{item.issue_type}</td><td><span className={`governance-severity ${item.severity.toLowerCase()}`}>{item.severity}</span></td><td>{item.evidence_text}</td></tr>)}</tbody></table></div> : <p className="governance-empty">硬规则未发现质量问题。</p>}</section>
      {pendingReview.length > 0 && <section><h4>待人工审核</h4><div className="governance-review-list">{pendingReview.map((item, index) => <article key={`${item.record_id}-${index}`}><strong>{item.record_id}</strong><span>{item.reason}</span><small>{item.evidence_text}</small></article>)}</div></section>}
      {(summary.missing_data?.length || 0) > 0 && <section><h4>缺失数据</h4><ul>{summary.missing_data?.map(item => <li key={item}>{item}</li>)}</ul></section>}
      <section><h4>治理依据</h4><p>{summary.evidence_text}</p><small>批次时间：{summary.as_of ? new Date(summary.as_of).toLocaleString("zh-CN") : "—"} · 运行记录 #{governanceResult.id}</small></section>
    </div></Dialog>}
    {dialog === "batch-result" && <Dialog title="批量治理结果" close={() => setDialog(null)}><div className="governance-result"><div className="table-wrap"><table><thead><tr><th>数据资产</th><th>结果</th><th>运行记录</th><th>说明</th></tr></thead><tbody>{batchResult.map(item => <tr key={item.target_id}><td>{assets.find(asset => asset.id === item.target_id)?.display_name || `#${item.target_id}`}</td><td>{label(item.status)}</td><td>{item.run_id ? `#${item.run_id}` : "—"}</td><td>{item.message || (item.status === "PENDING_REVIEW" ? "存在待审核事项" : "—")}</td></tr>)}</tbody></table></div></div></Dialog>}
    {dialog === "preview" && active && <Dialog title={`资产预览：${active.display_name}`} close={() => setDialog(null)}><div className="governance-detail">
      {error && <p className="form-error">{error}</p>}{preview && <><p>{preview.table_name} · 共 {preview.row_count.toLocaleString()} 行 · 只读预览前 {preview.rows.length} 行</p><div className="table-wrap"><table><thead><tr>{preview.columns.map(name => <th key={name}>{name}</th>)}</tr></thead><tbody>{preview.rows.map((row, index) => <tr key={index}>{preview.columns.map(name => <td key={name}>{asText(row[name])}</td>)}</tr>)}</tbody></table></div></>}
      <h3>最近治理结果</h3><pre>{JSON.stringify(active.governance_report_json || {}, null, 2)}</pre><h3>运行记录</h3>{runs.map(item => <p key={item.id}>#{item.id} · {label(item.status)} · {new Date(item.created_at).toLocaleString("zh-CN")} · {asText(item.summary_json.error || item.summary_json.evidence_text)}</p>)}</div></Dialog>}
  </section>;
}

const emptyBase = { kb_code: "", kb_name: "", description: "", source_tables: [] as string[], version: "1.0.0", enabled: true };
const emptyGraph: KnowledgeGraphPayload = { knowledge_base_id: 0, graph_code: "", graph_name: "", description: "", symbol: "", source_tables: [], version: "1.0.0", enabled: true };
export function GovernedKnowledgeTab({ knowledge, graphs, assets, agents, reload, notify }: CommonProps & { knowledge: KnowledgeBase[]; graphs: KnowledgeGraph[] }) {
  const [dialog, setDialog] = useState<"base" | "graph" | "documents" | "explore" | "govern" | null>(null);
  const [base, setBase] = useState<KnowledgeBase | null>(null);
  const [graph, setGraph] = useState<KnowledgeGraph | null>(null);
  const [baseForm, setBaseForm] = useState(emptyBase);
  const [graphForm, setGraphForm] = useState<KnowledgeGraphPayload>(emptyGraph);
  const [sources, setSources] = useState<number[]>([]);
  const [agentId, setAgentId] = useState<number>();
  const [selected, setSelected] = useState<number[]>([]);
  const [query, setQuery] = useState("");
  const [documents, setDocuments] = useState<Awaited<ReturnType<typeof api.searchKnowledgeBase>>>([]);
  const [explore, setExplore] = useState<GraphExplore | null>(null);
  const [runs, setRuns] = useState<GovernanceRun[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const run = async (action: () => Promise<unknown>, success: string) => {
    setBusy(true); setError("");
    try { await action(); await reload(); notify(success); return true; }
    catch (error) { setError(errText(error)); return false; }
    finally { setBusy(false); }
  };
  const saveBase = async (event: FormEvent) => {
    event.preventDefault(); const ok = await run(() => base ? api.updateKnowledgeBase(base.id, baseForm) : api.createKnowledgeBase(baseForm), "知识库已保存");
    if (ok) setDialog(null);
  };
  const saveGraph = async (event: FormEvent) => {
    event.preventDefault(); const ok = await run(() => graph ? api.updateKnowledgeGraph(graph.id, graphForm) : api.createKnowledgeGraph(graphForm), "知识图谱已保存");
    if (ok) setDialog(null);
  };
  const openDocuments = async (item: KnowledgeBase) => { setBase(item); setQuery(""); setDialog("documents"); setError(""); try { setDocuments(await api.searchKnowledgeBase(item.id)); } catch (error) { setError(errText(error)); } };
  const openGraph = async (item: KnowledgeGraph) => { setGraph(item); setQuery(""); setExplore(null); setRuns([]); setDialog("explore"); setError(""); try { const [data, history] = await Promise.all([api.exploreKnowledgeGraph(item.id), api.listGovernanceRuns("GRAPH", item.id)]); setExplore(data); setRuns(history); } catch (error) { setError(errText(error)); } };
  const govern = async (event: FormEvent) => { event.preventDefault(); if (!graph) return; const ok = await run(() => api.governKnowledgeGraph(graph.id, sources, agentId), "知识图谱治理完成"); if (ok) setDialog(null); };
  const batch = async () => {
    if (!selected.length) return;
    setBusy(true); setError("");
    try { const result = await api.batchGovernGraphs(selected); const failed = result.results.filter(item => item.status !== "SUCCESS"); setSelected([]); await reload(); notify(`批量治理完成：成功 ${result.results.length - failed.length} 项，未完成 ${failed.length} 项`); if (failed.length) setError(failed.map(item => `${item.target_id}: ${item.message || item.status}`).join("；")); }
    catch (error) { setError(errText(error)); } finally { setBusy(false); }
  };
  const graphSources = assets.filter(item => knowledge.find(kb => kb.id === (graph?.knowledge_base_id || graphForm.knowledge_base_id))?.source_tables.includes(item.table_name));
  return <div className="resource-stack">
    {error && <p className="form-error governance-inline-error">{error}</p>}
    <section className="panel resource-management-panel"><div className="panel-heading"><div><p className="eyebrow">KNOWLEDGE BASES</p><h2>知识库清单</h2><p>知识库管理资料来源；同一知识库可建立多张独立图谱。</p></div><button type="button" onClick={() => { setBase(null); setBaseForm({ ...emptyBase }); setError(""); setDialog("base"); }}>新增知识库</button></div>
      <div className="table-wrap resource-list-wrap"><table><thead><tr><th>知识库</th><th>来源数据资产</th><th>图谱数</th><th>状态</th><th>操作</th></tr></thead><tbody>{knowledge.map(item => <tr key={item.id}>
        <td><strong>{item.kb_name}</strong><code>{item.kb_code} · v{item.version}</code></td><td>{item.source_tables.join("、") || "未配置"}</td><td>{graphs.filter(g => g.knowledge_base_id === item.id).length}</td><td>{item.enabled ? "启用" : "停用"}</td>
        <td className="button-row"><button type="button" onClick={() => void openDocuments(item)}>查询资料</button><button type="button" onClick={() => { setBase(item); setBaseForm({ kb_code: item.kb_code, kb_name: item.kb_name, description: item.description || "", source_tables: [...item.source_tables], version: item.version, enabled: item.enabled }); setDialog("base"); }}>编辑</button>
          <button type="button" onClick={() => void run(() => api.updateKnowledgeBase(item.id, { enabled: !item.enabled }), "使用状态已更新")}>{item.enabled ? "停用" : "启用"}</button>
          <button type="button" onClick={() => { if (window.confirm(`删除知识库 ${item.kb_name}？`)) void run(() => api.deleteKnowledgeBase(item.id), "知识库已删除"); }}>删除</button></td></tr>)}</tbody></table></div></section>
    <section className="panel resource-management-panel"><div className="panel-heading"><div><p className="eyebrow">KNOWLEDGE GRAPHS</p><h2>知识图谱清单</h2><p>按股票或主题建立图谱，可查询实体、关系和证据。批量治理使用各图谱当前来源。</p></div><div className="panel-actions"><button type="button" disabled={!knowledge.length} onClick={() => { setGraph(null); setGraphForm({ ...emptyGraph, knowledge_base_id: knowledge[0]?.id || 0 }); setError(""); setDialog("graph"); }}>新增图谱</button><button type="button" disabled={!selected.length || busy} onClick={() => void batch()}>批量治理（{selected.length}）</button></div></div>
      <div className="table-wrap resource-list-wrap"><table><thead><tr><th>选择</th><th>图谱 / 知识库</th><th>范围与规模</th><th>治理状态</th><th>使用状态</th><th>操作</th></tr></thead><tbody>{graphs.map(item => <tr key={item.id}>
        <td><input aria-label={`选择${item.graph_name}`} type="checkbox" disabled={item.governance_status === "LOCKED"} checked={selected.includes(item.id)} onChange={() => setSelected(selected.includes(item.id) ? selected.filter(id => id !== item.id) : [...selected, item.id])} /></td>
        <td><strong>{item.graph_name}</strong><code>{item.graph_code} · {knowledge.find(kb => kb.id === item.knowledge_base_id)?.kb_name || item.knowledge_base_id}</code></td>
        <td>{item.symbol || "全部股票"}<br /><small>{item.entity_count.toLocaleString()} 实体 / {item.relation_count.toLocaleString()} 关系</small></td><td>{label(item.governance_status)}<br /><small>{item.last_governed_at ? new Date(item.last_governed_at).toLocaleString("zh-CN") : "尚未治理"}</small>{item.governance_report_json?.provider_code === "MOCK" && <><br /><small>本地模拟分析</small></>}</td><td>{item.enabled ? "启用" : "停用"}</td>
        <td className="button-row"><button type="button" onClick={() => void openGraph(item)}>查询关系</button><button type="button" onClick={() => { setGraph(item); setGraphForm({ knowledge_base_id: item.knowledge_base_id, graph_code: item.graph_code, graph_name: item.graph_name, description: item.description || "", symbol: item.symbol || "", source_tables: [...item.source_tables], version: item.version, enabled: item.enabled }); setDialog("graph"); }}>编辑</button>
          <button type="button" disabled={item.governance_status === "LOCKED" || busy} onClick={() => { setGraph(item); setSources([]); setAgentId(undefined); setError(""); setDialog("govern"); }}>治理</button><button type="button" onClick={() => void run(() => api.setGraphGovernanceState(item.id, item.governance_status === "LOCKED" ? "PENDING" : "LOCKED"), "图谱状态已更新")}>{item.governance_status === "LOCKED" ? "解锁" : "锁定"}</button>
          <button type="button" onClick={() => void run(() => api.updateKnowledgeGraph(item.id, { enabled: !item.enabled }), "使用状态已更新")}>{item.enabled ? "停用" : "启用"}</button>
          <button type="button" disabled={item.governance_status === "LOCKED"} onClick={() => { if (window.confirm(`删除图谱 ${item.graph_name} 及其实体关系？`)) void run(() => api.deleteKnowledgeGraph(item.id), "图谱已删除"); }}>删除</button></td></tr>)}</tbody></table></div></section>
    {dialog === "base" && <Dialog title={base ? "编辑知识库" : "新增知识库"} close={() => setDialog(null)}><form className="governance-form" onSubmit={saveBase}>{error && <p className="form-error">{error}</p>}
      <label>知识库编码<input required disabled={!!base} value={baseForm.kb_code} onChange={event => setBaseForm({ ...baseForm, kb_code: event.target.value.toUpperCase() })} /></label><label>名称<input required value={baseForm.kb_name} onChange={event => setBaseForm({ ...baseForm, kb_name: event.target.value })} /></label>
      <label>来源数据资产</label><div className="governance-source-grid">{assets.map(item => <label key={item.id}><input type="checkbox" checked={baseForm.source_tables.includes(item.table_name)} onChange={() => setBaseForm({ ...baseForm, source_tables: baseForm.source_tables.includes(item.table_name) ? baseForm.source_tables.filter(name => name !== item.table_name) : [...baseForm.source_tables, item.table_name] })} />{item.display_name}</label>)}</div>
      <label>版本<input value={baseForm.version} onChange={event => setBaseForm({ ...baseForm, version: event.target.value })} /></label><label>说明<textarea value={baseForm.description} onChange={event => setBaseForm({ ...baseForm, description: event.target.value })} /></label><label><input type="checkbox" checked={baseForm.enabled} onChange={event => setBaseForm({ ...baseForm, enabled: event.target.checked })} />启用</label><button className="primary-button" type="submit" disabled={busy}>保存</button></form></Dialog>}
    {dialog === "graph" && <Dialog title={graph ? "编辑知识图谱" : "新增知识图谱"} close={() => setDialog(null)}><form className="governance-form" onSubmit={saveGraph}>{error && <p className="form-error">{error}</p>}
      <label>所属知识库<select disabled={!!graph} value={graphForm.knowledge_base_id} onChange={event => setGraphForm({ ...graphForm, knowledge_base_id: Number(event.target.value), source_tables: [] })}>{knowledge.map(item => <option key={item.id} value={item.id}>{item.kb_name}</option>)}</select></label>
      <label>图谱编码<input required disabled={!!graph} value={graphForm.graph_code} onChange={event => setGraphForm({ ...graphForm, graph_code: event.target.value.toUpperCase() })} /></label><label>图谱名称<input required value={graphForm.graph_name} onChange={event => setGraphForm({ ...graphForm, graph_name: event.target.value })} /></label>
      <label>股票代码（可留空表示全部）<input value={graphForm.symbol || ""} onChange={event => setGraphForm({ ...graphForm, symbol: event.target.value.trim() })} /></label>
      <label>来源数据资产</label><div className="governance-source-grid">{assets.filter(item => knowledge.find(kb => kb.id === graphForm.knowledge_base_id)?.source_tables.includes(item.table_name)).map(item => <label key={item.id}><input type="checkbox" checked={graphForm.source_tables.includes(item.table_name)} onChange={() => setGraphForm({ ...graphForm, source_tables: graphForm.source_tables.includes(item.table_name) ? graphForm.source_tables.filter(name => name !== item.table_name) : [...graphForm.source_tables, item.table_name] })} />{item.display_name}</label>)}</div>
      <small>留空使用所属知识库的全部来源。</small><label>版本<input value={graphForm.version} onChange={event => setGraphForm({ ...graphForm, version: event.target.value })} /></label><label>说明<textarea value={graphForm.description || ""} onChange={event => setGraphForm({ ...graphForm, description: event.target.value })} /></label><label><input type="checkbox" checked={graphForm.enabled} onChange={event => setGraphForm({ ...graphForm, enabled: event.target.checked })} />启用</label><button className="primary-button" type="submit" disabled={busy}>保存</button></form></Dialog>}
    {dialog === "govern" && graph && <Dialog title={`治理知识图谱：${graph.graph_name}`} close={() => setDialog(null)}><form className="governance-form" onSubmit={govern}>{error && <p className="form-error">{error}</p>}<p>选择参与构建的数据资产；不勾选则使用图谱当前配置。</p><SourceChoices assets={graphSources} selected={sources} change={setSources} /><AgentChoice agents={agents} value={agentId} change={setAgentId} code="知识图谱智能体" /><button className="primary-button" type="submit" disabled={busy}>{busy ? "治理中…" : "开始治理"}</button></form></Dialog>}
    {dialog === "documents" && base && <Dialog title={`知识库资料：${base.kb_name}`} close={() => setDialog(null)}><div className="governance-detail"><form onSubmit={event => { event.preventDefault(); void api.searchKnowledgeBase(base.id, query).then(setDocuments).catch(error => setError(errText(error))); }}><input value={query} onChange={event => setQuery(event.target.value)} placeholder="股票代码、标题或正文" /><button type="submit">查询</button></form>{error && <p className="form-error">{error}</p>}<div className="table-wrap"><table><thead><tr><th>股票</th><th>来源</th><th>文档</th></tr></thead><tbody>{documents.map(item => <tr key={item.document_id}><td>{item.symbol || "--"}</td><td>{item.source_table}</td><td><strong>{item.title}</strong><p>{item.content.slice(0, 300)}</p></td></tr>)}</tbody></table></div></div></Dialog>}
    {dialog === "explore" && graph && <Dialog title={`图谱关系：${graph.graph_name}`} close={() => setDialog(null)}><div className="governance-detail"><form onSubmit={event => { event.preventDefault(); void api.exploreKnowledgeGraph(graph.id, query).then(setExplore).catch(error => setError(errText(error))); }}><input value={query} onChange={event => setQuery(event.target.value)} placeholder="股票代码、实体名称或键" /><button type="submit">查询</button></form>{error && <p className="form-error">{error}</p>}
      <h3>实体（{explore?.nodes.length || 0}）</h3><div className="table-wrap"><table><thead><tr><th>类型</th><th>名称</th><th>属性</th></tr></thead><tbody>{explore?.nodes.map(node => <tr key={node.id}><td>{node.type}</td><td>{node.name}</td><td>{asText(node.properties)}</td></tr>)}</tbody></table></div>
      <h3>关联关系（{explore?.relations.length || 0}）</h3><div className="table-wrap"><table><thead><tr><th>主体</th><th>关系</th><th>客体</th><th>证据</th></tr></thead><tbody>{explore?.relations.map(relation => <tr key={relation.id}><td>{explore.nodes.find(node => node.id === relation.from_id)?.name}</td><td>{relation.type}</td><td>{explore.nodes.find(node => node.id === relation.to_id)?.name}</td><td>{relation.evidence ? <details><summary>{relation.evidence.source_table} · {relation.evidence.title}</summary><p>{relation.evidence.excerpt}</p></details> : "--"}</td></tr>)}</tbody></table></div>
      <h3>最近治理结果</h3><pre>{JSON.stringify(graph.governance_report_json || {}, null, 2)}</pre>{runs.map(item => <p key={item.id}>#{item.id} · {item.status} · {new Date(item.created_at).toLocaleString("zh-CN")}</p>)}</div></Dialog>}
  </div>;
}
