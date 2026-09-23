package com.zhaocai.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * 供应商报表基本信息对象 tb_vendor_report
 *
 * @author lsn
 * @date 2024-12-17
 */
@Data
@TableName(value = "tb_vendor_report")
public class VendorReport {

    /** 主键 */
    @TableId
    @ApiModelProperty(value = "主键")
    private String id;


    /** 供应商分类 */
    @ApiModelProperty(value = "供应商分类")
    private String expenditureBusinessType;

    /** 供应商id */
    @ApiModelProperty(value = "供应商id")
    private String vendorId;

    /** 供应商名称 */
    @ApiModelProperty(value = "供应商名称")
    private String vendorName;

    /** 甲方id */
    @ApiModelProperty(value = "甲方id")
    private String partyAOrgId;

    /** 项目编码 */
    @ApiModelProperty(value = "项目编码")
    private String minAccountCode;

    /** 归属项目部id */
    @ApiModelProperty(value = "归属项目部id")
    private String projectDepartmentId;

    /** 甲方名称 */
    @ApiModelProperty(value = "甲方名称")
    private String partyAName;

    /** 合同金额（含税） */
    @ApiModelProperty(value = "合同金额")
    private BigDecimal contractAmount;

    /** 供应商所在省份 */
    @ApiModelProperty(value = "供应商所在省份")
    private String enterpriseProvinceCode;

    /** 供应商所在省份 */
    @ApiModelProperty(value = "供应商所在省份")
    private String enterpriseProvinceName;

    /** 供应商所在市区 */
    @ApiModelProperty(value = "供应商所在市区")
    private String enterpriseCityCode;

    /** 供应商所在市区 */
    @ApiModelProperty(value = "供应商所在市区")
    private String enterpriseCityName;

    /** 供应商所在地 */
    @ApiModelProperty(value = "供应商所在地")
    private String enterpriseLocation;

    /** 参与采购任务笔数 */
    @ApiModelProperty(value = "参与采购任务笔数")
    private Integer tbiCount;

    /** 中标任务笔数 */
    @ApiModelProperty(value = "中标任务笔数")
    private Integer tbrCount;

    /** 中标金额（含税） */
    @ApiModelProperty(value = "中标金额")
    private BigDecimal tbrAmount;

    /** 中标率 */
    @ApiModelProperty(value = "中标率")
    private BigDecimal tbrRate;

    /** 投标时间/报名时间 */
    @ApiModelProperty(value = "投标时间/报名时间")
    private Date applyTime;

    /** 合格次数(供应商评价确认状态) */
    @ApiModelProperty(value = "合格次数")
    private Integer qualifiedCount;

    /** 不合格次数(供应商评价确认状态) */
    @ApiModelProperty(value = "不合格次数")
    private Integer unqualifiedCount;
}
