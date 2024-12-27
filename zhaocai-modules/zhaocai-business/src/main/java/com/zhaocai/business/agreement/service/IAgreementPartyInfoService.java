package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementPartyInfo;
import com.zhaocai.business.agreement.domain.AgreementPartyInfoVO;

import java.util.List;

/**
 * 合同签约方信息 Service接口
 *
 * @author zs
 * @date 2024-12-27
 */
public interface IAgreementPartyInfoService  extends IService<AgreementPartyInfo>{

    /**
     * 保存合同签约方信息
     * @param agreementPartyInfoLists
     * @param agreementId
     */
    void saveAgreementPartyInfo(List<AgreementPartyInfo> agreementPartyInfoLists, Long agreementId);

    /**
     * 获取合同签约方信息
     * @param agreementId
     * @return
     */
    List<AgreementPartyInfoVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同签约方信息
     * @param agreementPartyInfoLists
     * @param agreementId
     */
    void updateAgreementPartyInfo(List<AgreementPartyInfo> agreementPartyInfoLists, Long agreementId);

}
