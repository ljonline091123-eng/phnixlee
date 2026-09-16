package com.zhaocai.business.report.vo.req;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 采购台账查询入参
 *
 * @author claude
 */
@Data
public class PurchaseLedgerQueryVo {

    /** 组织机构id(第三方部门id, 左树选中) */
    private String id;

    /** 组织机构id(前端筛选树, 与 id 等价) */
    private String deptId;

    /**
     * 数据权限范围单位id(第三方部门id)
     * 由后端根据当前登录用户所属单位强制赋值，前端不需要传；为空表示不限制(集团账号)
     */
    private String scopeId;

    /** 项目编号 */
    private String projectCode;

    /** 项目名称关键字 */
    private String projectKeyword;

    /** 采购需求类型(字典 procurement_plan_type) */
    private Integer demandType;

    /** 采购方式(字典 procurement_type) */
    private Integer method;

    /** 采购状态 pending/preopen/preaward/completed/exception */
    private String status;

    /** 当前环节 */
    private String currentStage;

    /** 采购经办人(方案负责人) */
    private String handler;

    /** 采购人 */
    private String purchaser;

    /** 计划/采购方案编号或名称 关键字 */
    private String keyword;

    /** 计划生效时间-起 */
    private String planEffectBegin;

    /** 计划生效时间-止 */
    private String planEffectEnd;

    /** 页数 */
    private int pageNum;

    /** 页面条数 */
    private int pageSize;

}
