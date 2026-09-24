import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, getUnifiedLoginUrl } from '@/utils/auth'
import { isRelogin } from '@/utils/request'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (whiteList.indexOf(to.path) !== -1) {
      next()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        store.dispatch('GetInfo').then(() => {
          // 用户信息和权限加载完成后再请求菜单；GenerateRoutes 内部会处理短暂的鉴权时序差异。
          return store.dispatch('GenerateRoutes')
        }).then(accessRoutes => {
          isRelogin.show = false
          // 根据 roles 权限生成可访问的路由表
          router.addRoutes(accessRoutes)
          next({ ...to, replace: true })
        }).catch(err => {
          isRelogin.show = false
          store.dispatch('FedLogOut').finally(() => {
            Message.error(err && err.message ? err.message : (err || '登录信息加载失败'))
            next({ path: '/login', query: { redirect: to.fullPath } })
          })
        })
      } else {
        next()
      }
    }
  } else {
    // 没有token
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单（/login 旧登录页保留备用、/register），直接进入
      next()
    } else {
      // 统一登录：未登录访问其余页面，整页跳转到供应商端统一登录页
      location.href = getUnifiedLoginUrl()
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
