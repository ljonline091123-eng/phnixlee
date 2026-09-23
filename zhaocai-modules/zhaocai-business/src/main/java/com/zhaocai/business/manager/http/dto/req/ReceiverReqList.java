package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

@Data
public class ReceiverReqList {

    /**
     * 接收人ID
     */
    private String receiverId;
    /**
     * 接收人姓名
     */
    private String receiverName;
}
