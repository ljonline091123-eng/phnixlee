package com.zhaocai.system.manager.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/9 14:05
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PlatUser implements Serializable {
    private static final long serialVersionUID = 7243553174276035725L;

    @ApiModelProperty(value = "用工类型")
    private String yglx;

    @ApiModelProperty(value = "所属部门")
    private String belgDeptName;

    @ApiModelProperty(value = "用户昵称")
    private String nickName;

    @ApiModelProperty(value = "用户性别（0男 1女 2未知）")
    private String sex;

    @ApiModelProperty(value = "部门ID")
    private String deptId;

    @ApiModelProperty(value = "手机号码")
    private String phonenumber;

    @ApiModelProperty(value = "用户账号")
    private String userName;

    @ApiModelProperty(value = "用户ID")
    private String userId;

    @ApiModelProperty(value = "所属本级组织CODE")
    private String belongCurrLvlOrgCode;

    @ApiModelProperty(value = "是否兼岗")
    private String jz;

    @ApiModelProperty(value = "岗位")
    private String postName;

    @ApiModelProperty(value = "所属部门CODE")
    private String belgDeptCode;

    @ApiModelProperty(value = "所属本级组织")
    private String belongCurrLvlOrg;

    @ApiModelProperty(value = "用户邮箱")
    private String email;

    public String getYglx() {
        return yglx;
    }

    public void setYglx(String yglx) {
        this.yglx = yglx;
    }

    public String getBelgDeptName() {
        return belgDeptName;
    }

    public void setBelgDeptName(String belgDeptName) {
        this.belgDeptName = belgDeptName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBelongCurrLvlOrgCode() {
        return belongCurrLvlOrgCode;
    }

    public void setBelongCurrLvlOrgCode(String belongCurrLvlOrgCode) {
        this.belongCurrLvlOrgCode = belongCurrLvlOrgCode;
    }

    public String getJz() {
        return jz;
    }

    public void setJz(String jz) {
        this.jz = jz;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getBelgDeptCode() {
        return belgDeptCode;
    }

    public void setBelgDeptCode(String belgDeptCode) {
        this.belgDeptCode = belgDeptCode;
    }

    public String getBelongCurrLvlOrg() {
        return belongCurrLvlOrg;
    }

    public void setBelongCurrLvlOrg(String belongCurrLvlOrg) {
        this.belongCurrLvlOrg = belongCurrLvlOrg;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
