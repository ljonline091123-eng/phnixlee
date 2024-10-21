package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.common.annotations.RepeatSubmit;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.UsersRoleListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleContractPlanListResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UsersRoleListResponseDTO;
import com.zhaocai.business.manager.http.service.PlatRoleService;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
import com.zhaocai.business.procurement.vo.req.*;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 采购计划Controller
 *
 * @author chenming
 * @date 2024-05-24
 */
@Api(value = "采购计划")
@RestController
@RequestMapping("/procurementPlan")
public class ProcurementPlanController extends BladeController {
    @Autowired
    private IProcurementPlanService procurementPlanService;

    @Autowired
    private IMaterialsListService materialsListService;

    /**
     * 列表查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "采购计划列表查询")
    public ResultData<PageResult<ProcurementPlanListVO>> listPage(@Valid ProcurementPlanListQueryVO queryVO) {
        return ResultData.data(procurementPlanService.listPage(queryVO));
    }

    /**
     * 项目合约规划列表查询
     */
    @GetMapping("/contractPlanningListPage")
    @ApiOperation(value = "项目合约规划列表查询")
    public ResultData<PageResult<ContractPlanningListVO>> contractPlanningListPage(@Valid ContractPlanningListQueryVO queryVO) {
        return ResultData.data(procurementPlanService.listContractPlanningPage(queryVO));
    }

    /**
     * 项目合约规划查询 采购总计划列表
     */
    @GetMapping("/contractPlanningList")
    @ApiOperation(value = "项目合约规划查询")
    public ResultData<ContractPlanningVO> contractPlanningList(@Valid ContractPlanningListQueryVO queryVO) {
        queryVO.setPageNumber(1);
        queryVO.setPageSize(1000000);

        return ResultData.data(procurementPlanService.listContractPlanning(queryVO));
    }

    /**
     * 获取项目合约规划物料清单
     */
    @GetMapping("/listContractMaterials")
    @ApiOperation(value = "获取项目合约规划物料清单")
    public ResultData<ContractPlanMaterialListVO> listContractMaterials(@Valid ContractPlanMaterialListQueryVO queryVO) {
        return ResultData.data(procurementPlanService.listContractMaterials(queryVO));
    }

    /**
     * 保存采购计划
     */
    @PostMapping("/saveProcurementPlan")
    @ApiOperation(value = "保存采购计划")
    @RepeatSubmit(key = "#requestVO.contractPlanning.contractPlanningId")
    public ResultData<Long> saveProcurementPlan(@RequestBody @Valid ProcurementPlanRequestVO requestVO) {
        return ResultData.data(procurementPlanService.saveProcurementPlan(requestVO));
    }

    /**
     * 提交采购计划
     */
    @PostMapping("/submitProcurementPlan")
    @ApiOperation(value = "提交采购计划")
    public ResultData<Boolean> submitProcurementPlan(@RequestParam Long id) {
        procurementPlanService.submitProcurementPlan(id);
        return ResultData.success();
    }

    /**
     * 作废采购计划
     */
    @PostMapping("/cancellationProcurementPlan")
    @ApiOperation(value = "作废采购计划")
    public ResultData<Boolean> cancellationProcurementPlan(@RequestParam Long id) {
        procurementPlanService.cancellationProcurementPlan(id);
        return ResultData.success();
    }

    /**
     * 推送采购计划
     */
    @PostMapping("/pushProcurementPlan")
    @ApiOperation(value = "推送采购计划")
    public ResultData<Boolean> pushProcurementPlan(@RequestBody ProcurementPlanPushVO planPushVO) {
        procurementPlanService.pushProcurementPlan(planPushVO);
        return ResultData.success();
    }

    /**
     * 详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "采购计划详情")
    public ResultData<ProcurementPlanDetailVO> detail(@RequestParam("id") Long id) {
        return ResultData.data(procurementPlanService.getProcurementPlanDetail(id));
    }

    /**
     * 获取采购计划的合约拆分记录
     */
    @GetMapping("/listContractSplit")
    @ApiOperation(value = "获取采购计划的合约拆分记录")
    public ResultData<List<ContractSplitListVO>> listContractSplit(@RequestParam Long id) {
        return ResultData.data(procurementPlanService.listContractSplit(id));
    }

    /**
     * 获取采购计划和拆分合约,过滤了存在采购方案的计划数据
     */
    @GetMapping("/listPlanContractSplit")
    @ApiOperation(value = "获取采购计划和拆分合约")
    public ResultData<PageResult<ProcurementPlanContractSplitVO>> listPlanContractSplit(@Valid ProcurementPlanContractSplitQueryVO queryVO) {
        queryVO.setProcurementOfficer(SecurityUtils.getUserId());
        return ResultData.data(procurementPlanService.listPlanContractSplit(queryVO));
    }

    /**
     * 获取采购计划的合约拆分物料信息
     */
    @PostMapping("/listContractSplitMaterials")
    @ApiOperation(value = "获取采购计划的合约拆分物料信息")
    public ResultData<List<ContractSplitMaterialsVO>> listContractSplitMaterials(@RequestBody ContractSplitMaterialsQueryVO queryVO) {
        return ResultData.data(materialsListService.listContractSplitMaterials(queryVO));
    }

    /**
     * 是否可拆分标识
     */
    @PostMapping("/setContractPlanSplitFlag")
    @ApiOperation(value = "是否可以拆分标识")
    public ResultData<String> setContractPlanSplitFlag(@RequestParam String flag) {
        procurementPlanService.setContractPlanSplitFlag(flag);
        return ResultData.data(flag);
    }

    /**
     * 获取合约是否可拆分标识
     */
    @GetMapping("/getContractPlanSplitFlag")
    @ApiOperation(value = "获取合约是否可拆分标识")
    public ResultData<String> getContractPlanSplitFlag() {
        return ResultData.data(procurementPlanService.getContractPlanSplitFlag());
    }


    @ApiOperation(value = "获取第三方角色用户信息接口并关联采购方案")
    @GetMapping("/getUsersRoleContractPlanList")
    public ResultData<UsersRoleContractPlanListResponseDTO> getUsersRoleContractPlanList(ContractPlanningQueryVO requestDTO) {
        return ResultData.data(procurementPlanService.getUsersRoleContractPlanList(requestDTO));
    }
}
