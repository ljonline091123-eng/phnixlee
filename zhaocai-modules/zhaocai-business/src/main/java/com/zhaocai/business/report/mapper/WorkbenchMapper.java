package com.zhaocai.business.report.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 工作台统计 Mapper
 *
 * 全部为只读聚合查询，数据源与四个报表的物化表保持一致：
 * tb_purchase_ledger(采购台账) / tb_bid_report(招标率统计) / tb_vendor_report(供应商报表)
 *
 * @author claude
 */
public interface WorkbenchMapper {

    /** 五张状态卡：采购台账按状态计数（待招采/待开标/待定标/已完成/异常终止） */
    Map<String, Object> selectStatusCount(@Param("scopeId") String scopeId);

    /**
     * 采购方式分析：采购台账已完成任务按采购方式计数（1公开 2邀标 3询价 4单一），
     * 时间口径同招采概览(招标完成时间 notifi_time)，支持单位+年份筛选
     * 返回 method / cnt
     */
    List<Map<String, Object>> selectMethodCount(@Param("orgId") String orgId, @Param("year") Integer year);

    /**
     * 招采概览-月度金额：已完成任务按招标完成时间(中标结果发布 notifi_time)聚合
     * 返回 monthNo(1-12) / purchaseCount / budgetAmount(采购预算) / awardAmount(采购金额)
     */
    List<Map<String, Object>> selectMonthlyAmount(@Param("orgId") String orgId, @Param("year") Integer year);

    /** 合作金额TOP5供应商：供应商报表按供应商汇总合同签订金额(含税)取前5 */
    List<Map<String, Object>> selectTopVendors(@Param("scopeId") String scopeId);
}
