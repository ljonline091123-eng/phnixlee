package com.zhaocai.business.manager.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.PlatUser;
import com.zhaocai.business.manager.http.service.PlatUserService;
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
 * @date 2024/7/9 13:55
 */
@RestController
@RequestMapping("/platUser")
@Api(value = "第三方系统用户", tags = "第三方系统用户接口")
public class PlatUserController extends BladeController {

    @Autowired
    private PlatUserService platUserService;

    @ApiOperation(value = "查询用户列表")
    @GetMapping("/getPlatUser")
    public ResultData<List<PlatUser>> getPlatUser() {
        List<PlatUser> users = platUserService.getPlatUser();

        return ResultData.data(users);
    }

}
