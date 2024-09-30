package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/20 10:20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ResultReleasVO", description = "结果发布VO")
public class ResultReleasVO implements Serializable {
    private static final long serialVersionUID = -7099964368944101377L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value = "中标通知书内容")
    private String notifiContent;

}
