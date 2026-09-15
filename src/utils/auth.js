import Cookies from 'js-cookie'

const TokenKey = 'Admin-Token-ZC'

const ExpiresInKey = 'Admin-Expires-In'

export function getToken() {
  return Cookies.get(TokenKey)
}

export function setToken(token) {
  return Cookies.set(TokenKey, token)
}

export function removeToken() {
  return Cookies.remove(TokenKey)
}

export function getExpiresIn() {
  return Cookies.get(ExpiresInKey) || -1
}

export function setExpiresIn(time) {
  return Cookies.set(ExpiresInKey, time)
}

export function removeExpiresIn() {
  return Cookies.remove(ExpiresInKey)
}

export function setControlToken(token) {
  return Cookies.set('Master-Control-Token', token)
}

export function getControlToken(token) {
  return Cookies.get('Master-Control-Token', token)
}

export function removeControlToken() {
  return Cookies.remove('Master-Control-Token')
}

export function getZcToken(token) {
  return Cookies.get('Admin-Token-XWCK', token)
}

// 统一登录页地址（供应商端），招采端退出/会话过期时跳转
export function getUnifiedLoginUrl() {
  // 环境变量缺失时兜底为旧登录页，避免跳转地址为 undefined 造成循环刷新
  return process.env.VUE_APP_UNIFIED_LOGIN_URL || process.env.BASE_URL + 'login'
}

// 供应商端token cookie（同host不同端口共享），退出时一并清理，避免跳转后残留失效会话
export function removePortalToken() {
  return Cookies.remove('Portal-Token')
}