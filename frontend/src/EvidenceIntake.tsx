import { useEffect, useState, type FormEvent } from "react";
import { api, request, type StockSymbol } from "./api";
import "./SelectionWatch.css";

const eventLabels: Record<string, string> = { POLICY: "国家 / 行业政策", RAW_MATERIAL: "原材料与成本", SUPPLY_CHAIN: "上下游 / 产业链", SHAREHOLDER: "回购 / 增持 / 减持", CONTRACT: "合同与订单", OTHER: "其他事件" };

export function EvidenceIntake() {
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [market, setMarket] = useState("CN_A");
  const [query, setQuery] = useState("");
  const [stock, setStock] = useState<StockSymbol | null>(null);
  const [matches, setMatches] = useState<StockSymbol[]>([]);
  const [form, setForm] = useState({ event_type: "POLICY", title: "", content: "", source_name: "", url: "", published_at: "", related_entity: "" });
  useEffect(() => {
    if (!open || !query.trim() || stock) { setMatches([]); return; }
    let valid = true;
    const timer = window.setTimeout(() => { void api.searchStocks(market, query, 8).then(rows => { if (valid) setMatches(rows); }).catch(() => { if (valid) setMatches([]); }); }, 250);
    return () => { valid = false; window.clearTimeout(timer); };
  }, [query, market, stock, open]);
  useEffect(() => {
    const key = (event: KeyboardEvent) => { if (event.key === "Escape" && !busy) setOpen(false); };
    if (open) window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, [open, busy]);
  async function save(event: FormEvent) {
    event.preventDefault();
    if (!stock) { setMessage("请先从搜索结果选择关联股票。"); return; }
    setBusy(true); setMessage("");
    try {
      await request("/resources/context-events", { method: "POST", body: JSON.stringify({ ...form, market: stock.market, symbol: stock.symbol, published_at: new Date(form.published_at).toISOString(), related_entity: form.related_entity || null }) });
      setOpen(false);
      setMessage("原文已保存到“股票外部证据”数据资产。请将其加入知识库 / 图谱来源并执行治理，核验关联关系。");
      setForm({ event_type: "POLICY", title: "", content: "", source_name: "", url: "", published_at: "", related_entity: "" });
      setStock(null); setQuery("");
    } catch (error) { setMessage(error instanceof Error ? error.message : "保存失败"); }
    finally { setBusy(false); }
  }
  return <div className="evidence-intake">
    <div><strong>补充周边与公司事件证据</strong><p>导入带来源的政策、产业链、原材料、股东变动和合同原文，供后续治理核验。</p></div>
    <button className="secondary-button" type="button" onClick={() => { setMessage(""); setOpen(true); }}>补充外部证据</button>
    {message && !open && <p className="selection-notice" role="status">{message}</p>}
    {open && <div className="selection-backdrop" onMouseDown={event => { if (event.target === event.currentTarget && !busy) setOpen(false); }}>
      <section className="selection-dialog" role="dialog" aria-modal="true" aria-labelledby="evidence-title">
        <header><div><p className="eyebrow">SOURCE EVIDENCE</p><h2 id="evidence-title">补充外部证据</h2></div><button type="button" disabled={busy} onClick={() => setOpen(false)}>关闭</button></header>
        <form onSubmit={save} className="selection-form">
          <div className="selection-form-grid">
            <label>市场<select value={market} onChange={event => { setMarket(event.target.value); setStock(null); }}><option value="CN_A">A 股</option><option value="HK">港股</option><option value="NEEQ">新三板</option><option value="NEEQ_INNOVATION">创新层</option></select></label>
            <label>关联股票<input required value={query} placeholder="搜索股票代码或名称" onChange={event => { setQuery(event.target.value); setStock(null); }} /></label>
          </div>
          {matches.length > 0 && <div className="selection-stock-matches">{matches.map(item => <button key={item.id} type="button" onClick={() => { setStock(item); setQuery(`${item.symbol} ${item.name}`); }}>{item.symbol} {item.name}</button>)}</div>}
          <div className="selection-form-grid">
            <label>证据类别<select value={form.event_type} onChange={event => setForm({ ...form, event_type: event.target.value })}>{Object.entries(eventLabels).map(([code, name]) => <option value={code} key={code}>{name}</option>)}</select></label>
            <label>原文发布时间<input required type="datetime-local" value={form.published_at} onChange={event => setForm({ ...form, published_at: event.target.value })} /></label>
            <label>来源机构<input required maxLength={256} value={form.source_name} onChange={event => setForm({ ...form, source_name: event.target.value })} placeholder="如公司公告、政策发布机构" /></label>
            <label>来源链接<input required type="url" maxLength={2048} value={form.url} onChange={event => setForm({ ...form, url: event.target.value })} placeholder="https://…" /></label>
          </div>
          <label>标题<input required maxLength={512} value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} /></label>
          <label>相关主体（选填）<input maxLength={256} value={form.related_entity} onChange={event => setForm({ ...form, related_entity: event.target.value })} placeholder="原材料、上下游企业或股东名称" /></label>
          <label>原文<textarea required minLength={10} maxLength={200000} rows={8} value={form.content} onChange={event => setForm({ ...form, content: event.target.value })} placeholder="粘贴来源原文，保留金额、日期、单位与上下文。" /></label>
          <p className="selection-help">保存原文后仍需执行治理；仅有主体提及不会直接认定为因果关系。</p>
          {message && <p className="form-error" role="alert">{message}</p>}
          <footer><button type="button" disabled={busy} onClick={() => setOpen(false)}>取消</button><button className="primary-button" disabled={busy || !stock}>{busy ? "保存中…" : "保存证据"}</button></footer>
        </form>
      </section>
    </div>}
  </div>;
}
