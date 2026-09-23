-- 旧合同和电子签章记录迁移。幂等执行，不删除旧表；执行前必须完成数据库一致性备份。
START TRANSACTION;

INSERT INTO tb_procurement_contract
  (id, award_decision_id, legacy_agreement_id, scheme_id, vendor_id, contract_no, contract_name,
   total_amount, signed_at, effective_at, status,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT UUID_SHORT(), awarded.award_decision_id, a.id, a.scheme_id, a.vendor_id, a.agreement_code,
       COALESCE(a.agreement_name, CONCAT('历史合同-', a.id)),
       a.total_amount_inc_tax, a.agreement_sign_date, a.agreement_effective_date,
       CASE
         WHEN a.agreement_state = 10 THEN 2
         WHEN a.agreement_state IN (7, 8, 9) THEN 1
         WHEN a.agreement_state IN (2, 20) THEN 5
         ELSE 0
       END,
       a.create_by, a.create_id, a.create_time, a.update_by, a.update_id, a.update_time, a.del_flag
FROM tb_agreement a
LEFT JOIN (
  SELECT d.scheme_id, c.vendor_id, MIN(d.id) AS award_decision_id
  FROM tb_award_decision d
  JOIN tb_award_candidate c ON c.id = d.selected_candidate_id
  GROUP BY d.scheme_id, c.vendor_id
) awarded ON awarded.scheme_id = a.scheme_id AND awarded.vendor_id = a.vendor_id
WHERE NOT EXISTS (
  SELECT 1 FROM tb_procurement_contract c WHERE c.legacy_agreement_id = a.id
);

INSERT INTO tb_contract_party
  (id, contract_id, party_type, party_id, party_name, need_sign,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT UUID_SHORT(), c.id, 1, a.party_a_org_id, a.party_a_name, 1,
       a.create_by, a.create_id, a.create_time, a.update_by, a.update_id, a.update_time, a.del_flag
FROM tb_agreement a
JOIN tb_procurement_contract c ON c.legacy_agreement_id = a.id
WHERE a.party_a_org_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_contract_party p WHERE p.contract_id = c.id AND p.party_type = 1);

INSERT INTO tb_contract_party
  (id, contract_id, party_type, party_id, party_name, need_sign,
   create_by, create_id, create_time, update_by, update_id, update_time, del_flag)
SELECT UUID_SHORT(), c.id, 2, CAST(a.vendor_id AS CHAR), a.party_b_name, 1,
       a.create_by, a.create_id, a.create_time, a.update_by, a.update_id, a.update_time, a.del_flag
FROM tb_agreement a
JOIN tb_procurement_contract c ON c.legacy_agreement_id = a.id
WHERE a.vendor_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM tb_contract_party p WHERE p.contract_id = c.id AND p.party_type = 2);

INSERT INTO tb_contract_sign_task
  (id, contract_id, party_id, platform, external_contract_id, status,
   create_time, update_time, del_flag)
SELECT UUID_SHORT(), c.id, 0, 'qiyuesuo-private', q.contract_id,
       CASE WHEN a.agreement_state = 10 THEN 2 ELSE 0 END,
       a.create_time, a.update_time, a.del_flag
FROM tb_procurement_contract c
JOIN tb_agreement a ON a.id = c.legacy_agreement_id
LEFT JOIN tb_agreement_sign s ON s.business_id = a.id
LEFT JOIN tb_agreement_sign_qys q ON q.sign_id = s.id
WHERE a.agreement_state IN (7, 8, 9, 10)
  AND NOT EXISTS (SELECT 1 FROM tb_contract_sign_task t WHERE t.contract_id = c.id AND t.party_id = 0);

COMMIT;
