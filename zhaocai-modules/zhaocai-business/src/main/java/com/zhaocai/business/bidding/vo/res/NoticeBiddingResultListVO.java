package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/6/20 13:49
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "NoticeBiddingResultListVO", description = "招标公告答疑列表VO")
public class NoticeBiddingResultListVO implements Serializable {
    private static final long serialVersionUID = 8496803580444008382L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "投标结果id")
    private Long biddingResultId;

    @ApiModelProperty(value = "中标结果（0未中标 1已中标）")
    private Integer bidResult;

    @ApiModelProperty(value = "综合排名")
    private Integer rank;

    @ApiModelProperty(value = "确定中标（1：确定中标人 其它：不确定中标人）")
    private Integer sureBid;

}
