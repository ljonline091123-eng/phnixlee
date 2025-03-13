import request from '@/utils/request'

// 查询劳务分类列表
export function listLabourType(query) {
  return request({
    url: '/archives/labourType/list',
    method: 'get',
    params: query
  })
}

// 查询劳务分类详细
export function getLabourType(id) {
  return request({
    url: '/archives/labourType/' + id,
    method: 'get'
  })
}

// 新增劳务分类
export function addLabourType(data) {
  return request({
    url: '/archives/labourType',
    method: 'post',
    data: data
  })
}

// 修改劳务分类
export function updateLabourType(data) {
  return request({
    url: '/archives/labourType',
    method: 'put',
    data: data
  })
}

// 删除劳务分类
export function delLabourType(id) {
  return request({
    url: '/archives/labourType/' + id,
    method: 'delete'
  })
}
