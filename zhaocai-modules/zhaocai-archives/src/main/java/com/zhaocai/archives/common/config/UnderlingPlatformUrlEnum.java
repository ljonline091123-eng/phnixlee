package com.zhaocai.archives.common.config;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 底层逻辑平台 url
 *
 * @author chenming
 * @date 2024-07-09
 */
@Getter
@AllArgsConstructor
public enum UnderlingPlatformUrlEnum {

    /***/

    CONTRACT_PLAN_LIST("/rest/contractPlan/pageContract","合约规划分页查询"),
    LIST_BY_PROJECT_CONTRACT("/rest/subjectDtl/listByProjectContract","根据项目id、合约规划查询清单"),
    VENDOR_EVALUATE_SUMMARIZE("/rest/contractSettle/contractSettleEvaluate/getEvaluateSummarize","根据合同乙方统计供应商评价内容"),
    CONTRACT_LIST("/rest/contract/list","合同列表"),
    UPDATE_PLAN_QUANTITY_AMOUNT("/rest/subjectDtl/updatePlanQuantityAmount","回写合约规划拆分"),

    GET_MIN_PROJECT("/rest/prj/getMinProject","查询最小核算项目详细信息"),
    GET_PROJECT_ALL("/rest/project/allList","查询全部项目"),
    GET_PRG_AMOUNT("/rest/project/selectPrgAmount","查看最小核算项目数"),
    BPM_OPERATE_SUBMIT("/rest/bpm/operate/submit","流程服务-提交接口"),
    BPM_OPERATE_AUDIT("/rest/bpm/operate/audit","流程服务-审批接口"),
    BPM_OPERATE_DELETE("/rest/bpm/operate/delete","流程服务-作废接口"),
    BPM_OPERATE_DISCARD("/rest/bpm/operate/discard","流程服务-弃审接口"),
    BPM_OPERATE_REVOKE("/rest/bpm/operate/revoke","流程服务-撤销接口"),
    BPM_OPERATE_INITIALIZE("/rest/bpm/operate/initialize","流程服务-初始化接口"),
    BPM_OPERATE_LISTPROCESSLOG("/rest/bpm/operate/listProcessLog","流程操作日志列表接口"),
    BPM_OPERATE_LOADTASKDEF("/rest/bpm/operate/loadTaskDef","加载定义接口"),
    BPM_OPERATE_SENDMSGNOPROCESS("/rest/bpm/operate/sendMsgNoProcess","发送无流程消息"),
    LEASE_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/lease/writeBackSupplierStatus","招采更新成控设备租赁台账供应商状态"),
    TURNLEDGER_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/turnLedger/writeBackSupplierStatus","招采更新成控周材租赁台账供应商状态"),
    RECONCILIATION_WRITE_BACK_SUPPLIER_STATUS("/rest/reconciliation/writeBackSupplierStatus","招采更新成控材料对账单供应商状态"),

    WAIT_HDL_HANDLE("/rest/waitHdl/handle","第三方待办数据传输"),
    BPM_OPERATE_TRYLOCK("/rest/bpm/operate/tryLock","尝试对流程进行加锁"),
    BPM_OPERATE_UNLOCK("/rest/bpm/operate/unlock","解锁"),

    BASE_DICT_LIST("/rest/ctrl/base/dict/list","系统字典-列表"),
    DICT_LIST_MAP("/rest/dict/listMap","系统字典-列表（大汉）"),

    DEVICE_CLASS_LIST("/rest/deviceClass/list","设备分类列表"),
    DEVICE_FEATURE_LIST("/rest/deviceClass/deviceFeatureList","设备特征项列表"),
    DEVICE_FEATURE_VALUE_LIST("/rest/deviceClass/deviceFeatureValueList","设备特征值列表"),

    MATERIALS_CLASS_LIST("/rest/mtrClass/list","材料分类列表"),
    MATERIALS_FEATURE_LIST("/rest/mtrClass/mtrFeatureList","材料特征项列表"),
    MATERIALS_FEATURE_VALUE_LIST("/rest/mtrClass/mtrFeatureValueList","材料特征值接口"),
    GET_L2_ORG_BY_USERID("/rest/ctrl/api/getL2OrgByUserId","根据人员获取所属的二级组织"),
    GET_L3_ORG_BY_USERID("/rest/ctrl/api/getL3OrgByUserId","根据人员获取所属的三级组织"),
    GET_L2_ORG_BY_ORGID("/rest/ctrl/api/getL2OrgByOrgId","根据组织获取对应的二级单位"),
    GET_L3_ORG_BY_ORGID("/rest/ctrl/api/getL3OrgByOrgId","根据组织获取对应的三级单位"),

    LIST_CATA_LOG("/rest/bpm/catalog/listCatalog","查询流程分组"),

    DW_MM_ASSET_INF("/rest/dwService/selectAsset","dm071 数据查询"),
    DW_MM_SERVICE_INF("/rest/dwService/selectSerInf","dm073 数据查询"),

    BASIC_DATA_ROLE("/rest/role/list","第三方角色列表查询"),
    BASIC_DATA_USER_ROLE("/rest/role/selectUsersRole","第三方角色用户信息列表查询"),

    MARKET_QUOTE_PRICE("/rest/open/queryQuotePrice","查询易料市集清单最新价格"),
    MARKET_MATERIAL_LIST_PUSH("/rest/open/receiveMaterialList","采购清单数据推送，用于生成合同"),

    ;

    /**
     * url
     */
    private final String url;

    /**
     * 描述
     */
    private final String desc;
}
