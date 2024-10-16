package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListLabor;
import com.zhaocai.business.report.mapper.ContractListLaborMapper;
import com.zhaocai.business.report.service.IContractListLaborService;
import com.zhaocai.business.report.vo.ContractListVo;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-劳务合同清单
 */
@Service
public class ContractListLaborServiceImpl extends ServiceImpl<ContractListLaborMapper, ContractListLabor> implements IContractListLaborService {

    @Override
    public List<ContractListVo> getDetailsByContractId(String contractId) {
        List<ContractListLabor> list = super.list(new LambdaQueryWrapper<ContractListLabor>()
                .eq(ContractListLabor::getConId, contractId)
                .eq(ContractListLabor::getValid, 0));
        return BeanCopierUtil.copyList(list, ContractListVo.class);
    }
}
