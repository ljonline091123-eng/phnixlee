package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 10:56
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingMarkItemVO", description = "模板评分项信息VO")
public class BiddingMarkItemVO implements Serializable {
    private static final long serialVersionUID = 8536655720187670297L;

    @ApiModelProperty(value = "评分项名称")
    private String name;

    @ApiModelProperty(value = "分值范围-低")
    private Integer lowRange;

    @ApiModelProperty(value = "分值范围-高")
    private Integer highRange;

    @ApiModelProperty(value = "子项集合")
    private List<BiddingMarkItemVO> subBiddingMarkItemVOList;


}
