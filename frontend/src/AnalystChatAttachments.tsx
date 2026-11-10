import { useEffect, useState } from "react";
import { X } from "lucide-react";
import { analystFollowupApi, type AnalystEvidence, type AnalystImageInput } from "./stockAnalystApi";

export async function prepareAnalystImage(file: File): Promise<AnalystImageInput> {
  if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) throw new Error("仅支持PNG、JPEG、WebP图片。");
  if (file.size > 24 * 1024 * 1024) throw new Error("原图过大，请先缩小到24MB以内。");
  const bitmap = await createImageBitmap(file);
  try {
    const scale = Math.min(1, 2400 / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(bitmap.width * scale));
    canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("当前浏览器无法处理图片。");
    ctx.fillStyle = "#fff";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    let dataUrl = canvas.toDataURL("image/png");
    if (dataUrl.length > 5_500_000) dataUrl = canvas.toDataURL("image/jpeg", .9);
    if (dataUrl.length > 5_500_000) throw new Error("压缩后图片仍超过4MB，请裁剪后重试。");
    return { name: (file.name || "粘贴图片.png").slice(0, 160), data_url: dataUrl };
  } finally { bitmap.close(); }
}

export function AnalystDocumentPicker({ analysisId, selected, onChange, onClose }: {
  analysisId: number; selected: AnalystEvidence[]; onChange: (rows: AnalystEvidence[]) => void; onClose: () => void;
}) {
  const [category, setCategory] = useState("ANY");
  const [keyword, setKeyword] = useState("");
  const [offset, setOffset] = useState(0);
  const [page, setPage] = useState<{ items: AnalystEvidence[]; total: number }>({ items: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError("");
    const timer = window.setTimeout(() => {
      void analystFollowupApi.localDocuments(analysisId, category, keyword, offset, controller.signal)
        .then(data => { if (!controller.signal.aborted) setPage(data); })
        .catch(e => { if (!controller.signal.aborted) setError(e.message); })
        .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    }, 180);
    return () => { controller.abort(); window.clearTimeout(timer); };
  }, [analysisId, category, keyword, offset]);
  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === "Escape") onClose(); };
    window.addEventListener("keydown", close);
    return () => window.removeEventListener("keydown", close);
  }, [onClose]);
  return <div className="analyst-modal-backdrop"><section className="analyst-modal analyst-document-picker" role="dialog" aria-modal="true" aria-labelledby="analyst-documents-title">
    <header><h3 id="analyst-documents-title">引用当前股票的新闻 / 公告</h3><button type="button" aria-label="关闭资料选择" onClick={onClose}><X size={19} /></button></header>
    <p className="analyst-muted">直接从该股票已入库资料中选择，可跨页多选。发送后AI优先阅读这些资料；只有标题的公告会明确标注。</p>
    <div className="analyst-document-filters"><select aria-label="引用资料类型" value={category} onChange={e => { setCategory(e.target.value); setOffset(0); }}><option value="ANY">新闻与公告</option><option value="NOTICE">公告</option><option value="NEWS">新闻</option></select><input aria-label="搜索引用资料" placeholder="搜索标题，如董事、回购、收购" value={keyword} onChange={e => { setKeyword(e.target.value); setOffset(0); }} /></div>
    {error ? <p role="alert" className="analyst-error">{error}</p> : loading ? <p role="status">正在读取本地资料…</p> : <div className="analyst-document-list">{!page.items.length ? <p>当前条件下没有已入库资料，请更换搜索条件。</p> : page.items.map(row => {
      const checked = selected.some(item => item.evidence_id === row.evidence_id);
      return <label key={row.evidence_id} className={checked ? "selected" : ""}><input type="checkbox" aria-label={`引用${row.title}`} checked={checked} disabled={!checked && selected.length >= 8} onChange={e => onChange(e.target.checked ? [...selected, row] : selected.filter(item => item.evidence_id !== row.evidence_id))} /><div><strong><span className="analyst-tag">{row.category_name}</span>{row.title}</strong><small>{row.observed_at?.slice(0, 10) || "日期未注明"} · {row.source} · {row.data?.content_status === "TITLE_ONLY" ? "仅标题，正文待补全" : "本地正文可引用"}</small>{row.excerpt && <p>{row.excerpt}</p>}</div></label>;
    })}</div>}
    <div className="analyst-pagination"><button type="button" disabled={!offset || loading} onClick={() => setOffset(value => Math.max(0, value - 12))}>上一页</button><span>{Math.floor(offset / 12) + 1} / {Math.max(1, Math.ceil(page.total / 12))}页 · {page.total}条</span><button type="button" disabled={loading || offset + 12 >= page.total} onClick={() => setOffset(value => value + 12)}>下一页</button></div>
    <footer><span>已选 {selected.length}/8 条，可在输入区移除</span><button type="button" className="analyst-primary" onClick={onClose}>完成引用</button></footer>
  </section></div>;
}
