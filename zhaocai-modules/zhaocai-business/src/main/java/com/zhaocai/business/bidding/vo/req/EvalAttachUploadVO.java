package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/26 17:50
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EvalAttachUploadVO", description = "评标附件上传VO")
public class EvalAttachUploadVO {

    @ApiModelProperty(value = "首轮报价投标单id")
    private Long biddingInfoId;

    @ApiModelProperty(value = "评标附件")
    private List<AttachmentRequestVO> attachmentList;

}
