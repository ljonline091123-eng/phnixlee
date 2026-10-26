import { useEffect, useMemo, useState } from "react";
import {
  selectionApi,
  type SelectionEvaluationSummary,
  type SelectionProfileItem,
  type SelectionScoringProfile,
} from "./selectionApi";

const statusName: Record<string, string> = { DRAFT: "草稿", ENABLED: "已发布", RETIRED: "已停用" };
const methodName: Record<string, string> = { RULE: "规则计算", LLM: "模型语义", HYBRID: "规则与模型融合" };
const pct = (value: number | null | undefined) => value == null ? "--" : `${(value * 100).toFixed(1)}%`;
const nextVersion = (version: string) => {
  const match = /^(\d+)\.(\d+)\.(\d+)$/.exec(version.trim());
  return match ? `${match[1]}.${Number(match[2]) + 1}.0` : `${version.trim() || "1.0.0"}-next`;
};

export function SelectionScoringAdmin() {
  const [profiles, setProfiles] = useState<SelectionScoringProfile[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [draftItems, setDraftItems] = useState<SelectionProfileItem[]>([]);
  const [evaluation, setEvaluation] = useState<SelectionEvaluationSummary | null>(null);
  const [busy, setBusy] = useState(false);
  const [cloneOpen, setCloneOpen] = useState(false);
  const [cloneVersion, setCloneVersion] = useState("1.1.0");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const selected = profiles.find(item => item.id === selectedId) || profiles[0] || null;
  const weightTotal = useMemo(() => draftItems.reduce((sum, item) => sum + Number(item.weight || 0), 0), [draftItems]);

  async function load(preferredId?: number) {
    const rows = await selectionApi.scoringProfiles();
    setProfiles(rows);
    const nextId = preferredId && rows.some(item => item.id === preferredId)
      ? preferredId : selectedId && rows.some(item => item.id === selectedId)
        ? selectedId : rows.find(item => item.enabled)?.id || rows[0]?.id || null;
    setSelectedId(nextId);
    const profile = rows.find(item => item.id === nextId);
    setDraftItems(profile?.items.map(item => ({...item})) || []);
    setEvaluation(await selectionApi.scoringEvaluations(nextId || undefined));
  }

  useEffect(() => {
    setBusy(true);
    void load().catch(err => setError(err instanceof Error ? err.message : "评价体系读取失败"))
      .finally(() => setBusy(false));
  }, []);

  async function act(action: () => Promise<SelectionScoringProfile>, success: string): Promise<boolean> {
    setBusy(true); setError(""); setMessage("");
    try { const row = await action(); await load(row.id); setMessage(success); return true; }
    catch (err) { setError(err instanceof Error ? err.message : "操作失败"); return false; }
    finally { setBusy(false); }
  }

  async function refresh() {
    setBusy(true); setError("");
    try { await load(); }
    catch (err) { setError(err instanceof Error ? err.message : "评价体系读取失败"); }
    finally { setBusy(false); }
  }

  function choose(profile: SelectionScoringProfile) {
    setSelectedId(profile.id);
    setDraftItems(profile.items.map(item => ({...item})));
    setError(""); setMessage("");
    void selectionApi.scoringEvaluations(profile.id).then(setEvaluation).catch(() => setEvaluation(null));
  }

  async function saveDraft() {
    if (!selected || selected.status !== "DRAFT") return;
    if (Math.abs(weightTotal - 100) > 0.001) { setError("权重合计必须为100%后才能保存。"); return; }
    await act(() => selectionApi.updateScoringProfile(selected.id, {items: draftItems.map(item => ({
      dimension_code: item.dimension_code, weight: Number(item.weight),
      minimum_coverage: Number(item.minimum_coverage), minimum_confidence: Number(item.minimum_confidence),
      skill_code: item.skill_code, skill_version: item.skill_version,
    }))}), "草稿权重和门槛已保存。历史批次不受影响。可在确认后发布。" );
  }

  function openClone() {
    if (!selected) return;
    setCloneVersion(nextVersion(selected.version));
    setCloneOpen(true);
  }

  async function clone() {
    const version = cloneVersion.trim();
    if (!selected || !version) { setError("请输入新版本号。"); return; }
    const completed = await act(() => selectionApi.cloneScoringProfile(selected.id, version), "已复制为新草稿，请调整并验证后发布。");
    if (completed) setCloneOpen(false);
  }

  return <section className="scoring-admin" aria-busy={busy}>
    <div className="scoring-intro">
      <div><h2>选股评价体系</h2><p>规则负责可复现计算，模型负责语义解释，知识库和图谱提供证据。雷达图展示原始维度分，权重只影响综合分。</p></div>
      <button type="button" onClick={() => void refresh()} disabled={busy}>刷新</button>
    </div>
    {error && <p className="form-error" role="alert">{error}</p>}
    {message && <p className="form-success" role="status">{message}</p>}
    <div className="scoring-layout">
      <aside className="scoring-profiles" aria-label="评价方案版本">
        <h3>方案版本</h3>
        {profiles.map(profile => <button type="button" key={profile.id} className={profile.id === selected?.id ? "active" : ""} onClick={() => choose(profile)}>
          <span><strong>{profile.profile_name}</strong><small>{profile.profile_code} · v{profile.version}</small></span>
          <em className={`scoring-status ${profile.status.toLowerCase()}`}>{statusName[profile.status] || profile.status}</em>
        </button>)}
      </aside>
      <div className="scoring-main">
        {selected ? <>
          <header className="scoring-profile-header">
            <div><h3>{selected.profile_name} <small>v{selected.version}</small></h3><p>{selected.description}</p><small>适用市场：{selected.market_scope.join("、")} · 评价周期：{selected.horizon_code}</small></div>
            <div className="scoring-actions">
              <button type="button" onClick={openClone} disabled={busy}>复制新版本</button>
              {selected.status === "DRAFT" && <><button type="button" onClick={() => void saveDraft()} disabled={busy || Math.abs(weightTotal - 100) > .001}>保存草稿</button><button type="button" className="primary" onClick={() => void act(() => selectionApi.publishScoringProfile(selected.id), "新版本已发布，后续选股任务将使用该版本。") } disabled={busy || Math.abs(weightTotal - 100) > .001}>发布</button></>}
              {selected.status === "RETIRED" && <button type="button" onClick={() => void act(() => selectionApi.rollbackScoringProfile(selected.id), "已回滚到该历史版本；历史任务仍保留原快照。") } disabled={busy}>回滚到此版本</button>}
              {selected.status === "ENABLED" && <button type="button" title={profiles.filter(item => item.enabled).length <= 1 ? "请先发布另一个版本，再停用当前唯一生产方案" : "停用该方案"} onClick={() => void act(() => selectionApi.retireScoringProfile(selected.id), "方案已停用。") } disabled={busy || profiles.filter(item => item.enabled).length <= 1}>停用</button>}
            </div>
          </header>
          <div className="scoring-kpis"><div><span>权重合计</span><strong className={Math.abs(weightTotal - 100) > .001 ? "invalid" : ""}>{weightTotal.toFixed(1)}%</strong></div><div><span>维度数量</span><strong>{draftItems.length}</strong></div><div><span>发布状态</span><strong>{statusName[selected.status] || selected.status}</strong></div><div><span>候选样本</span><strong>{Math.max(0, ...(evaluation?.horizons.map(row => row.total) || []))}</strong></div></div>
          <div className="scoring-table-wrap"><table><thead><tr><th>评价维度</th><th>计算方式</th><th>权重</th><th>最低覆盖</th><th>最低置信</th><th>执行 Skill</th></tr></thead><tbody>{draftItems.map((item, index) => <tr key={item.dimension_code}><td><strong>{item.dimension_name}</strong><small>{item.description}</small><small>{item.score_interpretation}</small></td><td>{methodName[item.method] || item.method}</td><td><input aria-label={`${item.dimension_name}权重`} type="number" min="0" max="100" step="0.5" disabled={selected.status !== "DRAFT"} value={item.weight} onChange={event => setDraftItems(rows => rows.map((row, rowIndex) => rowIndex === index ? {...row, weight: Number(event.target.value)} : row))}/>%</td><td><input aria-label={`${item.dimension_name}最低覆盖`} type="number" min="0" max="1" step="0.05" disabled={selected.status !== "DRAFT"} value={item.minimum_coverage} onChange={event => setDraftItems(rows => rows.map((row, rowIndex) => rowIndex === index ? {...row, minimum_coverage: Number(event.target.value)} : row))}/></td><td><input aria-label={`${item.dimension_name}最低置信`} type="number" min="0" max="1" step="0.05" disabled={selected.status !== "DRAFT"} value={item.minimum_confidence} onChange={event => setDraftItems(rows => rows.map((row, rowIndex) => rowIndex === index ? {...row, minimum_confidence: Number(event.target.value)} : row))}/></td><td><code>{item.skill_code}</code><small>v{item.skill_version}</small></td></tr>)}</tbody></table></div>
          <section className="scoring-evaluation"><div><h3>T+3 / T+5 / T+7 样本外验证</h3><p>仅统计已有真实交易日；缺失样本不会被算作命中。评价结果只辅助人工调整下一版权重。</p></div><div className="scoring-evaluation-grid">{evaluation?.horizons.map(row => <article key={row.horizon_sessions}><strong>T+{row.horizon_sessions}</strong><span>完成 {row.completed} / {row.total}</span><span>方向命中 {pct(row.direction_hit_rate)}</span><span>平均收益 {row.average_return_pct == null ? "--" : `${row.average_return_pct.toFixed(2)}%`}</span><span>平均回撤 {row.average_max_drawdown_pct == null ? "--" : `${row.average_max_drawdown_pct.toFixed(2)}%`}</span></article>)}</div></section>
        </> : <p>暂无评价方案。</p>}
      </div>
    </div>
    {cloneOpen && selected && <div className="scoring-modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setCloneOpen(false); }}>
      <section className="scoring-modal" role="dialog" aria-modal="true" aria-labelledby="scoring-clone-title">
        <header><div><h3 id="scoring-clone-title">复制评价方案</h3><p>{selected.profile_name} v{selected.version} 将完整复制为可编辑草稿，历史批次不受影响。</p></div></header>
        <label htmlFor="scoring-clone-version">新版本号</label>
        <input id="scoring-clone-version" autoFocus value={cloneVersion} onChange={event => setCloneVersion(event.target.value)} placeholder="例如 1.1.0" maxLength={32} />
        <footer><button type="button" onClick={() => setCloneOpen(false)} disabled={busy}>取消</button><button type="button" className="primary" onClick={() => void clone()} disabled={busy || !cloneVersion.trim()}>创建草稿</button></footer>
      </section>
    </div>}
  </section>;
}
