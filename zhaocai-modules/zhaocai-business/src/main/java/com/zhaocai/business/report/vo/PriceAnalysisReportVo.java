package com.zhaocai.business.report.vo;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class PriceAnalysisReportVo {

    /** id */
    private String id;

    /** 成本子目编码 */
    private String costItemCode;

    /** 成本子目名称 */
    private String costItemName;

    /** 成本子目品牌 */
    private String costItemBrand;

    /** 成本子目型号 */
    private String costItemSpecification;

    /** 计量单位 */
    private String costItemUnit;

    /** 地区编码 */
    private String areaCode;

    /** 地区名称 */
    private String areaName;

    /** 部门id */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 上级部门id */
    private String parentId;

    /** 项目id */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 合同id */
    private Long contractId;

    /** 合同编号 */
    private String contractCode;

    /** 合同名称 */
    private String contractName;

    /** 合同签约时间 */
    private Date contractTime;

    /** 方案id */
    private Long schemeId;

    /** 供应商id */
    private Long vendorId;

    /** 签约单价（元） */
    private BigDecimal taxUnitPrice;

    /** 签约不含税单价（元） */
    private BigDecimal notTaxUnitPrice;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 子项列表 */
    private List<PriceAnalysisReportVo> children;

    public List<PriceAnalysisReportVo> getChildren() {
        return children;
    }

    public void setChildren(List<PriceAnalysisReportVo> children) {
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCostItemCode() {
        return costItemCode;
    }

    public void setCostItemCode(String costItemCode) {
        this.costItemCode = costItemCode;
    }

    public String getCostItemName() {
        return costItemName;
    }

    public void setCostItemName(String costItemName) {
        this.costItemName = costItemName;
    }

    public String getCostItemBrand() {
        return costItemBrand;
    }

    public void setCostItemBrand(String costItemBrand) {
        this.costItemBrand = costItemBrand;
    }

    public String getCostItemSpecification() {
        return costItemSpecification;
    }

    public void setCostItemSpecification(String costItemSpecification) {
        this.costItemSpecification = costItemSpecification;
    }

    public String getCostItemUnit() {
        return costItemUnit;
    }

    public void setCostItemUnit(String costItemUnit) {
        this.costItemUnit = costItemUnit;
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public Date getContractTime() {
        return contractTime;
    }

    public void setContractTime(Date contractTime) {
        this.contractTime = contractTime;
    }

    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public BigDecimal getTaxUnitPrice() {
        return taxUnitPrice;
    }

    public void setTaxUnitPrice(BigDecimal taxUnitPrice) {
        this.taxUnitPrice = taxUnitPrice;
    }

    public BigDecimal getNotTaxUnitPrice() {
        return notTaxUnitPrice;
    }

    public void setNotTaxUnitPrice(BigDecimal notTaxUnitPrice) {
        this.notTaxUnitPrice = notTaxUnitPrice;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
