package com.zhaocai.business.manager.http.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserObj {

    /**
     * 业务id
     */
    private String businessId;
    /**
     * 待办类型（1 审批待办，2 工作通知）
     */
    private String toDoType;

    /**
     * 业务类型
     */
    private String businessType;


    /**
     * 通知id
     */
    private  Long noticeId;

    /**
     * 采购方案id
     */
    private  Long schemeId;






}
