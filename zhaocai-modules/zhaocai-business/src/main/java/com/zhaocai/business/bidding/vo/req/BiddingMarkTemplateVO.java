package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 10:35
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingMarkTemplateVO", description = "评分模板VO")
public class BiddingMarkTemplateVO implements Serializable {
    private static final long serialVersionUID = -6935086312656447416L;

    @ApiModelProperty(value = "模板id")
    private Long id;

    @ApiModelProperty(value =  "评分模板名称")
    private String name;

    @ApiModelProperty(value =  "维护人")
    private String createUser;

    @ApiModelProperty(value =  "使用单位")
    private String useUnit;

    /** --------------------评分模板项信息---------------- */
    @ApiModelProperty(value = "评分模板项信息")
    private List<BiddingMarkCategoryVO> biddingMarkCategoryVOList;

}
