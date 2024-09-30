package com.zhaocai.business.bidding.vo.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/22 15:21
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AbandonMoreVO", description = "废标更多操作参数VO")
public class AbandonMoreVO implements Serializable {
    private static final long serialVersionUID = -1474133511151105522L;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
