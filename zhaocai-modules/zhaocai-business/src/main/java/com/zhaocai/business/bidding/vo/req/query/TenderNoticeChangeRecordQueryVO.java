package com.zhaocai.business.bidding.vo.req.query;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/5/31 10:17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeChangeRecordQueryVO", description = "招标公告变更记录VO")
public class TenderNoticeChangeRecordQueryVO {

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
