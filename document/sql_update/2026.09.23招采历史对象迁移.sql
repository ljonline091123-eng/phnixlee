-- 招采历史对象增量迁移（幂等）
-- 目的：将旧的 BiddingInfo / BiddingEvaluatExpert / ExpertScore / BiddingResult
-- 映射到规范化领域表。执行前请完成数据库一致性备份；脚本不删除、不更新旧表。

START TRANSACTION;

-- 1. 投标提交：同一公告、供应商只保留一个聚合根，状态取最后一次报价记录。
INSERT INTO tb_bid_submission
  (id, notice_id, scheme_id, vendor_id, status, submitted_at,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT bi.id, bi.notice_id, bi.scheme_id, bi.vendor_id,
       CASE WHEN COALESCE(bi.submit_status, 0) = 1 THEN 1 ELSE 0 END,
       CASE WHEN COALESCE(bi.submit_status, 0) = 1 THEN COALESCE(bi.update_time, bi.create_time) ELSE NULL END,
       bi.create_by, bi.create_id, bi.create_time, bi.update_by, bi.update_id, bi.update_time, bi.del_flag
FROM (
  SELECT source.*,
         ROW_NUMBER() OVER (
           PARTITION BY source.notice_id, source.vendor_id
           ORDER BY COALESCE(source.twice_quot_version, 1) DESC,
                    COALESCE(source.update_time, source.create_time) DESC, source.id DESC
         ) AS row_no
  FROM tb_bidding_info source
  WHERE source.notice_id IS NOT NULL AND source.vendor_id IS NOT NULL
) bi
WHERE bi.row_no = 1
  AND NOT EXISTS (
    SELECT 1 FROM tb_bid_submission s
    WHERE s.notice_id = bi.notice_id AND s.vendor_id = bi.vendor_id
  );

-- 2. 投标版本：每个报价版本只取最后一条记录，保留完整调价版本链。
INSERT INTO tb_bid_submission_version
  (id, submission_id, previous_version_id, version_no, tax_price, not_tax_price, contact, phone, status,
   submitted_at, create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT bi.id, s.id,
       LAG(bi.id) OVER (PARTITION BY bi.notice_id, bi.vendor_id ORDER BY bi.version_no),
       bi.version_no, bi.tax_price, bi.not_tax_price, bi.contact, bi.phone,
       CASE WHEN COALESCE(bi.submit_status, 0) = 1 OR bi.bidding_status IS NOT NULL THEN 1 ELSE 0 END,
       CASE WHEN COALESCE(bi.submit_status, 0) = 1 OR bi.bidding_status IS NOT NULL
            THEN COALESCE(bi.update_time, bi.create_time) ELSE NULL END,
       bi.create_by, bi.create_id, bi.create_time, bi.update_by, bi.update_id, bi.update_time, bi.del_flag
FROM (
  SELECT ranked.*
  FROM (
    SELECT source.*,
           GREATEST(COALESCE(source.twice_quot_version, 1), 1) AS version_no,
           ROW_NUMBER() OVER (
             PARTITION BY source.notice_id, source.vendor_id,
                          GREATEST(COALESCE(source.twice_quot_version, 1), 1)
             ORDER BY COALESCE(source.update_time, source.create_time) DESC, source.id DESC
           ) AS version_row_no
    FROM tb_bidding_info source
    WHERE source.notice_id IS NOT NULL AND source.vendor_id IS NOT NULL
  ) ranked
  WHERE ranked.version_row_no = 1
) bi
JOIN tb_bid_submission s ON s.notice_id = bi.notice_id AND s.vendor_id = bi.vendor_id
WHERE NOT EXISTS (
  SELECT 1 FROM tb_bid_submission_version v
  WHERE v.submission_id = s.id AND v.version_no = bi.version_no
);

UPDATE tb_bid_submission s
JOIN (
  SELECT ranked.submission_id, ranked.id, ranked.status, ranked.submitted_at
  FROM (
    SELECT v.*,
           ROW_NUMBER() OVER (PARTITION BY v.submission_id ORDER BY v.version_no DESC, v.id DESC) AS row_no
    FROM tb_bid_submission_version v
  ) ranked
  WHERE ranked.row_no = 1
) current_version ON current_version.submission_id = s.id
SET s.current_version_id = current_version.id,
    s.status = current_version.status,
    s.submitted_at = current_version.submitted_at;

-- 3. 评标任务：保留旧专家类别，后续可按角色枚举逐步规范化。
INSERT INTO tb_evaluation_assignment
  (id, notice_id, expert_id, role_type, status, deadline, completed_at,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT e.id, e.notice_id, e.expert_id, e.expert_type,
       CASE WHEN COALESCE(e.eval_status, 0) = 1 THEN 2 ELSE 0 END,
       e.deadline,
       CASE WHEN COALESCE(e.eval_status, 0) = 1 THEN e.update_time ELSE NULL END,
       e.create_by, e.create_id, e.create_time, e.update_by, e.update_id, e.update_time, e.del_flag
FROM tb_bidding_evaluat_expert e
WHERE e.notice_id IS NOT NULL AND e.expert_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tb_evaluation_assignment a
    WHERE a.notice_id = e.notice_id AND a.expert_id = e.expert_id
      AND COALESCE(a.role_type, 0) = COALESCE(e.expert_type, 0)
  );

-- 4. 评标表：通过旧投标单和专家任务关联，自动重新计算总分。
INSERT INTO tb_evaluation_sheet
  (id, assignment_id, submission_version_id, business_score, technical_score,
   quotation_score, total_score, opinion, status, submitted_at,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT es.id, a.id, v.id, es.bus_score, es.tech_score, es.quotation,
       COALESCE(es.bus_score, 0) + COALESCE(es.tech_score, 0) + COALESCE(es.quotation, 0),
       es.eva_opinion, CASE WHEN COALESCE(es.eval_status, 0) = 1 THEN 1 ELSE 0 END,
       es.eva_time, es.create_by, es.create_id, es.create_time, es.update_by, es.update_id, es.update_time, es.del_flag
FROM tb_expert_score es
JOIN tb_evaluation_assignment a ON a.notice_id = es.notice_id AND a.expert_id = es.expert_id
JOIN tb_bid_submission s ON s.notice_id = es.notice_id AND s.vendor_id = es.vendor_id
JOIN tb_bid_submission_version v ON v.id = s.current_version_id
WHERE NOT EXISTS (
  SELECT 1 FROM tb_evaluation_sheet x
  WHERE x.assignment_id = a.id AND x.submission_version_id = v.id
);

-- 5. 定标决策：每个公告只建立一条决策聚合根，优先采用旧表 sure_bid=1 的记录。
INSERT INTO tb_award_decision
  (id, notice_id, scheme_id, status, decision_basis, decided_at,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT MIN(r.id), r.notice_id, MAX(r.scheme_id),
       CASE WHEN MAX(CASE WHEN COALESCE(r.sure_bid, 0) = 1 THEN 1 ELSE 0 END) = 1 THEN 1 ELSE 0 END,
       '历史 BiddingResult 迁移，待业务复核',
       MAX(CASE WHEN COALESCE(r.sure_bid, 0) = 1 THEN r.update_time ELSE NULL END),
       MAX(r.create_by), MAX(r.create_id), MIN(r.create_time), MAX(r.update_by), MAX(r.update_id), MAX(r.update_time), MAX(r.del_flag)
FROM tb_bidding_result r
WHERE r.notice_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_award_decision d WHERE d.notice_id = r.notice_id)
GROUP BY r.notice_id;

-- 6. 中标候选人：保留旧排名和综合得分，决策人员工信息不从历史猜测。
INSERT INTO tb_award_candidate
  (id, decision_id, submission_version_id, vendor_id, `rank`, total_score, recommendation_reason,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT r.id, d.id, s.current_version_id, r.vendor_id,
       ROW_NUMBER() OVER (PARTITION BY d.id ORDER BY COALESCE(r.`rank`, 999999), r.id), r.total_score,
       CASE WHEN COALESCE(r.sure_bid, 0) = 1 THEN '历史确定中标记录' ELSE '历史候选记录' END,
       r.create_by, r.create_id, r.create_time, r.update_by, r.update_id, r.update_time, r.del_flag
FROM tb_bidding_result r
JOIN tb_award_decision d ON d.notice_id = r.notice_id
JOIN tb_bid_submission s ON s.notice_id = r.notice_id AND s.vendor_id = r.vendor_id
WHERE NOT EXISTS (SELECT 1 FROM tb_award_candidate c WHERE c.id = r.id);

-- 公示日期可直接迁移；内容保留旧通知文本，结束时间为空时不自动发布。
INSERT INTO tb_award_publicity
  (id, decision_id, title, content, start_time, end_time, status, published_at,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT d.id, d.id, CONCAT('历史中标公示-', d.notice_id), MAX(r.notifi_content),
       MIN(r.publicity_start_time), MAX(r.publicity_end_time),
       CASE WHEN MAX(r.publicity_end_time) IS NOT NULL THEN 1 ELSE 0 END,
       CASE WHEN MAX(r.publicity_end_time) IS NOT NULL THEN MAX(r.notifi_time) ELSE NULL END,
       MAX(r.create_by), MAX(r.create_id), MIN(r.create_time), MAX(r.update_by), MAX(r.update_id), MAX(r.update_time), MAX(r.del_flag)
FROM tb_award_decision d
JOIN tb_bidding_result r ON r.notice_id = d.notice_id
WHERE NOT EXISTS (SELECT 1 FROM tb_award_publicity p WHERE p.decision_id = d.id)
GROUP BY d.id;

-- 迁移后将历史 sure_bid=1 的候选人挂到定标决策；没有确定中标人的记录保持待复核。
UPDATE tb_award_decision d
JOIN tb_award_candidate c ON c.decision_id = d.id
JOIN tb_bidding_result r ON r.id = c.id AND COALESCE(r.sure_bid, 0) = 1
SET d.selected_candidate_id = c.id,
    d.status = 1
WHERE d.selected_candidate_id IS NULL;

COMMIT;
