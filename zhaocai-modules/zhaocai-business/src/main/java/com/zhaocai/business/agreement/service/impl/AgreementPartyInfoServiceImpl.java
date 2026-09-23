package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementPartyInfo;
import com.zhaocai.business.agreement.vo.res.AgreementPartyInfoVO;
import com.zhaocai.business.agreement.mapper.AgreementPartyInfoMapper;
import com.zhaocai.business.agreement.service.IAgreementPartyInfoService;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同签约方信息Service业务层处理
 *
 * @author zs
 * @date 2024-12-27
 */
@Service
public class AgreementPartyInfoServiceImpl extends ServiceImpl<AgreementPartyInfoMapper,AgreementPartyInfo> implements IAgreementPartyInfoService{


    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Override
    public void saveAgreementPartyInfo(List<AgreementPartyInfo> agreementPartyInfoLists, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementPartyInfoLists)) {
            for (AgreementPartyInfo partyInfo : agreementPartyInfoLists) {
                /**
                 * 前端已经隐藏该字段，暂不清楚是否有意义，防止支出合同接口报错先赋值一个值
                 * Time:2025/1/10 下午5:05
                 * */
                if(partyInfo.getSignerCode() == null || partyInfo.getSignerCode().isEmpty()){
                    partyInfo.setSignerCode("6666666666");
                }
                partyInfo.setAgreementId(agreementId);
            }
            super.saveBatch(agreementPartyInfoLists);
        }
    }

    @Override
    public List<AgreementPartyInfoVO> listByAgreementId(Long agreementId) {
        List<AgreementPartyInfo> agreementPartyInfoLists = list(new LambdaQueryWrapper<AgreementPartyInfo>()
                .eq(AgreementPartyInfo::getAgreementId, agreementId));

        Map<String,String> partyInfoBaseTypeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_CON_ROLE_TYPE.getName());
        List<AgreementPartyInfoVO> agreementPartyInfoList = BeanCopierUtil.copyList(agreementPartyInfoLists,AgreementPartyInfoVO.class);
        for(AgreementPartyInfoVO partyInfoList : agreementPartyInfoList) {
            partyInfoList.setRoleTypeText(partyInfoBaseTypeMap.get(partyInfoList.getRoleType()));
        }
        return agreementPartyInfoList;
    }

    @Override
    public void updateAgreementPartyInfo(List<AgreementPartyInfo> agreementPartyInfoLists, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);
        this.saveAgreementPartyInfo(agreementPartyInfoLists,agreementId);
    }
}
