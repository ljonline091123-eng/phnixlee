package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 14:29
 */
@Data
@ApiModel(value = "BiddingMarkCategoryDatailVO", description = "评分模板项详情")
public class BiddingMarkCategoryDetailVO extends AdviceObject {

    @ApiModelProperty("主键id")
    private Long id;

    @ApiModelProperty(value = "评分模板项类型")
    private Integer itemType;

    @ApiModelProperty(value = "项总分")
    private Integer totalScore;

    @ApiModelProperty(value = "模板id")
    private Long templateId;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "评分模板项类型（文本）")
    @DictCache(dictBizEnum= DictBizEnum.MARK_ITEM_TYPE,filedName = "itemType")
    private String itemTypeText;

    @ApiModelProperty(value = "模板评分项信息")
    private List<BiddingMarkItemDetailVO> markItemDetailVOList;

}
