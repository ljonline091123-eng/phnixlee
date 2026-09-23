package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 10:49
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingMarkCategoryVO", description = "评分模板项VO")
public class BiddingMarkCategoryVO implements Serializable {
    private static final long serialVersionUID = 6458074878648447671L;

    @ApiModelProperty(value = "评分模板项类型")
    private Integer itemType;

    @ApiModelProperty(value = "项总分")
    private Integer totalScore;

    /** --------------------模板评分项信息---------------- */
    @ApiModelProperty(value = "模板评分项信息")
    private List<BiddingMarkItemVO> biddingMarkItemVOList;

}
