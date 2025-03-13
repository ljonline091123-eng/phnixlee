import request from '@/utils/request'

// 查询材料详情列表
export function listMaterialDetails(query) {
  return request({
    url: '/archives/laborArchives/list',
    method: 'get',
    params: query
  })
}

// 初使化
export function initDetails(query) {
  return request({
    url: '/archives/laborArchives/initDetails',
    method: 'get',
    params: query
  })
}
// 查询材料详情详细
export function getMaterialDetails(id) {
  return request({
    url: '/archives/laborArchives/' + id,
    method: 'get'
  })
}

// 新增材料详情
export function addMaterialDetails(data) {
  return request({
    url: '/archives/laborArchives/add',
    method: 'post',
    data: data
  })
}

// 修改材料详情
export function updateMaterialDetails(data) {
  return request({
    url: '/archives/laborArchives/edit',
    method: 'post',
    data: data
  })
}

// 删除材料详情
export function delMaterialDetails(id) {
  return request({
    url: '/archives/laborArchives/delete/' + id,
    method: 'POST'
  })
}
