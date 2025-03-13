import request from '@/utils/request'


// 根据id获取流程辅助信息
export function getMaterialApprove(id) {
  return request({
    url: '/archives/materialApprove/' + id,
    method: 'get'
  })
}

// 获取审批权限
export const getPermissionButton = (params) => {
  return request({
    url: "/archives/materialApprove/initialize",
    method: "get",
    params,
  });
};


// 审批
export function materialApprove(data) {
  return request({
    url: '/archives/materialApprove/audit',
    method: 'post',
    data: data
  })
}
//审批详情
export function archivesLoadTaskDef(data) {
  return request({
    url: '/business/process/loadTaskDef',
    method: 'post',
    data: data
  })
}

// 新增专业分包分类
export function addSubcontractingType(data) {
  return request({
    url: '/archives/subcontractingType',
    method: 'post',
    data: data
  })
}

// 修改专业分包分类
export function updateSubcontractingType(data) {
  return request({
    url: '/archives/subcontractingType',
    method: 'put',
    data: data
  })
}

// 删除专业分包分类
export function delSubcontractingType(id) {
  return request({
    url: '/archives/subcontractingType/' + id,
    method: 'delete'
  })
}
