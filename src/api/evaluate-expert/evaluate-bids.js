import request from '@/utils/request'

// 获取专家列表
export const getTodoEvalTaskList = (data) => {
  return request({
    url: '/business/expertEvaluation/todoEvalTaskPage',
    method: 'post',
    data
  })
}

/** 获取评分模板 */
export const getMarkTempInfo = (schemeId) => {
  return request({
    url: '/business/expertEvaluation/getMarkTempInfo',
    method: 'post',
    params:{
      schemeId
    }
  })
}

// 获取专家列表
export const expertEvaluation = (data) => {
  return request({
    url: '/business/expertEvaluation/eval',
    method: 'post',
    data
  })
}

// 获取已评标列表
export const getDoneEvalTaskPage = (data) => {
  return request({
    url: '/business/expertEvaluation/doneEvalTaskPage',
    method: 'post',
    data
  })
}


// 获取专家评分数据
export const getExpertEvalData = (params) => {
  return request({
    url: '/business/expertEvaluation/getExpertEvalData',
    method: 'post',
    params
  })
}

// 获取专家评分记录
export const getExpertEvalRecord = (params) => {
  return request({
    url: '/business/expertEvaluation/getExpertEvalRecord',
    method: 'post',
    params
  })
}
