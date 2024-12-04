package com.zhaocai.business.report.vo;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class VBidCountVo {

    /** 组织id(第三方) */
    private String id;

    /** 组织名称(第三方) */
    @Excel(name = "组织名称")
    private String DeptName;

    /** 上级组织id(第三方) */
    private String parentId;

    /** 子项 */
    private List<VBidCountVo> children;

    /** 类型 X:项目 G:公司 */
    private String type;

    /** 层级 */
    private String level;

    /** 部门id */
    private String deptId;

    /** 项目全称 */
    @Excel(name = "项目名称")
    private String minAccountFullName;

    /** 项目编码 */
    private String minAccountCode;

    /** 项目业态 */
    private String prjState;

    /** 项目业态名称 */
    @Excel(name = "项目业态")
    private String prjStateName;

    /** 责任单位 */
    private String dutyUnit;

    /** 归属管理组织 */
    private String managementOrgId;

    /** 归属本级组织 */
    private String belongingOrgId;

    /** 归属项目部 */
    private String projectDepartmentId;

    /** 归属项目部名称 */
    private String projectDepartmentName;

    /** 采购次数 */
    @Excel(name = "采购次数")
    private BigDecimal cgNum;

    /** 公开招标次数 */
    @Excel(name = "公开次数")
    private BigDecimal gkNum;

    /** 邀请招标次数 */
    @Excel(name = "邀标次数")
    private BigDecimal yqNum;

    /** 询价招标次数 */
    @Excel(name = "询价次数")
    private BigDecimal xjNum;

    /** 单一招标次数 */
    @Excel(name = "单一次数")
    private BigDecimal dyNum;

    /** 公开招标总次数 */
    @Excel(name = "公开总次数")
    private BigDecimal gkTotalNum;

    /** 非公开招标总次数 */
    @Excel(name = "非公开总次数")
    private BigDecimal ngkTotalNum;

    /** 非招标总数 */
    @Excel(name = "非招标总数")
    private BigDecimal nBidTotalNum;

    /** 公开招标率 */
    @Excel(name = "公开率（%）")
    private BigDecimal gkRatio;
}
