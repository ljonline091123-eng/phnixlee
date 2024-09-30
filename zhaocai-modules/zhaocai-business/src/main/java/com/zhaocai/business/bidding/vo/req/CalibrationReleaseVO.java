package com.zhaocai.business.bidding.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/20 9:40
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "CalibrationReleaseVO", description = "定标发布VO")
public class CalibrationReleaseVO implements Serializable {
    private static final long serialVersionUID = 8123246102145184542L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-起")
    @NotNull(message = "公示期时间-起不能为空")
    private Date publicityStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "公示期时间-止")
    @NotNull(message = "公示期时间-止不能为空")
    private Date publicityEndTime;

}
