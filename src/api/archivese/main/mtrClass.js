import request from '@/utils/request'

// 查询材料分类主列表
export function listMtrClass(query) {
  return request({
    url: '/archives/mtrClass/list',
    method: 'get',
    params: query
  })
}

// 查询材料分类主详细
export function getMtrClass(id) {
  return request({
    url: '/archives/mtrClass/' + id,
    method: 'get'
  })
}

// 新增材料分类主
export function addMtrClass(data) {
  return request({
    url: '/archives/mtrClass',
    method: 'post',
    data: data
  })
}

// 修改材料分类主
export function updateMtrClass(data) {
  return request({
    url: '/archives/mtrClass',
    method: 'put',
    data: data
  })
}

// 删除材料分类主
export function delMtrClass(id) {
  return request({
    url: '/archives/mtrClass/' + id,
    method: 'delete'
  })
}
