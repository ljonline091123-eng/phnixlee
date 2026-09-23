package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 投标清单报价Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface BiddingListQuotationMapper extends BaseMapper<BiddingListQuotation> {

    /**
     * 获取供应商的投标清单报价
     * @param schemeId
     * @param splitId
     * @param vendorId
     * @return
     */
    List<VendorBiddingListQuotationListVO> selectVendorBiddingListQuotation(@Param("schemeId") Long schemeId, @Param("splitId") Long splitId, @Param("vendorId") Long vendorId);

    /**
     * 获取供应商的投标清单数据
     *
     * @param schemeId
     * @param contractSplitId
     * @param vendorId
     * @return
     */
    List<BiddingListQuotation> selectVendorBiddingListQuotationList(@Param("schemeId") Long schemeId, @Param("contractSplitId") Long contractSplitId, @Param("vendorId") Long vendorId);
}
