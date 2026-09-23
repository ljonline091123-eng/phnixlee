package com.zhaocai.business.report.vo;

import java.math.BigDecimal;
import java.util.List;

public class TenderingRateReportVo {

    /** id */
    private String id;

    /** 部门id */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 项目id */
    private Long projectId;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 项目列表 */
    private List<TenderingRateReportVo> projectList;

    /** 采购需求类型编码 */
    private String procurementTypeCode;

    /** 采购需求类型名称 */
    private String procurementTypeName;

    /** 采购需求类型列表 */
    private List<TenderingRateReportVo> procurementTypeList;

    /** 采购方式 */
    private String procurementType;

    /** 次数 */
    private BigDecimal count;

    /** 采购次数 */
    private BigDecimal procurementCount;

    /** 公开招标次数 */
    private BigDecimal openBidCount;

    /** 邀请招标次数 */
    private BigDecimal inviteBidCount;

    /** 询价采购次数 */
    private BigDecimal enquiryProcurementCount;

    /** 单一来源次数 */
    private BigDecimal onlySourceCount;

    /** 公开招标总次数 */
    private BigDecimal openBidTotalCount;

    /** 非公开招标总次数 */
    private BigDecimal noOpenBidTotalCount;

    /** 公开招标率 */
    private BigDecimal openBidRate;

    /** 非公开招标率 */
    private BigDecimal noOpenBidRate;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 子项列表 */
    private List<TenderingRateReportVo> children;

    /** 上级部门id */
    private String parentId;

    public String getProcurementType() {
        return procurementType;
    }

    public void setProcurementType(String procurementType) {
        this.procurementType = procurementType;
    }

    public BigDecimal getCount() {
        return count;
    }

    public void setCount(BigDecimal count) {
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TenderingRateReportVo> getChildren() {
        return children;
    }

    public void setChildren(List<TenderingRateReportVo> children) {
        this.children = children;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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

    public List<TenderingRateReportVo> getProjectList() {
        return projectList;
    }

    public void setProjectList(List<TenderingRateReportVo> projectList) {
        this.projectList = projectList;
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

    public List<TenderingRateReportVo> getProcurementTypeList() {
        return procurementTypeList;
    }

    public void setProcurementTypeList(List<TenderingRateReportVo> procurementTypeList) {
        this.procurementTypeList = procurementTypeList;
    }

    public BigDecimal getProcurementCount() {
        return procurementCount;
    }

    public void setProcurementCount(BigDecimal procurementCount) {
        this.procurementCount = procurementCount;
    }

    public BigDecimal getOpenBidCount() {
        return openBidCount;
    }

    public void setOpenBidCount(BigDecimal openBidCount) {
        this.openBidCount = openBidCount;
    }

    public BigDecimal getInviteBidCount() {
        return inviteBidCount;
    }

    public void setInviteBidCount(BigDecimal inviteBidCount) {
        this.inviteBidCount = inviteBidCount;
    }

    public BigDecimal getEnquiryProcurementCount() {
        return enquiryProcurementCount;
    }

    public void setEnquiryProcurementCount(BigDecimal enquiryProcurementCount) {
        this.enquiryProcurementCount = enquiryProcurementCount;
    }

    public BigDecimal getOnlySourceCount() {
        return onlySourceCount;
    }

    public void setOnlySourceCount(BigDecimal onlySourceCount) {
        this.onlySourceCount = onlySourceCount;
    }

    public BigDecimal getOpenBidTotalCount() {
        return openBidTotalCount;
    }

    public void setOpenBidTotalCount(BigDecimal openBidTotalCount) {
        this.openBidTotalCount = openBidTotalCount;
    }

    public BigDecimal getNoOpenBidTotalCount() {
        return noOpenBidTotalCount;
    }

    public void setNoOpenBidTotalCount(BigDecimal noOpenBidTotalCount) {
        this.noOpenBidTotalCount = noOpenBidTotalCount;
    }

    public BigDecimal getOpenBidRate() {
        return openBidRate;
    }

    public void setOpenBidRate(BigDecimal openBidRate) {
        this.openBidRate = openBidRate;
    }

    public BigDecimal getNoOpenBidRate() {
        return noOpenBidRate;
    }

    public void setNoOpenBidRate(BigDecimal noOpenBidRate) {
        this.noOpenBidRate = noOpenBidRate;
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
