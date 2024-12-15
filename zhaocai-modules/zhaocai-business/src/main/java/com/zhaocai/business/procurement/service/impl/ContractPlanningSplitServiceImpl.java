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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public List<MaterialsList> saveContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId, ProcurementPlan procurementPlan) {
        System.out.println("保存合约信息:"+splitRequestList);
        List<MaterialsList> list = new ArrayList<>();
        Integer[] floatCount = {0};
        Integer[] fixedCount = {0};
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
                List<MaterialsList> materialsLists = materialsListService.saveMaterialsList(split.getMaterialsLists(), contractPlanningSplit.getId(), planId, procurementPlan,floatCount,fixedCount);
                list.addAll(materialsLists);
            } else {
                log.warn("合约拆分[{}-{}]的清单列表为空",split.getContractScope(),split.getSplitContractName());
            }
        });
        return list;
    }

    @Override
    public List<MaterialsList> updateContractPlanningSplit(List<ContractPlanningSplitRequestVO> splitRequestList, Long planId,ProcurementPlan procurementPlan) {
        // 删除合约拆分记录
        baseMapper.deleteByPlanId(planId);
        System.out.println("删除合约拆分记录");

        // 删除物料信息
        materialsListService.deleteByPlanId(planId);

        // 保存信息
        List<MaterialsList> list = this.saveContractPlanningSplit(splitRequestList, planId, procurementPlan);
        return list;
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

    @Override
    public void updateContractPlanningSplitUseAddByMarket(List<MaterialsList> materialsLists, List<AgreementMaterialsList> agreementMaterialsLists, List<AgreementMaterialsList> agreementMaterialsListsOld) {
        Map<Long, Boolean> useUp = new HashMap<>();
        // 判断物料是否已用完
        for (MaterialsList materialsList : materialsLists) {
            if(null == useUp.get(materialsList.getContractSplitId())){
                useUp.put(materialsList.getContractSplitId(),true);
            }
            if (NumberUtil.compare(materialsList.getCount(),materialsList.getUsedCount()) > 0) {
                useUp.put(materialsList.getContractSplitId(),false);
            }
        }
        // 设置本次使用金额
        Map<Long, BigDecimal> totalAmount = agreementMaterialsLists.stream().collect(Collectors.groupingBy(
                AgreementMaterialsList::getContractSplitId,
                Collectors.reducing(BigDecimal.ZERO, AgreementMaterialsList::getSignAmountInclTax, BigDecimal::add)
        ));
        /* 将原来的减去 */
        Map<Long, BigDecimal> totalAmountOld = new HashMap<>();
        if (agreementMaterialsListsOld != null) {
            totalAmountOld = agreementMaterialsListsOld.stream().collect(Collectors.groupingBy(
                    AgreementMaterialsList::getContractSplitId,
                    Collectors.reducing(BigDecimal.ZERO, AgreementMaterialsList::getSignAmountInclTax, BigDecimal::add)
            ));
        }
        // 更新合同规划分割表
        Map<Long, BigDecimal> finalTotalAmountOld = totalAmountOld;
        totalAmount.forEach((contractSplitId, amount)->{
            int isUseUpState = !useUp.get(contractSplitId) ? 1 : 2;
            ContractPlanningSplit contractPlanningSplit = this.getById(contractSplitId);
            if (contractPlanningSplit != null) {
                BigDecimal total = NumberUtil.add(amount, contractPlanningSplit.getTotalUsedAmount());
                total = NumberUtil.subtract(total, finalTotalAmountOld.get(contractSplitId));
                this.update(new LambdaUpdateWrapper<ContractPlanningSplit>()
                        .set(ContractPlanningSplit::getIsUseUp,isUseUpState)
                        .set(ContractPlanningSplit::getTotalUsedAmount,total)
                        .eq(ContractPlanningSplit::getId,contractSplitId));
            }
        });
    }

    @Override
    public void updateContractPlanningSplitUseSubByMarket(List<AgreementMaterialsList> agreementMaterialsLists) {
        // 设置使用金额
        Map<Long, BigDecimal> totalAmount = agreementMaterialsLists.stream().collect(Collectors.groupingBy(
                AgreementMaterialsList::getContractSplitId,
                Collectors.reducing(BigDecimal.ZERO, AgreementMaterialsList::getSignAmountInclTax, BigDecimal::add)
        ));
        totalAmount.forEach((contractSplitId,amount)->{
            ContractPlanningSplit contractPlanningSplit = this.getById(contractSplitId);
            BigDecimal total = NumberUtil.subtract(contractPlanningSplit.getTotalUsedAmount(),amount);

            this.update(new LambdaUpdateWrapper<ContractPlanningSplit>()
                    .set(ContractPlanningSplit::getIsUseUp,1)
                    .set(ContractPlanningSplit::getTotalUsedAmount,total)
                    .eq(ContractPlanningSplit::getId,contractSplitId));
        });
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
