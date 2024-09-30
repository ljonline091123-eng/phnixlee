import request from '@/utils/request'

// 获取专家列表
export const getExpertList = (data) => {
  return request({
    url: '/business/expert/page',
    method: 'post',
    data
  })
}

/** 获取其它模块维护的专家 */
export const getTPIExpertInfo = (params) => {
  return request({
    url: '/business/expert/getTPIExpertInfo',
    method: 'get',
    params
  })
}

/** 新增专家 */
export const addExpert = (data) => {
  return request({
    url: '/business/expert/add',
    method: 'post',
    data
  })
}