package com.zhaocai.common.signature.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.common.signature.domain.AgreementSign;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.command.CreateAgreementDocumentCommandRequest;
import com.zhaocai.common.signature.mapper.AgreementSignMapper;
import com.zhaocai.common.signature.utils.JacksonUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 合同签章信息Service业务层处理
 *
 * @author chenming
 * @date 2024-08-28
 */
@Service
public class AgreementSignService extends ServiceImpl<AgreementSignMapper, AgreementSign> implements IService<AgreementSign> {

    /**
     * 根据业务获取合同签章信息
     * @param businessCode
     * @param businessId
     * @return
     */
    public AgreementSign getAgreementSignByBusiness(String businessCode, Long businessId) {
        return this.getOne(new LambdaQueryWrapper<AgreementSign>()
                .eq(AgreementSign::getBusinessCode,businessCode)
                .eq(AgreementSign::getBusinessId,businessId));
    }

    /**
     * 设置合同签章信息
     * @param id
     * @param signMessage
     */
    public void updateSignMessage(Long id, String signMessage) {
        System.out.println("主键 signMessage for ID: " + id);
        System.out.println("长度 signMessage length: " + signMessage.length());
        this.update(new LambdaUpdateWrapper<AgreementSign>()
                .set(AgreementSign::getSignMessage,signMessage)
                .set(AgreementSign::getUpdateTime,new Date())
                .eq(AgreementSign::getId,id));
    }

    /**
     *
     * @param request
     * @return
     */
    public AgreementSign saveAgreementSign(CreateAgreementDocumentCommandRequest request) {
        AgreementSign agreementSign = new AgreementSign();
        agreementSign.setBusinessCode(request.getBusinessCode());
        agreementSign.setBusinessId(request.getBusinessId());
        agreementSign.setSignMessage(JacksonUtil.toJsonString(request));

        SignatureCreator signatureCreator = request.getSignatureCreator();
        agreementSign.setTenantId(signatureCreator.getTenantId());
        agreementSign.setTenantName(signatureCreator.getTenantName());
        agreementSign.setCreatorName(signatureCreator.getCreatorName());
        agreementSign.setCreatorContact(signatureCreator.getCreatorContact());
        agreementSign.setCreateTime(new Date());

        this.save(agreementSign);

        return agreementSign;
    }

    /**
     * 修改状态 {@link com.zhaocai.common.signature.mapper.AgreementSignMapper}
     * @param state
     * @param id
     */
    public void updateSignState(Integer state, Long id) {
        this.update(new LambdaUpdateWrapper<AgreementSign>()
                .set(AgreementSign::getSignState,state)
                .set(AgreementSign::getUpdateTime,new Date())
                .eq(AgreementSign::getId,id));
    }
}
