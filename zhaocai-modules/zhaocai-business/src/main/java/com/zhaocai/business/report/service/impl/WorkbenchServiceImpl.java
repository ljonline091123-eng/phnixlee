package com.zhaocai.business.report.service.impl;

import com.zhaocai.business.report.mapper.WorkbenchMapper;
import com.zhaocai.business.report.service.IWorkbenchService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计服务
 *
 * 取数口径（2026-09-16 与业务确认，勿改动）：
 * 1. 五张状态卡   = 采购台账 tb_purchase_ledger 按 status 计数（含 SPLIT/TASK 全部粒度）；
 * 2. 采购方式分析 = 招标率统计 tb_bid_report 合计 公开/邀标/询价/单一次数，
 *                  视图口径=方案完成+公告完成，即只统计已完成的采购；
 * 3. 招采概览     = 采购台账已完成(grain=TASK, status=completed)的数据：
 *                  采购预算=控制金额合计、采购金额=中标/成交金额(含税)合计、
 *                  成本节约率=(预算-金额)/预算；月度趋势按招标完成时间聚合
 *                  （招标完成时间=中标结果发布时写入 tb_bidding_result.notifi_time）；
 * 4. TOP5供应商   = 供应商报表 tb_vendor_report 按供应商汇总 合同签订金额(含税) 取前5。
 *
 * 筛选：单位树+年份只作用于招采概览；其余模块只按数据权限范围（ReportScopeUtil，
 * 集团账号=全部，其他=本人组织及下级）收敛，与各报表口径一致。
 *
 * @author claude
 */
@Service
public class WorkbenchServiceImpl implements IWorkbenchService {

    @Autowired
    private WorkbenchMapper workbenchMapper;

    @Override
    public Map<String, Object> getWorkbench(Integer year, String orgId) {
        // 数据权限上限：集团账号为 null(不限制)，其他账号为本人所属组织(thrid_dept_id)
        String scopeId = ReportScopeUtil.getScopeThridDeptId();
        // 概览筛选组织：前端传参收敛到权限范围内（集团可选任意单位，其他固定本人组织）
        String overviewOrgId = ReportScopeUtil.clampOrgId(orgId);
        if (overviewOrgId == null || overviewOrgId.isEmpty()) {
            overviewOrgId = scopeId;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statusCards", fillZero(workbenchMapper.selectStatusCount(scopeId)));
        result.put("methodAnalysis", fillZero(workbenchMapper.selectMethodCount(scopeId)));
        // 概览指标与月度趋势用同一份聚合结果，保证数字一致
        List<Map<String, Object>> monthlyList = workbenchMapper.selectMonthlyAmount(overviewOrgId, year);
        result.put("overview", buildOverview(monthlyList));
        result.put("monthlyTrend", buildMonthlyTrend(monthlyList));
        result.put("topVendors", workbenchMapper.selectTopVendors(scopeId));
        return result;
    }

    /**
     * 概览指标：由月度聚合结果汇总（同一份数据，保证指标与趋势图一致）
     * 成本节约率 = (预算-金额)/预算*100，预算为0或空时不展示(null)
     */
    private Map<String, Object> buildOverview(List<Map<String, Object>> monthlyList) {
        long purchaseCount = 0L;
        BigDecimal budgetAmount = BigDecimal.ZERO;
        BigDecimal awardAmount = BigDecimal.ZERO;
        for (Map<String, Object> row : monthlyList) {
            purchaseCount += row.get("purchaseCount") == null ? 0L : ((Number) row.get("purchaseCount")).longValue();
            budgetAmount = budgetAmount.add(toBigDecimal(row.get("budgetAmount")));
            awardAmount = awardAmount.add(toBigDecimal(row.get("awardAmount")));
        }
        Map<String, Object> overview = new HashMap<>();
        overview.put("purchaseCount", purchaseCount);
        overview.put("budgetAmount", budgetAmount);
        overview.put("awardAmount", awardAmount);
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            overview.put("savingRate", budgetAmount.subtract(awardAmount)
                    .multiply(new BigDecimal("100")).divide(budgetAmount, 2, RoundingMode.HALF_UP));
        } else {
            overview.put("savingRate", null);
        }
        return overview;
    }

    /**
     * 月度趋势：补全 1-12 月（无数据的月份补0），返回固定 12 行
     */
    private List<Map<String, Object>> buildMonthlyTrend(List<Map<String, Object>> monthlyList) {
        Map<Integer, Map<String, Object>> byMonth = new HashMap<>();
        for (Map<String, Object> row : monthlyList) {
            if (row.get("monthNo") != null) {
                byMonth.put(((Number) row.get("monthNo")).intValue(), row);
            }
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> row = byMonth.get(month);
            Map<String, Object> item = new HashMap<>();
            item.put("monthNo", month);
            item.put("budgetAmount", row == null ? BigDecimal.ZERO : toBigDecimal(row.get("budgetAmount")));
            item.put("awardAmount", row == null ? BigDecimal.ZERO : toBigDecimal(row.get("awardAmount")));
            trend.add(item);
        }
        return trend;
    }

    /**
     * 状态卡/方式分析聚合结果补0（无数据时 SQL 返回 NULL，前端直接用数字）
     */
    private Map<String, Object> fillZero(Map<String, Object> map) {
        if (map == null) {
            map = new HashMap<>();
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(0);
            }
        }
        return map;
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
    }
}
