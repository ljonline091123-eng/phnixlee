package com.zhaocai.business.manager.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.BpmService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 通用接口 具体业务都是调用这里的接口，直接被前端直接调用.
 */
@RestController
@RequestMapping("/bpm")
@Api(value = "第三方流程服务", tags = "第三方流程服务")
public class BpmController extends BladeController {

    @Autowired
    private BpmService bpmService;/* 具体业务注入了这个bpm服务拼接参数再调用这里的方法 */

    @ApiOperation(value = "初始化接口")
    @GetMapping ("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return ResultData.data(bpmService.initialize(requestDTO));
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return ResultData.data(bpmService.listProcessLog(requestDTO));
    }

    @ApiOperation(value = "弃审")
    @PostMapping ("/disCard")
    public ResultData<BpmDisCardResponseDTO> disCard(@RequestBody BpmDisCardRequestDTO requestDTO) {
        return ResultData.data(bpmService.disCard(requestDTO));
    }

    @ApiOperation(value = "提交")
    @PostMapping ("/submit")
    public ResultData<BpmSubmitResponseDTO> submit(@RequestBody BpmSubmitRequestDTO requestDTO) {
        return ResultData.data(bpmService.submit(requestDTO));
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<BpmAuditResponseDTO> audit(@RequestBody BpmAuditRequestDTO requestDTO) {
        return ResultData.data(bpmService.audit(requestDTO));
    }

    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return ResultData.data(bpmService.loadTaskDef(requestDTO));
    }

}
