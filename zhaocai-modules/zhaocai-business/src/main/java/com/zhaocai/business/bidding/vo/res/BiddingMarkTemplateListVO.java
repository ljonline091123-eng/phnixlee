package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/18 14:54
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingMarkTemplateListVO", description = "评分模板列表VO")
public class BiddingMarkTemplateListVO {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value =  "评分模板名称")
    private String name;

    @ApiModelProperty(value =  "启用状态（默认0未启用 1启用）")
    private Integer state;

    @ApiModelProperty(value =  "维护人")
    private String createUser;

    @ApiModelProperty(value =  "使用单位")
    private String useUnit;

    @ApiModelProperty(value =  "使用单位名称")
    private String useUnitName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

}
