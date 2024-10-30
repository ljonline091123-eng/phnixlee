package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.procurement.domain.ContractPlanningSplit;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.mapper.ContractPlanningSplitMapper;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.vo.req.ContractPlanningSplitRequestVO;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanContractSplitQueryVO;
import com.zhaocai.business.procurement.vo.res.ContractSplitListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanContractSplitVO;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeSplitListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合约规划拆分Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Slf4j
@Service
public class ContractPlanningSplitServiceImpl extends ServiceImpl<ContractPlanningSplitMapper,ContractPlanningSplit> implements IContractPlanningSplitService {

    @Autowired
    private IMaterialsListService materialsListService;

    @Override
    public void saveContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId, ProcurementPlan procurementPlan) {
        splitRequestList.forEach(split -> {
            if (CollectionUtil.isNotEmpty(split.getMaterialsLists())) {
                // 保存合约拆分
                ContractPlanningSplit contractPlanningSplit = BeanCopierUtil.copyBean(split,ContractPlanningSplit.class);
                contractPlanningSplit.setProcurementPlanId(planId);
                contractPlanningSplit.setTotalPlanAmount(calTotalPlanAmount(split.getMaterialsLists()));
                contractPlanningSplit.setIsUseUp(1);
                contractPlanningSplit.setTotalUsedAmount(BigDecimal.ZERO);
                baseMapper.insert(contractPlanningSplit);

                // 保存合约对应的物料数据
                materialsListService.saveMaterialsList(split.getMaterialsLists(), contractPlanningSplit.getId(),planId,procurementPlan);
            } else {
                log.warn("合约拆分[{}-{}]的清单列表为空",split.getContractScope(),split.getSplitContractName());
            }
        });
    }

    @Override
    public void updateContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId,ProcurementPlan procurementPlan) {
        // 删除合约拆分记录
        baseMapper.deleteByPlanId(planId);

        // 删除物料信息
        materialsListService.deleteByPlanId(planId);

        // 保存信息
        this.saveContractPlanningSplit(splitRequestList,planId,procurementPlan);
    }

    @Override
    public List<ContractSplitListVO> listContractSplitByPlanId(Long planId) {
        List<ContractPlanningSplit> splits = super.list(new LambdaQueryWrapper<ContractPlanningSplit>()
                .eq(ContractPlanningSplit::getProcurementPlanId,planId));

        return BeanCopierUtil.copyList(splits,ContractSplitListVO.class);
    }

    @Override
    public List<ProcurementSchemeSplitListVO> listContractSplitBySchemeId(Long schemeId) {
        List<ProcurementSchemeSplitListVO> resultList = baseMapper.selectContractSplitListBySchemeId(schemeId);
        for(ProcurementSchemeSplitListVO splitListVO : resultList) {
            if (splitListVO.getIsUseUp() != null && splitListVO.getIsUseUp() == 2) {
                splitListVO.setSplitContractName(splitListVO.getSplitContractName() + "(已使用完毕)");
            }
        }
        return resultList;
    }

    @Override
    public PageResult<ProcurementPlanContractSplitVO> listPlanContractSplit(ProcurementPlanContractSplitQueryVO queryVO) {
        IPage<ProcurementPlanContractSplitVO> ipage = baseMapper.selectPlanContractSplitList(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(ipage);
    }

    @Override
    public List<ContractPlanningSplit> listExistPlanningSplits(List<Long> contractSplitIds) {
        return baseMapper.selectExistPlanningSplits(contractSplitIds);
    }

    @Override
    public void updateContractPlanningSplitUseAdd(Long contractSplitId, List<MaterialsList> materialsLists, List<AgreementMaterialsList> agreementMaterialsLists, List<AgreementMaterialsList> agreementMaterialsListsOld) {
        boolean isUseUp = true;
        // 判断物料是否已用完
        for (MaterialsList materialsList : materialsLists) {
            if (NumberUtil.compare(materialsList.getCount(),materialsList.getUsedCount()) > 0) {
                isUseUp = false;
                break;
            }
        }

        // 设置使用金额
        BigDecimal totalUsedAmount = BigDecimal.ZERO;
        for (AgreementMaterialsList agreementMaterials : agreementMaterialsLists) {
            totalUsedAmount = NumberUtil.add(totalUsedAmount,agreementMaterials.getSignAmountInclTax());
        }

        int isUseUpState = !isUseUp ? 1 : 2;
        ContractPlanningSplit contractPlanningSplit = this.getById(contractSplitId);
        totalUsedAmount = NumberUtil.add(totalUsedAmount,contractPlanningSplit.getTotalUsedAmount());

        /* 将原来的减去 */
        if (agreementMaterialsListsOld != null) {
            for (AgreementMaterialsList agreementMaterials : agreementMaterialsListsOld) {
                totalUsedAmount = NumberUtil.subtract(totalUsedAmount,agreementMaterials.getSignAmountInclTax());
            }
        }

        this.update(new LambdaUpdateWrapper<ContractPlanningSplit>()
                .set(ContractPlanningSplit::getIsUseUp,isUseUpState)
                        .set(ContractPlanningSplit::getTotalUsedAmount,totalUsedAmount)
                .eq(ContractPlanningSplit::getId,contractSplitId));
    }

    @Override
    public void updateContractPlanningSplitUseSub(Long contractSplitId, List<AgreementMaterialsList> agreementMaterialsLists) {
        // 设置使用金额
        BigDecimal totalUsedAmount = BigDecimal.ZERO;
        for (AgreementMaterialsList agreementMaterials : agreementMaterialsLists) {
            totalUsedAmount = NumberUtil.add(totalUsedAmount,agreementMaterials.getSignAmountInclTax());
        }

        ContractPlanningSplit contractPlanningSplit = this.getById(contractSplitId);
        totalUsedAmount = NumberUtil.subtract(contractPlanningSplit.getTotalUsedAmount(),totalUsedAmount);

        this.update(new LambdaUpdateWrapper<ContractPlanningSplit>()
                .set(ContractPlanningSplit::getIsUseUp,1)
                .set(ContractPlanningSplit::getTotalUsedAmount,totalUsedAmount)
                .eq(ContractPlanningSplit::getId,contractSplitId));
    }

    /**
     * 计算计划金额
     * @param materialsLists
     * @return
     */
    private BigDecimal calTotalPlanAmount(List<MaterialsList> materialsLists) {
        BigDecimal totalPlanAmount = BigDecimal.ZERO;
        for (MaterialsList materialsList : materialsLists) {
            totalPlanAmount = NumberUtil.add(totalPlanAmount, AmountCalUtil.calTotalAmountInclTax(materialsList.getCount(),materialsList.getUnitPriceInclTax()));
        }
        return totalPlanAmount;
    }
}
