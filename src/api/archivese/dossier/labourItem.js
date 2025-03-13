import request from '@/utils/request'

//  编辑特征项
export function materialItemEdit(query) {
  return request({
    url: '/archives/labourItem/edit',
    method: 'post',
    data: query
  })
}
// 新增特征项
export function materialItemAdd(query) {
  return request({
    url: '/archives/labourItem/add',
    method: 'post',
    data: query
  })
}
// 查询特征项
export function getMaterialItem(id) {
  return request({
    url: '/archives/labourItem/' + id,
    method: 'get'
  })
}
// 删除材料类型
export function materialItemDelete(id) {
  return request({
    url: '/archives/labourItem/delete/' + id,
    method: 'post',
  })
}