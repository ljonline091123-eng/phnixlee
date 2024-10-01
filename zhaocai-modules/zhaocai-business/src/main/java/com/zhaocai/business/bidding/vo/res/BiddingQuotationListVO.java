package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/4 11:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingQuotationListVO", description = "投标单信息报价列表VO")
public class BiddingQuotationListVO extends AdviceObject implements Comparable<BiddingQuotationListVO>, Serializable {
    private static final long serialVersionUID = 7377944452027574414L;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "主键id")
    private Long id;

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "taxPrice")
    @ApiModelProperty(value =  "含税总价(元)（千分位）")
    private String taxPricePattern;

    @MoneyFormat(filedName = "notTaxPrice")
    @ApiModelProperty(value =  "不含税总价(元)（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "报价排名")
    private Integer rank;

    @ApiModelProperty(value = "中标候选人")
    private String candidate;

    @ApiModelProperty(value = "确定中标（1：确定中标人 其它：不确定中标人）")
    private Integer sureBid;

    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    @ApiModelProperty(value =  "父主键id")
    private Long parentId;

    @ApiModelProperty(value =  "投标单信息报价数据VO")
    private List<BiddingQuotationDataVO> quotationDataVOList;

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "投标标书附件")
    private List<AttachmentVO> attachments;

    @ApiModelProperty(value =  "综合得分")
    private BigDecimal score;

    @ApiModelProperty(value =  "商务评分分数")
    private BigDecimal avgBusTotalScore;

    @ApiModelProperty(value =  "技术评分分数")
    private BigDecimal avgTechTotalScore;

    @Override
    public int compareTo(@NotNull BiddingQuotationListVO o) {
        BigDecimal thisScore = this.getScore() == null ? BigDecimal.ZERO : this.getScore();
        BigDecimal oScore = o.getScore() == null ? BigDecimal.ZERO : o.getScore();
        if (thisScore.compareTo(oScore) > 0) {
            return -1;
        } else if (thisScore.compareTo(oScore) < 0) {
            return 1;
        } else {
            List<BiddingQuotationDataVO> dataVoList1 = this.getQuotationDataVOList();
            BigDecimal thisNotTaxPrice = dataVoList1.get(dataVoList1.size() - 1).getNotTaxPrice();

            List<BiddingQuotationDataVO> dataVoList2 = o.getQuotationDataVOList();
            BigDecimal oNotTaxPrice = dataVoList2.get(dataVoList2.size() - 1).getNotTaxPrice();

            if (thisNotTaxPrice.compareTo(oNotTaxPrice) > 0){
                return 1;
            } else if (thisNotTaxPrice.compareTo(oNotTaxPrice) < 0){
                return -1;
            } else {
                return 0;
            }
        }
    }

}
