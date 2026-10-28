import type { SelectionRecord } from "./selectionApi";

const directions: Record<string, string> = {POSITIVE: "偏利好", NEGATIVE: "偏利空", MIXED: "影响交织", NEUTRAL: "中性", UNKNOWN: "待判断"};
const signed = (value: unknown) => typeof value === "number" && Number.isFinite(value) ? `${value > 0 ? "+" : ""}${value.toFixed(2)}` : "未评分";
const date = (value: unknown) => {
  if (!value) return "未记录";
  const text = String(value);
  const parsed = new Date(/(?:Z|[+-]\d{2}:?\d{2})$/.test(text) ? text : text.length > 10 ? `${text}Z` : text);
  return Number.isNaN(parsed.getTime()) ? text : parsed.toLocaleString("zh-CN", {timeZone: "Asia/Shanghai"});
};

export function SelectionEvidenceCards({sections, onDocument, onBackfill, disabled}: {
  sections: SelectionRecord[]; onDocument?: (id: number) => void;
  onBackfill?: (dimension: string) => void; disabled?: boolean;
}) {
  const item = (row: SelectionRecord) => <li key={`${row.reference_type}:${row.reference_id}`} className="analysis-evidence-item">
    <div className="analysis-evidence-title">{row.reference_type === "DOCUMENT"
      ? <button type="button" className="analysis-reference" onClick={() => onDocument?.(Number(row.reference_id))}>{row.title}</button>
      : <strong>{row.title}</strong>}
      <span className={`analysis-item-score ${String(row.direction).toLowerCase()}`}>{directions[row.direction] || "待判断"} {signed(row.score)}</span></div>
    <small>数据获取时间：{date(row.acquired_at)}{row.published_at ? ` · 发布时间：${date(row.published_at)}` : ""}</small>
    {!row.acquired_at && row.indexed_at && <small>图谱入库时间：{date(row.indexed_at)}（原始获取时间未记录）</small>}
    {Array.isArray(row.values) && row.values.length > 0 && <dl className="analysis-evidence-values">{row.values.map((value: SelectionRecord, index: number) => <div key={index}><dt>{value.name}</dt><dd>{value.value}</dd></div>)}</dl>}
    <p>{row.rationale}</p>
  </li>;
  return <><h3>逐项研判与逐条证据</h3><p className="analysis-help">按数据获取时间倒序展示，默认显示最新5条。板块综合分＝逐条影响分平均值×板块权重；缺分不按0分计算。此处为七类资料的影响贡献，八维雷达仍按已发布评价方案计算。</p>
    <details className="analysis-input"><summary>查看板块权重与八维方案的对应关系</summary><p>新闻、公告各分配“事件催化与信披可信度”的一半权重；财务使用“基本面质量＋估值与盈利预期”；量价使用“价格趋势与风险收益”；资金使用“资金与交易行为”；公司使用“治理与股东稳定性”；图谱使用“产业链与外部韧性＋行业与板块景气”。权重总额保持100%，历史缺少评价方案时不推造权重。</p></details>
    <div className="analysis-dimensions">{sections.map(section => {
      const items: SelectionRecord[] = Array.isArray(section.items) ? section.items : [];
      return <article key={section.dimension} data-dimension={section.dimension} className={`analysis-dimension ${section.status === "INSUFFICIENT" ? "missing" : ""}`}>
        <div className="analysis-card-heading"><h4>{section.name}</h4><div className="analysis-section-score"><span className={`analysis-direction ${String(section.direction).toLowerCase()}`}>{section.status === "INSUFFICIENT" ? "证据不足" : directions[section.direction] || "待判断"}</span><strong title={section.formula}>综合分 {signed(section.weighted_score)}</strong></div></div>
        <p>{section.summary}</p><small>板块权重 {typeof section.weight === "number" ? `${section.weight}%` : "未记录"} · 原始影响分 {signed(section.raw_score)} · 已评分 {section.scored_count}/{section.total_count} 条</small>
        {items.length ? <><ol className="analysis-evidence-list">{items.slice(0, 5).map(item)}</ol>{items.length > 5 && <details className="analysis-evidence-more"><summary>展开其余 {items.length - 5} 条数据</summary><ol start={6} className="analysis-evidence-list">{items.slice(5).map(item)}</ol></details>}</> : <p className="batch-note">本次没有可归属于此板块的有效证据；未使用其他板块资料填充。</p>}
        {section.status === "INSUFFICIENT" && onBackfill && <button type="button" className="analysis-repair" disabled={disabled} onClick={() => onBackfill(section.dimension)}>{disabled ? "后台任务执行中…" : `补齐${section.name}数据`}</button>}
      </article>;
    })}</div></>;
}
