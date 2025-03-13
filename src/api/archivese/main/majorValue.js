import request from '@/utils/request'
//初始化付款材料特征值
export function materialEigenvalueInitData(query) {
  return request({
    url: '/archives/majorValue/initData',
    method: 'get',
    params: query
  })
}

// 获取副库材料特征值列表
export function getMaterialEigenvalueList(query) {
  return request({
    url: '/archives/majorValue/list',
    method: 'get',
    params: query
  })
}
// 新增特征值
export function materialEigenvalueAdd(query) {
  return request({
    url: '/archives/majorValue/add',
    method: 'post',
    data: query
  })
}
//  编辑特征值
export function materialEigenvalueEdit(query) {
  return request({
    url: '/archives/majorValue/edit',
    method: 'post',
    data: query
  })
}
// 查询特征值
export function getEigenvalue(id) {
  return request({
    url: '/archives/majorValue/' + id,
    method: 'get'
  })
}
// 删除特征值
export function materialEigenvalueDelete(id) {
  return request({
    url: '/archives/majorValue/delete/' + id,
    method: 'post',
  })
}