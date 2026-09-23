package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 招标率报表基本信息对象 tb_bid_report
 *
 * @author lsn
 * @date 2024-12-17
 */
@Data
@TableName(value = "tb_bid_report")
public class BidReport {

    /** 最小核算项目id */
    @ApiModelProperty(value = "最小核算项目id")
    private String minProjectId;

    /** 最小核算项目编码 */
    @TableId
    @ApiModelProperty(value = "最小核算项目编码")
    private String minAccountCode;

    /** 最小核算项目全称 */
    @ApiModelProperty(value = "最小核算项目全称")
    private String minAccountFullName;

    /** 归属项目部id */
    @ApiModelProperty(value = "归属项目部id")
    private String projectDepartmentId;

    /** 归属项目部名称 */
    @ApiModelProperty(value = "归属项目部名称")
    private String projectDepartment;

    /** 管理组织id(第三方部门id，部门级，报表树按此归组) */
    @ApiModelProperty(value = "管理组织id(第三方部门id，部门级)")
    private String managementOrgId;

    /** 管理组织名称 */
    @ApiModelProperty(value = "管理组织名称")
    private String managementOrg;

    /** 部门id */
    @ApiModelProperty(value = "部门id")
    private Long deptId;

    /** 祖级列表 */
    @ApiModelProperty(value = "祖级列表")
    private String ancestors;

    /** 项目业态 */
    @ApiModelProperty(value = "项目业态")
    private String prjState;

    /** 采购次数 */
    @ApiModelProperty(value = "采购次数")
    private int cgNum;

    /** 公开次数 */
    @ApiModelProperty(value = "公开次数")
    private int gkNum;

    /** 邀标次数 */
    @ApiModelProperty(value = "邀标次数")
    private int yqNum;

    /** 询价次数 */
    @ApiModelProperty(value = "询价次数")
    private int xjNum;

    /** 单一次数 */
    @ApiModelProperty(value = "单一次数")
    private int dyNum;

    /** 公开总次数 */
    @ApiModelProperty(value = "公开总次数")
    private int gkTotalNum;

    /** 非公开总次数 */
    @ApiModelProperty(value = "非公开总次数")
    private int ngkTotalNum;

    /** 公开率 */
    @ApiModelProperty(value = "公开率")
    private BigDecimal gkRatio;
}
