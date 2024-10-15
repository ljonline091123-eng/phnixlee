package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListOther;
import com.zhaocai.business.report.mapper.ContractListOtherMapper;
import com.zhaocai.business.report.service.IContractListOtherService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 合同-其他合同清单 Service服务
 */
@Service
public class ContractListOtherServiceImpl extends ServiceImpl<ContractListOtherMapper, ContractListOther> implements IContractListOtherService {

    @Override
    public List<Object> getDetailsByContractId(String contractId) {
        List<ContractListOther> list = super.list(new LambdaQueryWrapper<ContractListOther>()
                .eq(ContractListOther::getConId, contractId)
                .eq(ContractListOther::getValid, 0));
        return Collections.singletonList(list);
    }
}
