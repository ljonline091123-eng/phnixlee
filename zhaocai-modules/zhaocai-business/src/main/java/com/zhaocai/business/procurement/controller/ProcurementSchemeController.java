package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeRequestVO;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 采购方案Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@Api(value = "采购方案")
@RestController
@RequestMapping("/procurementScheme")
public class ProcurementSchemeController extends BladeController {

    @Autowired
    private IProcurementSchemeService procurementSchemeService;

    @Autowired
    private IContractPlanningSplitService  contractPlanningSplitService;

    @Autowired
    private IBiddingResultService biddingResultService;

    /**
     * 采购方案列表查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "采购方案列表查询")
    public ResultData<PageResult<ProcurementSchemeListVO>> listPage(@Valid ProcurementSchemeListQueryVO queryVO) {
        return ResultData.data(procurementSchemeService.listPage(queryVO));
    }

    /**
     * 校验采购方案选择的记录
     */
    @PostMapping("/checkProcurementSchemeSelect")
    @ApiModelProperty(value = "校验采购方案选择的记录")
    public ResultData<Boolean> checkProcurementSchemeSelect(@RequestBody List<Long> contractSplitIds) {
        procurementSchemeService.getProcurementSchemeCreateInfo(contractSplitIds);
        return ResultData.data(true);
    }

    /**
     * 获取采购方案新建数据
     */
    @PostMapping("/getProcurementSchemeCreateInfo")
    @ApiOperation(value = "获取采购方案新建数据")
    public ResultData<ProcurementSchemeCreateVO> getProcurementSchemeCreateInfo(@RequestBody List<Long> contractSplitIds) {
        return ResultData.data(procurementSchemeService.getProcurementSchemeCreateInfo(contractSplitIds));
    }

    /**
     * 招标管理列表查询
     */
    @GetMapping("/biddingSchemeListPage")
    @ApiOperation(value = "招标管理列表查询")
    public ResultData<PageResult<BiddingSchemeListVO>> biddingSchemeListPage(BiddingSchemeListQueryVO queryVO) {
        return ResultData.data(procurementSchemeService.biddingSchemeListPage(queryVO));
    }

    /**
     * 保存采购方案
     */
    @PostMapping("/saveProcurementScheme")
    @ApiOperation(value = "保存采购方案")
    public ResultData<Long> saveProcurementScheme(@RequestBody @Valid ProcurementSchemeRequestVO requestVO) {
        return ResultData.data(procurementSchemeService.saveProcurementScheme(requestVO));
    }

    /**
     * 提交采购方案
     */
    @PostMapping("/submitProcurementScheme")
    @ApiOperation(value = "提交采购方案")
    public ResultData<Boolean> submitProcurementScheme(Long id,String detailUrl,String operateComment) {
        procurementSchemeService.submitProcurementScheme(id,detailUrl,operateComment);
        return ResultData.success();
    }

    /**
     * 采购方案详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "采购方案详情")
    public ResultData<ProcurementSchemeDetailVO> detail(@RequestParam Long id) {
        return ResultData.data(procurementSchemeService.detail(id));
    }

    /**
     * 获取该采购方案 对应的采购计划 内所有的采购方案列表数据。
     */
    @GetMapping("/planSchemeDetail")
    @ApiOperation(value = "采购方案详情")
    public ResultData<List<ProcurementSchemeVO>> planSchemeDetail(@RequestParam Long id) {
        return ResultData.data(procurementSchemeService.planSchemeDetail(id));
    }

    /**
     * 获取采购方案的物料清单
     */
    @GetMapping("/listMaterials")
    @ApiOperation(value = "获取采购方案的物料清单")
    public ResultData<List<MaterialsVO>> listMaterials(@RequestParam Long id) {
        return ResultData.data(procurementSchemeService.listMaterials(id));
    }

    /**
     * 获取采购方案下的计划里的标包里的物料清单信息
     */
    @GetMapping("/listCompMaterials")
    @ApiOperation(value = "获取采购方案下的计划里的标包里的物料清单信息")
    public ResultData<List<CompMaterialsVO>> listCompMaterials(@RequestParam Long id) {
        return ResultData.data(procurementSchemeService.listCompMaterials(id));
    }

    /**
     * 获取采购方案的合约拆分
     */
    @GetMapping("/listContractSplit")
    @ApiOperation(value = "获取采购方案的合约拆分")
    public ResultData<List<ProcurementSchemeSplitListVO>> listContractSplit(@RequestParam Long id) {
        return ResultData.data(contractPlanningSplitService.listContractSplitBySchemeId(id));
    }

    /**
     * 获取采购方案的投标供应商
     */
    @GetMapping("/listBiddingVendor")
    @ApiOperation(value = "获取采购方案的投标供应商")
    public ResultData<List<ProcurementSchemeBiddingVendorVO>> listBiddingVendor(@RequestParam Long id) {
        return ResultData.data(biddingResultService.listBiddingVendorBySchemeId(id));
    }

    /**
     * 获取采购方案绑定的采购计划
     */
    @GetMapping("/listProcurementPlanByScheme")
    @ApiOperation(value = "获取采购方案绑定的采购计划")
    public ResultData<List<ProcurementPlanListVO>> listProcurementPlanByScheme(@RequestParam Long id) {
        return ResultData.data(procurementSchemeService.listProcurementPlanByScheme(id));
    }

    /**
     * 作废采购方案
     */
    @PostMapping("/cancellationProcurementScheme")
    @ApiOperation(value = "作废采购方案")
    public ResultData<Boolean> cancellationProcurementScheme(@RequestParam Long id) {
        procurementSchemeService.cancellationProcurementScheme(id);
        return ResultData.success();
    }

    /**
     * 撤回采购方案
     */
    @ApiOperation(value = "撤回采购方案")
    @PostMapping("/revokeProcurementScheme")
    public ResultData<Boolean> revokeProcurementScheme(@RequestParam Long id) {
        procurementSchemeService.revokeProcurementScheme(id);
        return ResultData.success();
    }
}
