package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementDetailVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商合同
 *
 * @author chenming
 * @date 2024/06/11
 */
@Api("供应商合同")
@RestController
@RequestMapping("/vendor/agreement")
public class VendorAgreementController extends BladeController {

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private IVendorService vendorService;

    /**
     * 列表查询
     */
    @VendorStateCheck
    @GetMapping("/listAgreement")
    @ApiOperation("合同列表查询")
    public ResultData<PageResult<VendorAgreementListVO>> listAgreement(VendorAgreementListQueryVO queryVO)  {
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        queryVO.setVendorId(vendor.getId());
        return ResultData.data(agreementService.listVendorAgreement(queryVO));
    }

    /**
     * 合同明细
     */
    @VendorStateCheck
    @ApiOperation(value = "合同明细")
    @GetMapping("/detail")
    public ResultData<VendorAgreementDetailVO> detail(@RequestParam Long id) {
        return ResultData.data(agreementService.getVendorAgreementDetail(id));
    }

    /**
     * 合同下载
     */
    @VendorStateCheck
    @ApiOperation(value = "合同下载")
    @GetMapping("/downloadAgreement")
    public ResultData<Boolean> downloadAgreement(@RequestParam Long id) {
        DownloadAgreementVO downloadAgreement = agreementService.getAgreementFileInputStream(id);

        // 下载文件
        download(downloadAgreement.getFileStream(),downloadAgreement.getFileName());

        return ResultData.success();
    }

    @PostMapping("/affirmAgreement")
    @ApiOperation(value = "确认合同")
    public ResultData<Boolean> affirmAgreement(@RequestParam Long id) {
        agreementService.vendorAffirmAgreement(id);
        return ResultData.success();
    }

    /**
     * 校验合同供应商确认是否成功
     */
    @GetMapping("/checkAgreementAffirmState")
    @ApiOperation(value = "校验合同供应商确认是否成功")
    public ResultData<Boolean> checkAgreementAffirmState(@RequestParam Long id) {
        return ResultData.data(agreementService.checkAgreementAffirmState(id));
    }

    /**
     * 供应商签署合同
     */
    @PostMapping("/signAgreement")
    @ApiOperation(value = "供应商签署合同")
    public ResultData<String> signAgreement(@RequestParam Long id) {
        return ResultData.data(agreementService.vendorSignAgreement(id));
    }
}
