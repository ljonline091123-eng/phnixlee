package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/9/19 19:39
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "UrgeExpertMesVO", description = "催办信息VO")
public class UrgeExpertMesVO {

    @ApiModelProperty(value =  "招标公告主键id")
    private Long noticeId;

    @ApiModelProperty(value =  "专家id")
    private Long expertId;

    @ApiModelProperty(value =  "采购人公司")
    private String belongOrgName;

    @ApiModelProperty(value =  "采购人所属项目")
    private String projectName;


}
