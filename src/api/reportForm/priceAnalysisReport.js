import request from '@/utils/request'

/**
 * 价格分析统计
 * @param query
 * @returns {*}
 */
export function priceAnalysisReport(query) {
  return request({
    url: '/business/report/priceAnalysisReport',
    method: 'get',
    params: query
  })
}
export function getPriceAnalysisReportByCon(query) {
  return request({
    url: '/business/report/priceAnalysisReportByCon',
    method: 'get',
    params: query
  })
}
