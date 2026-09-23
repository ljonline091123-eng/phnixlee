package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.vendor.service.IVendorCooperationService;
import com.zhaocai.business.vendor.vo.req.VendorCooperationAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperationListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperativePartnerListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperativePartnerListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 供应商合作记录
 *
 * @author chenming
 * @date 2024-06-25
 */
@Api(value = "供应商合作记录")
@RestController
@RequestMapping("/vendor/cooperation")
public class VendorCooperationController extends BladeController {

    @Autowired
    private IVendorCooperationService vendorCooperationService;

    /**
     * 列表查询
     */
    @GetMapping("/listPage")
    @ApiModelProperty("列表查询")
    public ResultData<PageResult<VendorCooperationListVO>> listPage(VendorCooperationListQueryVO queryVO) {
        return ResultData.data(vendorCooperationService.listPage(queryVO));
    }


    /**
     * 获取供应商合作单位
     */
    @GetMapping("/listVendorCooperativePartner")
    @ApiOperation("获取供应商合作单位")
    public ResultData<PageResult<VendorCooperativePartnerListVO>> listVendorCooperativePartner(VendorCooperativePartnerListQueryVO queryVO) {
        return ResultData.data(vendorCooperationService.listVendorCooperativePartner(queryVO));
    }

    /**
     * 供应商合作记录详情
     */
    @GetMapping("/listDetail")
    @ApiModelProperty("供应商合作记录详情")
    public ResultData<List<VendorCooperationAgreementVO>> listDetail(VendorCooperationAgreementListQueryVO queryVO) {
        return ResultData.data(vendorCooperationService.listDetail(queryVO));
    }
}
