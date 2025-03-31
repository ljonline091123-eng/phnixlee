import request from "@/utils/request";
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询列表
export function listProject(query) {
    return request({
      url: "/business/minProject/getMinProjectListByQuery",
      method: "get",
      params: query,
    });
}

// 查询所有部门的下拉树结构
export function AlldeptTreeSelect(params) {
  return request({
    url: "/system/dept/deptTree",
    method: "get",
    params
  });
}


// 查询项目详情
export function getMinProjectById(id) {
  return request({
    url: "/business/minProject/getMinProjectById",
    method: "get",
    params: {
      id,
    },
  });
}

// 保存项目
export function saveMinProjectInfo(data) {
  return request({
    url: "/business/minProject/saveMinProjectInfo",
    method: "post",
    data: data,
  });
}


export function deleteProject(id) {
  return request({
    url: "/business/minProject/deleteProject",
    method: "post",
    params: {
      id,
    },
  });
}

// 查询业务分类树
export function BusinessTypeTreeSelect(roleId) {
  return request({
    url: '/business/MinProjectBusinessType/getMinProjectBusinessTypeTree' ,
    method: 'get'
  })
}

// 查询资质分类树
export function CertificationTypeTreeSelect(roleId) {
  return request({
    url: '/business/ProjectCertificationType/getProjectCertificationTypeTree',
    method: 'get'
  })
}

// 查询工程分类树
export function dictProjectTypeTreeSelect(roleId) {
  return request({
    url: '/business/MinProjectDictProjectType/getMinProjectDictProjectTypeTree',
    method: 'get'
  })
}
