package com.zhaocai.business.bidding.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/5 15:10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TwiceBidConfVO", description = "二次洽商操作参数VO")
public class TwiceBidConfVO {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "二次报价截止时间")
    @NotNull(message = "二次报价截止时间不能为空")
    private Date twiceTime;

    @ApiModelProperty(value =  "公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "二次报价投标单id")
    private List<Long> biddingInfoIds;

}
