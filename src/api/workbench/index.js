import request from '@/utils/request'

/**
 * 工作台统计数据
 * 返回 statusCards(五张状态卡) / methodAnalysis(采购方式分析) / overview(概览指标)
 *      / monthlyTrend(月度金额趋势) / topVendors(合作金额TOP5供应商)
 * @param {Object} query { year: 年份(概览筛选用，空=全部年份), orgId: 组织id(sys_dept.thrid_dept_id，概览筛选用) }
 */
export function getWorkbenchStats(query) {
  return request({
    url: '/business/report/workbench',
    method: 'get',
    params: query
  })
}
