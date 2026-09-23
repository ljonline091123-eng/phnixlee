package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息中心对象 tb_message
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_message")
public class Message extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息标题 */
    @ApiModelProperty(value =  "消息标题")
    private String messageTitle;

    /** 消息类型 */
    @ApiModelProperty(value =  "消息类型")
    private Long messageType;

    /** 消息内容 */
    @ApiModelProperty(value =  "消息内容")
    private String messageContent;

    private Long msgMan;

    private String readFlag;

    private String businessId;

    private String detailUrl;

    private String msgManName;
}
