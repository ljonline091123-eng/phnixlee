import type { AnalystTrackingPage } from "./stockAnalystApi";

const names: Record<string, string> = { PENDING: "持续验证中", MISSING: "待补充行情", PASSED: "目标已验证", FAILED: "到期未通过", INVALIDATED: "预测已失效" };
const date = (s: string) => new Date(/Z$|[+-]\d\d:\d\d$/.test(s) ? s : s + "Z").toLocaleString("zh-CN", { hour12: false });
export function AnalystTracking({ page, offset, onPage, onTimeline, error }: {
  page: AnalystTrackingPage | null; offset: number; onPage: (offset: number) => void; onTimeline: () => void; error?: string;
}) {
  const stats = page?.stats || {};
  const final = (stats.PASSED || 0) + (stats.FAILED || 0) + (stats.INVALIDATED || 0);
  return <div className="analyst-tracking">
    <header><h3>跟踪复盘 · 从判断到验证</h3><p>重点观察命中 → AI研判趋势与目标价 → 每日行情验证 → 保留有效与失败逻辑。</p></header>
    <div className="analyst-tracking-stats">{[["跟踪预测", page?.total || 0], ["验证通过", stats.PASSED || 0], ["未通过 / 失效", (stats.FAILED || 0) + (stats.INVALIDATED || 0)], ["待验证", stats.PENDING || 0]].map(([title, value]) => <div key={title}><strong>{value}</strong><span>{title}</span></div>)}</div>
    <p className="analyst-muted">{final ? `已终结${final}项，目标价路径通过${stats.PASSED || 0}项。` : "尚无完成验证的预测样本。"} 目标价命中仅验证价格路径，经验保留为候选，不能证明因果或代表未来胜率。</p>
    <button type="button" className="analyst-secondary" onClick={onTimeline}>前往时间轴 · 检查最新数据</button>
    {error && <p className="analyst-error">{error}</p>}
    {!page ? <p role="status">正在读取跟踪复盘…</p> : !page.items.length ? <div className="analyst-empty"><h3>还没有可验证的目标价研判</h3><p>保存关注条件并运行每日检查。AI会逐项阅读报告重点观察，有信号命中且定价依据充分时，形成有期限、有失效条件的目标价；依据不足会说明原因。</p></div> : page.items.map(row => {
      const c = row.contract, v = row.latest_validation;
      const state = v.state === "MISSING" ? "MISSING" : row.status;
      return <article key={row.id} className={`analyst-forecast ${state.toLowerCase()}`}>
        <header><strong className={`direction-${c.direction}`}>{c.direction === "BULLISH" ? "上行观察" : "下行风险"} · 目标 {c.target_price.toFixed(2)} {c.currency}</strong><span className={`analyst-status ${state.toLowerCase()}`}>{names[state] || state}</span></header>
        <p className="analyst-muted">{date(row.created_at)} · 未来{c.horizon_sessions}个交易日 · 支持度{Math.round(c.confidence * 100)}%</p>
        <div className="analyst-forecast-prices"><span>基准价 <b>{row.entry_price.toFixed(2)}</b><small>{c.base_trade_date}</small></span><span>目标幅度 <b>{row.target_return_pct > 0 ? "+" : ""}{row.target_return_pct.toFixed(2)}%</b></span><span>失效价 <b>{c.invalidation_price?.toFixed(2) || "见失效条件"}</b></span></div>
        <h4>AI研判</h4><p>{c.thesis}</p><div className="analyst-condition-pills">{c.trigger_labels.map((s, i) => <span className="hit" key={i}>观察命中：{s}</span>)}</div>
        {c.pricing_basis && <><h4>目标价依据</h4><p>{c.pricing_basis}</p></>}
        <h4>可持续验证的逻辑链</h4><ol className="analyst-logic-chain">{c.logic_chain.map((s, i) => <li key={i}>{s}</li>)}</ol>
        <div className="analyst-validation"><strong>{names[state] || state}</strong><p>{v.detail || "预测已登记，等待发布次日起的真实行情。"}</p>{v.actual_price != null && <p>{v.price_date} · 收盘价 {v.actual_price.toFixed(2)} {c.currency} · 已观察 {v.sessions_observed} 个交易日</p>}{v.lesson && <p><strong>逻辑沉淀：</strong>{v.lesson}</p>}</div>
        <details><summary>失效条件与验证口径</summary><ul>{c.invalidation_conditions.map((s, i) => <li key={i}>{s}</li>)}</ul><p>{c.verification_rule}</p><p className="analyst-muted">新闻中的经营前提由每日AI复核，价位结果由真实行情核验。价格采用原币不复权口径，除权等公司行动发生时需复核可比性。</p></details>
        <details><summary>验证历程 · {row.checks.length}次</summary>{!row.checks.length && <p>尚无预测发布后的验证记录。</p>}{row.checks.map((r, i) => <div className="analyst-forecast-check" key={i}><small>{date(r.created_at)}</small><strong>{names[r.state]}</strong><p>{r.detail}</p>{r.price_date && <p>{r.price_date} · {r.actual_price} {c.currency}</p>}</div>)}</details>
        <details><summary>证据与版本</summary>{c.evidence.map((r, i) => <p key={i}>{r.claim} <small>（{r.evidence_id}）</small></p>)}<p className="analyst-muted">模型：{row.model_instance_code} · 分析技能版本：{row.skill_version} · 预测台账 #{row.ledger_id} · 调用 #{c.model_call_log_id}</p></details>
      </article>;
    })}
    {!!page && page.total > 10 && <div className="analyst-pagination"><button disabled={!offset} onClick={() => onPage(Math.max(0, offset - 10))}>上一页</button><span>{Math.floor(offset / 10) + 1} / {Math.ceil(page.total / 10)}页 · {page.total}条</span><button disabled={offset + 10 >= page.total} onClick={() => onPage(offset + 10)}>下一页</button></div>}
  </div>;
}
