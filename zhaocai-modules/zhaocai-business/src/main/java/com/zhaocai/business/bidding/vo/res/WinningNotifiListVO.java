package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author ssy
 * @date 2024/6/20 17:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "WinningNotifiListVO", description = "中标通知表数据VO")
public class WinningNotifiListVO {

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "中标通知发布时间")
    private Date notifiTime;

    @ApiModelProperty(value = "中标通知书内容")
    private String notifiContent;

    @ApiModelProperty(value = "投标结果id")
    private Long biddingResultId;

    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value = "公告id")
    private Long noticeId;

}
