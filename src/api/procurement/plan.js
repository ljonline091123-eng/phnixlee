import request from '@/utils/request'

// 查询物料分类树
export const getArchiveClass = (type) => {
  return request({
    url: '/archives/archivesClass/getArchivesTree',
    method: 'get',
    params:{
      type
    }
  })
}
// 选取物料
export const getArchivesDetailList = (classId,type) => {
  return request({
     url: '/archives/archivesClass/getArchivesDetailList',
      method: 'get',
      params:{
          classId,type
      }
      })
      }

// 采购计划列表
export const getPlanList = (params) => {
  return request({
    url: '/business/procurementPlan/listPage',
    method: 'get',
    params
  })
}

// 采购总计划列表
export const getMasterPlanningList = (params) => {
  return request({
    url: '/business/procurementPlan/contractPlanningList',
    method: 'get',
    params
  })
}

// 获取采购计划拆分列表
export const getSplitPlanList = (params) => {
  return request({
    url: '/business/procurementPlan/listPlanContractSplit',
    method: 'get',
    params
  })
}

// 获取合约规划列表
export const getContractPlanningList = (params) => {
  return request({
    url: '/business/procurementPlan/contractPlanningListPage',
    method: 'get',
    params
  })
}

// 获取合约规划清单
export const getContractMaterials = (conPlanId,projectId,procurementType,conPlanCode,projectName) => {
  return request({
    url: '/business/procurementPlan/listContractMaterials',
    method: 'get',
    params:{
      conPlanId,
      projectId,
      procurementType,
      conPlanCode,
      projectName
    }
  })
}


// 保存采购计划
export const saveProcurementPlan = (data) => {
  return request({
    url: '/business/procurementPlan/saveProcurementPlan',
    method: 'post',
    data
  })
}

// 提交采购计划
export const submitProcurementPlan = (id) => {
  return request({
    url: '/business/procurementPlan/submitProcurementPlan',
    method: 'post',
    params:{
      id
    }
  })
}

// 作废采购计划
export const cancellationProcurementPlan = (id) => {
  return request({
    url: '/business/procurementPlan/cancellationProcurementPlan',
    method: 'post',
    params:{
      id
    }
  })
}

// 获取采购计划详情
export const getPlanDetail = (id) => {
  return request({
    url: '/business/procurementPlan/detail',
    method: 'get',
    params:{
      id
    }
  })
}

// 获取采购经办人
export const getListProcurementOfficer = (deptId) => {
  return request({
    url: 'system/user/listUserByDeptId?pageNum=1&pageSize=1000000&deptId=' + deptId,
    method: 'get'
  })
}

// 获取最小核酸项目
export const getMinProject = (projectCode) => {
  return request({
    url: '/business/minProject/getMinProject',
    method: 'get',
    params:{
      projectCode
    }
  })
}

// 获取交易标的物
export const listDwMmServiceSubjectMatter = () => {
  return request({
    url: '/business/dwMmInfo/listDwMmServiceSubjectMatter',
    method: 'get'
  })
}

// 获取推送角色用户
export const getUsersRoleList = () => {
  return request({
    url: '/business/platRole/getUsersRoleList',
    method: 'get'
  })
}

// 获取推送角色用户
export const getUsersRoleContractPlanList = (projectCode,contractPlanningCode,contractPlanningId,contractIdList) => {
  return request({
    url: '/business/procurementPlan/getUsersRoleContractPlanList',
    method: 'get',
    params:{
      projectCode,/* 项目编码 */
      contractPlanningCode,/* 合约规划编码 */
      contractPlanningId,/* 合约规划id */
      contractIdList,/* 合约规划id 集合 */
    }
  })
}

//推送
export const pushProcurementPlan = (data) => {
  return request({
    url: '/business/procurementPlan/pushProcurementPlan',
    method: 'post',
    data
  })
}

//易料订单
export const getMarkeyList = (data) => {
  return request({
    url: '/business/marketMaterialsList/getList',
    method: 'post',
    data
  })
}

// 设置合约是否可拆分
export const setContractPlanSplitFlag = (flag) => {
  return request({
    url: '/business/procurementPlan/setContractPlanSplitFlag',
    method: 'post',
    params:{
      flag
    }
  })
}

// 设置合约是否可拆分标识
export const getContractPlanSplitFlag = () => {
  return request({
    url: '/business/procurementPlan/getContractPlanSplitFlag',
    method: 'get'
  })
}

// 推送易料市集采购
export const pushMaterialProcurementList = (data) => {
  return request({
    url: '/business/procurementPlan/pushMaterialProcurementList',
    method: 'post',
    data
  })
}


// 推送易料市集采购
export const revokePushMaterialProcurementList = (data) => {
  return request({
    url: '/business/procurementPlan/revokePushMaterialProcurementList',
    method: 'post',
    data
  })
}
// 获取url
export const getYjtUrl = (code) => {
  return request({
    url: '/business/procurementPlan/getYjtUrl',
    method: 'get',
    params:{
      code,type:1
    }
  })
}
