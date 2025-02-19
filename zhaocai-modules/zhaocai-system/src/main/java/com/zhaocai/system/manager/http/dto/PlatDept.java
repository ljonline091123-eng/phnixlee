package com.zhaocai.system.manager.http.dto;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/8 15:11
 */
public class PlatDept implements Serializable {
    private static final long serialVersionUID = 3342095024366483895L;

    @ApiModelProperty(value = "部门名称")
    private String deptName;

    @ApiModelProperty(value = "负责人")
    private String leader;

    @ApiModelProperty(value = "id")
    private String deptId;

    @ApiModelProperty(value = "显示顺序")
    private String orderNum;

    @ApiModelProperty(value = "纳税识别号")
    private String unifiedSociCrdtCd;

    @ApiModelProperty(value = "所属上级组织机构")
    private String superMgmtHirchyLpCorpOrgName;

    @ApiModelProperty(value = "父部门id")
    private String parentId;

    @ApiModelProperty(value = "所属上级组织机构编码")
    private String superMgmtHirchyLpCorpOrgCode;

    @ApiModelProperty(value = "")
    private String interialId;

    @ApiModelProperty(value = "是否纳税主体")
    private String payTaxMainIndCd;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "ztDeptId")
    private String ztDeptId;

    @ApiModelProperty(value = "")
    private String simpleName;

    @ApiModelProperty(value = "组织编号")
    private String orgCode;

    @ApiModelProperty(value = "项目类型")
    /** 张贵荣07.17 确定afxd代表公司 BM代表部门 */
    private String orgTypeCd;

    @ApiModelProperty(value = "所属二级组织机构")
    private String belgMgmtScdLvlLpCorpOrgName;

    @ApiModelProperty(value = "")
    private String addr;

    @ApiModelProperty(value = "邮箱")
    private String email;

    @ApiModelProperty(value = "所属二级组织机构编码")
    private String belgMgmtScdLvlLpCorpOrgCode;

    @ApiModelProperty(value = "简称")
    private String remark;

    @ApiModelProperty(value = "子节点")
    private List<PlatDept> children;

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

    public String getUnifiedSociCrdtCd() {
        return unifiedSociCrdtCd;
    }

    public void setUnifiedSociCrdtCd(String unifiedSociCrdtCd) {
        this.unifiedSociCrdtCd = unifiedSociCrdtCd;
    }

    public String getSuperMgmtHirchyLpCorpOrgName() {
        return superMgmtHirchyLpCorpOrgName;
    }

    public void setSuperMgmtHirchyLpCorpOrgName(String superMgmtHirchyLpCorpOrgName) {
        this.superMgmtHirchyLpCorpOrgName = superMgmtHirchyLpCorpOrgName;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getSuperMgmtHirchyLpCorpOrgCode() {
        return superMgmtHirchyLpCorpOrgCode;
    }

    public void setSuperMgmtHirchyLpCorpOrgCode(String superMgmtHirchyLpCorpOrgCode) {
        this.superMgmtHirchyLpCorpOrgCode = superMgmtHirchyLpCorpOrgCode;
    }

    public String getInterialId() {
        return interialId;
    }

    public void setInterialId(String interialId) {
        this.interialId = interialId;
    }

    public String getPayTaxMainIndCd() {
        return payTaxMainIndCd;
    }

    public void setPayTaxMainIndCd(String payTaxMainIndCd) {
        this.payTaxMainIndCd = payTaxMainIndCd;
    }

    public String getPhone() {
        return phone;
    }

    public String getZtDeptId() {
        return ztDeptId;
    }

    public void setZtDeptId(String ztDeptId) {
        this.ztDeptId = ztDeptId;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public void setSimpleName(String simpleName) {
        this.simpleName = simpleName;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgTypeCd() {
        return orgTypeCd;
    }

    public void setOrgTypeCd(String orgTypeCd) {
        this.orgTypeCd = orgTypeCd;
    }

    public String getBelgMgmtScdLvlLpCorpOrgName() {
        return belgMgmtScdLvlLpCorpOrgName;
    }

    public void setBelgMgmtScdLvlLpCorpOrgName(String belgMgmtScdLvlLpCorpOrgName) {
        this.belgMgmtScdLvlLpCorpOrgName = belgMgmtScdLvlLpCorpOrgName;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBelgMgmtScdLvlLpCorpOrgCode() {
        return belgMgmtScdLvlLpCorpOrgCode;
    }

    public void setBelgMgmtScdLvlLpCorpOrgCode(String belgMgmtScdLvlLpCorpOrgCode) {
        this.belgMgmtScdLvlLpCorpOrgCode = belgMgmtScdLvlLpCorpOrgCode;
    }

    public List<PlatDept> getChildren() {
        return children;
    }

    public void setChildren(List<PlatDept> children) {
        this.children = children;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
