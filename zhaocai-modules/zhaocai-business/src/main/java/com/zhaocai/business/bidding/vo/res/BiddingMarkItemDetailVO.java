package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 14:30
 */
@Data
@ApiModel(value = "BiddingMarkItemDetailVO", description = "模板评分项详情")
public class BiddingMarkItemDetailVO {

    @ApiModelProperty("主键id")
    private Long id;

    @ApiModelProperty(value = "评分项名称")
    private String name;

    @ApiModelProperty(value = "分值范围-低")
    private Integer lowRange;

    @ApiModelProperty(value = "分值范围-高")
    private Integer highRange;

    @ApiModelProperty(value = "模板项id")
    private Long categoryId;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value =  "父项id")
    private Long parentId;

    @ApiModelProperty(value =  "子项模板评分项详情")
    private List<BiddingMarkItemDetailVO> subBiddingMarkItemDetailVOList;


}
