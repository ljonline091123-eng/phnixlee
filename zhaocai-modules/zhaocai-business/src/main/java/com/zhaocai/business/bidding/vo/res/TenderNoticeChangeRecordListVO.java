package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ssy
 * @date 2024/5/31 10:18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeChangeRecordListVO", description = "招标公告变更记录VO")
public class TenderNoticeChangeRecordListVO implements Serializable {
    private static final long serialVersionUID = -3813907374934882636L;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "变更类型（1变更时间 2变更内容）")
    private Integer type;

    @ApiModelProperty(value =  "变更类型（文本）")
    private String typeText;

    @ApiModelProperty(value =  "变更前信息")
    private String updateBefore;

    @ApiModelProperty(value =  "变更后信息")
    private String updateAfter;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date createTime;

}
