package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 模板评分项对象 tb_bidding_mark_item
 *
 * @author WH
 * @date 2024-06-18
 */
@Getter
@Setter
@TableName(value = "tb_bidding_mark_item")
public class BiddingMarkItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 评分项名称 */
    @ApiModelProperty(value =  "评分项名称")
    private String name;

    /** 分值范围-低 */
    @ApiModelProperty(value =  "分值范围-低")
    private Integer lowRange;

    /** 分值范围-高 */
    @ApiModelProperty(value =  "分值范围-高")
    private Integer highRange;

    /** 模板项id */
    @ApiModelProperty(value =  "模板项id")
    private Long categoryId;

    @ApiModelProperty(value =  "父项id")
    private Long parentId;

}
