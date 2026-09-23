package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class BpmSendMsgNoProcessRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 业务id
     */
    private String businessId;
    /**
     * 消息内容
     */
    private String dynamicContent;
    /**
     * 发送系统标识
     */
    private String sendSystemKey;
    /**
     * 发送人ID
     */
    private String senderId;
    /**
     * 发送人姓名
     */
    private String senderName;
    /**
     * 业务类型
     */
    private String businessType;
    /**
     * 发送时间
     */
    private String sendTime;
    /**
     * 消息类型  1待办  2通知
     */
    private String messageType;
    private String state;
    private int remindIntervalDays;
    /*
    * 接收人列表
     */
    private List<ReceiverReqList> receiverReqListList;


}
