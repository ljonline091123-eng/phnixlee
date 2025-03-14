package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.procurement.domain.ContractPlanning;
import com.zhaocai.business.procurement.domain.ContractPlanningSplit;
import com.zhaocai.business.procurement.mapper.ContractPlanningMapper;
import com.zhaocai.business.procurement.service.IContractPlanningService;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.vo.req.ContractPlanningListQueryVO;
import com.zhaocai.business.procurement.vo.res.ContractPlanningListVO;
import com.zhaocai.business.procurement.vo.res.ProcurementContractPlanListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 合约规划Service业务层处理
 *
 * @author WH
 * @date 2024-06-17
 */
@Slf4j
@Service
public class ContractPlanningServiceImpl extends ServiceImpl<ContractPlanningMapper, ContractPlanning> implements IContractPlanningService {

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    @Autowired
    private ContractPlanService contractPlanService;

    @Override
    public void addContractPlanning(ContractPlanning contractPlanning, Long planId) {
        //plannedAmountInclTaxText: item.plannedAmountInclTaxText, // 用户填写的计划金额
        //                bidResponsibleOrgName:
        contractPlanning.setPlanId(planId);
        if(contractPlanning.getContractPlanningName() != null ){
            contractPlanning.setContractPlanningName(contractPlanning.getContractPlanningName());
        }
        if(contractPlanning.getPlannedAmountInclTax() != null ){
            contractPlanning.setPlannedAmountInclTax(contractPlanning.getPlannedAmountInclTax());
        }


        super.save(contractPlanning);
    }


    @Override
    public List<ProcurementContractPlanListVO> listProcurementContractPlanByContractSplit(List<Long> contractSplitIds) {
        /* 合约规划拆分对象 */
        List<ContractPlanningSplit> planningSplits = contractPlanningSplitService.listByIds(contractSplitIds);
        /* 采购计划 id */
        List<Long> planIdList = planningSplits.stream()
                .map(ContractPlanningSplit::getProcurementPlanId)
                .distinct().collect(Collectors.toList());

        return baseMapper.selectProcurementContractPlanningList(planIdList);
    }

    @Override
    public ContractPlanning getByContractPlanningId(String contractPlanningId) {
        return super.getOne(new LambdaQueryWrapper<ContractPlanning>()
                .eq(ContractPlanning::getContractPlanningId, contractPlanningId));
    }

    @Override
    public ContractPlanningListVO getByProcurementIdFromUnderling(Long procurementPlanId) {
        ContractPlanning contractPlanning = super.getOne(new LambdaQueryWrapper<ContractPlanning>()
                .eq(ContractPlanning::getPlanId, procurementPlanId));

        // 去商务策划查询最新的合约规划数据
        ContractPlanningListQueryVO queryVO = new ContractPlanningListQueryVO();
        queryVO.setContractName(contractPlanning.getContractPlanningName());
        queryVO.setContractType(contractPlanning.getContractPlanningCategory());
        queryVO.setProjectId(contractPlanning.getProjectId());
        queryVO.setPageSize(100);

//        ContractPlanningListVO contractPlanningList = getContractPlanningFromList(queryVO, contractPlanning.getContractPlanningId());
//        contractPlanningList.setProjectCode(contractPlanning.getProjectCode());
//        contractPlanningList.setProjectName(contractPlanning.getProjectName());
        ContractPlanningListVO contractPlanningList = BeanCopierUtil.copyBean(contractPlanning,ContractPlanningListVO.class );
        return contractPlanningList;
    }

    @Override
    public ContractPlanning getByContractSplitId(Long splitId) {
        return baseMapper.selectByContractSplitId(splitId);
    }

    @Override
    public ContractPlanning getByProcurementId(Long procurementPlanId) {
        return super.getOne(new LambdaQueryWrapper<ContractPlanning>()
                .eq(ContractPlanning::getPlanId, procurementPlanId));
    }

    @Override
    public void updateContractPlanning(ContractPlanning contractPlanning, Long planId) {
        baseMapper.deleteByPlanId(planId);

        this.addContractPlanning(contractPlanning, planId);
    }

    @Override
    public List<ProcurementContractPlanListVO> listByPlanIds(List<Long> planIdList) {
        return baseMapper.selectProcurementContractPlanningList(planIdList);
    }

    /**
     * 从
     *
     * @param queryVO
     */
    private ContractPlanningListVO getContractPlanningFromList(ContractPlanningListQueryVO queryVO, String contractPlanningId) {
        PageResult<ContractPlanningListVO> pageResult = contractPlanService.getContractPlanningList(queryVO, false);
        if (CollectionUtil.isNotEmpty(pageResult.getRows())) {
            for (ContractPlanningListVO planningListVO : pageResult.getRows()) {
                if (planningListVO.getContractPlanningId().equals(contractPlanningId)) {
                    return planningListVO;
                }
            }
            log.warn("第[{}]页无[{}]的合约规划", String.valueOf(queryVO.getPageNumber()), queryVO.getContractName());
            queryVO.setPageNumber(queryVO.getPageNumber() + 1);
            return getContractPlanningFromList(queryVO, contractPlanningId);
        }
        return new ContractPlanningListVO();
    }

}
