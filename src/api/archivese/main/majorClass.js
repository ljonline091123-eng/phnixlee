import request from '@/utils/request'

// 查询专业分包分类主列表
export function listMajorClass(query) {
  return request({
    url: '/archives/majorClass/list',
    method: 'get',
    params: query
  })
}

// 查询专业分包分类主详细
export function getMajorClass(id) {
  return request({
    url: '/archives/majorClass/' + id,
    method: 'get'
  })
}

// 新增专业分包分类主
export function addMajorClass(data) {
  return request({
    url: '/archives/majorClass',
    method: 'post',
    data: data
  })
}

// 修改专业分包分类主
export function updateMajorClass(data) {
  return request({
    url: '/archives/majorClass',
    method: 'put',
    data: data
  })
}

// 删除专业分包分类主
export function delMajorClass(id) {
  return request({
    url: '/archives/majorClass/' + id,
    method: 'delete'
  })
}
