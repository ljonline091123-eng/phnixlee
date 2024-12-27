package com.zhaocai.business.vendor.controller;

import com.alibaba.fastjson.JSONObject;
import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.enums.SupplierRegistSourceEnum;
import com.zhaocai.business.common.enums.VendorContactStateEnum;
import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.BpmAuditRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmInitializeRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmListProcessLogRequestDTO;
import com.zhaocai.business.manager.http.dto.req.BpmLoadTaskDefRequestDTO;
import com.zhaocai.business.manager.http.dto.res.BpmAuditResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.utils.YOZOfileUtils;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorOneRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorRegisterRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorSaveRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorDetailVO;
import com.zhaocai.business.vendor.vo.res.VendorIndexInfoVO;
import com.zhaocai.business.vendor.vo.res.VendorSignAuthInfo;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

/**
 * 供应商Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@Api("供应商")
@RestController
@RequestMapping("/vendor")
public class VendorController extends BladeController {

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private YOZOfileUtils yozOfileUtils;

    @Autowired
    private IVendorContactService vendorContactService;

    /**
     * 投标管理-把招标公告转为PDF、加水印并生成预览URL
     */
    @GetMapping("/getViewNoticeURL")
    @ApiOperation("招标公告转PDF并预览")
    public ResultData<String> getViewNoticeURL(@RequestParam("attachmentId") Long attachmentId,@RequestParam("unit") String unit) {
        AttachmentVO attachmentVO = attachmentService.getAttachmentById(attachmentId);
        String fileName = attachmentVO.getFileName();
        String fileUrl = attachmentVO.getFileUrl();
        String waterMarkContent = unit;
        if(yozOfileUtils.isNULLFileURL(fileUrl)){
            return ResultData.fail("该文件存储的fileUrl为空，无法预览文件！！！");
        }
        String suffix = yozOfileUtils.getSuffix(fileName).toLowerCase();
        if (yozOfileUtils.isWordExtension(suffix)) {
            String PDFUrl = attachmentService.convertOfficeToPdf(fileName,fileUrl,waterMarkContent);
            String PDFfileName = yozOfileUtils.removeSuffix(fileName) + ".pdf";
            String viewURL = attachmentService.viewPDFFileURL(PDFfileName,PDFUrl);
            return ResultData.data(viewURL);
        }else {
            return ResultData.fail("非word文档格式，文件格式错误，无法预览");
        }
    }

    /**
     * 供应商注册
     */
    @Log(title = "供应商注册", businessType = BusinessType.INSERT)
    @PostMapping("/register")
    @ApiOperation("供应商注册")
    public ResultData<Boolean> register(@RequestBody @Valid VendorRegisterRequestVO requestVO) {
        vendorService.register(requestVO);
        return ResultData.success();
    }

    /**
     * 注册详情查看
     */
    @GetMapping("/registerDetail")
    @ApiOperation("供应商注册详情")
    public ResultData<VendorRegisterRequestVO> registerDetail(Long vendorId) {
        return ResultData.data(vendorService.getVendorUpdateDetail(vendorId));
    }

    /**
     * 判断是否已经注册成功了
     */
    @GetMapping("/checkRegister")
    @ApiOperation("供应商注册详情")
    public ResultData<String> checkregister(Long vendorId) {
        return ResultData.success(vendorService.checkRegister(vendorId));
    }


    /**
     * 供应商详情
     */
    @GetMapping("/detail")
    @ApiOperation("供应商详情")
    public ResultData<VendorDetailVO> detail() {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        return ResultData.data(vendorService.getVendorDetail(vendor.getId()));
    }

    /**
     * 保存供应商信息
     */
    @VendorStateCheck
    @Log(title = "保存供应商信息", businessType = BusinessType.UPDATE)
    @PostMapping("/saveVendor")
    @ApiOperation("保存供应商信息")
    public ResultData<String> saveVendor(@RequestBody @Valid VendorSaveRequestVO requestVO) {
        vendorService.saveVendor(requestVO);
        return ResultData.success();
    }

    /**
     * 获取基本信息
     */
    @GetMapping("/getBaseInfo")
    @ApiOperation("获取基本信息")
    public ResultData<VendorIndexInfoVO> getBaseInfo() {
        return ResultData.data(vendorService.getBaseInfo());
    }

    /**
     * 校验企业名称
     *
     * @param enterpriseName
     * @return
     */
    @GetMapping("/checkEnterpriseName")
    @ApiOperation(value = "校验企业名称")
    public ResultData<String> checkEnterpriseName(@RequestParam String enterpriseName) {
        return ResultData.success(vendorService.checkEnterpriseName(enterpriseName));
    }

    /**
     * 校验企业名称
     *
     * @param enterpriseName
     * @param vendorId
     * @return
     */
    @GetMapping("/checkEnterpriseNameAndId")
    @ApiOperation(value = "校验企业名称和id")
    public ResultData<String> checkEnterpriseNameAndId(@RequestParam("enterpriseName") String enterpriseName ,@RequestParam("vendorId") Long vendorId) {
        return ResultData.success(vendorService.checkEnterpriseNameAndId(enterpriseName,vendorId));
    }

    /**
     * 签章注册
     */
    @PostMapping("/vendorSignAuth")
    @ApiOperation(value = "签章注册")
    public ResultData<String> vendorSignAuth() {
        return ResultData.data(vendorService.vendorSignAuth());
    }

    /**
     * 获取供应商认证状态
     */
    @GetMapping("/getVendorSignAuthInfo")
    @ApiOperation(value = "获取供应商认证状态")
    public ResultData<VendorSignAuthInfo> getVendorSignAuthInfo() {
        return ResultData.data(vendorService.getVendorSignAuthInfo());
    }


    @ApiOperation(value = "初始化接口")
    @GetMapping ("/initialize")
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return vendorService.initialize(requestDTO);
    }

    @ApiOperation(value = "流程操作日志列表接口")
    @GetMapping ("/listProcessLog")
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return vendorService.listProcessLog(requestDTO);
    }

    @ApiOperation(value = "审批")
    @PostMapping ("/audit")
    public ResultData<String> audit(@ApiIgnore @RequestBody JSONObject body) {
        String processKey = body.getString("processKey");
        body.remove("processKey");
        try {
            return ResultData.data(vendorService.audit(processKey, body));
        } catch (Exception e) {
            return ResultData.fail(e.getLocalizedMessage());
        }
    }
    @ApiOperation(value = "加载定义接口")
    @GetMapping("/loadTaskDef")
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return vendorService.loadTaskDef(requestDTO);
    }

    @ApiOperation(value = "撤回供应商流程")
    @PostMapping("/revokeVendor")
    public ResultData<Boolean> revokeVendor(@RequestParam Long id) {
        vendorService.revokeVendor(id);
        return ResultData.success();
    }

    /**
     * 供应商注册
     */
    @Log(title = "供应商联系人注册", businessType = BusinessType.INSERT)
    @PostMapping("/registerLinkman")
    @ApiOperation("供应商联系人注册")
    public ResultData<Boolean> registerLinkman(@RequestBody @Valid VendorOneRequestVO requestVO) {
        vendorService.registerLinkman(requestVO);
        return ResultData.success();
    }

    @GetMapping("/getVendor")
    @ApiOperation("获取用户供应商")
    public ResultData<VendorOneRequestVO> getVendor() {
        VendorOneRequestVO bean = new VendorOneRequestVO();
        long userId = SecurityUtils.getUserId();
        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(userId);
        ValidateUtils.validateStatusNotEquals(VendorContactStateEnum.VALID::equalsState,vendorContact.getState(),"您当前已被禁用!!!");
        Vendor vendor = vendorService.getById(vendorContact.getVendorId());
        bean.setVendor(vendor);
        bean.setVendorContact(vendorContact);
        return ResultData.data(bean);
    }

    @GetMapping("/loginUserDetail")
    @ApiOperation("供应商注册详情")
    public ResultData<VendorOneRequestVO> loginUserDetail(Long loginUserId) {
        return ResultData.data(vendorService.getLoginUserDetail(loginUserId));
    }

    @VendorStateCheck
    @Log(title = "供应商注册保存", businessType = BusinessType.UPDATE)
    @PostMapping("/registerSave")
    @ApiOperation("供应商注册保存")
    public ResultData<String> registerSave(@RequestBody @Valid VendorOneRequestVO requestVO) {
        //vendorService.saveVendor(requestVO);
        vendorService.registerSave(requestVO);
        return ResultData.success();
    }
}
