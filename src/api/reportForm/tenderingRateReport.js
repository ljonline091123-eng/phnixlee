import request from '@/utils/request'

/**
 * 投标率统计
 * @param query
 * @returns {*}
 */
export function tenderingRateReport(query) {
  return request({
    url: '/business/report/tenderingRateReport',
    method: 'get',
    params: query
  })
}
