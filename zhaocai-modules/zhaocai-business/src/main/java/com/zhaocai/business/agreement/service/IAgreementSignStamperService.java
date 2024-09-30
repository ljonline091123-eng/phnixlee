package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementSignStamper;

import java.util.List;

/**
 * 合同签章签署位置Service接口
 *
 * @author chenming
 * @date 2024-09-14
 */
public interface IAgreementSignStamperService extends IService<AgreementSignStamper> {

    /**
     * 保存合同签署位置
     * @param templateId
     * @param agreementSignStamperList
     */
    void saveAgreementSignStamper(Long templateId, List<AgreementSignStamper> agreementSignStamperList);

    /**
     * 根据合同模板删除合同签署位置
     * @param templateId
     */
    void deleteByTemplateId(Long templateId);

    /**
     * 根据合同模板获取
     * @param templateId
     * @return
     */
    List<AgreementSignStamper> listByTemplateId(Long templateId);
}
