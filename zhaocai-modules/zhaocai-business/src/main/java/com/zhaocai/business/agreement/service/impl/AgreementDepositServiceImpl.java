package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementDeposit;
import com.zhaocai.business.agreement.mapper.AgreementDepositMapper;
import com.zhaocai.business.agreement.service.IAgreementDepositService;
import com.zhaocai.business.agreement.vo.res.AgreementDepositVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同保证金Service业务层处理
 *
 * @author chenming
 * @date 2024-05-24
 */
@Service
public class AgreementDepositServiceImpl extends ServiceImpl<AgreementDepositMapper,AgreementDeposit> implements IAgreementDepositService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Override
    public void saveAgreementDeposit(List<AgreementDeposit> agreementDeposits, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementDeposits)) {
            for (AgreementDeposit deposit : agreementDeposits) {
                deposit.setAgreementId(agreementId);
            }
            super.saveBatch(agreementDeposits);
        }
    }

    @Override
    public List<AgreementDepositVO> listByAgreementId(Long agreementId) {
        List<AgreementDeposit> agreementDeposits = list(new LambdaQueryWrapper<AgreementDeposit>()
                .eq(AgreementDeposit::getAgreementId,agreementId));
        Map<String,String> depositTypeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_DEPOSIT_TYPE.getName());
        Map<String,String> depositModeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_DEPOSIT_MODE.getName());
        Map<String,String> depositBaseTypeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_DEPOSIT_BASE_TYPE.getName());

        List<AgreementDepositVO> agreementDepositList = BeanCopierUtil.copyList(agreementDeposits,AgreementDepositVO.class);
        for (AgreementDepositVO depositVO : agreementDepositList) {
            depositVO.setDepositTypeText(depositTypeMap.get(depositVO.getDepositType()));
            depositVO.setDepositWayText(depositModeMap.get(depositVO.getDepositWay()));
            depositVO.setDepositBaseAmountText(depositBaseTypeMap.get(depositVO.getDepositBaseAmount()));
        }
        return agreementDepositList;
    }

    @Override
    public void updateAgreementDeposit(List<AgreementDeposit> agreementDeposits, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementDeposit(agreementDeposits,agreementId);
    }
}
