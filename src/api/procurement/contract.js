import request from "@/utils/request";


/**
 * 获取预览URL
 * @param params
 * @returns {*}
 */
export const getAgreementViewURL = (params) => {
  return request({
    url: "/business/agreement/getAgreementViewURL",
    method: "get",
    params,
  });
};


/**
 * 获取编辑URL
 * @param data
 * @returns {*}
 */
export const getAgreementEditURL = (data) => {
  return request({
    url: "/business/agreement/getAgreementEditURL",
    method: "post",
    data,
  });
};

/**
 * 获取采购合同列表
 * @param params
 * @returns {*}
 */
export const listSignAgreementScheme = (params) => {
  return request({
    url: "/business/agreement/listSignAgreementScheme",
    method: "get",
    params,
  });
};

export const listAgreement = (params) => {
  return request({
    url: "/business/agreement/listPage",
    method: "get",
    params,
  });
};

/**
 * 获取采购方案下拉
 * @param params
 * @returns {*}
 */
export const listContractSplit = (params) => {
  return request({
    url: "/business/procurementScheme/listContractSplit",
    method: "get",
    params,
  });
};

/**
 * 获取供应商下拉
 * @param params
 * @returns {*}
 */
export const listBiddingVendor = (params) => {
  return request({
    url: "/business/procurementScheme/listBiddingVendor",
    method: "get",
    params,
  });
};

/**
 * 获取供应商的报价清单
 * @param params
 * @returns {*}
 */
export const listVendorBiddingListQuotation = (params) => {
  return request({
    url: "/business/quotation/listVendorBiddingListQuotation",
    method: "get",
    params,
  });
};

/**
 * 获取详细信息
 * @param params
 * @returns {*}
 */
export const getAgreementCreateInfo = (data) => {
  return request({
    url: "/business/agreement/getAgreementCreateInfo",
    method: "post",
    data,
  });
};

/**
 * 获取合同生成信息
 * @param params
 * @returns {*}
 */
export const checkAgreementCreateInfo = (data) => {
  return request({
    url: "/business/agreement/checkAgreementCreateInfo",
    method: "post",
    data,
  });
};

/**
 * 获取详细信息
 * @param data
 * @returns {*}
 */
export const saveAgreement = (data) => {
  return request({
    url: "/business/agreement/saveAgreement",
    method: "post",
    data,
  });
};

/**
 * 获取详细信息
 * @param params
 * @returns {*}
 */
export const getAgreementDetail = (params) => {
  return request({
    url: "/business/agreement/detail",
    method: "get",
    params,
  });
};

export const listAgreementPage = (params) => {
  return request({
    url: "/business/agreement/listPage",
    method: "get",
    params,
  });
};

/**
 * 获取合同附件
 * @param params
 * @returns {*}
 */
export const getAgreementAttachmentId = (id) => {
  return request({
    url: "/business/agreement/getAgreementAttachmentId",
    method: "get",
    params: {
      id,
    },
  });
};

/**
 * 检查修改按钮是否可操作
 * @param params
 * @returns {*}
 */
export const checkAgreementUpdate = (id) => {
  return request({
    url: "/business/agreement/checkAgreementUpdate",
    method: "get",
    params: {
      id,
    },
  });
};

/**
 * 检查修改按钮是否可操作
 * @param params
 * @returns {*}
 */
// export const getLabelAttachmentId = (id) => {
//   return request({
//     url: '/business/agreement/getLabelAttachmentId',
//     method: 'get',
//     params:{
//       id
//     }
//   })
// }
export const getLabelAttachmentId = (params) => {
  return request({
    url: "/business/agreement/getLabelAttachmentId",
    method: "get",
    params,
  });
};
// 提交合同
export const submitAgreement = (params) => {
  return request({
    url: "/business/agreement/submitAgreement",
    method: "post",
    params,
  });
};

// 作废合同
export const cancellationAgreement = (id) => {
  return request({
    url: "/business/agreement/cancellationAgreement",
    method: "post",
    params: {
      id,
    },
  });
};

// 撤回合同
export const revokeAgreement = (id) => {
  return request({
    url: "/business/agreement/revokeAgreement",
    method: "post",
    params: {
      id,
    },
  });
};

// 推送合同至供应商
export const pushAgreementToVendor = (id) => {
  return request({
    url: "/business/agreement/pushAgreementToVendor",
    method: "post",
    params: {
      id,
    },
  });
};

// 推送合同至电子签章平台
export const pushAgreementToSignPlatform = (data) => {
  return request({
    url: "/business/agreement/pushAgreementToSignPlatform",
    method: "post",
    data,
  });
};

// 签署合同
export const signAgreement = (id) => {
  return request({
    url: "/business/agreement/signAgreement",
    method: "post",
    params: {
      id,
    },
  });
};

