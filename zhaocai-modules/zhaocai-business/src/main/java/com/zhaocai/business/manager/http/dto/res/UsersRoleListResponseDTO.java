package com.zhaocai.business.manager.http.dto.res;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/12 11:25
 */
@Data
public class UsersRoleListResponseDTO {

    @ApiModelProperty(value = "角色名称")
    private String roleName;

    @ApiModelProperty(value = "角色权限字符串")
    private String roleKey;

    @ApiModelProperty(value = "显示顺序")
    private Integer roleSort;

    @ApiModelProperty(value = "角色id")
    private Long roleId;

    @ApiModelProperty(value = "用户列表")
    private List<UserListResponseDTO> userList;

    @Data
    public static class UserListResponseDTO{

        @ApiModelProperty(value = "用户账号")
        private String username;

        @ApiModelProperty(value = "用户编号")
        private Long userId;

        @ApiModelProperty(value = "用户昵称")
        private String nickName;

        @ApiModelProperty(value = "所属当前组织CODE")
        private String belongCurrLvlOrgCode;

        @ApiModelProperty(value = "所属当前组织名称")
        private String belongCurrLvlOrg;

        @ApiModelProperty(value = "所属管理部门CODE")
        private String belgDeptCode;

        @ApiModelProperty(value = "所属管理部门名称")
        private String belgDeptName;
    }

}
