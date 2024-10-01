package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 招标公告/招标内容 答疑对象 tb_tender_notice_answer
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_notice_answer")
public class TenderNoticeAnswer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 关联业务id（招标公告id/招标内容id） */
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

    /** 提问供应商id */
    @ApiModelProperty(value =  "提问供应商id")
    private Long vendorId;

    /** 提问供应商名称 */
    @ApiModelProperty(value =  "提问供应商名称")
    private String vendorName;

}
