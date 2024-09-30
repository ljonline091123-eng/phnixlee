package com.zhaocai.business.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;

import java.math.BigDecimal;
import java.util.Date;

public class ManagePageReportVo {

    /** id */
    private String id;

    /** 采购计划id */
    private Long planId;

    /** 项目id */
    private Long projectId;

    /** 项目编码 */
    @Excel(name = "项目编码")
    private String projectCode;

    /** 项目名称 */
    @Excel(name = "项目名称")
    private String projectName;

    /** 合同id */
    private Long contractId;

    /** 合同编码 */
    private String contractCode;

    /** 合同名称 */
    private String contractName;

    /** 合同签订金额(含税) */
    private BigDecimal totalAmountIncTax;

    /** 合同状态 */
    private String agreementState;

    /** 方案id */
    private String schemeId;

    /** 招标进展 */
    private String noticeStatus;

    /** 合同金额（万元） */
    @Excel(name = "合同金额（万元）")
    private BigDecimal contractAmount;

    /** 合同金额-开始 */
    private BigDecimal contractStartAmount;

    /** 合同金额-结束 */
    private BigDecimal contractEndAmount;

    /** 建设单位 */
    @Excel(name = "建设单位")
    private String buildUnit;

    /** 责任单位 */
    @Excel(name = "责任单位")
    private String responsibleUnit;

    /** 项目规模 */
    @Excel(name = "项目规模")
    private String projectScale;

    /** 项目规模-开始 */
    private String projectStartScale;

    /** 项目规模-结束 */
    private String projectEndScale;

    /** 项目业态编码 */
    private String projectBusinessCode;

    /** 项目业态名称 */
    @Excel(name = "项目业态")
    private String projectBusinessName;

    /** 工程类型编码 */
    private String engineerTypeCode;

    /** 工程类型名称 */
    @Excel(name = "工程类型")
    private String engineerTypeName;

    /** 结构类型编码 */
    private String structureTypeCode;

    /** 结构类型名称 */
    @Excel(name = "结构类型")
    private String structureTypeName;

    /** 项目状态编码 */
    private String projectStatusCode;

    /** 项目状态名称 */
    @Excel(name = "项目状态")
    private String projectStatusName;

    /** 实际开工日期 */
    @Excel(name = "实际开工日期")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date realityBeginDate;

    /** 实际开工日期-开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date beginStartTime;

    /** 实际开工日期-结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date beginEndTime;

    /** 实际竣工日期 */
    @Excel(name = "实际竣工日期")
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date realityFinishDate;

    /** 实际开工日期-开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date finishStartTime;

    /** 实际开工日期-结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date finishEndTime;

    /** 项目地点 */
    @Excel(name = "项目地点")
    private String address;

    /** 采购计划数 */
    @Excel(name = "采购计划数")
    private BigDecimal planCount;

    /** 已完成招标 */
    @Excel(name = "已完成招标")
    private BigDecimal bidCount;

    /** 已签合同数 */
    @Excel(name = "已签合同数")
    private BigDecimal signCount;

    /** 已签合同金额（元） */
    @Excel(name = "已签合同金额（元）")
    private BigDecimal signContractAmount;

    /** 项目所在区域编码 */
    private String areaCode;

    /** 项目所在区域名称 */
    private String areaName;

    /** 部门id */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 页数 */
    private int pageNum;

    /** 页面条数 */
    private int pageSize;

    /** 公司端GS/集团端JT */
    private String type;

    /** 所属第三方组织id */
    private String thridOrgId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getContractCode() {
        return contractCode;
    }