// 作废签署合同
export const cancelledSignAgreement = (data) => {
  return request({
    url: "/business/agreement/cancelledSignAgreement",
    method: "post",
    data,
  });
};

//获取合同字典
export const listUnderlingDict = (type) => {
  return request({
    url: "/business/dict/listDict",
    method: "get",
    params: {
      type,
    },
  });
};

//获取设备分类树形
export const listDeviceClass = () => {
  return request({
    url: "/business/materialsClass/listDeviceClass",
    method: "get",
  });
};

//获取设备分类列表
export const listDevice = (params) => {
  return request({
    url: "/business/materialsClass/listDevice",
    method: "get",
    params,
  });
};

//获取物资分类树形
export const listMaterialsClass = () => {
  return request({
    url: "/business/materialsClass/listMaterialsClass",
    method: "get",
  });
};

//获取设备分类列表
export const listMaterials = (params) => {
  return request({
    url: "/business/materialsClass/listMaterials",
    method: "get",
    params,
  });
};

//获取设备特征项
// export const deviceFeatureList = (queryId) => {
//   return request({
//     url: "/business/materialsClass/deviceFeatureList",
//     method: "get",
//     params: {
//       queryId,
//     },
//   });
// };

export const deviceFeatureList = (deviceClassId) => {
  return request({
    url: "/archives/deviceFeature/list",
    method: "get",
    params: {
      deviceClassId,
    },
  });
};

//获取设备特征值
// export const deviceFeatureValueList = (queryId) => {
//   return request({
//     url: "/business/materialsClass/deviceFeatureValueList",
//     method: "get",
//     params: {
//       queryId,
//     },
//   });
// };
export const deviceFeatureValueList = (deviceFeatureId) => {
  return request({
    url: "archives/featureValue/list",
    method: "get",
    params: {
      deviceFeatureId,
    },
  });
};


//获取物资特征项
// export const listMaterialsFeature = (queryId) => {
//   return request({
//     url: "/business/materialsClass/listMaterialsFeature",
//     method: "get",
//     params: {
//       queryId,
//     },
//   });
// };
export const listMaterialsFeature = (mtrClassId) => {
  return request({
    url: "/archives/mtrFeature/list",
    method: "get",
    params: {
      mtrClassId,
    },
  });
};


//获取物资特征值
// export const listMaterialsFeatureValue = (queryId) => {
//   return request({
//     url: "/business/materialsClass/listMaterialsFeatureValue",
//     method: "get",
//     params: {
//       queryId,
//     },
//   });
// };
export const listMaterialsFeatureValue = (mtrFeatureId) => {
  return request({
    url: "/archives/mtrValue/list",
    method: "get",
    params: {
      mtrFeatureId,
    },
  });
};

/**
 * 获取供应商下拉
 * @param params
 * @returns {*}
 */
export const getAgreementSelectedProcurementInfo = (params) => {
  return request({
    url: "/business/agreement/getAgreementSelectedProcurementInfo",
    method: "get",
    params,
  });
};

// 获取采购经办人
export const getPartyAUserList = (deptId) => {
  return request({
    url:
      "system/user/listUserByDeptId?pageNum=1&pageSize=1000000&deptId=" +
      deptId,
    method: "get",
  });
};

//获取易料订单
export const getMarketOrder = (data) => {
  return request({
    url: "/business/marketOrder/page",
    method: "post",
    data,
  });
};
/**
 * 获取易料单据
 * @param params
 * @returns {*}
 */
export const listMarketMaterialContract = (params) => {
  return request({
    url: "/business/marketMaterialContract/listMarketMaterialContract",
    method: "get",
    params,
  });
};
/**
 *  获取创建合同的基本信息
 * @param params
 * @returns {*}
 */
export const getAgreementCreateInfoYl = (id) => {
  return request({
    url: "/business/marketMaterialContract/getAgreementCreateInfo",
    method: "post",
    data: {
      id,
    },
  });
};

/**
 *  获取创建合同的基本信息
 * @param params
 * @returns {*}
 */
export const agreementCreateAttachmentHandle = (data) => {
  return request({
    url: "/business/marketMaterialContract/agreementCreateAttachmentHandle ",
    method: "post",
    data,
  });
};
/**
 * 免审
 * @param params
 * @returns {*}
 */
export const avoidSubmitByMarket = (id) => {
  return request({
    url: "/business/agreement/avoidSubmitByMarket",
    method: "get",
    params: {
      id,
    },
  });
};
/**
 *  校验创建合同的基本信息
 * @param params
 * @returns {*}
 */
export const getCheckAgreementCreateInfo = (id) => {
  return request({
    url: "/business/marketMaterialContract/checkAgreementCreateInfo",
    method: "post",
    data: {
      id,
    },
  });
};
