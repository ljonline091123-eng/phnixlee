import request from '@/utils/request'
//初始化付款材料特征值
export function materialEigenvalueInitData(query) {
  return request({
    url: '/archives/laborValue/initData',
    method: 'get',
    params: query
  })
}

// 获取副库材料特征值列表
export function getMaterialEigenvalueList(query) {
  return request({
    url: '/archives/laborValue/list',
    method: 'get',
    params: query
  })
}
// 新增特征值
export function materialEigenvalueAdd(query) {
  return request({
    url: '/archives/laborValue/add',
    method: 'post',
    data: query
  })
}
//  编辑特征值
export function materialEigenvalueEdit(query) {
  return request({
    url: '/archives/laborValue/edit',
    method: 'post',
    data: query
  })
}
// 查询特征值
export function getEigenvalue(id) {
  return request({
    url: '/archives/laborValue/' + id,
    method: 'get'
  })
}
// 删除特征值
export function materialEigenvalueDelete(id) {
  return request({
    url: '/archives/laborValue/delete/' + id,
    method: 'post',
  })
}