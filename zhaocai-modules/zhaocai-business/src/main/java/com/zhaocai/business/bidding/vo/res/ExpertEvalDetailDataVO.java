package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author ssy
 * @date 2024/6/24 15:02
 */
@Data
@ApiModel(value = "ExpertEvalDetailDataVO", description = "专家评分详情数据VO")
public class ExpertEvalDetailDataVO implements Serializable {
    private static final long serialVersionUID = -1061081620623938562L;

    @ApiModelProperty(value =  "分数")
    private BigDecimal score;

    @ApiModelProperty(value =  "评分项id")
    private Long itemId;

    @ApiModelProperty(value =  "专家评分表id")
    private Long expertScoreId;


    @ApiModelProperty(value =  "评分项名称")
    private String name;

    @ApiModelProperty(value =  "分值范围-低")
    private Integer lowRange;

    @ApiModelProperty(value =  "分值范围-高")
    private Integer highRange;

    @ApiModelProperty(value =  "模板项id")
    private Long categoryId;

}
