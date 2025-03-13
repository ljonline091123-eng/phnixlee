import request from '@/utils/request'

// 查询设备分类主列表
export function listDeviceClass(query) {
  return request({
    url: '/archives/deviceClass/list',
    method: 'get',
    params: query
  })
}

// 查询设备分类主详细
export function getDeviceClass(id) {
  return request({
    url: '/archives/deviceClass/' + id,
    method: 'get'
  })
}

// 新增设备分类主
export function addDeviceClass(data) {
  return request({
    url: '/archives/deviceClass',
    method: 'post',
    data: data
  })
}

// 修改设备分类主
export function updateDeviceClass(data) {
  return request({
    url: '/archives/deviceClass',
    method: 'put',
    data: data
  })
}

// 删除设备分类主
export function delDeviceClass(id) {
  return request({
    url: '/archives/deviceClass/' + id,
    method: 'delete'
  })
}
