import request from "@/utils/request";

// 招标管理列表
export const getBiddingSchemeList = (params) => {
  return request({
    url: "/business/procurementScheme/biddingSchemeListPage",
    method: "get",
    params,
  });
};

// 发布招标公告
export const addTenderNotice = (data) => {
  return request({
    url: "/business/notice/addNotice",
    method: "post",
    data,
  });
};

// 报名情况进入下一步
export const applyNext = (data) => {
  return request({
    url: "/business/notice/registerStatus",
    method: "post",
    data,
  });
};

// 发布招标文件
export const addNotice = (data) => {
  return request({
    url: "/business/notice/add",
    method: "post",
    data,
  });
};

//重新招标
export const aNewAdd = (data) => {
  return request({
    url: "/business/notice/aNewAdd",
    method: "post",
    data,
  });
};

// 获取公告详情
export const getNoticeDetail = (schemeId, noticeId) => {
  return request({
    url: "/business/notice/getInfo",
    method: "get",
    params: {
      schemeId,
      noticeId,
    },
  });
};

// 获取审批权限
export const getPermissionButton = (params) => {
  return request({
    url: "/business/bpm/initialize",
    method: "get",
    params,
  });
};
// 采购方案流程 初始化接口
export const getPermissionButtonScheme = (params) => {
  return request({
    url: "/business/procurementScheme/initialize",
    method: "get",
    params,
  });
};

// 获取供应商审批权限
export const getPermissionButtonVendor = (params) => {
  return request({
    url: "/business/vendor/initialize",
    method: "get",
    params,
  });
};

// 加载定义接口 公共
export const getLoadTaskDef = (params) => {
  return request({
    url: "/business/bpm/loadTaskDef",
    method: "get",
    params,
  });
};

// 加载定义接口 定标
export const getLoadTaskDefBidding = (params) => {
  return request({
    url: "/business/result/loadTaskDef",
    method: "get",
    params,
  });
};
// 采购方案流程列表接口
export const getLoadTaskDefScheme = (params) => {
  return request({
    url: "/business/procurementScheme/loadTaskDef",
    method: "get",
    params,
  });
};
export const getLoadTaskDefVendor = (params) => {
  return request({
    url: "/business/vendor/loadTaskDef",
    method: "get",
    params,
  });
};
// 流程操作日志列表
export const getProcessLogList = (params) => {
  // 手动拼接 businessId 到 URL 中
  let url = `/business/bpm/listProcessLog?processId=${params.processId}`;
  // 如果 businessId 存在，即使为空字符串，也将其拼接到 URL 中
  if (params.businessId !== undefined) {
    url += `&businessId=${params.businessId}`;
  }
  return request({
    url: url,
    method: "get",
  });
};

// 流程操作日志列表 定标
export const getProcessLogListBidding = (params) => {
  // 手动拼接 businessId 到 URL 中
  let url = `/business/result/listProcessLog?processId=${params.processId}`;
  // 如果 businessId 存在，即使为空字符串，也将其拼接到 URL 中
  if (params.businessId !== undefined) {
    url += `&businessId=${params.businessId}`;
  }
  return request({
    url: url,
    method: "get",
  });
};
// 流程操作日志列表
export const getProcessLogListVendor = (params) => {
  // 手动拼接 businessId 到 URL 中
  let url = `/business/vendor/listProcessLog?processId=${params.processId}`;
  // 如果 businessId 存在，即使为空字符串，也将其拼接到 URL 中
  if (params.businessId !== undefined) {
    url += `&businessId=${params.businessId}`;
  }
  return request({
    url: url,
    method: "get",
  });
};
// 审批流程
export const postAuditProcess = (data) => {
  return request({
    url: "/business/process/auditProcess",
    method: "post",
    data,
  });
};
// 采购方案流程 审批接口
export const postAuditProcessScheme = (data) => {
  return request({
    url: "/business/procurementScheme/audit",
    method: "post",
    data,
  });
};
// 定标流程 审批接口
export const postAuditProcessBidding = (data) => {
  return request({
    url: "/business/result/audit",
    method: "post",
    data,
  });
};
// 获取二三级单位
export const postGetOrg = (org) => {
  return request({
    url: "/business/process/getOrg",
    method: "post",
    data: {
      org,
    },
  });
};
// 获取二三级单位
export const getOrgByUserId = (userId) => {
  return request({
    url: "/business/process/getOrgByUserId",
    method: "post",
    data: {
      userId
    },
  });
};
export const postAuditProcessVendor = (data) => {
  return request({
    url: "/business/vendor/audit",
    method: "post",
    data,
  });
};
// 获取公告文件修改
export const getNoticeUpdateListNotice = (noticeId) => {
  return request({
    url: "/business/noticeChangeRecord/getListNotice",
    method: "post",
    data: {
      noticeId,
    },
  });
};
// 获取公告文件修改
export const getNoticeUpdateList = (noticeId) => {
  return request({
    url: "/business/noticeChangeRecord/getList",
    method: "post",
    data: {
      noticeId,
    },
  });
};

