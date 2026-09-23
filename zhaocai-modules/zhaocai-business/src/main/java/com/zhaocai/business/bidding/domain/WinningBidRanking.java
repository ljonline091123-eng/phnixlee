package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 中标排行榜对象 tb_winning_bid_ranking
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_winning_bid_ranking")
public class WinningBidRanking extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 采购类型 */
    @ApiModelProperty(value =  "采购类型")
    private String procurementType;

    /** 中标供应商id */
    @ApiModelProperty(value =  "中标供应商id")
    private Long vendorId;

    /** 中标供应商名称 */
    @ApiModelProperty(value =  "中标供应商名称")
    private String vendorName;

    /** 中标次数 */
    @ApiModelProperty(value =  "中标次数")
    private Long winningBidCount;

    /** 合同总金额（元） */
    @ApiModelProperty(value =  "合同总金额")
    private BigDecimal totalAgreementAmount;

    /** 合同总次数 */
    @ApiModelProperty(value =  "合同总次数")
    private BigDecimal totalAgreementCount;

    /** 已结算金额（元） */
    @ApiModelProperty(value =  "已结算金额")
    private BigDecimal totalSettledAmount;

    /** 已付款金额（元） */
    @ApiModelProperty(value =  "已付款金额")
    private BigDecimal totalPaidAmount;

    /** 未付款金额（元） */
    @ApiModelProperty(value =  "未付款金额")
    private BigDecimal totalUnpaidAmount;
}
