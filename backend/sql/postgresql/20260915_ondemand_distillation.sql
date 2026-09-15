-- PostgreSQL 16 additive migration. Apply after backing up the target database.
-- Existing SQLite development databases are intentionally not migrated by this file.
BEGIN;

ALTER TABLE stock_news
    ADD COLUMN IF NOT EXISTS fetch_status varchar(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS content_raw text,
    ADD COLUMN IF NOT EXISTS is_distilled boolean NOT NULL DEFAULT false;
ALTER TABLE stock_notice
    ADD COLUMN IF NOT EXISTS fetch_status varchar(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS content_raw text,
    ADD COLUMN IF NOT EXISTS is_distilled boolean NOT NULL DEFAULT false;
ALTER TABLE stock_financial_report
    ADD COLUMN IF NOT EXISTS fetch_status varchar(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS content_raw text,
    ADD COLUMN IF NOT EXISTS is_distilled boolean NOT NULL DEFAULT false;

-- Backfill only fields that contain actual source text. Do not turn a title or
-- numeric JSON payload into a fabricated full-text document.
UPDATE stock_news SET content_raw = content
 WHERE content_raw IS NULL AND NULLIF(btrim(content), '') IS NOT NULL;
UPDATE stock_notice SET content_raw = COALESCE(
    NULLIF(content_json->>'content', ''),
    NULLIF(content_json->>'body', ''),
    NULLIF(content_json->>'text', '')
) WHERE content_raw IS NULL;
UPDATE stock_financial_report SET content_raw = COALESCE(
    NULLIF(data_json->>'content', ''),
    NULLIF(data_json->>'body', ''),
    NULLIF(data_json->>'text', '')
) WHERE content_raw IS NULL;
UPDATE stock_news SET fetch_status = 'DONE'
 WHERE fetch_status = 'PENDING' AND NULLIF(btrim(content_raw), '') IS NOT NULL;
UPDATE stock_notice SET fetch_status = 'DONE'
 WHERE fetch_status = 'PENDING' AND NULLIF(btrim(content_raw), '') IS NOT NULL;
UPDATE stock_financial_report SET fetch_status = 'DONE'
 WHERE fetch_status = 'PENDING' AND NULLIF(btrim(content_raw), '') IS NOT NULL;

DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['stock_news', 'stock_notice', 'stock_financial_report'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_' || t || '_fetch_status') THEN
            EXECUTE format(
                'ALTER TABLE %I ADD CONSTRAINT %I CHECK (fetch_status IN (''PENDING'', ''FETCHING'', ''DONE'', ''FAILED''))',
                t, 'ck_' || t || '_fetch_status'
            );
        END IF;
    END LOOP;
END $$;

-- A graph must belong to the task's selected knowledge base.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_knowledge_graph_id_base') THEN
        ALTER TABLE knowledge_graph ADD CONSTRAINT uq_knowledge_graph_id_base
            UNIQUE (id, knowledge_base_id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS asset_distillation_task (
    task_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_table varchar(64) NOT NULL CHECK (source_table IN ('stock_news', 'stock_notice', 'stock_financial_report')),
    source_doc_id integer NOT NULL,
    knowledge_document_id integer REFERENCES knowledge_document(id) ON DELETE SET NULL,
    knowledge_base_id integer NOT NULL REFERENCES knowledge_base(id) ON DELETE RESTRICT,
    graph_id integer NOT NULL,
    governance_run_id integer REFERENCES governance_run(id) ON DELETE SET NULL,
    content_sha256 char(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    kb_status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (kb_status IN ('PENDING', 'DONE', 'FAILED')),
    kg_status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (kg_status IN ('PENDING', 'DONE', 'FAILED')),
    kb_attempts integer NOT NULL DEFAULT 0 CHECK (kb_attempts >= 0),
    kg_attempts integer NOT NULL DEFAULT 0 CHECK (kg_attempts >= 0),
    error_log jsonb NOT NULL DEFAULT '{}'::jsonb,
    next_retry_at timestamptz,
    lease_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_asset_distillation_graph_base FOREIGN KEY (graph_id, knowledge_base_id)
        REFERENCES knowledge_graph(id, knowledge_base_id) ON DELETE RESTRICT,
    CONSTRAINT uq_asset_distillation_revision UNIQUE
        (source_table, source_doc_id, knowledge_base_id, graph_id, content_sha256)
);
CREATE INDEX IF NOT EXISTS ix_asset_distillation_pending
    ON asset_distillation_task (next_retry_at, created_at)
    WHERE kb_status <> 'DONE' OR kg_status <> 'DONE';
CREATE INDEX IF NOT EXISTS ix_asset_distillation_source
    ON asset_distillation_task (source_table, source_doc_id);

CREATE OR REPLACE FUNCTION touch_asset_distillation_updated_at()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS trg_asset_distillation_updated_at ON asset_distillation_task;
CREATE TRIGGER trg_asset_distillation_updated_at
    BEFORE UPDATE ON asset_distillation_task
    FOR EACH ROW EXECUTE FUNCTION touch_asset_distillation_updated_at();

-- Reuse the existing draft table without letting entity reviews become Skill edits.
ALTER TABLE skill_optimization_draft
    ADD COLUMN IF NOT EXISTS draft_type varchar(32) NOT NULL DEFAULT 'SKILL_OPTIMIZATION',
    ADD COLUMN IF NOT EXISTS review_context_json jsonb NOT NULL DEFAULT '{}'::jsonb;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_skill_optimization_draft_type') THEN
        ALTER TABLE skill_optimization_draft ADD CONSTRAINT ck_skill_optimization_draft_type
            CHECK (draft_type IN ('SKILL_OPTIMIZATION', 'DISTILLATION_REVIEW'));
    END IF;
END $$;

COMMENT ON TABLE asset_distillation_task IS '按源文档和版本追踪知识库、知识图谱两条独立蒸馏轨道';
COMMENT ON COLUMN stock_news.fetch_status IS '按需正文抓取状态：PENDING/FETCHING/DONE/FAILED';
COMMENT ON COLUMN stock_news.content_raw IS '可核验的新闻原文，不用标题补造';
COMMENT ON COLUMN stock_news.is_distilled IS '当前选定目标版本两轨均完成的派生快捷标志';
COMMENT ON COLUMN stock_notice.fetch_status IS '按需公告正文抓取状态';
COMMENT ON COLUMN stock_notice.content_raw IS '可核验的公告原文';
COMMENT ON COLUMN stock_notice.is_distilled IS '当前选定目标版本两轨均完成的派生快捷标志';
COMMENT ON COLUMN stock_financial_report.fetch_status IS '按需财报正文抓取状态';
COMMENT ON COLUMN stock_financial_report.content_raw IS '可核验的财报原文，区别于指标 JSON';
COMMENT ON COLUMN stock_financial_report.is_distilled IS '当前选定目标版本两轨均完成的派生快捷标志';
COMMENT ON COLUMN asset_distillation_task.source_doc_id IS '来源业务表的主键；不是 knowledge_document.id';
COMMENT ON COLUMN asset_distillation_task.knowledge_document_id IS '统一知识文档资产主键，承接来源及证据';
COMMENT ON COLUMN asset_distillation_task.content_sha256 IS '原文 SHA-256，保证同源同版本幂等并允许新版本重跑';
COMMENT ON COLUMN asset_distillation_task.kb_status IS '知识库分块及向量写入状态';
COMMENT ON COLUMN asset_distillation_task.kg_status IS '知识图谱三元组校验及写入状态';
COMMENT ON COLUMN asset_distillation_task.error_log IS '按 kb/kg 键记录失败原因与时间；重试不抹除历史';
COMMENT ON COLUMN skill_optimization_draft.draft_type IS 'Skill 修订与蒸馏实体审核的隔离类型';
COMMENT ON COLUMN skill_optimization_draft.review_context_json IS '低置信度或新图节点的来源、证据及待审结构化数据';

-- Keep the project's UI-facing table/field dictionary in sync with the DDL.
INSERT INTO database_table_comment
    (table_name, display_name, domain, description, column_comments_json)
VALUES
    ('stock_news', '股票新闻', '业务数据', '股票新闻及原文',
     '{"fetch_status":"按需正文抓取状态","content_raw":"可核验的新闻原文","is_distilled":"当前目标版本两轨完成标志"}'::json),
    ('stock_notice', '股票公告', '业务数据', '股票公告及原文',
     '{"fetch_status":"按需正文抓取状态","content_raw":"可核验的公告原文","is_distilled":"当前目标版本两轨完成标志"}'::json),
    ('stock_financial_report', '财务报告', '业务数据', '财务指标与报告原文',
     '{"fetch_status":"按需正文抓取状态","content_raw":"可核验的报告原文","is_distilled":"当前目标版本两轨完成标志"}'::json),
    ('skill_optimization_draft', 'Skill 与实体审核草稿', '模型治理', '待人工审核的 Skill 修订或蒸馏实体',
     '{"draft_type":"草稿类型：Skill 修订或蒸馏实体审核","review_context_json":"蒸馏待审三元组、证据与任务上下文"}'::json),
    ('asset_distillation_task', '双轨蒸馏任务', '知识治理', '按文档版本追踪知识库和知识图谱的独立写入状态',
     '{"task_id":"任务 UUID","source_table":"来源业务表白名单","source_doc_id":"来源业务表行 ID","knowledge_document_id":"统一知识文档 ID","knowledge_base_id":"目标知识库 ID","graph_id":"目标独立图谱 ID","governance_run_id":"关联治理运行 ID","content_sha256":"来源原文哈希及版本键","kb_status":"知识库轨道状态","kg_status":"知识图谱轨道状态","kb_attempts":"知识库重试次数","kg_attempts":"图谱重试次数","error_log":"分轨错误记录","next_retry_at":"下次允许重试时间","lease_until":"工作者租约截止时间","created_at":"入队时间","updated_at":"最近状态更新时间"}'::json)
ON CONFLICT (table_name) DO UPDATE SET
    column_comments_json = (
        COALESCE(database_table_comment.column_comments_json::jsonb, '{}'::jsonb)
        || EXCLUDED.column_comments_json::jsonb
    )::json;

COMMIT;
