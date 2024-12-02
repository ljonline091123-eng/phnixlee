import request from "@/utils/request";

// 范本管理-查询模板附件的-文档中台的预览url
export function getPreviewFileUrl(id) {
  return request({
    url: "/business/template/PreviewFile",
    method: "get",
    params: {
      id,
    },
  });
}

// 范本管理-查询模板附件的-文档中台的编辑url
export function editFile(params) {
  return request({
    url: "/business/template/getEditFileURL",
    method: "get",
    params,
  });
}

//招标公告-预览招标公告附件-文档中台的预览url && 招标文件附件预览
export function getViweFileURL(params) {
  return request({
    url: "/business/attachment/getViweFileURL",
    method: "get",
    params,
  });
}


//合同签订-预览合同附件-文档中台的预览url 
export function getViewAttachmentURLByID(params) {
  return request({
    url: "/business/attachment/getViweFileUrlByID",
    method: "get",
    params,
  });
}

//合同签订新增-编辑合同附件-文档中台的编辑url 
export function getEditFileUrlByID(params) {
  return request({
    url: "/business/attachment/getEditFileUrlByID",
    method: "get",
    params,
  });
}

// 查询合同或招标模板
export function getFileTemplate(params) {
  return request({
    url: "/business/template/fanListPage",
    method: "get",
    params,
  });
}

// 新增合同或招标模板
export function saveTemplate(data) {
  return request({
    url: "/business/template/saveTemplate",
    method: "post",
    data,
  });
}
// 合同类型列表
export function getContractTypeList() {
  return request({
    url: "/business/template/contractTypeList",
    method: "get",
  });
}

// 查询模板详情
export function getTemplateDetail(id) {
  return request({
    url: "/business/template/detail",
    method: "get",
    params: {
      id,
    },
  });
}

// 删除合同或招标模板
export function deleteTemplate(id) {
  return request({
    url: "/business/template/deleteTemplate",
    method: "post",
    params: {
      id,
    },
  });
}

// 提交文件地址，返回ID
export function addAttachment(data) {
  return request({
    url: "/business/attachment/addAttachment",
    method: "post",
    data,
  });
}

// 获取模板使用单位选择
export function listOrganization4Company() {
  return request({
    url: "/business/organization/listOrganization4Company",
    method: "get",
  });
}

// 范本选择获取公司
export function listOrganizationCall() {
  return request({
    url: "/business/organization/listOrganizationCalligraphy",
    method: "get",
  });
}
