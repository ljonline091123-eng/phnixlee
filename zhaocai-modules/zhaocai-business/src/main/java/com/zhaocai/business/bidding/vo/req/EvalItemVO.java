package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/19 15:05
 */
@Data
@ApiModel(value = "EvalItemDTO", description = "专家评分项DTO")
public class EvalItemVO implements Serializable {
    private static final long serialVersionUID = 3961384726682312215L;

    @ApiModelProperty(value = "评分项id")
    private Long itemId;

    @ApiModelProperty(value = "分数")
    private BigDecimal score;

    @ApiModelProperty(value = "评分模板项类型")
    private Integer itemType;

}
