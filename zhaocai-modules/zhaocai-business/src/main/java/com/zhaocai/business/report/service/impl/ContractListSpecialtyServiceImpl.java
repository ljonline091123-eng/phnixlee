package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListSpecialty;
import com.zhaocai.business.report.mapper.ContractListSpecialtyMapper;
import com.zhaocai.business.report.service.IContractListSpecialtyService;
import com.zhaocai.business.report.vo.ContractListVo;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-专业分包合同清单 Service服务
 */
@Service
public class ContractListSpecialtyServiceImpl extends ServiceImpl<ContractListSpecialtyMapper, ContractListSpecialty> implements IContractListSpecialtyService {

    @Override
    public List<ContractListVo> getDetailsByContractId(String contractId) {
        List<ContractListSpecialty> list = super.list(new LambdaQueryWrapper<ContractListSpecialty>()
                .eq(ContractListSpecialty::getConId, contractId)
                .eq(ContractListSpecialty::getValid, 0));
        return BeanCopierUtil.copyList(list, ContractListVo.class);
    }
}
