package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementMachineShift;
import com.zhaocai.business.agreement.mapper.AgreementMachineShiftMapper;
import com.zhaocai.business.agreement.service.IAgreementMachineShiftService;
import com.zhaocai.business.agreement.vo.res.AgreementMachineShiftVO;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-机械台班Service业务层处理
 *
 * @author chenming
 * @date 2024-06-26
 */
@Service
public class AgreementMachineShiftServiceImpl extends ServiceImpl<AgreementMachineShiftMapper, AgreementMachineShift> implements IAgreementMachineShiftService {

    @Override
    public void saveAgreementMachineShift(List<AgreementMachineShift> agreementMachineShifts, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementMachineShifts)) {
            for (AgreementMachineShift machineShift : agreementMachineShifts) {
                machineShift.setUnitPriceExcTax(AmountCalUtil.calUnitPriceExclTax(machineShift.getUnitPriceIncTax(),machineShift.getTaxRate()));
                machineShift.setAgreementId(agreementId);
            }

            super.saveBatch(agreementMachineShifts);
        }
    }

    @Override
    public List<AgreementMachineShiftVO> listByAgreementId(Long agreementId) {
        List<AgreementMachineShift> machineShifts = list(new LambdaQueryWrapper<AgreementMachineShift>()
                .eq(AgreementMachineShift::getAgreementId,agreementId));
        return BeanCopierUtil.copyList(machineShifts, AgreementMachineShiftVO.class);
    }

    @Override
    public void updateAgreementMachineShift(List<AgreementMachineShift> agreementMachineShifts, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementMachineShift(agreementMachineShifts,agreementId);
    }
}
