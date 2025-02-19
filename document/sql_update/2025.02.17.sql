#  采购方案增加报名截止时间、招标公告附件字段
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_procurement_scheme_bidding ADD apply_time_notice datetime NULL COMMENT '发布公告报名截止时间';
ALTER TABLE `hnjiantou-zhaocai-dev`.tb_procurement_scheme_bidding ADD notice_attachment_id bigint NULL COMMENT '招标公告附件 id';
