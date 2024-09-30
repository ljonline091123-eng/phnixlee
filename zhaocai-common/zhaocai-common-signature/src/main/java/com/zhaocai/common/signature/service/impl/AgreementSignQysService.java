package com.zhaocai.common.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.common.signature.common.enums.QiYueSuoSignStepEnum;
import com.zhaocai.common.signature.domain.AgreementSignQys;
import com.zhaocai.common.signature.mapper.AgreementSignQysMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AgreementSignQysService  extends ServiceImpl<AgreementSignQysMapper, AgreementSignQys> implements IService<AgreementSignQys> {

    /**
     * 获取和保存合同签章信息-契约锁
     * @param signId
     * @param businessCode
     * @param businessId
     * @return
     */
    public AgreementSignQys addAgreementSignQys(Long signId, String businessCode, Long businessId) {
        AgreementSignQys agreementSignQys = new AgreementSignQys();
        agreementSignQys.setBusinessCode(businessCode);
        agreementSignQys.setBusinessId(businessId);
        agreementSignQys.setSignId(signId);
        agreementSignQys.setSignStep(QiYueSuoSignStepEnum.CREATE_BY_FILE.getStep());
        agreementSignQys.setCreateTime(new Date());

        this.save(agreementSignQys);

        return agreementSignQys;
    }

    /**
     * 设置合同文档 id
     * @param id
     * @param documentId
     */
    public void setDocumentId(Long id, String documentId) {
        this.update(new LambdaUpdateWrapper<AgreementSignQys>()
                .set(AgreementSignQys::getDocumentId,documentId)
                .set(AgreementSignQys::getSignStep, QiYueSuoSignStepEnum.CREATE_BY_CATEGORY.getStep())
                .set(AgreementSignQys::getUpdateTime,new Date())
                .eq(AgreementSignQys::getId,id));
    }

    /**
     * 根据 signId 获取签署记录
     * @param signId
     * @return
     */
    public AgreementSignQys getBySignId(Long signId) {
        return this.getOne(new LambdaQueryWrapper<AgreementSignQys>()
                .eq(AgreementSignQys::getSignId,signId));
    }

    /**
     * 设置 contractId
     * @param contractId
     * @param id
     */
    public void setContractId(String contractId, Long id) {
        this.update(new LambdaUpdateWrapper<AgreementSignQys>()
                .set(AgreementSignQys::getContractId,contractId)
                .set(AgreementSignQys::getSignStep, QiYueSuoSignStepEnum.TO_SIGN.getStep())
                .set(AgreementSignQys::getUpdateTime,new Date())
                .eq(AgreementSignQys::getId,id));
    }

    /**
     * 根据 contractId 获取对应记录
     * @param contractId
     * @return
     */
    public AgreementSignQys getByContractId(Long contractId) {
        return this.getOne(new LambdaQueryWrapper<AgreementSignQys>()
                .eq(AgreementSignQys::getContractId,contractId));
    }


    /**
     * 设置签署完成
     * @param id
     */
    public void setSignFinished(Long id) {
        this.update(new LambdaUpdateWrapper<AgreementSignQys>()
                .set(AgreementSignQys::getSignStep, QiYueSuoSignStepEnum.SIGN_FINISHED.getStep())
                .set(AgreementSignQys::getUpdateTime,new Date())
                .eq(AgreementSignQys::getId,id));
    }
}
