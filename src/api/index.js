import request from '@/utils/request'



/** 获取未处理信息 */
export const geTaskTodoList = (params) => {
  return request({
    url: '/flowable/task/todoList',
    method: 'get',
    params
  })
}
