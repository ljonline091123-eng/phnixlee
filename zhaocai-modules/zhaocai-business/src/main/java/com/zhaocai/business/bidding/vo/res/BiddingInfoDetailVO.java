package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.procurement.vo.res.CompMaterialsVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/3 15:04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingInfoDetailVO", description = "投标单详情信息VO")
public class BiddingInfoDetailVO extends AdviceObject {

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @MoneyFormat(filedName = "taxPrice",scale = 2)
    @ApiModelProperty(value =  "含税总价（千分位）")
    private String taxPricePattern;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "notTaxPrice",scale = 2)
    @ApiModelProperty(value =  "不含税总价（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "标书附件id")
    private Long bidAttachId;

    @ApiModelProperty(value =  "投标标书附件")
    private List<AttachmentVO> attachments;

    @ApiModelProperty(value =  "投标单清单报价详情信息")
    private List<BiddingQuotationDetailVO> quotationDetailVOList;

    @ApiModelProperty(value = "价格类型(1.固定价 2.浮动价)")
    private Integer priceType;

//    @ApiModelProperty(value = "交易标的物（ 1（钢筋） 2（砼））")
//    private String subjectMatter;

    @ApiModelProperty(value = "交易标的物类型（ 1（钢筋） 2（砼））")
    private Integer subjectMatterType;

    @ApiModelProperty(value =  "供应商投标清单信息")
    private List<CompMaterialsVO> materialsList;

}
