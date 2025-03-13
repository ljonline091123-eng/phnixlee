import request from '@/utils/request'

//  编辑特征项
export function materialItemEdit(query) {
  return request({
    url: '/archives/subcontractingItem/edit',
    method: 'post',
    data: query
  })
}
// 新增特征项
export function materialItemAdd(query) {
  return request({
    url: '/archives/subcontractingItem/add',
    method: 'post',
    data: query
  })
}
// 查询特征项
export function getMaterialItem(id) {
  return request({
    url: '/archives/subcontractingItem/' + id,
    method: 'get'
  })
}
// 删除材料类型
export function materialItemDelete(id) {
  return request({
    url: '/archives/subcontractingItem/delete/' + id,
    method: 'post',
  })
}