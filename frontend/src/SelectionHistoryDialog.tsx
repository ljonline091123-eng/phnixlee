import { ChevronLeft, ChevronRight, History, Search, X } from "lucide-react";
import { useEffect, useState } from "react";

import { SelectionAnalysis } from "./SelectionAnalysis";
import {
  selectionApi,
  type SelectionCandidate,
  type SelectionHistoryItem,
  type SelectionHistoryPage,
  type SelectionHistoryVersion,
  type SelectionRecord,
} from "./selectionApi";

const marketNames: Record<string, string> = {
  CN_A: "A股", HK: "港股", NEEQ: "新三板基础层", NEEQ_INNOVATION: "新三板创新层",
};
const statusNames: Record<string, string> = {
  READY_REVIEW: "有效结论", CONCLUSION_FAILED: "结论校验失败", SCORE_FAILED: "评分失败",
  MODEL_FAILED: "模型失败", DATA_INSUFFICIENT: "数据不足", NOT_ANALYZED: "未分析",
  WATCH: "建议关注", PASS: "建议等待", APPROVED: "已确认", REJECTED: "已排除", PENDING: "待复核",
  COMPLETED: "已完成", PARTIAL: "部分完成", FAILED: "失败", RUNNING: "进行中", QUEUED: "排队中",
};
const text = (value: unknown) => typeof value === "string" || typeof value === "number" ? String(value) : "";
const errorText = (value: unknown) => value instanceof Error ? value.message : "历史分析读取失败";
const dateTime = (value: string | null | undefined) => value
  ? new Date(/(?:Z|[+-]\d{2}:?\d{2})$/.test(value) ? value : `${value}Z`).toLocaleString("zh-CN", {timeZone: "Asia/Shanghai"})
  : "—";
const score = (value: number | null | undefined) => value == null ? "—" : value.toFixed(1);

