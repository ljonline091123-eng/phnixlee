import request from '@/utils/request'


// 获取一二级单位树
export function getWaitDeptTree(id) {
  return request({
    url: '/archives/materialType/getDeptTree',
    method: 'get'
  })
}
// 新增至主库
export function addToMain(data,url) {
  return request({
    // url: '/archives/materialType/addToMain',
    url: `${url}/addToMain`,
    method: 'post',
    data: data
  })
}
// 关联至主库
export function associationToMain(data,url) {
  return request({
    // url: '/archives/materialType/associationToMain',
    url: `${url}/associationToMain`,
    method: 'post',
    data: data
  })
}
export function unAssociationToMain(data,url) {
  return request({
    // url: '/archives/materialType/associationToMain',
    url: `${url}/unAssociationToMain`,
    method: 'post',
    data: data
  })
}
