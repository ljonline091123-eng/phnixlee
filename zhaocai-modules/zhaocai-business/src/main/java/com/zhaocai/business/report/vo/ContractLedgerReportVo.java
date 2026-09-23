package com.zhaocai.business.report.vo;

import java.math.BigDecimal;
import java.util.List;

public class ContractLedgerReportVo {

    /**
     * id
     */
    private String id;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 上级部门id
     */
    private String parentId;

    /**
     * 采购需求类型编码
     */
    private String procurementTypeCode;

    /**
     * 采购需求类型名称
     */
    private String procurementTypeName;

    /**
     * 采购需求类型列表
     */
    private List<ContractLedgerReportVo> procurementTypeList;

    /**
     * 合同id
     */
    private Long contractId;

    /**
     * 合同编号
     */
    private String contractCode;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 供应商id
     */
    private Long vendorId;

    /**
     * 供应商名称
     */
    private String vendorName;

    /**
     * 供应商联系人
     */
    private String vendorMainContact;

    /**
     * 供应商联系电话
     */
    private String vendorMainContactTel;

    /**
     * 合同列表
     */
    private List<ContractLedgerReportVo> contractList;

    /**
     * id
     */
    private Long schemeId;

    /**
     * 成本子目编码
     */
    private String costItemCode;

    /**
     * 成本子目名称
     */
    private String costItemName;

    /**
     * 成本子目品牌
     */
    private String costItemBrand;

    /**
     * 成本子目型号
     */
    private String costItemSpecification;

    /**
     * 计量单位
     */
    private String costItemUnit;

    /**
     * 合同单价（元）
     */
    private BigDecimal costItemUnitPrice;

    /**
     * 数量
     */
    private BigDecimal costItemCount;

    /**
     * 合同金额（元）
     */
    private BigDecimal costItemAmount;

    /**
     * 成本子目列表
     */
    private List<ContractLedgerReportVo> costItemList;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 子项列表
     */
    private List<ContractLedgerReportVo> children;

    public String getCostItemCode() {
        return costItemCode;
    }

    public void setCostItemCode(String costItemCode) {
        this.costItemCode = costItemCode;
    }

    public Long getSchemeId() {
        return schemeId;
    }

    public void setSchemeId(Long schemeId) {
        this.schemeId = schemeId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getProcurementTypeCode() {
        return procurementTypeCode;
    }

    public void setProcurementTypeCode(String procurementTypeCode) {
        this.procurementTypeCode = procurementTypeCode;
    }

    public String getProcurementTypeName() {
        return procurementTypeName;
    }

    public void setProcurementTypeName(String procurementTypeName) {
        this.procurementTypeName = procurementTypeName;
    }

    public List<ContractLedgerReportVo> getProcurementTypeList() {
        return procurementTypeList;
    }

    public void setProcurementTypeList(List<ContractLedgerReportVo> procurementTypeList) {
        this.procurementTypeList = procurementTypeList;
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

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorMainContact() {
        return vendorMainContact;
    }

    public void setVendorMainContact(String vendorMainContact) {
        this.vendorMainContact = vendorMainContact;
    }

    public String getVendorMainContactTel() {
        return vendorMainContactTel;
    }

    public void setVendorMainContactTel(String vendorMainContactTel) {
        this.vendorMainContactTel = vendorMainContactTel;
    }

    public List<ContractLedgerReportVo> getContractList() {
        return contractList;
    }

    public void setContractList(List<ContractLedgerReportVo> contractList) {
        this.contractList = contractList;
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

    public BigDecimal getCostItemUnitPrice() {
        return costItemUnitPrice;
    }

    public void setCostItemUnitPrice(BigDecimal costItemUnitPrice) {
        this.costItemUnitPrice = costItemUnitPrice;
    }

    public BigDecimal getCostItemCount() {
        return costItemCount;
    }

    public void setCostItemCount(BigDecimal costItemCount) {
        this.costItemCount = costItemCount;
    }

    public BigDecimal getCostItemAmount() {
        return costItemAmount;
    }

    public void setCostItemAmount(BigDecimal costItemAmount) {
        this.costItemAmount = costItemAmount;
    }

    public List<ContractLedgerReportVo> getCostItemList() {
        return costItemList;
    }

    public void setCostItemList(List<ContractLedgerReportVo> costItemList) {
        this.costItemList = costItemList;
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

    public List<ContractLedgerReportVo> getChildren() {
        return children;
    }

    public void setChildren(List<ContractLedgerReportVo> children) {
        this.children = children;
    }
}
