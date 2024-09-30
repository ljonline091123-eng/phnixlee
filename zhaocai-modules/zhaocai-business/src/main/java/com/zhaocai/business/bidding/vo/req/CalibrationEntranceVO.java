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
 * @date 2024/8/11 11:38
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "CalibrationVO", description = "定标VO")
public class CalibrationEntranceVO implements Serializable {
    private static final long serialVersionUID = -7506071520292178234L;

    @ApiModelProperty(value =  "定标数据")
    private List<CalibrationVO> calibrationVOList;

    @ApiModelProperty(value =  "跳转地址")
    private String detailUrl;

    @ApiModelProperty(value =  "批注")
    private  String operateComment;

    @ApiModelProperty(value = "定标文件附件")
    private List<AttachmentRequestVO> calibrationDocAttachList;

}
