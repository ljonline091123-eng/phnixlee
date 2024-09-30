package com.zhaocai.business.agreement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import lombok.Data;

import java.util.Date;

@Data
public class AgreementJkpthtListVO {

    /**
     * 合同 id
     */
    private Long agreementId;

    /**
     * 合同名称
     */
    private String agreementName;

    /**
     * 支出业务类型
     */
    @JsonIgnore
    private Integer expenditureBusinessType;

    /**
     * 支出业务分类
     */
    private String businessType;

    /**
     * 供应商 id
     */
    private Long vendorId;

    /**
     * 供应商名称
     */
    private String vendorName;


    /**
     * 中标日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date winBiddingDate;

    public String getBusinessType() {
        return ProcurementPlanTypeConver.converFromProcurementPlanType(this.getExpenditureBusinessType());
    }
}
