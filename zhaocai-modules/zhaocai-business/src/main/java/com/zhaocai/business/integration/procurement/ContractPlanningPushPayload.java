package com.zhaocai.business.integration.procurement;

import com.alibaba.fastjson.annotation.JSONField;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanPushVO;
import lombok.Getter;
import lombok.Setter;

/** 合约规划推送事件载荷；鉴权令牌仅用于当前调用，不写入发件箱。 */
@Getter
@Setter
public class ContractPlanningPushPayload {
    private ProcurementPlanPushVO plan;
    private String senderName;
    private Long senderThirdUserId;
    @JSONField(serialize = false, deserialize = false)
    private String authorization;
}
