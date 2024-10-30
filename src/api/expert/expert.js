import request from '@/utils/request'

// 获取审批通过专家列表
export const getExpertList = (data) => {
  return request({
    url: '/business/expert/page',
    method: 'post',
    data
  })
}
// 获取所有专家列表
export const getExpertListAll = (data) => {
  return request({
    url: '/business/expert/pageAll',
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
export const submitExpert = (data) => {
  return request({
    url: '/business/expert/submit',
    method: 'post',
    data
  })
}
export const saveExpert = (data) => {
  return request({
    url: '/business/expert/save',
    method: 'post',
    data
  })
}

//getInfo?id=
export const getInfo = (id) => {
  return request({
    url: '/business/expert/getInfo',
    method: 'get',
    params:{
      id
    }
  })
}
