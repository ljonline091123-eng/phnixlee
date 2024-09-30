package com.zhaocai.business.agreement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementSignStamper;
import com.zhaocai.business.agreement.mapper.AgreementSignStamperMapper;
import com.zhaocai.business.agreement.service.IAgreementSignStamperService;
import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import com.zhaocai.common.signature.common.enums.StamperTypeEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 合同签章签署位置Service业务层处理
 *
 * @author WH
 * @date 2024-09-14
 */
@Service
public class AgreementSignStamperServiceImpl extends ServiceImpl<AgreementSignStamperMapper, AgreementSignStamper> implements IAgreementSignStamperService {

    @Override
    public void saveAgreementSignStamper(Long templateId, List<AgreementSignStamper> agreementSignStamperList) {
        // 先删除
        baseMapper.deleteByTemplateId(templateId);

        // 后保存
        for (AgreementSignStamper signStamper : agreementSignStamperList) {
            signStamper.setTemplateId(templateId);
            if (StamperTypeEnum.isAcrossPage(signStamper.getSignType())) {
                signStamper.setKeyWord(null);
                signStamper.setSignPage(-1);
                signStamper.setOffsetX(new BigDecimal("0.70"));
                if (SignatureTypeEnum.PARTY_A.equalsType(signStamper.getType())) {
                    signStamper.setOffsetY(new BigDecimal("0.15"));
                }else if (SignatureTypeEnum.PARTY_B.equalsType(signStamper.getType())) {
                    signStamper.setOffsetY(new BigDecimal("0.80"));
                }
            }
        }

        this.saveBatch(agreementSignStamperList);
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        baseMapper.deleteByTemplateId(templateId);
    }

    @Override
    public List<AgreementSignStamper> listByTemplateId(Long templateId) {
        return this.list(new LambdaQueryWrapper<AgreementSignStamper>()
                .eq(AgreementSignStamper::getTemplateId,templateId));
    }
}
