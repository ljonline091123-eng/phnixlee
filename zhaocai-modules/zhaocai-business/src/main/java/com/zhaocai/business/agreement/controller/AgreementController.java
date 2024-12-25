package com.zhaocai.business.agreement.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementPaymentItemService;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.agreement.vo.req.*;
import com.zhaocai.business.agreement.vo.res.*;
import com.zhaocai.business.common.annotations.RepeatSubmit;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.service.IContractPlanningService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.utils.BookmarkUtils;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
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

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private BookmarkUtils bookmarkUtils;
    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Autowired
    private IAgreementPaymentItemService agreementPaymentItemService;

    /**
     * 查看合同信息时，合同附件填充数据到书签部分，并且返回填充数据后的文件预览URL
     */
    @GetMapping("/getAgreementViewURL")
    @ApiOperation(value = "合同附件预览URL")
    public ResultData<String> getAgreementViewURL(@RequestParam("attachmentId") Long attachmentId, @RequestParam("agreementId") Long agreementId, @RequestParam("waterMarkContent")String waterMarkContent) {
        if(attachmentId==null || agreementId==null || waterMarkContent==null){
            return ResultData.fail("请求参数attachmentId、agreementId、waterMarkContent中有空值，请检查！");
        }
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        ValidateUtils.isNullException(attachmentVO,"合同不存在,请确认");
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        AgreementDetailVO agreementDetailVO = agreementService.detail(agreementId);
        //合同基本信息
        AgreementVO agreementVO = agreementDetailVO.getAgreement();
        // 合同款项信息
        AgreementPaymentItemVO agreementPaymentItemVO = agreementPaymentItemService.getByAgreementId(agreementId);
        agreementPaymentItemVO.setTotalAmountExcTax(agreementVO.getTotalAmountExcTax());
        agreementPaymentItemVO.setTotalAmountIncTax(agreementVO.getTotalAmountIncTax());
        //复制到书签库
        AgreementBookmarkVO agreementBookmarkVO = new AgreementBookmarkVO();
        BeanCopierUtil.copyBean(agreementVO, agreementBookmarkVO);
        BeanCopierUtil.copyBean(agreementPaymentItemVO, agreementBookmarkVO);

        //获取计租方式的label
        Integer rentalMethod = agreementBookmarkVO.getRentalMethod();
        if(rentalMethod != null){
            String RentalMethodText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_RENTAL_METHOD.getName(),rentalMethod.toString());
            agreementBookmarkVO.setRentalMethodText(RentalMethodText);
        }
        //获取支付方式
        String PaymentWay = agreementBookmarkVO.getPaymentWay();
        if (PaymentWay != null && !PaymentWay.trim().isEmpty()) {
            // 将 paymentWay 按逗号分隔成数组
            String[] paymentWays = PaymentWay.split(",");
            // 创建一个列表来保存所有的支付方式文本
            List<String> paymentWayTexts = new ArrayList<>();
            // 遍历每一个支付方式编码并获取其文本描述
            for (String way : paymentWays) {
                if (way.trim().isEmpty()) continue; // 忽略空字符串

                String paymentWayText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PAYMENT_WAY.getName(), way.trim());
                if (paymentWayText != null && !paymentWayText.trim().isEmpty()) {
                    paymentWayTexts.add(paymentWayText);
                }
            }
            // 将所有支付方式文本用逗号连接起来
            String combinedPaymentWayText = String.join(", ", paymentWayTexts);
            // 设置支付方式文本到 VO 对象中
            agreementBookmarkVO.setPaymentWayText(combinedPaymentWayText);
        }
        //获取支付周期
        String PaymentCycle = agreementBookmarkVO.getPaymentCycle();
        if(PaymentCycle != null && !PaymentCycle.isEmpty()){
            String PaymentCycleText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PAYMENT_CYCLE.getName(),PaymentCycle);
            agreementBookmarkVO.setPaymentCycleText(PaymentCycleText);
        }
        //获取支出业务分类
        Long SplitId = agreementVO.getContractSplitId();
        if(SplitId != null){
            ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(SplitId);
            String expenditureBusinessTypeText = contractPlanning.getContractPlanningCategoryName();
            agreementBookmarkVO.setExpenditureBusinessTypeText(expenditureBusinessTypeText);
        }
        //获取价格形式
        String priceForm = agreementVO.getPriceForm();
        if(SplitId != null){
            String priceFormText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PRICE_FORM.getName(),priceForm);
            agreementBookmarkVO.setPriceFormText(priceFormText);
        }
        //进场日期和出场日期格式转换
        Date entryDate = agreementBookmarkVO.getEntryDate();
        Date finishDate = agreementBookmarkVO.getFinishDate();
        if(entryDate != null && finishDate != null){
            agreementBookmarkVO.setEntryDateText(yozOfileUtils.formatDate(entryDate, "yyyy-MM-dd"));
            agreementBookmarkVO.setFinishDateText(yozOfileUtils.formatDate(finishDate, "yyyy-MM-dd"));
        }

        String newfileURL= bookmarkUtils.FillBookmarkData(fileUrl,fileName,agreementBookmarkVO);
        System.out.println("newfileURL:"+ newfileURL);
        if(yozOfileUtils.isNULLFileURL(newfileURL)){
            return ResultData.fail("填充书签数据失败，无法编辑文件！！！");
        }
        return ResultData.data(attachmentService.viewWordFileURLWithWaterMarK(fileName,newfileURL,waterMarkContent));
    }

    /**
     * 新增合同信息时，合同附件填充数据到书签部分，并且返回填充数据后的文件编辑URL
     */
    @PostMapping("/getAgreementEditURL")
    @ApiOperation(value = "合同附件编辑URL")
    public ResultData<String> getAgreementEditURL(@RequestBody AgreementAttachmentEditRequestVO requestVO) {
        String waterMarkContent = requestVO.getAgreement().getPartyAName();
        if (waterMarkContent==null){
            return ResultData.fail("水印内容为空，检查是否获取到甲方名称！");
        }
        //获取请求参数里的agreementId
        Agreement agreement = requestVO.getAgreement();
        //获取协议付款项目
        AgreementPaymentItemVO agreementPaymentItemVO = requestVO.getAgreementPaymentItem();
        Long attachmentId = agreement.getAttachmentId();
        if(attachmentId == null){
            return ResultData.fail("attachmentId为空，请检查！");
        }
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        ValidateUtils.isNullException(attachmentVO,"合同附件不存在,请确认");
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(fileName == null || fileUrl == null){
            return ResultData.fail("fileName或者fileUrl为空，请检查！");
        }
        //获取需填充的合同数据
        AgreementBookmarkVO agreementBookmarkVO = new AgreementBookmarkVO();
        BeanCopierUtil.copyBean(agreement, agreementBookmarkVO);
        BeanCopierUtil.copyBean(agreementPaymentItemVO, agreementBookmarkVO);
        //获取计租方式的label
        Integer rentalMethod = agreementBookmarkVO.getRentalMethod();
        if(rentalMethod != null){
            String RentalMethodText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_RENTAL_METHOD.getName(),rentalMethod.toString());
            agreementBookmarkVO.setRentalMethodText(RentalMethodText);
        }
        //获取支付方式
        String PaymentWay = agreementBookmarkVO.getPaymentWay();
        if (PaymentWay != null && !PaymentWay.trim().isEmpty()) {
            // 将 paymentWay 按逗号分隔成数组
            String[] paymentWays = PaymentWay.split(",");
            // 创建一个列表来保存所有的支付方式文本
            List<String> paymentWayTexts = new ArrayList<>();
            // 遍历每一个支付方式编码并获取其文本描述
            for (String way : paymentWays) {
                if (way.trim().isEmpty()) continue; // 忽略空字符串

                String paymentWayText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PAYMENT_WAY.getName(), way.trim());
                if (paymentWayText != null && !paymentWayText.trim().isEmpty()) {
                    paymentWayTexts.add(paymentWayText);
                }
            }
            // 将所有支付方式文本用逗号连接起来
            String combinedPaymentWayText = String.join(", ", paymentWayTexts);
            // 设置支付方式文本到 VO 对象中
            agreementBookmarkVO.setPaymentWayText(combinedPaymentWayText);
        }
        //获取支付周期
        String PaymentCycle = agreementBookmarkVO.getPaymentCycle();
        if(PaymentCycle != null && !PaymentCycle.isEmpty()){
            String PaymentCycleText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PAYMENT_CYCLE.getName(),PaymentCycle);
            agreementBookmarkVO.setPaymentCycleText(PaymentCycleText);
        }
        //获取支出业务分类
        Long SplitId = agreement.getContractSplitId();
        if(SplitId != null){
            ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(SplitId);
            String expenditureBusinessTypeText = contractPlanning.getContractPlanningCategoryName();
            agreementBookmarkVO.setExpenditureBusinessTypeText(expenditureBusinessTypeText);
        }
        //获取价格形式
        String priceForm = agreement.getPriceForm();
        if(priceForm != null && !priceForm.isEmpty()){
            String priceFormText = sysDictDataService.getLabel(DictBizEnum.AGREEMENT_PRICE_FORM.getName(),priceForm);
            agreementBookmarkVO.setPriceFormText(priceFormText);
        }
        //进场日期和出场日期格式转换
        Date entryDate = agreementBookmarkVO.getEntryDate();
        Date finishDate = agreementBookmarkVO.getFinishDate();
        if(entryDate != null && finishDate != null){
            agreementBookmarkVO.setEntryDateText(yozOfileUtils.formatDate(entryDate, "yyyy-MM-dd"));
            agreementBookmarkVO.setFinishDateText(yozOfileUtils.formatDate(finishDate, "yyyy-MM-dd"));
        }

        //填充到书签位置
        String newfileURL= bookmarkUtils.FillBookmarkData(fileUrl,fileName,agreementBookmarkVO);
        if(yozOfileUtils.isNULLFileURL(newfileURL)){
            return ResultData.fail("填充书签数据失败，无法编辑文件！！！");
        }
        return ResultData.data(attachmentService.editWordURLWithWaterMark(attachmentId,fileName,newfileURL,waterMarkContent));
    }


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
    public ResultData<Boolean> submitAgreement(@RequestParam Long id, @RequestParam(required = false) String detailUrl, @RequestParam(required = false) String operateComment) {
        agreementService.submitAgreement(id, detailUrl,operateComment);
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

    /**
     * 易料合同免审提交
     * @return
     */
    @GetMapping("/avoidSubmitByMarket")
    public ResultData<Boolean> avoidSubmitByMarket(@RequestParam Long id) {
        return ResultData.data(agreementService.avoidSubmitByMarket(id));
    }
}
