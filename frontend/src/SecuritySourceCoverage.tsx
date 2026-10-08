import { useEffect, useState } from "react";
import { api, type SourceCoverage } from "./api";
import "./SecuritySourceCoverage.css";

export function SecuritySourceCoverage() {
  const [data, setData] = useState<SourceCoverage | null>(null);
  const [board, setBoard] = useState("MAIN");
  const [error, setError] = useState("");
  useEffect(() => { void api.sourceCoverage().then(setData).catch(e => setError(e instanceof Error ? e.message : "覆盖清单读取失败")); }, []);
  if (error) return <p role="alert">{error}</p>;
  if (!data) return <p role="status">正在核对板块与数据接口…</p>;
  const current = data.boards.find(item => item.code === board);
  return <div className="security-source-coverage">
    <p>“已接入”表示已实现并启用对应接口，实际采集结果请查看每只股票的数据采集详情。部分覆盖与待接入不能当成采集完成。</p>
    <div className="coverage-board-tabs">{data.boards.map(item => <button key={item.code} className={board === item.code ? "active" : ""} onClick={() => setBoard(item.code)}>{item.name}</button>)}</div>
    {current && <><p title={current.definition}><strong>{current.name}</strong> · 保留主数据 {current.record_count.toLocaleString()} 条<br />{current.definition}</p>
      <div className="table-wrap"><table><thead><tr><th>所需数据</th><th>覆盖状态</th><th>匹配来源 / 接口</th><th>范围与缺口</th></tr></thead><tbody>{current.capabilities.map(item => <tr key={item.category}>
        <td>{item.label}</td><td><span className={`coverage-status ${item.status.toLowerCase()}`}>{item.status_name}</span></td>
        <td>{item.sources.length ? item.sources.map(source => <div key={`${source.source_code}:${source.interface_code}`} title={source.interface_code}>{source.source_name}{!source.enabled && "（未启用）"}</div>) : "暂无匹配接口"}</td>
        <td>{item.sources.map(source => source.limitation).filter(Boolean).join("；") || item.note}</td>
      </tr>)}</tbody></table></div></>}
    <p>未分类记录 {data.unclassified_count.toLocaleString()} 条仍保留，可在主数据中筛选查看；不凭港股代码推测板块。</p>
  </div>;
}
