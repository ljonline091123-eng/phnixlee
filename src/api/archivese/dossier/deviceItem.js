import request from '@/utils/request'


// 新增设备特征项
export function addDeviceItem(data) {
  return request({
    url: '/archives/deviceItem/add',
    method: 'post',
    data: data
  })
}
// 编辑设备特征项
export function editDeviceItem(data) {
  return request({
    url: '/archives/deviceItem/edit',
    method: 'post',
    data: data
  })
}
// 查询设备特征项详细
export function getDeviceItem(id) {
  return request({
    url: '/archives/deviceItem/' + id,
    method: 'get'
  })
}
// 删除设备特征项
export function delDeviceItem(id) {
  return request({
    url: '/archives/deviceItem/delete/' + id,
    method: 'post'
  })
}






// 查询设备特征项列表
export function listDeviceItem(query) {
  return request({
    url: '/archives/deviceItem/list',
    method: 'get',
    params: query
  })
}




// 修改设备特征项
export function updateDeviceItem(data) {
  return request({
    url: '/archives/deviceItem',
    method: 'put',
    data: data
  })
}

