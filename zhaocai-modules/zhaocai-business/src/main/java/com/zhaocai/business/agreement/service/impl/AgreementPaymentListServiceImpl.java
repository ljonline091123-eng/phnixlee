package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementPaymentList;
import com.zhaocai.business.agreement.mapper.AgreementPaymentListMapper;
import com.zhaocai.business.agreement.service.IAgreementPaymentListService;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentListVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同结算与付款节点信息Service业务层处理
 *
 * @author chenming
 * @date 2024-05-24
 */
@Service
public class AgreementPaymentListServiceImpl extends ServiceImpl<AgreementPaymentListMapper,AgreementPaymentList> implements IAgreementPaymentListService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void saveAgreementPaymentList(List<AgreementPaymentList> agreementPaymentLists, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementPaymentLists)) {
            for (AgreementPaymentList paymentList : agreementPaymentLists) {
                paymentList.setAgreementId(agreementId);
            }
            super.saveBatch(agreementPaymentLists);
        }
    }

    @Override
    public List<AgreementPaymentListVO> listByAgreementId(Long agreementId) {
        List<AgreementPaymentList> agreementPaymentLists = list(new LambdaQueryWrapper<AgreementPaymentList>()
                .eq(AgreementPaymentList::getAgreementId, agreementId));

//        Map<String,String> paymentBaseTypeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_PAYMENT_BASE_TYPE.getName());
        List<AgreementPaymentListVO> agreementPaymentList = BeanCopierUtil.copyList(agreementPaymentLists,AgreementPaymentListVO.class);
//        for(AgreementPaymentListVO paymentList : agreementPaymentList) {
//            paymentList.setPaymentBasisText(paymentBaseTypeMap.get(paymentList.getPaymentBasis()));
//        }
        return agreementPaymentList;
    }

    @Override
    public void updateAgreementPaymentList(List<AgreementPaymentList> agreementPaymentLists, Long agreementId) {
       baseMapper.deleteByAgreementId(agreementId);

       this.saveAgreementPaymentList(agreementPaymentLists,agreementId);
    }
}
