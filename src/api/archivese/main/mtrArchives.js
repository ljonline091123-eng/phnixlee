import request from '@/utils/request'

// 查询材料详情列表
export function listMaterialDetails(query) {
  return request({
    url: '/archives/mtrArchives/list',
    method: 'get',
    params: query
  })
}

// 查询材料详情详细
export function getMaterialDetails(id) {
  return request({
    url: '/archives/mtrArchives/' + id,
    method: 'get'
  })
}

// 新增材料详情
export function addMaterialDetails(data) {
  return request({
    url: '/archives/mtrArchives/add',
    method: 'post',
    data: data
  })
}

// 修改材料详情
export function updateMaterialDetails(data) {
  return request({
    url: '/archives/mtrArchives/edit',
    method: 'post',
    data: data
  })
}

// 删除材料详情
export function delMaterialDetails(id) {
  return request({
    url: '/archives/mtrArchives/delete/' + id,
    method: 'POST'
  })
}
