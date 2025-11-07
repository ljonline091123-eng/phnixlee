import request from "@/utils/request";
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询用户列表
export function listUser(query) {
  return request({
    url: "/system/user/list",
    method: "get",
    params: query,
  });
}

// 查询用户详细
export function getUser(userId) {
  return request({
    url: "/system/user/" + parseStrEmpty(userId),
    method: "get",
  });
}

// 新增用户
export function addUser(data) {
  return request({
    url: "/system/user",
    method: "post",
    data: data,
  });
}

// 修改用户
export function updateUser(data) {
  return request({
    url: "/system/user",
    method: "put",
    data: data,
  });
}

// 删除用户
export function delUser(userId) {
  return request({
    url: "/system/user/" + userId,
    method: "delete",
  });
}

// 用户密码重置
export function resetUserPwd(userId, password) {
  const data = {
    userId,
    password,
  };
  return request({
    url: "/system/user/resetPwd",
    method: "put",
    data: data,
  });
}

// 用户状态修改
export function changeUserStatus(userId, status) {
  const data = {
    userId,
    status,
  };
  return request({
    url: "/system/user/changeStatus",
    method: "put",
    data: data,
  });
}

// 查询用户个人信息
export function getUserProfile() {
  return request({
    url: "/system/user/profile",
    method: "get",
  });
}

// 修改用户个人信息
export function updateUserProfile(data) {
  return request({
    url: "/system/user/profile",
    method: "put",
    data: data,
  });
}

// 用户密码重置
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword,
  };
  return request({
    url: "/system/user/profile/updatePwd",
    method: "put",
    params: data,
  });
}

// 用户头像上传
export function uploadAvatar(data) {
  return request({
    url: "/system/user/profile/avatar",
    method: "post",
    data: data,
  });
}

// 查询授权角色
export function getAuthRole(userId) {
  return request({
    url: "/system/user/authRole/" + userId,
    method: "get",
  });
}

// 保存授权角色
export function updateAuthRole(data) {
  return request({
    url: "/system/user/authRole",
    method: "put",
    params: data,
  });
}
// 查询集团下部门下拉树结构
export function deptTreeCondSelect(params) {
  return request({
    url: "/system/user/getDeptTreeCond",
    method: "get",
    params,
  });
}
// 查询集团下部门下拉树结构
export function getDeptTreeByThridDeptId(params) {
  return request({
    url: "/system/dept/getDeptTreeByThridDeptId",
    method: "get",
    params,
  });
}

// 查询部门下拉树结构
export function deptTreeSelect(params) {
  return request({
    url: "/system/user/deptTree",
    method: "get",
    params
  });
}
// 修改专家号启用状态
export function postStatus(id, state) {
  return request({
    url: `/business/expert/updateStatus/${id}/${state}`,
    method: "post",
  });
}

export function getMyList(data) {
  return request({
    url: "/business/message/getMyList",
    method: "get",
    data: data,
  });
}

export function messageRead(id) {
  return request({
    url: "/business/message/messageRead/" + id,
    method: "post",
  });
}


