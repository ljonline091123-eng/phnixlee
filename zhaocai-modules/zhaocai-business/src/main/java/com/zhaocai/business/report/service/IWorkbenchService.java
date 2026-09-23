package com.zhaocai.business.report.service;

import java.util.Map;

/**
 * 工作台统计服务
 *
 * @author claude
 */
public interface IWorkbenchService {

    /**
     * 工作台统计数据
     *
     * @param year        年份(招采概览筛选用，空=全部年份)
     * @param orgId       组织id(sys_dept.thrid_dept_id，招采概览筛选用，空=全部单位)
     * @param methodYear  年份(采购方式分析筛选用，空=全部年份)
     * @param methodOrgId 组织id(sys_dept.thrid_dept_id，采购方式分析筛选用，空=全部单位)
     * @return statusCards 五张状态卡 / methodAnalysis 采购方式分析 / overview 概览指标
     *         / monthlyTrend 月度金额趋势 / topVendors 合作金额TOP5供应商
     */
    Map<String, Object> getWorkbench(Integer year, String orgId, Integer methodYear, String methodOrgId);
}
