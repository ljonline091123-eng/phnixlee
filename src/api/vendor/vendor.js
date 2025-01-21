import request from '@/utils/request'

// 获取供应商列表
export const getVendorList = (params) => {
  return request({
    url: '/business/vendor/management/listVendor',
    method: 'get',
    params
  })
}

// 获取供应商详情
export const getVendorDetail = (id) => {
  debugger;
  return request({
    url: '/business/vendor/management/vendorDetail',
    method: 'get',
    params:{
      id
    }
  })
}

// 修改供应商等级
export const updateVendorLevel = (data) => {
  return request({
    url: '/business/vendor/management/updateVendorLevel',
    method: 'post',
    data
  })
}
// 修改供应商等级
export const saveVendorLevel = (data) => {
  return request({
    url: '/business/vendorChangeLevel/saveVendorLevel',
    method: 'post',
    data
  })
}

// 修改供应商黑名单状态
export const updateBlackState = (data) => {
  return request({
    url: '/business/vendor/management/updateBlackState',
    method: 'post',
    data
  })
}

// 供应商黑名单审批
export const saveVendorBlack = (data) => {
  return request({
    url: '/business/vendorChangeBlack/saveVendorBlack',
    method: 'post',
    data
  })
}

// 查看履约评价
export const listVendorPerformance = (id) => {
  return request({
    url: '/business/vendor/management/listVendorPerformance',
    method: 'get',
    params:{
      id
    }
  })
}

// 获取所有供应商联系人
export const getVendorContactList = (params) => {
  return request({
    url: '/business/vendorContact/management/listPage',
    method: 'get',
    params
  })
}

// 获取供应商合作记录
export const getVendorRecordList = (params) => {
  return request({
    url: '/business/vendor/cooperation/listPage',
    method: 'get',
    params
  })
}

// 更新供应商联系人状态
export const updateContactState = (data) => {
  return request({
    url: '/business/vendorContact/management/updateContactState',
    method: 'post',
    data
  })
}

// 获取联系人授权书
export const getAuthorization = (id) => {
  return request({
    url: '/business/vendorContact/management/getAuthorization',
    method: 'get',
    params:{
      id
    }
  })
}

// 查询供应商合作单位
export const getVendorCooperativePartner = (params) => {
  return request({
    url: '/business/vendor/cooperation/listVendorCooperativePartner',
    method: 'get',
    params
  })
}

// 更新供应商联系人状态
export const updateContactManager = (data) => {
  return request({
    url: '/business/vendorContact/management/updateContactManager',
    method: 'post',
    data
  })
}

/** 获取供应商合作记录 */
export const getCooperationList = (params) => {
  return request({
    url: '/business/vendor/cooperation/listDetail',
    method: 'get',
    params
  })
}
// 获取账户列表查
export const listBankAccountContact=(upId)=> {
  return request({
    url: "/business/account/list",
    method: "get",
    params: {
      upId,
    },
  })
}
// 获取账户列表查
export const listAccountBank = (params)=> {
  return request({
    url: "/business/account/list",
    method: "get",
    params: {
      /* 供应商id */
      upId: params.vendorId,
      /* 银行名称 */
      openingBranch: params.name,
      /* 支行名称 */
      affiliatedBank: params.parentName,
    },
  })
}
// 获取所有银行支行开户账户信息(数据库隔一段时间会同步一次数据)
export function getBankList(params) {
  return request({
    url: "/business/bank/list",
    method: "get",
    params
  })
}
