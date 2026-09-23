package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 14:26
 */
@Data
@ApiModel(value = "BiddingMarkTemplateDetailVO", description = "评分模板详情")
public class BiddingMarkTemplateDetailVO {

    @ApiModelProperty(value = "模板主键id")
    private Long id;

    @ApiModelProperty(value =  "评分模板名称")
    private String name;

    @ApiModelProperty(value =  "启用状态（默认0未启用 1启用）")
    private Integer state;

    @ApiModelProperty(value = "启用状态（文本）")
    private String stateText;

    /** 维护人 */
    @ApiModelProperty(value =  "维护人")
    private String createUser;

    /** 使用单位 */
    @ApiModelProperty(value =  "使用单位")
    private String useUnit;

    @ApiModelProperty(value =  "描述")
    private String contant;

    @ApiModelProperty(value = "评分模板项信息")
    private List<BiddingMarkCategoryDetailVO> markCategoryDatailVOList;

}
