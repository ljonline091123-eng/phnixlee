package com.zhaocai.business.bidding.vo.res;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/30 16:43
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeAnswerListVO", description = "招标公告答疑列表VO")
public class TenderNoticeAnswerListVO implements Serializable {
    private static final long serialVersionUID = -6971891261314993088L;

    @ApiModelProperty(value =  "主键id")
    private Long id;

    @ApiModelProperty(value =  "关联业务id")
    private Long busId;

    /** 业务类型（招标公告/招标内容） */
    @ApiModelProperty(value =  "业务类型")
    private Integer busType;

    /** 提问内容 */
    @ApiModelProperty(value =  "提问内容")
    private String question;

    /** 答疑内容 */
    @ApiModelProperty(value =  "答疑内容")
    private String content;

    /** 提问用户 */
    @ApiModelProperty(value =  "提问用户")
    private Long questionUser;

    /** 答疑用户 */
    @ApiModelProperty(value =  "答疑用户")
    private Long answerUser;

    @ApiModelProperty(value =  "提问供应商名称")
    private String vendorName;

}
