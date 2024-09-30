package com.zhaocai.business.manager.http.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/7/9 14:05
 */
@Data
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



}
