package com.zhaocai.business.manager.controller;

import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.RoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.UsersRoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.RoleListResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleListResponseDTO;
import com.zhaocai.business.manager.http.service.PlatRoleService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/11 17:35
 */
@RestController
@RequestMapping("/platRole")
@Api(value = "第三方角色服务", tags = "第三方角色服务")
public class PlatRoleController {

    @Autowired
    private PlatRoleService platRoleService;

    @ApiOperation(value = "获取第三方角色接口")
    @GetMapping("/getRoleList")
    public ResultData<List<RoleListResponseDTO>> getRoleList(RoleListRequestDTO requestDTO) {
        return ResultData.data(platRoleService.getRoleList(requestDTO));
    }

    @ApiOperation(value = "获取第三方角色用户信息接口")
    @GetMapping("/getUsersRoleList")
    public ResultData<List<UsersRoleListResponseDTO>> getUsersRoleList(UsersRoleListRequestDTO requestDTO) {
        return ResultData.data(platRoleService.getUsersRoleList(requestDTO));
    }

}
