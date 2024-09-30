import request from '@/utils/request'

/**
 * 合同台帐统计
 * @param query
 * @returns {*}
 */
export function contractLedgerReport(query) {
  return request({
    url: '/business/report/contractLedgerReport',
    method: 'get',
    params: query
  })
}
