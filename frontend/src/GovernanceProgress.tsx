import type { BatchGovernanceJob } from "./batchGovernanceApi";

const phases: Record<string, string> = { PREPARING: "准备文档", BODY_FETCH: "读取／补取原文", ANALYZING: "语义分析",
  SEMANTIC_READING: "模型理解原文", STRUCTURED_EXTRACTION: "抽取事件与关系", FINISHED: "本阶段处理结束" };
const stages: Record<string, string> = { QUEUED: "等待后台领取", VALIDATION: "校验范围", BUSINESS_DATA: "业务数据采集",
  STRUCTURED_EVENTS: "新闻公告语义治理", DOCUMENT_CHUNKS: "文档切片", KNOWLEDGE_PIPELINE: "湖仓与知识发布", COMPLETE: "本轮处理结束" };

export function GovernanceProgress({ job }: { job: Pick<BatchGovernanceJob, "progress" | "current_stage" | "document_progress" | "job_status"> }) {
  const detail = job.document_progress;
  return <div className="governance-progress" style={{ display: "grid", gap: 5, minWidth: 130, maxWidth: "100%", overflowWrap: "anywhere" }}>
    <progress aria-label="治理进度" max={100} value={job.progress || 0} style={{ width: "100%", height: 10, accentColor: "#217b56" }} />
    <small>{job.progress || 0}% · {stages[job.current_stage || ""] || job.current_stage || "等待执行"}</small>
    {detail && typeof detail.total === "number" && <>
      <small>已处理 {detail.processed || 0} / {detail.total} 篇 · 成功 {detail.succeeded || 0} · 未完成 {detail.partial || 0} · 失败 {detail.failed || 0} · 已完成跳过 {detail.skipped || 0}</small>
      {job.job_status === "RUNNING" && <small>{phases[detail.phase || ""] || "处理文档"}{detail.current_document ? `：${detail.current_document.title}` : ""}</small>}
    </>}
  </div>;
}
