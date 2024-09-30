package com.zhaocai.business.agreement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementPaymentItem;
import com.zhaocai.business.agreement.mapper.AgreementPaymentItemMapper;
import com.zhaocai.business.agreement.service.IAgreementPaymentItemService;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentItemVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 合同款项信息Service业务层处理
 *
 * @author chenming
 * @date 2024-05-24
 */
@Service
public class AgreementPaymentItemServiceImpl extends ServiceImpl<AgreementPaymentItemMapper,AgreementPaymentItem> implements IAgreementPaymentItemService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void saveAgreementPaymentItem(AgreementPaymentItem agreementPaymentItem, Long agreementId) {
        if (agreementPaymentItem == null) {
            throw new BusinessException("合同款项信息不能为空");
        }

        agreementPaymentItem.setAgreementId(agreementId);
        super.save(agreementPaymentItem);
    }

    @Override
    public AgreementPaymentItemVO getByAgreementId(Long agreementId) {
        AgreementPaymentItem paymentItem = super.getOne(new LambdaQueryWrapper<AgreementPaymentItem>()
                .eq(AgreementPaymentItem::getAgreementId,agreementId));

        AgreementPaymentItemVO paymentItemVO = BeanCopierUtil.copyBean(paymentItem, AgreementPaymentItemVO.class);
        paymentItemVO.setCurrencyText(underlingSystemService.listDictMap(DictBizEnum.UNDERLING_SYS_CURRENCY.getName()).get(paymentItem.getCurrency()));
        paymentItemVO.setInvoiceTypeText(underlingSystemService.listDictMap(DictBizEnum.UNDERLING_INVOICE_TYPE.getName()).get(paymentItem.getInvoiceType()));

        return paymentItemVO;
    }

    @Override
    public void updateAgreementPaymentItem(AgreementPaymentItem agreementPaymentItem, Long agreementId) {
        if (agreementPaymentItem == null) {
            throw new BusinessException("合同款项信息不能为空");
        }

        baseMapper.deleteByAgreementId(agreementId);

        agreementPaymentItem.setAgreementId(agreementId);
        super.save(agreementPaymentItem);
    }
}
