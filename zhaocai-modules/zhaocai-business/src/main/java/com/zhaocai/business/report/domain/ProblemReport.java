package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 问题报表-异常报表基本信息对象 tb_problem_report
 *
 * @author lsn
 * @date 2024-12-17
 */
@Data
@TableName(value = "tb_problem_report")
public class ProblemReport {
    /** id */
    @TableId
    @ApiModelProperty(value = "id")
    private String id;

    /** 最小核算项目id */
    @ApiModelProperty(value = "最小核算项目id")
    private String minProjectId;

    /** 最小核算项目编码 */
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

    /** 部门id */
    @ApiModelProperty(value = "部门id")
    private Long deptId;

    /** 祖级列表 */
    @ApiModelProperty(value = "祖级列表")
    private String ancestors;

    /** 公告id */
    @ApiModelProperty(value = "公告id")
    private String noticeId;

    /** 采购方案名称 */
    private String procurementSchemeName;

    /** 采购经办人id */
    private String procurementOfficer;

    /** 采购经办人名称 */
    private String procurementOfficerName;

    /** 投标人 */
    private String vendorInfo;

    /** 投标时间 */
    private String createTime;

    /** 项目区域 */
    private String prjAddr;

    /** 项目区域-省 */
    private String prjAddrProvince;

    /** 项目区域-市 */
    private String prjAddrCity;

    /** 项目区域-区 */
    private String prjAddrRegion;

    /** 项目区域-详细地址 */
    private String prjAddrInfo;

    /** 异常类型 */
    private String type;

    /** 异常类型名称 */
    private String typeName;
}
