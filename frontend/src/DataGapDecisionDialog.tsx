import { useEffect } from "react";
import type { SelectionPreflight, SelectionRecord } from "./selectionApi";

export type DataGapDecision = "UPDATE" | "CONTINUE_PARTIAL" | "CANCEL";

export function DataGapDecisionDialog({
  preflight,
  onDecision,
}: {
  preflight: SelectionPreflight;
  onDecision: (decision: DataGapDecision) => void;
}) {
  const data = (preflight.data || {}) as SelectionRecord;
  const missingSymbols = Array.isArray(data.missing_symbols)
    ? data.missing_symbols.filter((item): item is string => typeof item === "string")
    : [];

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onDecision("CANCEL");
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onDecision]);

  return <div className="selection-backdrop" role="presentation">
    <section className="selection-dialog selection-data-gap-dialog" role="dialog" aria-modal="true" aria-labelledby="data-gap-title">
      <header>
        <div>
          <h2 id="data-gap-title">分析前发现数据缺口</h2>
          <p className="selection-help">本次范围内有 {missingSymbols.length} 只股票的数据不完整，请选择后续处理方式。</p>
        </div>
      </header>
      {missingSymbols.length > 0 && <div className="selection-data-gap-symbols" aria-label="存在数据缺口的股票">
        {missingSymbols.slice(0, 12).map(symbol => <span key={symbol}>{symbol}</span>)}
        {missingSymbols.length > 12 && <span>另有 {missingSymbols.length - 12} 只</span>}
      </div>}
      <div className="selection-data-gap-options">
        <button type="button" className="primary-button" onClick={() => onDecision("UPDATE")}>
          <strong>更新数据</strong>
          <small>先采集并更新所选股票的数据，再继续分析</small>
        </button>
        <button type="button" className="partial-button" onClick={() => onDecision("CONTINUE_PARTIAL")}>
          <strong>继续部分数据分析</strong>
          <small>使用当前已有数据，结果中保留缺口和置信度限制</small>
        </button>
        <button type="button" onClick={() => onDecision("CANCEL")}>
          <strong>取消</strong>
          <small>不创建本次分析任务</small>
        </button>
      </div>
    </section>
  </div>;
}
