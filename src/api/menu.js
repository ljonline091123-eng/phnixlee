import request from '@/utils/request'

//获取是否显示菜单
// 获取路由
export const getMenuStatus = (configKey) => {
  return request({
    url: `/system/config/configKeyStr/${configKey}`,
    method: 'get'
  })
}

// 获取路由
export const getRouters = () => {
  return request({
    url: '/system/menu/getRouters',
    method: 'get'
  })
}