    public void setContractCode(String contractCode) {
        this.contractCode = contractCode;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public BigDecimal getTotalAmountIncTax() {
        return totalAmountIncTax;
    }

    public void setTotalAmountIncTax(BigDecimal totalAmountIncTax) {
        this.totalAmountIncTax = totalAmountIncTax;
    }

    public String getAgreementState() {
        return agreementState;
    }

    public void setAgreementState(String agreementState) {
        this.agreementState = agreementState;
    }

    public String getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(String schemeId) {
        this.schemeId = schemeId;
    }

    public String getNoticeStatus() {
        return noticeStatus;
    }

    public void setNoticeStatus(String noticeStatus) {
        this.noticeStatus = noticeStatus;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public BigDecimal getContractStartAmount() {
        return contractStartAmount;
    }

    public void setContractStartAmount(BigDecimal contractStartAmount) {
        this.contractStartAmount = contractStartAmount;
    }

    public BigDecimal getContractEndAmount() {
        return contractEndAmount;
    }

    public void setContractEndAmount(BigDecimal contractEndAmount) {
        this.contractEndAmount = contractEndAmount;
    }

    public String getBuildUnit() {
        return buildUnit;
    }

    public void setBuildUnit(String buildUnit) {
        this.buildUnit = buildUnit;
    }

    public String getResponsibleUnit() {
        return responsibleUnit;
    }

    public void setResponsibleUnit(String responsibleUnit) {
        this.responsibleUnit = responsibleUnit;
    }

    public String getProjectScale() {
        return projectScale;
    }

    public void setProjectScale(String projectScale) {
        this.projectScale = projectScale;
    }

    public String getProjectStartScale() {
        return projectStartScale;
    }

    public void setProjectStartScale(String projectStartScale) {
        this.projectStartScale = projectStartScale;
    }

    public String getProjectEndScale() {
        return projectEndScale;
    }

    public void setProjectEndScale(String projectEndScale) {
        this.projectEndScale = projectEndScale;
    }

    public String getProjectBusinessCode() {
        return projectBusinessCode;
    }

    public void setProjectBusinessCode(String projectBusinessCode) {
        this.projectBusinessCode = projectBusinessCode;
    }

    public String getProjectBusinessName() {
        return projectBusinessName;
    }

    public void setProjectBusinessName(String projectBusinessName) {
        this.projectBusinessName = projectBusinessName;
    }

    public String getEngineerTypeCode() {
        return engineerTypeCode;
    }

    public void setEngineerTypeCode(String engineerTypeCode) {
        this.engineerTypeCode = engineerTypeCode;
    }

    public String getEngineerTypeName() {
        return engineerTypeName;
    }

    public void setEngineerTypeName(String engineerTypeName) {
        this.engineerTypeName = engineerTypeName;
    }

    public String getStructureTypeCode() {
        return structureTypeCode;
    }

    public void setStructureTypeCode(String structureTypeCode) {
        this.structureTypeCode = structureTypeCode;
    }

    public String getStructureTypeName() {
        return structureTypeName;
    }

    public void setStructureTypeName(String structureTypeName) {
        this.structureTypeName = structureTypeName;
    }

    public String getProjectStatusCode() {
        return projectStatusCode;
    }

    public void setProjectStatusCode(String projectStatusCode) {
        this.projectStatusCode = projectStatusCode;
    }

    public String getProjectStatusName() {
        return projectStatusName;
    }

    public void setProjectStatusName(String projectStatusName) {
        this.projectStatusName = projectStatusName;
    }

    public Date getRealityBeginDate() {
        return realityBeginDate;
    }

    public void setRealityBeginDate(Date realityBeginDate) {
        this.realityBeginDate = realityBeginDate;
    }

    public Date getBeginStartTime() {
        return beginStartTime;
    }

    public void setBeginStartTime(Date beginStartTime) {
        this.beginStartTime = beginStartTime;
    }

    public Date getBeginEndTime() {
        return beginEndTime;
    }

    public void setBeginEndTime(Date beginEndTime) {
        this.beginEndTime = beginEndTime;
    }

    public Date getRealityFinishDate() {
        return realityFinishDate;
    }

    public void setRealityFinishDate(Date realityFinishDate) {
        this.realityFinishDate = realityFinishDate;
    }

    public Date getFinishStartTime() {
        return finishStartTime;
    }

    public void setFinishStartTime(Date finishStartTime) {
        this.finishStartTime = finishStartTime;
    }

    public Date getFinishEndTime() {
        return finishEndTime;
    }

    public void setFinishEndTime(Date finishEndTime) {
        this.finishEndTime = finishEndTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getPlanCount() {
        return planCount;
    }

    public void setPlanCount(BigDecimal planCount) {
        this.planCount = planCount;
    }

    public BigDecimal getBidCount() {
        return bidCount;
    }

    public void setBidCount(BigDecimal bidCount) {
        this.bidCount = bidCount;
    }

    public BigDecimal getSignCount() {
        return signCount;
    }

    public void setSignCount(BigDecimal signCount) {
        this.signCount = signCount;
    }

    public BigDecimal getSignContractAmount() {
        return signContractAmount;
    }

    public void setSignContractAmount(BigDecimal signContractAmount) {
        this.signContractAmount = signContractAmount;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getThridOrgId() {
        return thridOrgId;
    }

    public void setThridOrgId(String thridOrgId) {
        this.thridOrgId = thridOrgId;
    }
}
