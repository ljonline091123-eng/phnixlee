package com.zhaocai.business.bidding.vo.req.query;

import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "VendorBidPdfFileRequstVO", description = "投标数据详情里的pdf招标文件VO")
public class VendorBidPdfFileRequstVO implements Serializable {

    @ApiModelProperty(value =  "招标公告id",required = true)
    private List<AttachmentVO> oldAttachmentList;


    @ApiModelProperty(value =  "招标单位")
    private String unit;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

}
