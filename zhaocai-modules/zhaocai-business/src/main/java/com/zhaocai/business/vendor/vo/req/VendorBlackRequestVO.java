package com.zhaocai.business.vendor.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 供应商级请求
 *
 * @author chenming
 * @date 2024/05/31
 */
@Data
public class VendorBlackRequestVO {

    @ApiModelProperty(value =  "供应商 id")
    private Long id;

    @ApiModelProperty(value =  "黑名单状态")
    private Integer blackState;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-开始日期")
    private Date blackBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "黑名单限制期-结束日期")
    private Date blackEndDate;

    @ApiModelProperty(value = "附件")
    private List<AttachmentRequestVO> attachmentList;

    @ApiModelProperty(value =  "黑名单状态")
    private Integer isBlack;
}
