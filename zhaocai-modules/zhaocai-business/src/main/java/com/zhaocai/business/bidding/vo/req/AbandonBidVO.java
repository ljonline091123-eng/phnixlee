package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/31 18:03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AbandonBidVO", description = "废标操作参数VO")
public class AbandonBidVO implements Serializable {
    private static final long serialVersionUID = 8821027445051440846L;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "废标原因")
    private String reason;

    @ApiModelProperty(value =  "废标附件id")
    private Long attachId;

    @ApiModelProperty(value =  "投标单id")
    private List<Long> biddingInfoIds;

    @ApiModelProperty(value = "投标标书附件")
    private List<AttachmentRequestVO> attachmentList;

    @ApiModelProperty(value = "废标更多信息")
    private List<AbandonMoreVO> abandonMoreVOList;



}
