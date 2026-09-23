-- 新旧招采对象对账报表，只读，不修改数据。
SELECT '投标提交数量' AS metric,
       (SELECT COUNT(*) FROM tb_bidding_info WHERE notice_id IS NOT NULL AND vendor_id IS NOT NULL) AS legacy_count,
       (SELECT COUNT(*) FROM tb_bid_submission) AS normalized_count;

SELECT bi.notice_id, bi.vendor_id, COUNT(*) AS legacy_rows, COUNT(DISTINCT s.id) AS normalized_rows
FROM tb_bidding_info bi
LEFT JOIN tb_bid_submission s ON s.notice_id = bi.notice_id AND s.vendor_id = bi.vendor_id
WHERE bi.notice_id IS NOT NULL AND bi.vendor_id IS NOT NULL
GROUP BY bi.notice_id, bi.vendor_id
HAVING normalized_rows <> 1;

SELECT '评标评分数量' AS metric,
       (SELECT COUNT(*) FROM tb_expert_score) AS legacy_count,
       (SELECT COUNT(*) FROM tb_evaluation_sheet) AS normalized_count;

SELECT es.notice_id, es.expert_id, es.vendor_id, COUNT(*) AS legacy_rows,
       COUNT(DISTINCT x.id) AS normalized_rows
FROM tb_expert_score es
LEFT JOIN tb_evaluation_assignment a ON a.notice_id = es.notice_id AND a.expert_id = es.expert_id
LEFT JOIN tb_evaluation_sheet x ON x.assignment_id = a.id
WHERE es.notice_id IS NOT NULL AND es.expert_id IS NOT NULL AND es.vendor_id IS NOT NULL
GROUP BY es.notice_id, es.expert_id, es.vendor_id
HAVING normalized_rows = 0;

SELECT '定标候选数量' AS metric,
       (SELECT COUNT(*) FROM tb_bidding_result) AS legacy_count,
       (SELECT COUNT(*) FROM tb_award_candidate) AS normalized_count;

SELECT r.notice_id, r.vendor_id, r.id AS legacy_result_id
FROM tb_bidding_result r
LEFT JOIN tb_award_candidate c ON c.id = r.id
WHERE c.id IS NULL;
