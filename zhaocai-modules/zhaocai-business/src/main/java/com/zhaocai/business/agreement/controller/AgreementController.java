package com.zhaocai.business.agreement.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.agreement.vo.req.*;
import com.zhaocai.business.agreement.vo.res.*;
import com.zhaocai.business.common.annotations.RepeatSubmit;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
 * 合同基本信息Controller
 *
 * @author chenming
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/agreement")
@Api(value = "合同管理")
public class AgreementController extends BladeController {

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private IProcurementSchemeService procurementSchemeService;

    /**
     * 合同列表查询
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "合同列表查询")
    public ResultData<PageResult<AgreementListVO>> listPage(@Valid AgreementListQueryVO queryVO) {
        return ResultData.data(agreementService.listPage(queryVO));
    }

    /**
     * 获取可签订合同的采购方案
     */
    @GetMapping("/listSignAgreementScheme")
    @ApiOperation(value = "获取可签订合同的采购方案")
    public ResultData<PageResult<AgreementSchemeListVO>> listSignAgreementScheme(@Valid AgreementSchemeQueryVO queryVO) {
        return ResultData.data(procurementSchemeService.listSignAgreementScheme(queryVO));
    }

    /**
     * 校验创建合同基本信息
     */
    @PostMapping("/checkAgreementCreateInfo")
    @ApiOperation(value = "校验创建合同基本信息")
    public ResultData<Boolean> checkAgreementCreateInfo(@Valid @RequestBody AgreementCreateInfoRequestVO requestVO) {
        return ResultData.data(agreementService.checkAgreementCreateInfo(requestVO));
    }

    /**
     * 获取创建合同的基本信息
     */
    @PostMapping("/getAgreementCreateInfo")
    @ApiOperation(value = "获取创建合同的基本信息")
    public ResultData<AgreementCreateBaseInfoVO> getAgreementCreateInfo(@Valid @RequestBody AgreementCreateInfoRequestVO requestVO) {
        return ResultData.data(agreementService.getAgreementCreateInfo(requestVO));
    }

    /**
     * 保存合同
     */
    @PostMapping("/saveAgreement")
    @ApiOperation(value = "保存合同")
    @RepeatSubmit(key = "#requestVO.agreement.schemeId + '-' + #requestVO.agreement.contractSplitId + '-' + #requestVO.agreement.vendorId")
    public ResultData<AgreementSaveVO> saveAgreement(@RequestBody AgreementSaveRequestVO requestVO) {
        return ResultData.data(agreementService.saveAgreement(requestVO));
    }

    /**
     * 合同详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "合同详情")
    public ResultData<AgreementDetailVO> detail(@RequestParam Long id) {
        return ResultData.data(agreementService.detail(id));
    }

    /**
     * 获取合同附件
     */
    @GetMapping("/getAgreementAttachmentId")
    @ApiOperation(value = "获取合同附件")
    public ResultData<AgreementFileVO> getAgreementAttachmentId(@RequestParam Long id) {
        return ResultData.data(agreementService.getAgreementAttachmentId(id));
    }

    /**
     * 作废合同
     */
    @PostMapping("/cancellationAgreement")
    @ApiOperation(value = "作废合同")
    public ResultData<Boolean> cancellationAgreement(@RequestParam Long id) {
        agreementService.cancellationAgreement(id);
        return ResultData.success();
    }

    /**
     * 提交合同
     */
    @PostMapping("/submitAgreement")
    @ApiOperation(value = "提交合同")
    public ResultData<Boolean> submitAgreement(@RequestParam Long id, @RequestParam(required = false) String detailUrl) {
        agreementService.submitAgreement(id, detailUrl);
        return ResultData.success();
    }

    /**
     * 校验合同是否可修改
     */
    @GetMapping("/checkAgreementUpdate")
    public ResultData<Boolean> checkAgreementUpdate(@RequestParam Long id) {
        return ResultData.data(agreementService.checkAgreementUpdate(id));
    }

    /**
     * 获取合同标签附件 id
     * @return
     */
    @GetMapping("/getLabelAttachmentId")
    public ResultData<Long> getLabelAttachmentId(@RequestParam Long id) {
        return ResultData.data(agreementService.getLabelAttachmentId(id));
    }

    /**
     * 撤回合同
     */
    @ApiOperation(value = "撤回合同")
    @PostMapping("/revokeAgreement")
    public ResultData<Boolean> revokeAgreement(@RequestParam Long id) {
        agreementService.revokeAgreement(id);
        return ResultData.success();
    }

    /**
     * 获取合同选定的采购相关信息
     */
    @GetMapping("/getAgreementSelectedProcurementInfo")
    @ApiOperation(value = "获取合同选定的采购相关信息")
    public ResultData<AgreementSelectedProcurementInfoVO> getAgreementSelectedProcurementInfo(@RequestParam("schemeId") Long schemeId, @RequestParam("contractSplitId") Long contractSplitId) {
        return ResultData.data(agreementService.getAgreementSelectedProcurementInfo(schemeId,contractSplitId));
    }


    /**
     * 推送合同至供应商
     */
    @ApiOperation(value = "推送合同至供应商")
    @PostMapping("/pushAgreementToVendor")
    public ResultData<Boolean> pushAgreementToVendor(@RequestParam Long id) {
        agreementService.pushAgreementToVendor(id);
        return ResultData.success();
    }

    /**
     * 推送合同至电子签章平台
     */
    @ApiOperation(value = "推送合同至电子签章平台")
    @PostMapping("/pushAgreementToSignPlatform")
    @RepeatSubmit(key = "#pushAgreementToSign.id")
    public ResultData<Boolean> pushAgreementToSignPlatform(@RequestBody @Valid PushAgreementToSignVO pushAgreementToSign) {
        agreementService.pushAgreementToSignPlatform(pushAgreementToSign);
        return ResultData.success();
    }

    /**
     * 签署合同
     */
    @ApiOperation(value = "签署合同")
    @PostMapping("/signAgreement")
    public ResultData<String> signAgreement(@RequestParam Long id) {
        return ResultData.data(agreementService.signAgreement(id));
    }

    /**
     * 作废已签署合同
     */
    @ApiOperation(value = "作废已签署合同")
    @PostMapping("/cancelledSignAgreement")
    public ResultData<Boolean> cancelledSignAgreement(@RequestBody CancelledSignAgreementRequestVO requestVO) {
        agreementService.cancelledSignAgreement(requestVO);
        return ResultData.success();
    }


    @ApiOperation(value = "初始化接口")
    @GetMapping ("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return agreementService.initialize(requestDTO);
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return agreementService.listProcessLog(requestDTO);
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(agreementService.audit(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return agreementService.loadTaskDef(requestDTO);
    }
}
