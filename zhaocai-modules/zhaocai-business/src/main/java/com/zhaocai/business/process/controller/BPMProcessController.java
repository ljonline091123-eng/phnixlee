package com.zhaocai.business.process.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author ssy
 * @date 2024/7/30 10:09
 */
@RestController
@RequestMapping("/process")
@Api(value = "流程接口", tags = "流程接口")
public class BPMProcessController {

    @Autowired
    private IBPMProcessService processService;

    @PostMapping("startProcess")
    @ApiOperation("发起流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processDefKey", value = "流程定义key", required = true),
    })
    public ResultData<String> startProcess(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(processService.startProcessInstance(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @PostMapping("auditProcess")
    @ApiOperation("审批流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processDefKey", value = "流程定义key", required = true),
    })
    public ResultData<String> auditProcess(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(processService.auditProcessInstance(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @PostMapping("revokedProcess")
    @ApiOperation("撤销流程")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "processDefKey", value = "流程定义key", required = true),
    })
    public ResultData<String> revokedProcess(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(processService.revokedProcessInstance(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @PostMapping("getOrg")
    @ApiOperation("根据第三方组织获取二三级单位值")
    public ResultData<String> getOrg(@ApiIgnore @RequestBody String org) {
        try {
            return ResultData.data(processService.getOrg(org));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @PostMapping("getOrgByUserId")
    @ApiOperation("根据用户id获取二三级单位值")
    public ResultData<String> getOrgByUserId(@ApiIgnore @RequestBody String userId) {
        try {
            return ResultData.data(processService.getOrgByUserId(userId));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

}
