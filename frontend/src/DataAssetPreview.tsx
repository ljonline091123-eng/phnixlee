import { useState } from "react";
import type { DataPreview } from "./api";
import "./ResourcePreview.css";

function structuredValue(value: unknown): unknown {
  if (typeof value !== "string" || !/^[\s]*[\[{]/.test(value)) return value;
  try { return JSON.parse(value); } catch { return value; }
}

function valueText(value: unknown): string {
  if (value == null) return "未提供";
  if (value === "") return "空字符串";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

export function DataAssetPreview({ preview }: { preview: DataPreview }) {
  const [selected, setSelected] = useState<{ row: number; name: string } | null>(null);
  const metadata = new Map((preview.column_meta || []).map(column => [column.name, column]));
  const detail = selected ? preview.rows[selected.row]?.[selected.name] : undefined;
  const parsedDetail = structuredValue(detail);
  const detailText = parsedDetail && typeof parsedDetail === "object" ? JSON.stringify(parsedDetail, null, 2) : valueText(detail);
  return <section className="asset-preview" aria-label="数据资产只读预览">
    <div className="asset-preview-summary"><strong>{preview.display_name || preview.table_name}</strong><span>共 {preview.row_count.toLocaleString()} 行 · 当前预览 {preview.rows.length} 行</span><small>{preview.description || "悬停表头查看字段释义；点击长文本或结构化数据查看完整内容。"}</small></div>
    <div className="table-wrap asset-preview-table" tabIndex={0} aria-label="资产记录，可横向滚动">
      <table><thead><tr><th className="asset-preview-index">序号</th>{preview.columns.map(name => {
        const column = metadata.get(name);
        return <th key={name} title={[column?.description, column?.type && `数据类型：${column.type}`, column?.nullable === false ? "必填字段" : ""].filter(Boolean).join("\n")}><span>{column?.label || name}</span><code>{name}</code></th>;
      })}</tr></thead><tbody>{preview.rows.map((row, index) => <tr key={index}><td className="asset-preview-index">{index + 1}</td>{preview.columns.map(name => {
        const original = row[name], value = structuredValue(original);
        const structured = value !== null && typeof value === "object";
        const fullText = valueText(original), expandable = structured || fullText.length > 100 || fullText.includes("\n");
        const summary = structured ? Array.isArray(value) ? `列表 · ${value.length} 项` : `结构化数据 · ${Object.keys(value as object).length} 个字段` : fullText.slice(0, 100) + (fullText.length > 100 ? "…" : "");
        return <td key={name}><div className={`asset-preview-cell${original == null ? " is-empty" : ""}`}><span>{summary}</span>{expandable && <button type="button" aria-label={`查看第 ${index + 1} 行${metadata.get(name)?.label || name}完整内容`} onClick={() => setSelected({ row: index, name })}>查看完整内容</button>}</div></td>;
      })}</tr>)}</tbody></table>
      {!preview.rows.length && <p className="governance-empty">当前资产尚无可预览的记录。</p>}
    </div>
    {selected && <section className="asset-preview-detail" aria-label="字段完整内容"><header><div><strong>{metadata.get(selected.name)?.label || selected.name}</strong><small>第 {selected.row + 1} 行 · {selected.name} · {detailText.length.toLocaleString()} 字符</small></div><button type="button" onClick={() => setSelected(null)}>收起内容</button></header>{metadata.get(selected.name)?.description && <p>{metadata.get(selected.name)?.description}</p>}<pre tabIndex={0}>{detailText}</pre></section>}
  </section>;
}