export function SelectionHistoryDialog({
  onClose,
  onOpenStock,
}: {
  onClose: () => void;
  onOpenStock?: (stock: {market: string; symbol: string; name: string}) => void;
}) {
  const [query, setQuery] = useState("");
  const [market, setMarket] = useState("");
  const [page, setPage] = useState(1);
  const [result, setResult] = useState<SelectionHistoryPage | null>(null);
  const [selected, setSelected] = useState<SelectionHistoryItem | null>(null);
  const [versions, setVersions] = useState<SelectionHistoryVersion[]>([]);
  const [candidate, setCandidate] = useState<SelectionCandidate | null>(null);
  const [document, setDocument] = useState<SelectionRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [versionLoading, setVersionLoading] = useState(false);
  const [candidateLoading, setCandidateLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true); setError("");
      void selectionApi.history({query, market, page, page_size: 20})
        .then(value => { if (active) setResult(value); })
        .catch(reason => { if (active) setError(errorText(reason)); })
        .finally(() => { if (active) setLoading(false); });
    }, query ? 260 : 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query, market, page]);

  useEffect(() => { setPage(1); }, [query, market]);

  async function openStock(item: SelectionHistoryItem) {
    setSelected(item); setVersions([]); setCandidate(null); setDocument(null); setVersionLoading(true); setError("");
    try {
      const history = await selectionApi.historyVersions(item.market, item.symbol);
      setVersions(history);
      if (history[0]) await openVersion(history[0]);
    } catch (reason) { setError(errorText(reason)); }
    finally { setVersionLoading(false); }
  }

  async function openVersion(version: SelectionHistoryVersion) {
    setCandidateLoading(true); setDocument(null); setError("");
    try { setCandidate(await selectionApi.candidate(version.candidate_id)); }
    catch (reason) { setError(errorText(reason)); }
    finally { setCandidateLoading(false); }
  }

  async function openDocument(id: number) {
    if (!candidate) return;
    setDocument({id, loading: true});
    try { setDocument(await selectionApi.candidateDocument(candidate.id, id)); }
    catch (reason) { setDocument({id, error: errorText(reason)}); }
  }

  const pages = Math.max(1, Math.ceil((result?.total || 0) / (result?.page_size || 20)));
  return <div className="selection-history-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="selection-history-dialog" role="dialog" aria-modal="true" aria-label="历史分析中心">
      <header className="selection-history-header">
        <div><span><History size={17} /> 历史分析中心</span><h2>按股票查找全部分析记录</h2><p>新分析不会覆盖旧结果；可逐次核对主营逻辑、证据范围、模型和结论变化。</p></div>
        <button type="button" className="selection-history-close" title="关闭" aria-label="关闭历史分析" onClick={onClose}><X size={20} /></button>
      </header>
      <div className="selection-history-filters">
        <label><Search size={16} /><input aria-label="搜索历史分析股票" value={query} onChange={event => setQuery(event.target.value)} placeholder="输入股票名称或代码" /></label>
        <select aria-label="筛选市场" value={market} onChange={event => setMarket(event.target.value)}><option value="">全部市场</option>{Object.entries(marketNames).map(([code, name]) => <option key={code} value={code}>{name}</option>)}</select>
        <span>{loading ? "正在查询…" : `共 ${result?.total || 0} 只股票`}</span>
      </div>
      {error && <p className="batch-alert error" role="alert">{error}</p>}
      <div className="selection-history-layout">
        <aside className="selection-history-stocks" aria-label="历史股票列表">
          {(result?.items || []).map(item => <button type="button" key={`${item.market}:${item.symbol}`} className={selected?.market === item.market && selected.symbol === item.symbol ? "active" : ""} onClick={() => void openStock(item)}>
            <span><strong>{item.name || item.symbol}</strong><small>{item.symbol} · {marketNames[item.market] || item.market}</small></span>
            <span><b>{statusNames[item.recommendation || ""] || statusNames[item.analysis_status] || item.analysis_status}</b><small>{item.analysis_count} 次分析 · {dateTime(item.analyzed_at)}</small></span>
          </button>)}
          {!loading && !(result?.items.length) && <p>没有符合条件的历史分析。</p>}
          <nav className="selection-history-pages" aria-label="历史分析分页"><button type="button" title="上一页" aria-label="上一页" disabled={page <= 1} onClick={() => setPage(value => value - 1)}><ChevronLeft size={17} /></button><span>{page} / {pages}</span><button type="button" title="下一页" aria-label="下一页" disabled={page >= pages} onClick={() => setPage(value => value + 1)}><ChevronRight size={17} /></button></nav>
        </aside>
        <aside className="selection-history-versions" aria-label="股票分析版本">
          <div><strong>{selected ? `${selected.name || selected.symbol}的分析历程` : "分析历程"}</strong>{selected && onOpenStock && <button type="button" onClick={() => onOpenStock({market: selected.market, symbol: selected.symbol, name: selected.name || selected.symbol})}>查看个股资料</button>}</div>
          {versionLoading && <p>正在读取历次分析…</p>}
          {versions.map((version, index) => <button type="button" key={version.candidate_id} className={candidate?.id === version.candidate_id ? "active" : ""} onClick={() => void openVersion(version)}>
            <span>第 {versions.length - index} 次 · {dateTime(version.analyzed_at)}</span><strong>{statusNames[version.recommendation || ""] || statusNames[version.analysis_status] || version.analysis_status}</strong><small>批次 #{version.run_id} · {version.model_instance_code || "未记录模型"}</small><small>可信分 {score(version.trusted_score)} · 数据截止 {version.data_cutoff || "—"}</small>
          </button>)}
          {selected && !versionLoading && !versions.length && <p>该股票暂无可读取的分析版本。</p>}
        </aside>
        <main className="selection-history-result">
          {!selected && <div className="selection-history-empty"><History size={30} /><strong>选择一只股票查看历次分析</strong><p>默认按最近分析时间排序。</p></div>}
          {candidateLoading && <div className="selection-history-empty"><strong>正在读取分析与证据…</strong></div>}
          {candidate && !candidateLoading && <>
            <div className="selection-history-result-head"><div><strong>{candidate.name || candidate.symbol}</strong><span>{candidate.symbol} · 批次 #{candidate.run_id} · {dateTime(candidate.created_at)}</span></div><span className={`selection-history-state ${candidate.analysis_json.publication?.conclusion_available ? "success" : "warning"}`}>{statusNames[candidate.analysis_json.publication?.status || ""] || candidate.analysis_json.publication?.status || "历史记录"}</span></div>
            <SelectionAnalysis candidate={candidate} backfillDisabled onDocument={id => void openDocument(id)} />
          </>}
          {document && <aside className="selection-history-document"><div><strong>{text(document.title) || `原文 #${document.id}`}</strong><button type="button" title="关闭原文" aria-label="关闭原文" onClick={() => setDocument(null)}><X size={16} /></button></div><p>{document.loading ? "正在读取原文…" : text(document.error) || text(document.content) || "该历史快照没有可展示的正文。"}</p></aside>}
        </main>
      </div>
    </section>
  </div>;
}
