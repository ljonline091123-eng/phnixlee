package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/18 14:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingMarkTemplateQueryVO", description = "评分模板列表查询VO")
public class BiddingMarkTemplateQueryVO extends PageRecive implements Serializable {
    private static final long serialVersionUID = -5441792836635998825L;

    @ApiModelProperty(value = "评分模板名称")
    private String name;

    @ApiModelProperty(value = "启用状态（0未启用 1启用）")
    private Integer state;

    @ApiModelProperty(value = "单位id")
    private String[] useUnit;


    /**
     * 模板类型切换
     */
    @ApiModelProperty(value = "模板类型切换")
    private String switchTemplateType;

}
