package com.zhaocai.business.expert.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 专家评分明细对象 tb_expert_score_detail
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_expert_score_detail")
public class ExpertScoreDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 专家评分主表id */
    @ApiModelProperty(value =  "专家评分主表id")
    private Long expertScoreId;

    /** 评分评分类型（1商务评分 2技术评分 ） */
    @ApiModelProperty(value =  "评分评分类型")
    private Integer type;

    /** 序号 */
    @ApiModelProperty(value =  "序号")
    private Integer number;

    /** 评分项 */
    @ApiModelProperty(value =  "评分项")
    private String items;

    /** 分值范围 */
    @ApiModelProperty(value =  "分值范围")
    private String range;

    /** 得分 */
    @ApiModelProperty(value =  "得分")
    private BigDecimal score;
}
