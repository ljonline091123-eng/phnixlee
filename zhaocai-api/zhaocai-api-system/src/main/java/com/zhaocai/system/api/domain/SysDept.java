package com.zhaocai.system.api.domain;

import java.io.Serializable;
import java.util.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zhaocai.common.core.web.domain.BaseEntity;

/**
 * 部门表 sys_dept
 *
 * @author ruoyi
 */
public class SysDept implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    @TableId(value = "dept_id")
    private Long deptId;

    /** 父部门ID */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 部门名称 */
    private String deptName;

    /** 显示顺序 */
    private Integer orderNum;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 部门状态:0正常,1停用 */
    private String status;

    /** 父部门名称 */
    @TableField(exist = false)
    private String parentName;

    /** 来源 */
    private String origin;

    /** 第三方部门id */
    private String thridDeptId;

    /** 第三方父部门id */
    private String thridParentId;

    /**
     * 第三方组织类型
     */
    private String thridOrgType;

    /**
     * 第三方组织层次（一级组织|二级组织|.....）
     */
    private Integer thridOrgLevel;

    /** 创建者 */
    @ApiModelProperty(value = "创建者")
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者 */
    @ApiModelProperty(value = "更新者")
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    /** 是否删除 */
    @TableField("del_flag")
    @TableLogic//逻辑删除注解
    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private String delFlag;

    @TableField(exist = false)
    @ApiModelProperty(hidden = true)
    @JsonIgnore
    private Map<String, Object> params;

    @TableField(exist = false)
    @ApiModelProperty(hidden = true, value = "只查询组织机构，不查部门（1是|0否）")
    @JsonIgnore
    private Integer onlyQueryOrg;

    /** 子部门 */
    @TableField(exist = false)
    @JsonIgnore
    private List<SysDept> children = new ArrayList<SysDept>();

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getAncestors()
    {
        return ancestors;
    }

    public void setAncestors(String ancestors)
    {
        this.ancestors = ancestors;
    }

    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 100, message = "部门名称长度不能超过100个字符")
    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    @NotNull(message = "显示顺序不能为空")
    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getLeader()
    {
        return leader;
    }

    public void setLeader(String leader)
    {
        this.leader = leader;
    }

    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getParentName()
    {
        return parentName;
    }

    public void setParentName(String parentName)
    {
        this.parentName = parentName;
    }

    public List<SysDept> getChildren()
    {
        return children;
    }

    public void setChildren(List<SysDept> children)
    {
        this.children = children;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getThridDeptId() {
        return thridDeptId;
    }

    public void setThridDeptId(String thridDeptId) {
        this.thridDeptId = thridDeptId;
    }

    public String getThridParentId() {
        return thridParentId;
    }

    public void setThridParentId(String thridParentId) {
        this.thridParentId = thridParentId;
    }

    public String getThridOrgType() {
        return thridOrgType;
    }

    public void setThridOrgType(String thridOrgType) {
        this.thridOrgType = thridOrgType;
    }

    public Integer getOnlyQueryOrg() {
        return onlyQueryOrg;
    }

    public void setOnlyQueryOrg(Integer onlyQueryOrg) {
        this.onlyQueryOrg = onlyQueryOrg;
    }

    public Integer getThridOrgLevel() {
        return thridOrgLevel;
    }

    public void setThridOrgLevel(Integer thridOrgLevel) {
        this.thridOrgLevel = thridOrgLevel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("deptId", getDeptId())
            .append("parentId", getParentId())
            .append("ancestors", getAncestors())
            .append("deptName", getDeptName())
            .append("orderNum", getOrderNum())
            .append("leader", getLeader())
            .append("phone", getPhone())
            .append("email", getEmail())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("origin", getOrigin())
            .append("thridDeptId", getThridDeptId())
            .append("thridParentId", getThridParentId())
            .append("thridOrgType", getThridOrgType())
            .append("thridOrgLevel", getThridOrgLevel())
            .toString();
    }
}
