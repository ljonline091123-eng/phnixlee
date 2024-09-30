package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.procurement.service.IContractPlanningService;
import com.zhaocai.business.procurement.vo.res.ProcurementContractPlanListVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 合约规划
 *
 * @author chenming
 * @date 2024-06-19
 */
@Api(value = "合约规划")
@RestController
@RequestMapping("/contractPlanning")
public class ContractPlanningController extends BladeController {

    @Autowired
    private IContractPlanningService contractPlanningService;

    /**
     *获取合约规划信息
     */
    @PostMapping("/listProcurementContractPlan")
    @ApiOperation(value = "获取合约规划信息")
    public ResultData<List<ProcurementContractPlanListVO>> listProcurementContractPlan(@RequestBody List<Long> contractSplitIds) {
        return ResultData.data(contractPlanningService.listProcurementContractPlanByContractSplit(contractSplitIds));
    }
}
