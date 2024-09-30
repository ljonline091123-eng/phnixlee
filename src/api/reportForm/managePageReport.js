import request from '@/utils/request'

/**
 * 报表查询
 * @param query
 * @returns {*}
 */
export function managePageReport(query) {
  return request({
    url: '/business/report/managePageReport',
    method: 'get',
    params: query
  })
}

export function deptTree(query) {
  return request({
    url: '/business/report/managePageReportDept',
    method: 'get',
    params: query
  })
}


