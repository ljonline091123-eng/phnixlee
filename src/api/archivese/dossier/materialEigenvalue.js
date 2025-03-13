import request from '@/utils/request'

// 查询材料特征值列表
export function listMaterialEigenvalue(query) {
  return request({
    url: '/archives/materialEigenvalue/list',
    method: 'get',
    params: query
  })
}

// 查询材料特征值详细
export function getMaterialEigenvalue(id) {
  return request({
    url: '/archives/materialEigenvalue/' + id,
    method: 'get'
  })
}

// 新增材料特征值
export function addMaterialEigenvalue(data) {
  return request({
    url: '/archives/materialEigenvalue',
    method: 'post',
    data: data
  })
}

// 修改材料特征值
export function updateMaterialEigenvalue(data) {
  return request({
    url: '/archives/materialEigenvalue',
    method: 'put',
    data: data
  })
}

// 删除材料特征值
export function delMaterialEigenvalue(id) {
  return request({
    url: '/archives/materialEigenvalue/' + id,
    method: 'delete'
  })
}
