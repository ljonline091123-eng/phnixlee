package com.zhaocai.business.process.service;

import com.zhaocai.business.agreement.service.impl.AgreementServiceImpl;
import com.zhaocai.business.bidding.service.impl.BiddingResultServiceImpl;
import com.zhaocai.business.common.enums.ProcessKeyEnum;
import com.zhaocai.business.expert.service.impl.ExpertServiceImpl;
import com.zhaocai.business.procurement.service.impl.ProcurementSchemeServiceImpl;
import com.zhaocai.business.vendor.service.impl.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ssy
 * @date 2024/7/30 10:19
 */
public interface IBPMProcessService {

    Map<String, Class<IProcessBusinessBaseService>> PROCESS_BUSINESS_MAP = new HashMap() {{
        put(ProcessKeyEnum.ZHAOCAI_TENDER_CALIBRATE.getIdentifying(), BiddingResultServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_PROCUREMENT_SCHEME.getIdentifying(), ProcurementSchemeServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_AGREEMENT_SIGN.getIdentifying(), AgreementServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_VENDOR_REGISTER.getIdentifying(), VendorServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_EXPERT_ADD.getIdentifying(), ExpertServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATEINFO.getIdentifying(), VendorChangeServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_VENDOR_ADDCONTACT.getIdentifying(), VendorContactServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_VENDOR_MOVE_INOROUT_BLACK.getIdentifying(), VendorChangeBlackServiceImpl.class);
        put(ProcessKeyEnum.ZHAOCAI_VENDOR_UPDATE_LEVEL.getIdentifying(), VendorChangeLevelServiceImpl.class);
    }};

    /**
     * 发起流程
     * */
    String startProcessInstance(String processKey, Map<String, Object> variables);

    /**
     * 审批流程
     * */
    String auditProcessInstance(String processKey, Map<String, Object> variables);


    public String revokedProcessInstance(String processKey, Map<String, Object> variables);

    /**
     * 撤回流程
     * */
    String revokeProcess(String processKey, Map<String, Object> variables);
}
