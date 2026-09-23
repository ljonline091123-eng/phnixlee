package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationVO;

import java.util.List;

/**
 * 投标清单报价Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IBiddingListQuotationService  extends IService<BiddingListQuotation> {

    /**
     * 获取供应商的报价清单
     * @param schemeId
     * @param splitId
     * @param vendorId
     * @return
     */
    VendorBiddingListQuotationVO getVendorBiddingListQuotation(Long schemeId, Long splitId, Long vendorId);

    /**
     * 获取供应商的投标清单数据
     * @param schemeId
     * @param contractSplitId
     * @param vendorId
     * @return
     */
    List<BiddingListQuotation> listVendorBiddingListQuotation(Long schemeId, Long contractSplitId, Long vendorId);
}
