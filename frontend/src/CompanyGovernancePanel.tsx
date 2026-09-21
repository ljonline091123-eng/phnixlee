import { useEffect, useRef, useState, type FormEvent } from "react";
import { Play, RefreshCw } from "lucide-react";
import { companyGraphApi, type CompanyGovernanceJob, type CompanyGovernanceStatus, type CompanySecurity } from "./companyGraphApi";

const names: Record<string, string> = { QUEUED: "已入队，等待后台执行", PENDING: "等待后台执行", RUNNING: "执行中", RETRY: "等待重试", RETRYING: "重试中", SUCCESS: "成功", SUCCEEDED: "成功", COMPLETED: "完成", FAILED: "失败", CANCELLED: "已取消", DEAD: "重试耗尽", DEAD_LETTER: "重试耗尽", PARTIAL: "部分获取", EMPTY: "未返回记录", UNAVAILABLE: "暂不可用", AUTH_REQUIRED: "需要授权", CONFIGURED_UNVERIFIED: "已配置，尚未验证", PUBLIC: "公开披露", PUBLIC_DISCLOSURE: "公开披露", UNKNOWN: "尚未核实", UNSUPPORTED: "暂不支持", SCHEMA_CHANGED: "来源字段变更", NOT_REQUESTED: "未请求采集", NOT_COLLECTED: "尚未采集", DISABLED: "已停用", INVALID: "数据未通过校验", DISCLOSURES_ONLY: "仅有披露线索", EVIDENCE_ONLY: "仅有披露线索", TEXT_AVAILABLE: "已提取正文", TITLE_ONLY: "仅有公告信息", OCR_REQUIRED: "需文字识别", STOCK_MISSING: "股票尚未入库", PROFILE_UNAVAILABLE: "公司资料暂不可用" };
const sourceNames: Record<string, string> = { profile: "公司资料", holders: "股东披露", legal: "司法披露", business: "经营披露", supply_chain: "供应链披露", marketcap: "市值快照", COMPANY_REGISTRY_AUTHORIZED: "企业登记授权数据", COURT_DATA_AUTHORIZED: "司法授权数据" };
const marketNames: Record<string, string> = { CN_A: "中国A股", HK: "香港", NEEQ: "新三板", NEEQ_INNOVATION: "新三板创新层" };
const sourceStatusNames: Record<string, string> = { PARTIAL: "部分获取", SUCCESS: "本次请求来源均已获取", COMPLETE: "本次请求来源均已获取", COMPLETED: "本次请求来源均已获取", COMPLETE_WITH_COVERAGE_LIMITS: "采集完成（来源覆盖有限）", FAILED: "获取失败", EMPTY: "无返回记录", NOT_COLLECTED: "尚未采集", AUTH_REQUIRED: "需要授权" };
const sourceStatus = (job: CompanyGovernanceJob) => job.result?.source_status ? sourceStatusNames[job.result.source_status] || job.result.source_status : "尚未统计";
const terminal = new Set(["COMPLETED", "SUCCESS", "SUCCEEDED", "FAILED", "CANCELLED", "DEAD", "DEAD_LETTER"]);
const errorText = (value: unknown) => value instanceof Error ? value.message : "任务请求失败";
const formatTime = (value: string | undefined) => value ? new Date(value).toLocaleString("zh-CN") : "未提供";

