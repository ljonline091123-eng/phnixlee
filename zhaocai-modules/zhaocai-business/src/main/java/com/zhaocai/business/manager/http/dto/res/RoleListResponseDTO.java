package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/12 10:05
 */
@Data
public class RoleListResponseDTO {

    @ApiModelProperty(value = "用户列表")
    private List<Object> userList;

    @ApiModelProperty(value = "公司id")
    private String corpId;

    @ApiModelProperty(value = "角色名称")
    private String roleName;

    @ApiModelProperty(value = "部门列表")
    private List<Object> departmentRespList;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "角色id")
    private String id;

    @ApiModelProperty(value = "类型 0-角色组 1-角色")
    private String roleType;

    @ApiModelProperty(value = "内置角色标记 1.内置角色 2非内置角色")
    private String roleBuiltIn;

    @ApiModelProperty(value = "上级ID，一级为0")
    private String parentId;

    @ApiModelProperty(value = "菜单列表")
    private List<Object> menuIdList;

}
