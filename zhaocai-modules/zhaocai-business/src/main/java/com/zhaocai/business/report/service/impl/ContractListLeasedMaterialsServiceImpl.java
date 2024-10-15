package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListLeasedMaterials;
import com.zhaocai.business.report.mapper.ContractListLeasedMaterialsMapper;
import com.zhaocai.business.report.service.IContractListLeasedMaterialsService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 合同-租赁材料合同清单 Service服务
 */
@Service
public class ContractListLeasedMaterialsServiceImpl extends ServiceImpl<ContractListLeasedMaterialsMapper, ContractListLeasedMaterials> implements IContractListLeasedMaterialsService {

    @Override
    public List<Object> getDetailsByContractId(String contractId) {
        List<ContractListLeasedMaterials> list = super.list(new LambdaQueryWrapper<ContractListLeasedMaterials>()
                .eq(ContractListLeasedMaterials::getConId, contractId)
                .eq(ContractListLeasedMaterials::getValid, 0));
        return Collections.singletonList(list);
    }
}
