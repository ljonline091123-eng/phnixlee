package com.zhaocai.business.report.vo;

import com.zhaocai.common.core.annotation.Excel;

import java.math.BigDecimal;
import java.util.List;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeptName() {
        return DeptName;
    }

    public void setDeptName(String deptName) {
        DeptName = deptName;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<VBidCountVo> getChildren() {
        return children;
    }

    public void setChildren(List<VBidCountVo> children) {
        this.children = children;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getMinAccountFullName() {
        return minAccountFullName;
    }

    public void setMinAccountFullName(String minAccountFullName) {
        this.minAccountFullName = minAccountFullName;
    }

    public String getMinAccountCode() {
        return minAccountCode;
    }

    public void setMinAccountCode(String minAccountCode) {
        this.minAccountCode = minAccountCode;
    }

    public String getPrjState() {
        return prjState;
    }

    public void setPrjState(String prjState) {
        this.prjState = prjState;
    }

    public String getPrjStateName() {
        return prjStateName;
    }

    public void setPrjStateName(String prjStateName) {
        this.prjStateName = prjStateName;
    }

    public String getDutyUnit() {
        return dutyUnit;
    }

    public void setDutyUnit(String dutyUnit) {
        this.dutyUnit = dutyUnit;
    }

    public String getManagementOrgId() {
        return managementOrgId;
    }

    public void setManagementOrgId(String managementOrgId) {
        this.managementOrgId = managementOrgId;
    }

    public String getBelongingOrgId() {
        return belongingOrgId;
    }

    public void setBelongingOrgId(String belongingOrgId) {
        this.belongingOrgId = belongingOrgId;
    }

    public BigDecimal getCgNum() {
        return cgNum;
    }

    public void setCgNum(BigDecimal cgNum) {
        this.cgNum = cgNum;
    }

    public BigDecimal getGkNum() {
        return gkNum;
    }

    public void setGkNum(BigDecimal gkNum) {
        this.gkNum = gkNum;
    }

    public BigDecimal getYqNum() {
        return yqNum;
    }

    public void setYqNum(BigDecimal yqNum) {
        this.yqNum = yqNum;
    }

    public BigDecimal getXjNum() {
        return xjNum;
    }

    public void setXjNum(BigDecimal xjNum) {
        this.xjNum = xjNum;
    }

    public BigDecimal getDyNum() {
        return dyNum;
    }

    public void setDyNum(BigDecimal dyNum) {
        this.dyNum = dyNum;
    }

    public BigDecimal getGkTotalNum() {
        return gkTotalNum;
    }

    public void setGkTotalNum(BigDecimal gkTotalNum) {
        this.gkTotalNum = gkTotalNum;
    }

    public BigDecimal getNgkTotalNum() {
        return ngkTotalNum;
    }

    public void setNgkTotalNum(BigDecimal ngkTotalNum) {
        this.ngkTotalNum = ngkTotalNum;
    }

    public BigDecimal getnBidTotalNum() {
        return nBidTotalNum;
    }

    public void setnBidTotalNum(BigDecimal nBidTotalNum) {
        this.nBidTotalNum = nBidTotalNum;
    }

    public BigDecimal getGkRatio() {
        return gkRatio;
    }

    public void setGkRatio(BigDecimal gkRatio) {
        this.gkRatio = gkRatio;
    }
}
