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