export function CompanyGovernancePanel({ changed }: { changed: () => void }) {
  const [open, setOpen] = useState(false), [status, setStatus] = useState<CompanyGovernanceStatus | null>(null), [stocks, setStocks] = useState<CompanySecurity[]>([]);
  const [selected, setSelected] = useState<CompanySecurity[]>([]), [job, setJob] = useState<CompanyGovernanceJob | null>(null), [error, setError] = useState("");
  const [busy, setBusy] = useState(false), [loading, setLoading] = useState(false), [revision, setRevision] = useState(0);
  const [query, setQuery] = useState(""), [searchQuery, setSearchQuery] = useState(""), [market, setMarket] = useState(""), [sampleMode, setSampleMode] = useState(false);
  const [offset, setOffset] = useState(0), [total, setTotal] = useState(0), [stocksLoading, setStocksLoading] = useState(false), [stocksError, setStocksError] = useState("");
  const [holders, setHolders] = useState(true), [legal, setLegal] = useState(true), [business, setBusiness] = useState(true), [supplyChain, setSupplyChain] = useState(true), [accept, setAccept] = useState(false);
  const changedRef = useRef(changed), completedJob = useRef<number | null>(null); changedRef.current = changed;
  const requestKey = useRef<string | null>(null);
  useEffect(() => {
    if (!open) return;
    let active = true; setLoading(true); setError("");
    void companyGraphApi.governanceStatus().then(value => { if (active) setStatus(value); }).catch(err => { if (active) setError(errorText(err)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [open, revision]);
  useEffect(() => {
    if (!open) return;
    const controller = new AbortController(); setStocksLoading(true); setStocksError(""); setStocks([]);
    const options = { q: searchQuery, market: market || undefined, offset, limit: 20 };
    const call = sampleMode ? companyGraphApi.samples(options, controller.signal) : companyGraphApi.searchSecurities(options, controller.signal);
    void call.then(value => { if (!controller.signal.aborted) { setStocks(value.items); setTotal(value.total); } })
      .catch(err => { if (!controller.signal.aborted) { setStocksError(errorText(err)); setTotal(0); } })
      .finally(() => { if (!controller.signal.aborted) setStocksLoading(false); });
    return () => controller.abort();
  }, [open, searchQuery, market, offset, sampleMode, revision]);
  useEffect(() => {
    if (!job || terminal.has(job.status)) return;
    let active = true, polling = false;
    const poll = async () => {
      if (polling) return; polling = true;
      try {
        const value = await companyGraphApi.governanceJob(job.id);
        if (!active) return;
        setJob(value); setError("");
        if (terminal.has(value.status) && completedJob.current !== value.id) { completedJob.current = value.id; setRevision(current => current + 1); changedRef.current(); }
      } catch (err) { if (active) setError(errorText(err)); } finally { polling = false; }
    };
    const timer = window.setInterval(() => void poll(), 5000);
    return () => { active = false; window.clearInterval(timer); };
  }, [job?.id, job?.status]);
  function editSelection(items: CompanySecurity[]) {
    if (items.length > Math.min(status?.max_stocks_per_job || 30, 30)) { setError("每次最多选择 30 只证券，请先移除部分已选项。"); return; }
    setSelected(items); requestKey.current = null;
  }
  function searchStocks() { setSearchQuery(query.trim()); setOffset(0); setRevision(value => value + 1); }
  async function submit(event: FormEvent) {
    event.preventDefault(); setError("");
    if (!selected.length) { setError("请选择至少一只证券。"); return; }
    if (selected.length > (status?.max_stocks_per_job || 30)) { setError("所选证券超过单次任务上限。"); return; }
    if (!requestKey.current) requestKey.current = window.crypto.randomUUID();
    setBusy(true);
    try { const result = await companyGraphApi.submitGovernance({ stock_symbol_ids: selected.map(item => item.stock_symbol_id!).filter(id => id != null), include_holders: holders, include_legal: legal, include_business: business, include_supply_chain: supplyChain, accept_structured: accept, request_key: requestKey.current }); setJob(result); requestKey.current = null; }
    catch (err) { setError(errorText(err)); } finally { setBusy(false); }
  }
  const shownRun = job || status?.latest_run;
  const selectedIds = new Set(selected.map(item => item.stock_symbol_id));
  const canCollect = (item: CompanySecurity) => item.stock_symbol_id != null && ["CN_A", "HK"].includes(item.market) && item.mapping_status !== "NOT_APPLICABLE";
  const available = stocks.filter(canCollect), allCurrentSelected = available.length > 0 && available.every(item => selectedIds.has(item.stock_symbol_id));
  const selectionLimit = Math.min(status?.max_stocks_per_job || 30, 30);
  const waiting = job != null && !terminal.has(job.status);
  return <details className="cg-governance" onToggle={event => setOpen(event.currentTarget.open)}><summary>来源与治理任务{shownRun && <span>执行：{names[shownRun.status] || shownRun.status} · 来源：{sourceStatus(shownRun)}</span>}</summary>
    <div className="cg-governance-body"><div className="cg-governance-heading"><h3>数据源状态</h3><button type="button" className="cg-icon" title="刷新来源与任务" aria-label="刷新来源与任务" disabled={loading} onClick={() => setRevision(value => value + 1)}><RefreshCw size={16} /></button></div>
      {error && <p className="cg-error" role="alert">{error}</p>}{loading && <p className="cg-meta">正在读取来源与任务...</p>}
      {status && <><div className="cg-table-scroll"><table><thead><tr><th>数据源</th><th>启用状态</th><th>授权或接入状态</th></tr></thead><tbody>{status.sources.map(item => <tr key={item.code}><td>{item.name}</td><td>{item.enabled ? "启用" : "停用"}</td><td>{names[item.authorization_status] || item.authorization_status}</td></tr>)}{status.restricted_sources.map(item => <tr key={item.source_code}><td>{sourceNames[item.source_code] || item.name}</td><td>{item.connected ? "已连接" : "未连接"}</td><td>{names[item.status] || item.status}</td></tr>)}</tbody></table></div>
        <form className="cg-governance-form" onSubmit={submit}><fieldset disabled={busy || waiting}><div className="cg-governance-heading"><h3>采集范围 <span>{selected.length} / {selectionLimit}</span></h3><label><input type="checkbox" checked={allCurrentSelected} disabled={!available.length || (!allCurrentSelected && selected.length + available.filter(item => !selectedIds.has(item.stock_symbol_id)).length > selectionLimit)} onChange={event => editSelection(event.target.checked ? [...selected, ...available.filter(item => !selectedIds.has(item.stock_symbol_id))] : selected.filter(item => !available.some(row => row.stock_symbol_id === item.stock_symbol_id)))} />选择本页可采集股票</label></div>
          <div className="cg-governance-search"><div className="cg-modes"><button type="button" className={!sampleMode ? "active" : ""} onClick={() => { setSampleMode(false); setOffset(0); }}>全部股票</button><button type="button" className={sampleMode ? "active" : ""} onClick={() => { setSampleMode(true); setOffset(0); }}>示例股票</button></div><input value={query} aria-label="搜索治理股票" placeholder="股票代码或名称" onChange={event => setQuery(event.target.value)} onKeyDown={event => { if (event.key === "Enter") { event.preventDefault(); searchStocks(); } }} /><select aria-label="治理股票市场" value={market} onChange={event => { setMarket(event.target.value); setOffset(0); }}><option value="">全部市场</option>{Object.entries(marketNames).map(([key, name]) => <option key={key} value={key}>{name}</option>)}</select><button type="button" onClick={searchStocks}>搜索股票</button></div>
          {selected.length > 0 && <div className="cg-governance-selected" aria-label="已选治理股票">{selected.map(item => <button type="button" key={item.stock_symbol_id} onClick={() => editSelection(selected.filter(row => row.stock_symbol_id !== item.stock_symbol_id))} aria-label={`移除${item.name} ${item.symbol}`}>{item.name} · {item.symbol} ×</button>)}<button type="button" onClick={() => editSelection([])}>清空已选</button></div>}
          {stocksError && <p className="cg-error" role="alert">{stocksError}<button type="button" onClick={() => setRevision(value => value + 1)}>重试股票列表</button></p>}
          <div className="cg-governance-samples" aria-busy={stocksLoading}>{stocksLoading ? <p className="cg-meta" role="status">正在搜索股票...</p> : stocks.map(item => <label key={item.market + ":" + item.symbol}><input type="checkbox" disabled={!canCollect(item) || (!selectedIds.has(item.stock_symbol_id) && selected.length >= selectionLimit)} checked={selectedIds.has(item.stock_symbol_id)} onChange={event => editSelection(event.target.checked ? [...selected, item] : selected.filter(row => row.stock_symbol_id !== item.stock_symbol_id))} /><span><strong>{item.name}</strong><small>{marketNames[item.market] || item.market} · {item.symbol}{item.stock_symbol_id == null ? " · 未入库" : !canCollect(item) ? " · 暂不支持此类采集" : ""}</small></span></label>)}{!stocksLoading && !stocksError && !stocks.length && <p className="cg-meta">未找到符合条件的股票。</p>}</div>
          <div className="cg-list-pages"><span>共 {total} 条 · 已选项在翻页和搜索后保留</span><button type="button" disabled={stocksLoading || offset === 0} onClick={() => setOffset(value => Math.max(0, value - 20))}>上一页</button><button type="button" disabled={stocksLoading || offset + 20 >= total} onClick={() => setOffset(value => value + 20)}>下一页</button></div>
          <div className="cg-governance-options"><label><input type="checkbox" checked={holders} onChange={event => { setHolders(event.target.checked); requestKey.current = null; }} />股东披露</label><label><input type="checkbox" checked={supplyChain} onChange={event => { setSupplyChain(event.target.checked); requestKey.current = null; }} />供应链披露</label><label><input type="checkbox" checked={business} onChange={event => { setBusiness(event.target.checked); requestKey.current = null; }} />经营披露</label><label><input type="checkbox" checked={legal} onChange={event => { setLegal(event.target.checked); requestKey.current = null; }} />司法披露</label><label><input type="checkbox" checked={accept} onChange={event => { setAccept(event.target.checked); requestKey.current = null; }} />采纳结构化来源观察</label></div>
          <p className="cg-meta">供应链、经营和司法披露目前采集中国A股公告与原文，港股会标为暂不支持。采集结果提供待核对线索，不自动发布为已确认关系。</p>
          <div className="cg-governance-submit"><span>{accept ? "结构化来源观察：采纳；模型及司法线索：待审" : "新关系与分类：待审"}</span><button type="submit" disabled={!selected.length}><Play size={15} />{busy ? "提交中..." : waiting ? "任务处理中" : "提交治理任务"}</button></div>
        </fieldset></form>
      </>}
      {shownRun && <section className="cg-governance-run"><h3>{job ? "任务" : "最近治理运行"} #{shownRun.id}</h3><p className="cg-meta">执行状态：{names[shownRun.status] || shownRun.status} · 来源完整状态：{sourceStatus(shownRun)}</p><p className="cg-meta">{formatTime(shownRun.created_at)} · 请求 {shownRun.result?.requested ?? "待统计"} · 已映射 {shownRun.result?.mapped ?? "待统计"}</p>{shownRun.error_message && <p className="cg-error">{shownRun.error_message}</p>}
        {!!shownRun.result?.items?.length && <div className="cg-table-scroll"><table><thead><tr><th>证券</th><th>来源</th><th>结果</th><th>缺口或异常</th></tr></thead><tbody>{shownRun.result.items.flatMap(item => Object.entries(item.sources || {}).map(([key, source]) => <tr key={item.market + item.symbol + key}><td>{marketNames[item.market] || item.market} · {item.symbol}</td><td>{sourceNames[key] || key}</td><td>{names[source.status || ""] || source.status || "未提供"}{source.disclosure_count !== undefined && <small>披露 {source.disclosure_count} 份</small>}</td><td>{source.warnings?.join("；") || "未报告"}</td></tr>))}</tbody></table></div>}
      </section>}
    </div>
  </details>;
}
