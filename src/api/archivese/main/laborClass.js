import request from '@/utils/request'

// 查询劳务分类主列表
export function listLaborClass(query) {
  return request({
    url: '/archives/laborClass/list',
    method: 'get',
    params: query
  })
}

// 查询劳务分类主详细
export function getLaborClass(id) {
  return request({
    url: '/archives/laborClass/' + id,
    method: 'get'
  })
}

// 新增劳务分类主
export function addLaborClass(data) {
  return request({
    url: '/archives/laborClass',
    method: 'post',
    data: data
  })
}

// 修改劳务分类主
export function updateLaborClass(data) {
  return request({
    url: '/archives/laborClass',
    method: 'put',
    data: data
  })
}

// 删除劳务分类主
export function delLaborClass(id) {
  return request({
    url: '/archives/laborClass/' + id,
    method: 'delete'
  })
}
