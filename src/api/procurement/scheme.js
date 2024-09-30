import request from "@/utils/request";

// 采购需求列表
export const getSchemeList = (params) => {
  return request({
    url: "/business/procurementScheme/listPage",
    method: "get",
    params,
  });
};

// 保存采购需求
export const saveProcurementScheme = (data) => {
  return request({
    url: "/business/procurementScheme/saveProcurementScheme",
    method: "post",
    data,
  });
};

// 获取采购方案详情
export const getSchemeDetail = (id) => {
  return request({
    url: "/business/procurementScheme/detail",
    method: "get",
    params: {
      id,
    },
  });
};

// 获取合约规划列表
export const getContractPlan = (data) => {
  return request({
    url: "/business/contractPlanning/listProcurementContractPlan",
    method: "post",
    data,
  });
};

// 获取合约计划清单列表
export const getListMaterials = (data) => {
  return request({
    url: "/business/procurementPlan/listContractSplitMaterials",
    method: "POST",
    data,
  });
};

// 获取财务人员
export const getFinanceList = (deptId) => {
  return request({
    url:
      "system/user/listUserByDeptId?pageNum=1&pageSize=1000000&deptId=" +
      deptId,
    method: "get",
  });
};

// 提交采购方案
export const submitProcurementScheme = (params) => {
  return request({
    url: "/business/procurementScheme/submitProcurementScheme",
    method: "post",
    params,
  });
};

// 获取评分模板列表
export const getTemplateList = (data) => {
  return request({
    url: "/business/markTemplate/page",
    method: "post",
    data,
  });
};
export const getTemplateSwitchList = (data) => {
  return request({
    url: "/business/markTemplate/switchListPage",
    method: "post",
    data,
  });
};

// 获取经办人和上限价
export const getProcurementSchemeCreateInfo = (data) => {
  return request({
    url: "/business/procurementScheme/getProcurementSchemeCreateInfo",
    method: "post",
    data,
  });
};

//作废采购方案
export const cancellationProcurementScheme = (id) => {
  return request({
    url: "/business/procurementScheme/cancellationProcurementScheme",
    method: "post",
    params: {
      id,
    },
  });
};

// 判断采购计划是否可以合并提交
export const checkProcurementSchemeSelect = (data) => {
  return request({
    url: "/business/procurementScheme/checkProcurementSchemeSelect",
    method: "post",
    data,
  });
};

// 撤回采购方案
export const withdrawalPlan = (id) => {
  return request({
    url: "/business/procurementScheme/revokeProcurementScheme",
    method: "post",
    params: {
      id,
    },
  });
};