// 选择招标文件模板
export const getSwitchPageList = (params) => {
  return request({
    url: "/business/template/switchListPage",
    method: "get",
    params,
  });
};

// 进行变更
export const addUpdateTenderNotice = (data) => {
  return request({
    url: "/business/noticeChangeRecord/addNotice",
    method: "post",
    data,
  });
};

// 新增公告文件修改
export const addUpdateNotice = (data) => {
  return request({
    url: "/business/noticeChangeRecord/add",
    method: "post",
    data,
  });
};

// 查询答疑列表
export const getQAListNotice = (busId, answerType = "") => {
  return request({
    url: "/business/answer/getListNotice",
    method: "post",
    data: {
      busId,
      answerType,
    },
  });
};

// 新增公告文件修改
export const getQAList = (busId, answerType = "") => {
  return request({
    url: "/business/answer/getList",
    method: "post",
    data: {
      busId,
      answerType,
    },
  });
};

// 公告答疑回复
export const addAnswerNotice = (data) => {
  return request({
    url: "/business/answer/editNotice",
    method: "post",
    data,
  });
};

// 答疑回复
export const addAnswer = (data) => {
  return request({
    url: "/business/answer/edit",
    method: "post",
    data,
  });
};

//获取回标
export const getBackList = (data) => {
  return request({
    url: "/business/info/getList",
    method: "post",
    data,
  });
};

//财务收取保证金操作
export const updateDeposit = (data) => {
  return request({
    url: "/business/info/collectDeposit",
    method: "post",
    params: {
      ...data,
    },
  });
};

//废标操作
export const abandonBid = (data) => {
  return request({
    url: "/business/info/abandonBid",
    method: "post",
    data,
  });
};

/** 查询开标人员 */
export const getWorker = (data) => {
  return request({
    url: "/business/people/getList",
    method: "post",
    data,
  });
};

/** 新增开标人员 */
export const addWorker = (data) => {
  return request({
    url: "/business/people/add",
    method: "post",
    data,
  });
};
// 提交开标人员信息
export const submitWorker = (noticeId) => {
  return request({
    url: "/business/people/submit",
    method: "post",
    params: {
      noticeId,
    },
  });
};
/** 查询投票单和评分单 */
export const getBiddingQuotationList = (data) => {
  return request({
    url: "/business/info/getBiddingQuotationList",
    method: "post",
    data,
  });
};

/** 新增评标专家 */
export const addEvaluatExpert = (data) => {
  return request({
    url: "/business/evaluatExpert/add",
    method: "post",
    data,
  });
};
/** 开标人员开标 */
export const openBidPeople = (data) => {
  return request({
    url: "/business/people/openBid",
    method: "post",
    data,
  });
};

/** 结束评标 */
export const evaluatBidOver = (noticeId) => {
  return request({
    url: "/business/info/evaluatBid",
    method: "post",
    data: {
      noticeId,
    },
  });
};

/** 定标报告列表 */
export const getCalibrationReportList = (noticeId) => {
  return request({
    url: "/business/info/getCalibrationReportList",
    method: "get",
    params: {
      noticeId,
    },
  });
};

/** 提交定标 */
export const calibration = (data, detailUrl) => {
  return request({
    url: "/business/result/calibration",
    method: "post",
    data: data,
    params: {
      detailUrl: detailUrl,
    },
  });
};
/** 撤回定标 */
export const revokeBidding = (id) => {
  return request({
    url: "/business/result/revokeBidding",
    method: "post",
    params: {
      id,
    },
  });
};

/** 查询定标供应商数据 */
export const getBiddingResult = (noticeId) => {
  return request({
    url: "/business/result/getBiddingResult",
    method: "get",
    params: {
      noticeId,
    },
  });
};

/** 定标发布 */
export const calibrationRelease = (data) => {
  return request({
    url: "/business/result/calibrationRelease",
    method: "post",
    data,
  });
};

