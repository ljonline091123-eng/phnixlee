package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.business.procurement.vo.res.CompMaterialsVO;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/29 17:25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BidVO", description = "投标信息VO")
public class BidVO implements Serializable {
    private static final long serialVersionUID = 4959809980892151356L;

    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

//    @ApiModelProperty(value =  "含税总价（自动计算）")
//    private BigDecimal taxPrice;
//
//    @ApiModelProperty(value =  "不含税总价（自动计算）")
//    private BigDecimal notTaxPrice;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "提交状态（0草稿 1已提交）")
    private Integer submitStatus;

    /** ---------投标清单信息----------- */
    @ApiModelProperty(value =  "投标清单信息")
    private List<BidQuotationVO> bidQuotationVoS;

    /** ---------投标标书附件----------- */
    @ApiModelProperty(value = "投标标书附件")
    private List<AttachmentRequestVO> attachmentList;

    @ApiModelProperty(value =  "供应商投标清单信息")
    private List<CompMaterialsVO> materialsList;

}
