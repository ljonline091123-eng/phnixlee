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
//查询身份证号码查询记录是否存在
export const checkIdentityCardId = (identityCardId) => {
  return request({
    url: '/business/exam/exam/checkIdentityCardId',
    method: 'get',
    params:{
      identityCardId
    }
  })
}
export const repaceWord = (identityCardId) => {
  return request({
    url: '/business/exam/exam/repaceWord',
    method: 'get',
    params:{
      identityCardId
    }
  })
}
export const saveExam = (data) => {
  return request({
    url: '/business/exam/exam/avatar',
    method: 'post',
    data
  })
}
export const ReportExportPDF= () => {
  return request({
    url: 'http://192.168.30.42:9000/wh-hnjt/2024/11/24/examPDF8bb2e548-f01e-452d-85aa-152e1f90b0e8_20241124162419A001.pdf',
    method: 'post',
    responseType: 'blob',
    type: "application/json;chartset=UTF-8"
  })
}
