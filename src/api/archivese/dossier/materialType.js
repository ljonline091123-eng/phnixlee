import request from '@/utils/request'

// 流程提交
export function processAuditPass(query,url) {
  return request({
    // url: '/archives/subcontractingType/processAuditPass',
    url: `${url}`,
    method: 'post',
    data: query
  })
}
// 初始化材料类型详情表
export function materialDetailsInitData(query,url) {
  return request({
    // url: '/archives/materialDetails/initData',//labourDetails/initData
    url: `${url}/initData`,
    method: 'get',
    params: query
  })
}
// 初始化副库材料特征项
export function materialInitData(query,url) {
  return request({
    // url: '/archives/materialItem/initData',
    url: `${url}/initData`,
    method: 'get',
    params: query
  })
}
// 查询材料分类详细
export function getMaterialTypeTree(query,url) {
  return request({
    // url: '/archives/materialType/getMaterialTypeTree',
    url: `${url}`,
    method: 'get',
    params: query
  })
}
// 新增设备类型获取设备编码
export function initMaterialType(query,url) {
  return request({
    // url: '/archives/materialType/initMaterialType',
    url: `${url}`,
    method: 'get',
    params: query
  })
}
// 新增材料类型
export function materialAdd(query,url) {
  return request({
    // url: '/archives/materialType/add',
    url: `${url}/add`,
    method: 'post',
    data: query
  })
}
// 查询材料分类详细
export function getMaterialType(id,url) {
  return request({
    // url: '/archives/materialType/' + id,
    url: `${url}/`+id,
    method: 'get'
  })
}

// 新增材料类型
export function materialEdit(query,url) {
  return request({
    // url: '/archives/materialType/edit',
    url: `${url}/edit`,
    method: 'post',
    data: query
  })
}
// 删除材料类型
export function materialDelete(id,url) {
  return request({
    // url: '/archives/materialType/delete/' + id,
    url: `${url}/delete/`+ id,
    method: 'post',
  })
}



// 获取副库材料特征项列表
export function getMaterialItemList(query,url) {
  return request({
    // url: '/archives/materialItem/list',
    url: `${url}/list`,
    method: 'get',
    params: query
  })
}
//
export function getMaterialDetailsList(query,url) {
  return request({
    // url: '/archives/materialDetails/list',
    url: `${url}/list`,
    method: 'get',
    params: query
  })
}

// 查询材料分类详细
export function initDeviceTypea(query) {
  return request({
    url: '/archives/deviceType/initDeviceType',
    method: 'get',
    params: query
  })
}











// 查询材料分类列表
export function listMaterialType(query) {
  return request({
    url: '/archives/materialType/list',
    method: 'get',
    params: query
  })
}



// 新增材料分类
export function addMaterialType(data) {
  return request({
    url: '/archives/materialType',
    method: 'post',
    data: data
  })
}

// 修改材料分类
export function updateMaterialType(data) {
  return request({
    url: '/archives/materialType',
    method: 'put',
    data: data
  })
}

// 删除材料分类
export function delMaterialType(id) {
  return request({
    url: '/archives/materialType/' + id,
    method: 'delete'
  })
}
// 根据组织编号判断是否可编辑，且返回所属二级机构编码
export function getSecondaryUnit(code) {
  return request({
    url: '/archives/materialType/getSecondaryUnit?organCode=' + code,
    method: 'get'
  })
}