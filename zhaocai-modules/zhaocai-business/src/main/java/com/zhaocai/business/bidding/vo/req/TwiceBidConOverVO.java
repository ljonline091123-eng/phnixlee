package com.zhaocai.business.bidding.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@ApiModel(value = "TwiceBidConOverVO", description = "二次洽商操作参数VO")
public class TwiceBidConOverVO {

    @ApiModelProperty(value =  "公告id")
    @NotNull(message = "公告id不能为空")
    private Long noticeId;

}
