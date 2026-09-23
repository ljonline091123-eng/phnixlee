package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;


/**
 * 供应商投标清单报价
 *
 * @author chenming
 * @date 2024-06-18
 */
@Data
public class VendorBiddingListQuotationVO extends AdviceObject {

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @ApiModelProperty(value = "交易标的物编码")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物名称")
    private String subjectMatterName;

    @ApiModelProperty(value = "价格类型")
    private Integer priceType;

    @ApiModelProperty(value = "清单列表")
    List<VendorBiddingListQuotationListVO> vendorBiddingListQuotationList;
}
