package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import lombok.Data;

@Data
public class AgreementJkpthtListQueryVO extends PageRecive {

    /**
     * 归属最小核算项目编码
     */
    private String belongAccountingItemCode;

    /**
     * 合同名称
     */
    private String agreementName;
}
