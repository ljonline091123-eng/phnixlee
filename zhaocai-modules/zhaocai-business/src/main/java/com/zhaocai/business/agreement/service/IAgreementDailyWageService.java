package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementDailyWage;
import com.zhaocai.business.agreement.vo.res.AgreementDailyWageVO;

import java.util.List;

/**
 * 合同-计日工Service接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface IAgreementDailyWageService  extends IService<AgreementDailyWage> {

    /**
     * 保存合同-计日工信息
     * @param agreementDailyWageList
     * @param agreementId
     */
    void saveAgreementDailyWage(List<AgreementDailyWage> agreementDailyWageList, Long agreementId);

    /**
     * 根据合同编号获取合同-计日工
     * @param agreementId
     * @return
     */
    List<AgreementDailyWageVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同-计日工
     * @param agreementDailyWageList
     * @param agreementId
     */
    void updateAgreementDailyWage(List<AgreementDailyWage> agreementDailyWageList, Long agreementId);
}
