package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/19 17:34
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "CalibrationReportListVO", description = "定标报告列表数据VO")
public class CalibrationReportListVO implements Comparable<CalibrationReportListVO>, Serializable {
    private static final long serialVersionUID = 399181303516855059L;

    @ApiModelProperty(value =  "投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

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

    @ApiModelProperty(value =  "商务评分分数")
    private BigDecimal avgBusTotalScore;

    @ApiModelProperty(value =  "技术评分分数")
    private BigDecimal avgTechTotalScore;

    @ApiModelProperty(value =  "综合分")
    private BigDecimal totalScore;

    @ApiModelProperty(value = "综合排名")
    private Integer rank;

    @ApiModelProperty(value = "中标候选人")
    private String candidate;

    @ApiModelProperty(value = "确定中标（1：确定中标人 其它：不确定中标人）")
    private Integer sureBid;

    @Override
    public int compareTo(@NotNull CalibrationReportListVO o) {
        if (this.getTotalScore().compareTo(o.getTotalScore()) > 0) {
            return -1;
        } else if (this.getTotalScore().compareTo(o.getTotalScore()) < 0) {
            return 1;
        } else {
            if (this.getNotTaxPrice().compareTo(o.getNotTaxPrice()) > 0){
                return 1;
            } else if (this.getNotTaxPrice().compareTo(o.getNotTaxPrice()) < 0){
                return -1;
            } else {
                return 0;
            }
        }
    }

}
