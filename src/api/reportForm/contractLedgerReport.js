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
export function contractLedgerByConBase(query) {
  return request({
    url: '/business/report/contractLedgerByConBase',
    method: 'get',
    params: query
  })
}
export function contractLedgerDetails(id) {
  return request({
    url: '/business/report/contractLedgerDetails',
    method: 'get',
    params: {id}
  })
}

