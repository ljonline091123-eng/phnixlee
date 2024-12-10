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

/**
 * 投标率统计
 * @param query
 * @returns {*}
 */
export function bidCountReport(query) {
  return request({
    url: '/business/report/bidCountReport',
    method: 'get',
    params: query
  })
}

/**
 * 获取组织及以下所有项目编码
 * @param id
 * @returns {*}
 */
export function getProjectCode(id) {
  return request({
    url: '/business/report/getBidCountProjectCode',
    method: 'get',
    params: {id}
  })
}
/**
 * 获取组织
 * @param id
 * @returns {*}
 */
export function getOrgList(id) {
  return request({
    url: '/business/report/getOrgList',
    method: 'get',
    params: {id}
  })
}
