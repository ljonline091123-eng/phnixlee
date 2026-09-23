package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementMaterialSupply;
import com.zhaocai.business.agreement.mapper.AgreementMaterialSupplyMapper;
import com.zhaocai.business.agreement.service.IAgreementMaterialSupplyService;
import com.zhaocai.business.agreement.vo.res.AgreementMaterialSupplyVO;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-甲供材料清单Service业务层处理
 *
 * @author chenming
 * @date 2024-06-26
 */
@Service
public class AgreementMaterialSupplyServiceImpl extends ServiceImpl<AgreementMaterialSupplyMapper, AgreementMaterialSupply> implements IAgreementMaterialSupplyService {

    @Override
    public void saveAgreementMaterialSupply(List<AgreementMaterialSupply> agreementMaterialSupplies, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementMaterialSupplies)) {
            for(AgreementMaterialSupply materialSupply : agreementMaterialSupplies) {
                // 不含税单价
                materialSupply.setEstimatedUnitPriceExcTax(AmountCalUtil.calUnitPriceExclTax(materialSupply.getEstimatedUnitPriceIncTax(),materialSupply.getEstimatedTaxRate()));

                // 含税总额
                materialSupply.setEstimatedAmountIncTax(AmountCalUtil.calTotalAmountInclTax(materialSupply.getEstimatedCount(),materialSupply.getEstimatedUnitPriceIncTax()));

                // 不含税总额
                materialSupply.setEstimatedAmountExcTax(AmountCalUtil.calTotalAmountExclTax(materialSupply.getEstimatedAmountIncTax(),materialSupply.getEstimatedTaxRate()));

                // 税额
                materialSupply.setEstimatedTaxAmount(AmountCalUtil.calTaxAmount(materialSupply.getEstimatedAmountIncTax(),materialSupply.getEstimatedAmountExcTax()));

                materialSupply.setAgreementId(agreementId);
            }

            super.saveBatch(agreementMaterialSupplies);
        }
    }

    @Override
    public List<AgreementMaterialSupplyVO> listByAgreementId(Long agreementId) {
        List<AgreementMaterialSupply> materialSupplies = list(new LambdaQueryWrapper<AgreementMaterialSupply>()
                .eq(AgreementMaterialSupply::getAgreementId,agreementId));
        return BeanCopierUtil.copyList(materialSupplies,AgreementMaterialSupplyVO.class);
    }

    @Override
    public void updateAgreementMaterialSupply(List<AgreementMaterialSupply> agreementMaterialSupplies, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementMaterialSupply(agreementMaterialSupplies,agreementId);
    }
}
