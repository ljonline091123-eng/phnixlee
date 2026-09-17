import request from '@/utils/request'

/**
 * 采购台账-汇总视图聚合查询
 * @param query {id(orgId,thrid_dept_id), projectKeyword, demandType}
 * @returns {*}
 */
export function getPurchaseLedgerSummary(query) {
  return request({
    url: '/business/report/purchaseLedgerSummary',
    method: 'get',
    params: query
  })
}

/**
 * 采购台账-明细宽表查询
 * @param query {id, status, projectCode, demandType, method, keyword, currentStage, purchaser, handler, planFinishBegin/End, pageNum, pageSize}
 * @returns {*}
 */
export function getPurchaseLedgerList(query) {
  return request({
    url: '/business/report/purchaseLedgerList',
    method: 'get',
    params: query
  })
}

// 导出（汇总/明细）不走本文件：与其它报表一致，用全局 this.download(url, params, filename)
// 直连 business/report/purchaseLedgerSummaryExport、business/report/purchaseLedgerExport（POST，返回 Excel 流）
