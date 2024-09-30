package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementDailyWage;
import com.zhaocai.business.agreement.mapper.AgreementDailyWageMapper;
import com.zhaocai.business.agreement.service.IAgreementDailyWageService;
import com.zhaocai.business.agreement.vo.res.AgreementDailyWageVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同-计日工Service业务层处理
 *
 * @author chenming
 * @date 2024-06-26
 */
@Service
public class AgreementDailyWageServiceImpl extends ServiceImpl<AgreementDailyWageMapper, AgreementDailyWage> implements IAgreementDailyWageService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void saveAgreementDailyWage(List<AgreementDailyWage> agreementDailyWageList, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementDailyWageList)) {
            for (AgreementDailyWage dailyWage : agreementDailyWageList) {
                dailyWage.setUnitMeasurement("工日");
                dailyWage.setUnitPriceExcTax(AmountCalUtil.calUnitPriceExclTax(dailyWage.getUnitPriceIncTax(),dailyWage.getTaxRate()));

                dailyWage.setAgreementId(agreementId);
            }

            super.saveBatch(agreementDailyWageList);
        }
    }

    @Override
    public List<AgreementDailyWageVO> listByAgreementId(Long agreementId) {
        List<AgreementDailyWage> dailyWages = list(new LambdaQueryWrapper<AgreementDailyWage>()
                .eq(AgreementDailyWage::getAgreementId,agreementId));

        Map<String,String> jobTitleMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_DATALLER_WORK_TYPE.getName());
        List<AgreementDailyWageVO> dailyWageList = BeanCopierUtil.copyList(dailyWages, AgreementDailyWageVO.class);

        for (AgreementDailyWageVO dailyWageVO : dailyWageList) {
            dailyWageVO.setJobTitleName(jobTitleMap.get(dailyWageVO.getJobTitleCode()));
        }

        return dailyWageList;
    }

    @Override
    public void updateAgreementDailyWage(List<AgreementDailyWage> agreementDailyWageList, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementDailyWage(agreementDailyWageList,agreementId);
    }
}
