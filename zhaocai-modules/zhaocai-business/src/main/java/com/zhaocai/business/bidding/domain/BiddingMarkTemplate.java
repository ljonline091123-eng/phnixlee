package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 评分模板对象 tb_bidding_mark_template
 *
 * @author WH
 * @date 2024-06-18
 */
@Getter
@Setter
@TableName(value = "tb_bidding_mark_template")
public class BiddingMarkTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 评分模板名称 */
    @ApiModelProperty(value =  "评分模板名称")
    private String name;

    /** 启用状态（默认0未启用 1启用） */
    @ApiModelProperty(value =  "启用状态（默认0未启用 1启用）")
    private Integer state;

    /** 维护人 */
    @ApiModelProperty(value =  "维护人")
    private String createUser;

    /** 使用单位 */
    @ApiModelProperty(value =  "使用单位")
    private String useUnit;


}
