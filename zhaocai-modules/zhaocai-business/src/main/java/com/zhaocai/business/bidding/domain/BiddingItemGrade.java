package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 评分项分数对象 tb_bidding_item_grade
 *
 * @author WH
 * @date 2024-06-18
 */
@Getter
@Setter
@TableName(value = "tb_bidding_item_grade")
public class BiddingItemGrade extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 分数 */
    @ApiModelProperty(value =  "分数")
    private BigDecimal score;

    /** 评分项id */
    @ApiModelProperty(value =  "评分项id")
    private Long itemId;

    /** 专家用户id */
    @ApiModelProperty(value =  "专家用户id")
    private Long expertId;

    /** 评标的供应商id */
    @ApiModelProperty(value =  "评标的供应商id")
    private Long vendorId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemaId;

    /** 专家评分表id */
    @ApiModelProperty(value =  "专家评分表id")
    private Long expertScoreId;

}
