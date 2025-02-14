package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingListQuotationService;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 投标清单报价Controller
 * 
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/quotation")
@Api(value ="投标清单报价" )
public class BiddingListQuotationController extends BladeController {

    @Autowired
    private IBiddingListQuotationService biddingListQuotationService;

    /**
     * 获取供应商的报价清单 在 新增合同签订 时使用到了
     */
    @ApiOperation("获取供应商的报价清单")
    @GetMapping("/listVendorBiddingListQuotation")
    private ResultData<VendorBiddingListQuotationVO> listVendorBiddingListQuotation(@RequestParam Long schemeId,@RequestParam Long splitId,@RequestParam Long vendorId) {
        return ResultData.data(biddingListQuotationService.getVendorBiddingListQuotation(schemeId,splitId,vendorId));
    }
}
