import request from '@/utils/request'

// 查询评分列表
export function listRating(data) {
  return request({
    url: '/business/markTemplate/page',
    method: 'post',
    data
  })
}
export function getFanList(data) {
  return request({
    url: '/business/markTemplate/fanListPage',
    method: 'post',
    data
  })
}
// 查询评分详细
export function getRating(params) {
  return request({
    url: '/business/markTemplate/detail',
    method: 'get',
    params
  })
}

// 新增评分
export function addUpdateRating(data) {
  return request({
    url: '/business/markTemplate/addOrUpdate',
    method: 'post',
    data
  })
}


// 删除评分
export function delRating(data) {
  return request({
    url: '/business/markTemplate/delete',
    method: 'post',
    params:{
      id:data
    }
  })
}

// 修改启用状态
export function updateStatus(id,state) {
  return request({
    url: `/business/markTemplate/updateStatus/${id}/${state}`,
    method: 'post',
  })
}
