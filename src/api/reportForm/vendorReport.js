import request from '@/utils/request'

/**
 * 供应商报表
 * @param query
 * @returns {*}
 */
export function getVendorReport(query) {
  return request({
    url: '/business/report/vendorReport',
    method: 'get',
    params: query
  })
}

/**
 * 问题报表-供应商评价不合格记录(tab2)
 * @param query
 * @returns {*}
 */
export function getEvaluationBadReport(query) {
  return request({
    url: '/business/report/evaluationBadReport',
    method: 'get',
    params: query
  })
}

/**
 * 问题报表-异常报表(初始化)
 * @param query
 * @returns {*}
 */
export function getProblemReport(query) {
  return request({
    url: '/business/report/problemReport',
    method: 'get',
    params: query
  })
}
