package com.zhaocai.business.report.vo;

import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.bean.PageRecive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProblemReportVo extends PageRecive {

    /** 组织机构id */
    private String id;

    /** 组织机构id */
    private String deptId;

    /** 祖级列表 */
    private String ancestors;

    /** 上级id */
    private String parentId;

    /** 组织机构idList */
    private List<String> idList;

    /** 组织机构名称 */
    private String deptName;

    /** 组织名称 */
    @Excel(name = "一级单位")
    private String OneDeptName;

    /** 组织名称 */
    @Excel(name = "二级单位")
    private String TwoDeptName;

    /** 组织名称 */
    @Excel(name = "三级单位")
    private String ThreeDeptName;

    /** 组织名称 */
    @Excel(name = "四级单位")
    private String FourDeptName;

    /** 项目名称 */
    @Excel(name = "项目")
    private String minAccountFullName;

    /** 笔数 */
    @Excel(name = "笔数")
    private BigDecimal num;

    /** 归属项目部id */
    private String projectDepartmentId;

    /** 归属项目部 */
    private String projectDepartment;

    /** 项目编码 */
    private String minAccountCode;

    /** 项目id */
    private String minProjectId;

    /** 采购方案名称 */
    @Excel(name = "标包")
    private String procurementSchemeName;

    /** 采购经办人id */
    private String procurementOfficer;

    /** 采购经办人名称 */
    @Excel(name = "经办人")
    private String procurementOfficerName;

    /** 投标人 */
    @Excel(name = "投标人")
    private String vendorInfo;

    /** 投标时间 */
    @Excel(name = "招标时间")
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
    @Excel(name = "出现情况")
    private String typeName;

    /** 页数 */
    private int pageNum;

    /** 页面条数 */
    private int pageSize;

    /** 子项 */
    private List<ProblemReportVo> children;

    /** 区域 */
    private String area;

    /** 开始时间 */
    private String startDate;

    /** 结束时间 */
    private String endDate;
}
