package com.zhaocai.business.procurement.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeRequestVO;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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

    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private IAttachmentService attachmentService;

    /*
    * 获取采购方案的招标文件和合同模板的文档中台的编辑URL
    * */
    @GetMapping("/getSchemeEditFileUrl")
    @ApiOperation(value = "采购方案的文档编辑URL")
    public ResultData<String> getSchemeEditFileUrl(@RequestParam("attachmentId") Long attachmentId, @RequestParam("procurementSchemeId") Long procurementSchemeId) {
        String createBy = procurementSchemeService.getProcurementSchemeInfoByID(procurementSchemeId).getCreateBy();
        String LoginUserNickName = SecurityUtils.getLoginUserNickName();
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法编辑文件！！！");
        }
        //只有创建者可以编辑文档，其他用户只能预览文档
        if(createBy.equals(LoginUserNickName)){
            return ResultData.data(attachmentService.editWordURL(attachmentId,fileName,fileUrl));
        }else {
            return ResultData.data(attachmentService.viewWordFileURL(fileName,fileUrl));
        }

    }

    @GetMapping("/ViweProcurementSchemeFile")
    @ApiModelProperty(value = "据attachmentId获取预览采购方案部分的招标文件以及合同附件附件url")
    public ResultData<String> getViweFileUrlByID(@RequestParam("attachmentId") Long attachmentId) {
        if(attachmentId == null){
            return ResultData.fail("生成文件预览url失败,attachmentId为空，请检查！");
        }
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法预览文件！！！");
        }
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        if (yozOfileUtils.isWordExtension(suffix)) {
            //显示修订记录
            return ResultData.data(attachmentService.viewWordFileUrlWithRevise(fileName,fileUrl));
        } else if(yozOfileUtils.isPdfExtension(suffix)){
            return ResultData.data(attachmentService.viewPDFFileURL(fileName,fileUrl));
        } else if (yozOfileUtils.isImageExtension(suffix)) {
            return ResultData.data(attachmentService.viewImageURL(fileName,fileUrl));
        }else {
            return ResultData.fail("无法预览该文件格式！");
        }
    }

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
    @ApiOperation(value = "获取该采购方案 对应的采购计划 内所有的采购方案列表数据。")
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
     * 作废采购方案和采购计划
     */
    @PostMapping("/cancellationProcurementSchemePlan")
    @ApiOperation(value = "作废采购方案和采购计划")
    public ResultData<Boolean> cancellationProcurementSchemePlan(@RequestParam Long id) {
        procurementSchemeService.cancellationProcurementSchemePlan(id);
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


    @ApiOperation(value = "初始化接口")
    @GetMapping ("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return procurementSchemeService.initialize(requestDTO);
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return procurementSchemeService.listProcessLog(requestDTO);
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(procurementSchemeService.audit(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }

    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return procurementSchemeService.loadTaskDef(requestDTO);
    }

}