/** 查询中标结果数据 */
export const getWinningBidResult = (noticeId) => {
  return request({
    url: "/business/result/getWinningBidResult",
    method: "get",
    params: {
      noticeId,
    },
  });
};

/** 进入下一环节 */
export const intoBidOpeningStage = (noticeId) => {
  return request({
    url: "/business/info/intoBidOpeningStage",
    method: "post",
    params: {
      noticeId,
    },
  });
};

/** 发布通知 */
export const winningBidResultRelease = (data) => {
  return request({
    url: "/business/result/winningBidResultRelease",
    method: "post",
    data,
  });
};

/** 查询开标人员开标状态 */
export const getPeopleList = (noticeId) => {
  return request({
    url: "/business/people/getList",
    method: "post",
    data: {
      noticeId,
    },
  });
};

/** 报价汇总 */
export const getBiddingQuotationSummary = (noticeId, schemeId) => {
  return request({
    url: "/business/info/getBiddingQuotationSummary",
    method: "post",
    data: {
      noticeId,
      schemeId,
    },
  });
};

/** 报价汇总 */
export const getBidEvaluationList = (noticeId, scoreType) => {
  return request({
    url: "/business/info/getBidEvaluationList",
    method: "get",
    params: {
      noticeId,
      scoreType,
    },
  });
};

/** 开启评标 */
export const startEvaluat = (noticeId) => {
  return request({
    url: "/business/info/startEvaluat",
    method: "post",
    params: {
      noticeId,
    },
  });
};

/** 开启调价 */
export const twiceBidConf = (biddingInfoIds, noticeId,twiceTime) => {
  return request({
    url: "/business/info/twiceBidConf",
    method: "post",
    data: {
      biddingInfoIds,
      noticeId,
      twiceTime
    },
  });
};

/** 结束调价 */
export const twiceBidFinish = (noticeId) => {
  return request({
    url: "/business/info/twiceBidFinish",
    method: "post",
    data: {
      noticeId,
    },
  });
};

/** 获取回标详情 */
export const getBidInfo = (id) => {
  return request({
    url: "/business/info/" + id,
    method: "get",
  });
};

/** 评标附件上传 */
export const uploadEvalAttach = (data) => {
  return request({
    url: "/business/info/uploadEvalAttach",
    method: "post",
    data,
  });
};

/** 查询专家评标状态 */
export const getExpertEvalStatus = (params) => {
  return request({
    url: "/business/info/getExpertEvalStatus",
    method: "post",
    params,
  });
};

/** 招标管理列表废标 */
export const abandonBidMore = (data) => {
  return request({
    url: "/business/info/abandonBidMore",
    method: "post",
    data,
  });
};

/** 招标管理列表废标 同时 废除采购方案 */
export const abandonBidMoreScheme = (data) => {
  return request({
    url: "/business/info/abandonBidMoreScheme",
    method: "post",
    data,
  });
};

//获取省市树型
export const listAreaDivisionTree = () => {
  return request({
    url: "/business/division/listAreaDivisionTree",
    method: "get",
  });
};

//获取供应商分类
export const getVendorClassifyTree = () => {
  return request({
    url: "/business/vendorClassify/getVendorClassifyTree",
    method: "get",
  });
};
// 未评标专家催办
export const saveUrgeExpertMes = (data) => {
  return request({
    url: "/business/info/urgeExpertMes",
    method: "post",
    data,
  });
};
// 获取专家责任单位审批权限
export const getPermissionButtonNew = (params) => {
  return request({
    url: "/business/expert/initialize",
    method: "get",
    params,
  });
};
// 审批流程
export const postAuditProcessNew = (data) => {
  return request({
    url: "/business/expert/audit",
    method: "post",
    data,
  });
};
// 加载责任单位定义接口
export const getLoadTaskDefNew= (params) => {
  return request({
    url: "/business/expert/loadTaskDef",
    method: "get",
    params,
  });
};
// 流程操作日志列表 专家
export const getProcessLogListNew = (params) => {
  // 手动拼接 businessId 到 URL 中
  let url = `/business/expert/listProcessLog?processId=${params.processId}`;
  // 如果 businessId 存在，即使为空字符串，也将其拼接到 URL 中
  if (params.businessId !== undefined) {
    url += `&businessId=${params.businessId}`;
  }
  // 如果 processType 存在，即使为空字符串，也将其拼接到 URL 中
  if (params.processType !== undefined) {
    url += `&processType=${params.processType}`;
  }
  return request({
    url: url,
    method: "get",
  });
};
