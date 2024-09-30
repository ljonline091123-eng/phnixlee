package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 评分模板项对象 tb_bidding_mark_category
 *
 * @author WH
 * @date 2024-06-18
 */
@Getter
@Setter
@TableName(value = "tb_bidding_mark_category")
public class BiddingMarkCategory extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 评分模板项类型 */
    @ApiModelProperty(value =  "评分模板项类型")
    private Integer itemType;

    /** 项总分 */
    @ApiModelProperty(value =  "项总分")
    private Integer totalScore;

    /** 模板id */
    @ApiModelProperty(value =  "模板id")
    private Long templateId;

}
