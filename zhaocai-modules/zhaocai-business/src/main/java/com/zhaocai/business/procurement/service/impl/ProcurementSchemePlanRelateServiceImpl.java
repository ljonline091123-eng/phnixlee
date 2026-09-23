package com.zhaocai.business.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.procurement.domain.ContractPlanningSplit;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.domain.ProcurementSchemePlanRelate;
import com.zhaocai.business.procurement.mapper.ProcurementSchemePlanRelateMapper;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.service.IProcurementSchemePlanRelateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 采购方案-采购计划对应关系Service业务层处理
 *
 * @author WH
 * @date 2024-05-29
 */
@Service
public class ProcurementSchemePlanRelateServiceImpl extends ServiceImpl<ProcurementSchemePlanRelateMapper, ProcurementSchemePlanRelate> implements IProcurementSchemePlanRelateService {

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    @Override
    public void saveRelate(List<Long> contractSplitId, Long schemeId) {
        List<ContractPlanningSplit> planningSplits = contractPlanningSplitService.listByIds(contractSplitId);
        for (ContractPlanningSplit planningSplit : planningSplits) {
            ProcurementSchemePlanRelate planRelate = new ProcurementSchemePlanRelate();
            planRelate.setProcurementPlanId(planningSplit.getProcurementPlanId());
            planRelate.setContractSplitId(planningSplit.getId());
            planRelate.setProcurementSchemeId(schemeId);

            baseMapper.insert(planRelate);
        }
    }

    @Override
    public List<ProcurementSchemePlanRelate> listBySchemeId(Long schemeId) {
        return super.list(new LambdaQueryWrapper<ProcurementSchemePlanRelate>()
                .eq(ProcurementSchemePlanRelate::getProcurementSchemeId,schemeId));
    }

    @Override
    public List<ProcurementScheme> listProcurementSchemeByRelatePlanId(Long planId) {
        return baseMapper.selectProcurementSchemeByRelatePlanId(planId);
    }

}
