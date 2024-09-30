package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementEquipmentSupply;
import com.zhaocai.business.agreement.mapper.AgreementEquipmentSupplyMapper;
import com.zhaocai.business.agreement.service.IAgreementEquipmentSupplyService;
import com.zhaocai.business.agreement.vo.res.AgreementEquipmentSupplyVO;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-甲供设备清单Service业务层处理
 *
 * @author chenming
 * @date 2024-06-26
 */
@Service
public class AgreementEquipmentSupplyServiceImpl extends ServiceImpl<AgreementEquipmentSupplyMapper, AgreementEquipmentSupply> implements IAgreementEquipmentSupplyService {

    @Override
    public void saveAgreementEquipmentSupply(List<AgreementEquipmentSupply> agreementEquipmentSupplies, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementEquipmentSupplies)) {
            for (AgreementEquipmentSupply equipmentSupply : agreementEquipmentSupplies) {
                // 不含税单价
                equipmentSupply.setEstimatedUnitPriceExcTax(AmountCalUtil.calUnitPriceExclTax(equipmentSupply.getEstimatedUnitPriceIncTax(),equipmentSupply.getEstimatedTaxRate()));

                // 预估含税金额
                equipmentSupply.setEstimatedAmountIncTax(AmountCalUtil.calTotalAmountInclTax(equipmentSupply.getEstimatedCount(),equipmentSupply.getEstimatedUnitPriceIncTax()));

                // 预估不含税金额
                equipmentSupply.setEstimatedAmountExcTax(AmountCalUtil.calTotalAmountExclTax(equipmentSupply.getEstimatedAmountIncTax(),equipmentSupply.getEstimatedTaxRate()));

                // 税额
                equipmentSupply.setEstimatedTaxAmount(AmountCalUtil.calTaxAmount(equipmentSupply.getEstimatedAmountIncTax(),equipmentSupply.getEstimatedAmountExcTax()));

                equipmentSupply.setAgreementId(agreementId);
            }

            super.saveBatch(agreementEquipmentSupplies);
        }
    }

    @Override
    public List<AgreementEquipmentSupplyVO> listByAgreementId(Long agreementId) {
        List<AgreementEquipmentSupply> equipmentSupplies = list(new LambdaQueryWrapper<AgreementEquipmentSupply>()
                .eq(AgreementEquipmentSupply::getAgreementId,agreementId));
        return BeanCopierUtil.copyList(equipmentSupplies,AgreementEquipmentSupplyVO.class);
    }

    @Override
    public void updateAgreementEquipmentSupply(List<AgreementEquipmentSupply> agreementEquipmentSupplies, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementEquipmentSupply(agreementEquipmentSupplies,agreementId);
    }
}
