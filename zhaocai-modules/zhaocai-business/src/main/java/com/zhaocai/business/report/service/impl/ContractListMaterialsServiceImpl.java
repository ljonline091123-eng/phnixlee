package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.domain.ContractListMaterials;
import com.zhaocai.business.report.mapper.ContractListMaterialsMapper;
import com.zhaocai.business.report.service.IContractListMaterialsService;
import com.zhaocai.business.report.vo.ContractListVo;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同-购买材料合同清单 Service服务
 */
@Service
public class ContractListMaterialsServiceImpl extends ServiceImpl<ContractListMaterialsMapper, ContractListMaterials> implements IContractListMaterialsService {

    @Override
    public List<ContractListVo> getDetailsByContractId(String contractId) {
        List<ContractListMaterials> list = super.list(new LambdaQueryWrapper<ContractListMaterials>()
                .eq(ContractListMaterials::getConId, contractId)
                .eq(ContractListMaterials::getValid, 0));
        return BeanCopierUtil.copyList(list, ContractListVo.class);
    }
}
